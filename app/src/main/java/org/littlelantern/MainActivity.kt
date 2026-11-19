package org.littlelantern

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.littlelantern.data.Prefs
import org.littlelantern.feedback.Feedback
import org.littlelantern.feedback.FeedbackFormat
import org.littlelantern.feedback.FeedbackKind
import org.littlelantern.gate.ParentGate
import org.littlelantern.gate.PressSequenceDetector
import org.littlelantern.lock.LockController
import org.littlelantern.lock.LockState
import org.littlelantern.session.AfterVideo
import org.littlelantern.session.SessionRules
import org.littlelantern.ui.ChildHome
import org.littlelantern.ui.ClosingScreen
import org.littlelantern.ui.FeedbackHost
import org.littlelantern.ui.FeedbackScreen
import org.littlelantern.ui.LittleLanternTheme
import org.littlelantern.ui.ParentHome
import org.littlelantern.ui.ParentStatus
import org.littlelantern.ui.ParentVideosScreen
import org.littlelantern.ui.PauseScreen
import org.littlelantern.ui.PickerItem
import org.littlelantern.ui.PinEntryScreen
import org.littlelantern.ui.PinSetupScreen
import org.littlelantern.ui.PlayerScreen
import org.littlelantern.ui.RestingScreen
import org.littlelantern.ui.VideoPicker
import org.littlelantern.ui.games.GameDoneScreen
import org.littlelantern.ui.games.GameKind
import org.littlelantern.ui.games.GameScreen
import org.littlelantern.ui.LoadingScreen
import org.littlelantern.video.ResolvedVideo
import org.littlelantern.video.VideoEntry
import org.littlelantern.video.VideoStatus
import java.time.LocalDate
import java.time.ZoneId

enum class Mode { SETUP, CHILD, PIN, PARENT, PARENT_VIDEOS, PARENT_FEEDBACK }

sealed interface ChildScreen {
    data object Home : ChildScreen
    data object Picker : ChildScreen
    data class Loading(val video: VideoEntry) : ChildScreen
    data class Playing(val video: VideoEntry, val streams: ResolvedVideo) : ChildScreen
    data class Paused(val video: VideoEntry) : ChildScreen
    data class Game(val kind: GameKind, val startedAt: Long) : ChildScreen
    data class GameDone(val kind: GameKind) : ChildScreen
    data object Closing : ChildScreen
    data object Resting : ChildScreen
}

/**
 * Single activity. Boots into Child mode (or PIN setup on first run).
 * Parent mode is reached only by: volume key x5 within 3 s -> fingerprint (or PIN fallback).
 * Recreating the activity (rotation, process death) always lands back in Child mode, the safe default.
 */
class MainActivity : FragmentActivity() {

    private lateinit var prefs: Prefs
    private lateinit var lock: LockController
    private lateinit var gate: ParentGate
    private val app by lazy { application as LittleLanternApp }
    private val repo by lazy { app.videos }
    private val volumeSequence = PressSequenceDetector(requiredPresses = 5, windowMillis = 3_000)

    private var mode by mutableStateOf(Mode.CHILD)
    private var child by mutableStateOf<ChildScreen>(ChildScreen.Home)
    private var lockState by mutableStateOf(LockState.NONE)
    /** Bumped whenever parent settings or usage change, so parent screens re-read them. */
    private var settingsVersion by mutableIntStateOf(0)
    private var feedbackItems by mutableStateOf(emptyList<Feedback>())
    private var feedbackSending by mutableStateOf(false)
    private var feedbackMessage by mutableStateOf<String?>(null)
    private var showFeedback by mutableStateOf(true)
    private var sessionWatchedMs = 0L
    private var lastInteraction = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        lock = LockController(this)
        gate = ParentGate(this, onUnlocked = ::enterParentMode, onNeedPin = { mode = Mode.PIN })

        showFeedback = prefs.showFeedbackButton
        feedbackItems = app.feedback.list()
        lock.configure()
        mode = if (prefs.hasPin) Mode.CHILD else Mode.SETUP
        child = if (isResting()) ChildScreen.Resting else ChildScreen.Home

        // Back never leaves the app. In parent screens it steps back; in Child mode it does nothing.
        onBackPressedDispatcher.addCallback(this) {
            when (mode) {
                Mode.PARENT_VIDEOS, Mode.PARENT_FEEDBACK -> mode = Mode.PARENT
                Mode.PIN, Mode.PARENT -> enterChildMode()
                else -> Unit
            }
        }

        setContent {
            LittleLanternTheme {
                val videos by repo.videos.collectAsState()
                FeedbackHost(enabled = showFeedback, screenName = screenName(), onSave = ::saveFeedback) {
                when (mode) {
                    Mode.SETUP -> PinSetupScreen(onPinChosen = { pin ->
                        prefs.setPin(pin)
                        enterChildMode()
                    })

                    Mode.CHILD -> ChildFlow(videos.filter { it.status == VideoStatus.APPROVED })

                    Mode.PIN -> PinEntryScreen(
                        checkPin = prefs::checkPin,
                        onSuccess = ::enterParentMode,
                        onCancel = ::enterChildMode,
                    )

                    Mode.PARENT -> {
                        val status = remember(settingsVersion, videos, lockState) {
                            ParentStatus(
                                isDeviceOwner = lock.isDeviceOwner,
                                lock = lockState,
                                todayMinutes = (prefs.todayWatchedMs / 60_000).toInt(),
                                dailyLimitMin = prefs.dailyLimitMin,
                                sessionMin = prefs.sessionMin,
                                restMin = prefs.restMin,
                                isResting = isResting(),
                                voluntaryStops = prefs.voluntaryStops,
                                toReview = videos.count { it.status == VideoStatus.REVIEW },
                                approved = videos.count { it.status == VideoStatus.APPROVED },
                                unsentFeedback = feedbackItems.count { !it.sent },
                            )
                        }
                        ParentHome(
                            status = status,
                            onOpenLockSettings = ::openLockSettings,
                            onOpenVideos = { mode = Mode.PARENT_VIDEOS },
                            onOpenFeedback = { feedbackMessage = null; mode = Mode.PARENT_FEEDBACK },
                            onCycleDailyLimit = { prefs.dailyLimitMin = next(prefs.dailyLimitMin, listOf(30, 45, 60, 90)); settingsVersion++ },
                            onCycleSession = { prefs.sessionMin = next(prefs.sessionMin, listOf(10, 15, 20, 30)); settingsVersion++ },
                            onCycleRest = { prefs.restMin = next(prefs.restMin, listOf(30, 60, 120, 180)); settingsVersion++ },
                            onWake = ::wakeLantern,
                            onBackToChild = ::enterChildMode,
                            onPauseLock = ::pauseLock,
                            onReleaseDeviceOwner = ::releaseDeviceOwner,
                            lastInteraction = { lastInteraction },
                        )
                    }

                    Mode.PARENT_VIDEOS -> {
                        val progress by repo.progress.collectAsState()
                        val message by repo.message.collectAsState()
                        ParentVideosScreen(
                            videos = videos,
                            progress = progress,
                            message = message,
                            onAddLink = repo::addLink,
                            onApprove = repo::approve,
                            onReject = repo::reject,
                            onRetry = repo::retry,
                            onMarkFantastical = repo::setParentFantastical,
                            onBack = { repo.clearMessage(); mode = Mode.PARENT },
                        )
                    }

                    Mode.PARENT_FEEDBACK -> FeedbackScreen(
                        items = feedbackItems,
                        repo = prefs.githubRepo,
                        tokenSet = prefs.githubToken.isNotEmpty(),
                        showButton = showFeedback,
                        sending = feedbackSending,
                        message = feedbackMessage,
                        onSaveSettings = { repoName, token ->
                            prefs.githubRepo = repoName
                            if (token != null) prefs.githubToken = token
                            feedbackMessage = "GitHub settings saved."
                            settingsVersion++
                        },
                        onToggleButton = { on -> showFeedback = on; prefs.showFeedbackButton = on },
                        onSendAll = ::sendFeedbackToGitHub,
                        onCopyAll = ::copyFeedback,
                        onDelete = { id -> app.feedback.delete(id); feedbackItems = app.feedback.list() },
                        onBack = { mode = Mode.PARENT },
                    )
                }
                }
            }
        }
    }

    // --- Feedback ---

    private fun screenName(): String = when (mode) {
        Mode.SETUP -> "PIN setup"
        Mode.PIN -> "Parent PIN"
        Mode.PARENT -> "Parent home"
        Mode.PARENT_VIDEOS -> "Parent videos"
        Mode.PARENT_FEEDBACK -> "Parent feedback"
        Mode.CHILD -> "Child: " + when (child) {
            ChildScreen.Home -> "Home"
            ChildScreen.Picker -> "Video picker"
            is ChildScreen.Loading -> "Video loading"
            is ChildScreen.Playing -> "Video playing"
            is ChildScreen.Game -> "Game: " + (child as ChildScreen.Game).kind.title
            is ChildScreen.GameDone -> "Game done"
            is ChildScreen.Paused -> "Pause"
            ChildScreen.Closing -> "Closing"
            ChildScreen.Resting -> "Resting"
        }
    }

    private fun saveFeedback(screen: String, kind: FeedbackKind, text: String) {
        app.feedback.add(screen, kind, text)
        feedbackItems = app.feedback.list()
        Toast.makeText(this, "Feedback saved. Send it from Parent mode.", Toast.LENGTH_SHORT).show()
    }

    private val appVersion: String
        get() = runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull() ?: "unknown"

    private val deviceInfo: String
        get() = "${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE}"

    private fun sendFeedbackToGitHub() {
        val repoName = prefs.githubRepo
        val token = prefs.githubToken
        val pending = feedbackItems.filter { !it.sent }
        if (pending.isEmpty() || token.isEmpty()) return
        feedbackSending = true
        feedbackMessage = null
        lifecycleScope.launch {
            var sent = 0
            var error: String? = null
            withContext(Dispatchers.IO) {
                for (f in pending) {
                    try {
                        val url = app.github.create(
                            repoName,
                            token,
                            FeedbackFormat.title(f),
                            FeedbackFormat.body(f, appVersion, deviceInfo),
                            FeedbackFormat.labels(f),
                        )
                        app.feedback.markSent(f.id, url)
                        sent++
                    } catch (e: Exception) {
                        error = e.message ?: e.javaClass.simpleName
                        break
                    }
                }
            }
            feedbackItems = app.feedback.list()
            feedbackSending = false
            feedbackMessage = if (error == null) "Sent $sent to GitHub." else "Sent $sent. Stopped: $error"
        }
    }

    private fun copyFeedback() {
        val text = FeedbackFormat.asText(feedbackItems.filter { !it.sent }, appVersion, deviceInfo)
        getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Little Lantern feedback", text))
        feedbackMessage = "Copied. Paste it into a GitHub issue or a message."
    }

    @androidx.compose.runtime.Composable
    private fun ChildFlow(approved: List<VideoEntry>) {
        val settings = prefs.sessionSettings()
        when (val screen = child) {
            ChildScreen.Home -> ChildHome(
                hasVideos = approved.isNotEmpty(),
                onOpenVideos = ::startVideoSession,
                onOpenGame = ::startGame,
            )

            ChildScreen.Picker -> {
                val weekIndex = LocalDate.now().toEpochDay() / 7
                val choices = SessionRules.choicesForWeek(approved, settings.choices, weekIndex)
                VideoPicker(
                    items = choices.map { PickerItem(it.id, it.title, repo.library.thumbFile(it.id)) },
                    onPick = { id -> approved.firstOrNull { it.id == id }?.let(::loadVideo) },
                    onBack = { child = ChildScreen.Home },
                )
            }

            is ChildScreen.Loading -> LoadingScreen()

            is ChildScreen.Playing -> PlayerScreen(
                videoUrl = screen.streams.videoUrl,
                audioUrl = screen.streams.audioUrl,
                fallbackDurationMs = screen.video.durationMs,
                saturation = settings.saturation,
                volume = settings.volume,
                onFinished = { watched -> onVideoFinished(screen.video, watched) },
                onError = ::onPlaybackError,
            )

            is ChildScreen.Game -> GameScreen(
                kind = screen.kind,
                onFinished = { endGame(screen, done = true) },
                onBack = { endGame(screen, done = false) },
            )

            is ChildScreen.GameDone -> GameDoneScreen(screen.kind, onHome = ::goHomeOrRest)

            is ChildScreen.Paused -> PauseScreen(
                thumb = repo.library.thumbFile(screen.video.id),
                pauseMs = settings.pauseMs,
                onOneMore = { child = ChildScreen.Picker },
                onAllDone = {
                    prefs.voluntaryStops = prefs.voluntaryStops + 1
                    endSession(AfterVideo.END_SESSION)
                },
            )

            ChildScreen.Closing -> ClosingScreen(onDone = { child = ChildScreen.Resting })

            ChildScreen.Resting -> RestingScreen(
                canWake = { !isResting() },
                onWake = { child = ChildScreen.Home },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (mode == Mode.CHILD) startLockIfPossible()
        lockState = lock.state()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val isVolumeKey = event.keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
            event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
        if (isVolumeKey && mode == Mode.CHILD) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0 &&
                volumeSequence.onPress(event.eventTime)
            ) {
                gate.open()
            }
            return true // Child cannot change the volume.
        }
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        lastInteraction = SystemClock.uptimeMillis()
        return super.dispatchTouchEvent(ev)
    }

    // --- Child session ---

    private fun isResting(): Boolean =
        !SessionRules.canStartSession(prefs.todayWatchedMs, System.currentTimeMillis(), prefs.restUntil, prefs.sessionSettings())

    private fun startVideoSession() {
        if (isResting()) {
            child = ChildScreen.Resting
            return
        }
        sessionWatchedMs = 0
        child = ChildScreen.Picker
    }

    private fun onVideoFinished(video: VideoEntry, watchedMs: Long) {
        prefs.addWatched(watchedMs)
        sessionWatchedMs += watchedMs
        when (val next = SessionRules.afterVideo(sessionWatchedMs, prefs.todayWatchedMs, prefs.sessionSettings())) {
            AfterVideo.PAUSE -> child = ChildScreen.Paused(video)
            else -> endSession(next)
        }
    }

    private fun endSession(kind: AfterVideo) {
        val startOfNextDay = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        prefs.restUntil = SessionRules.restUntil(kind, System.currentTimeMillis(), startOfNextDay, prefs.sessionSettings())
        sessionWatchedMs = 0
        child = ChildScreen.Closing
    }

    private fun wakeLantern() {
        prefs.restUntil = 0
        settingsVersion++
        if (!isResting()) child = ChildScreen.Home
    }

    // --- Modes ---

    private fun enterChildMode() {
        mode = Mode.CHILD
        if (child == ChildScreen.Resting && !isResting()) child = ChildScreen.Home
        startLockIfPossible()
        lockState = lock.state()
    }

    private fun enterParentMode() {
        lastInteraction = SystemClock.uptimeMillis()
        lockState = lock.state()
        settingsVersion++
        mode = Mode.PARENT
    }

    /**
     * Device owner: full lock. Otherwise Android App pinning (the phone may ask to confirm).
     * Re-applied every time Child mode is shown, so after a restart the app pins itself again.
     */
    private fun startLockIfPossible() {
        if (!lock.isLocked()) runCatching { startLockTask() }
    }

    private fun openLockSettings() {
        if (lock.isLocked()) runCatching { stopLockTask() }
        lockState = lock.state()
        runCatching { startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS)) }
            .onFailure { runCatching { startActivity(Intent(Settings.ACTION_SETTINGS)) } }
    }

    // --- Games ---

    private fun startGame(kind: GameKind) {
        if (isResting()) {
            child = ChildScreen.Resting
            return
        }
        child = ChildScreen.Game(kind, System.currentTimeMillis())
    }

    /** Game time counts toward the daily limit. */
    private fun endGame(game: ChildScreen.Game, done: Boolean) {
        prefs.addWatched((System.currentTimeMillis() - game.startedAt).coerceAtLeast(0))
        if (done) child = ChildScreen.GameDone(game.kind) else goHomeOrRest()
    }

    private fun goHomeOrRest() {
        child = if (isResting()) ChildScreen.Resting else ChildScreen.Home
    }

    // --- Video streaming ---

    private fun loadVideo(video: VideoEntry) {
        val loading = ChildScreen.Loading(video)
        child = loading
        lifecycleScope.launch {
            val streams = runCatching { repo.resolveForPlayback(video) }.getOrNull()
            if (child != loading) return@launch
            if (streams != null) {
                child = ChildScreen.Playing(video, streams)
            } else {
                onPlaybackError()
            }
        }
    }

    private fun onPlaybackError() {
        child = ChildScreen.Picker
        Toast.makeText(this, "Can't play right now. Is Wi-Fi on?", Toast.LENGTH_LONG).show()
    }

    private fun pauseLock() {
        if (lock.isLocked()) stopLockTask()
        lockState = lock.state()
        Toast.makeText(this, "Lock paused until Child mode", Toast.LENGTH_SHORT).show()
    }

    private fun releaseDeviceOwner() {
        if (lock.isLocked()) stopLockTask()
        lock.releaseDeviceOwner()
        lockState = lock.state()
        Toast.makeText(this, "Device owner removed. You can uninstall now.", Toast.LENGTH_LONG).show()
    }

    private fun next(current: Int, options: List<Int>): Int =
        options[(options.indexOf(current) + 1).mod(options.size)]
}
