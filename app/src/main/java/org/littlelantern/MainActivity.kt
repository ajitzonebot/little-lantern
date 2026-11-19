package org.littlelantern

import android.os.Bundle
import android.os.SystemClock
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
import org.littlelantern.data.Prefs
import org.littlelantern.data.installedLaunchableApps
import org.littlelantern.gate.ParentGate
import org.littlelantern.gate.PressSequenceDetector
import org.littlelantern.lock.LockController
import org.littlelantern.session.AfterVideo
import org.littlelantern.session.SessionRules
import org.littlelantern.ui.ChildHome
import org.littlelantern.ui.ClosingScreen
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
import org.littlelantern.video.VideoEntry
import org.littlelantern.video.VideoStatus
import java.time.LocalDate
import java.time.ZoneId

enum class Mode { SETUP, CHILD, PIN, PARENT, PARENT_VIDEOS }

sealed interface ChildScreen {
    data object Home : ChildScreen
    data object Picker : ChildScreen
    data class Playing(val video: VideoEntry) : ChildScreen
    data class Paused(val video: VideoEntry) : ChildScreen
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
    private val repo by lazy { (application as LittleLanternApp).videos }
    private val volumeSequence = PressSequenceDetector(requiredPresses = 5, windowMillis = 3_000)

    private var mode by mutableStateOf(Mode.CHILD)
    private var child by mutableStateOf<ChildScreen>(ChildScreen.Home)
    private var allowedGames by mutableStateOf(emptySet<String>())
    private var isLocked by mutableStateOf(false)
    /** Bumped whenever parent settings or usage change, so parent screens re-read them. */
    private var settingsVersion by mutableIntStateOf(0)
    private var sessionWatchedMs = 0L
    private var lastInteraction = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        lock = LockController(this)
        gate = ParentGate(this, onUnlocked = ::enterParentMode, onNeedPin = { mode = Mode.PIN })

        allowedGames = prefs.allowedGames
        lock.configure(allowedGames)
        mode = if (prefs.hasPin) Mode.CHILD else Mode.SETUP
        child = if (isResting()) ChildScreen.Resting else ChildScreen.Home

        // Back never leaves the app. In parent screens it steps back; in Child mode it does nothing.
        onBackPressedDispatcher.addCallback(this) {
            when (mode) {
                Mode.PARENT_VIDEOS -> mode = Mode.PARENT
                Mode.PIN, Mode.PARENT -> enterChildMode()
                else -> Unit
            }
        }

        setContent {
            LittleLanternTheme {
                val videos by repo.videos.collectAsState()
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
                        val apps = remember { installedLaunchableApps(this@MainActivity) }
                        val status = remember(settingsVersion, videos, isLocked) {
                            ParentStatus(
                                isDeviceOwner = lock.isDeviceOwner,
                                isLocked = isLocked,
                                todayMinutes = (prefs.todayWatchedMs / 60_000).toInt(),
                                dailyLimitMin = prefs.dailyLimitMin,
                                sessionMin = prefs.sessionMin,
                                restMin = prefs.restMin,
                                isResting = isResting(),
                                voluntaryStops = prefs.voluntaryStops,
                                toReview = videos.count { it.status == VideoStatus.REVIEW },
                                approved = videos.count { it.status == VideoStatus.APPROVED },
                            )
                        }
                        ParentHome(
                            status = status,
                            apps = apps,
                            allowedGames = allowedGames,
                            onToggleGame = ::setGameAllowed,
                            onOpenVideos = { mode = Mode.PARENT_VIDEOS },
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
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun ChildFlow(approved: List<VideoEntry>) {
        val settings = prefs.sessionSettings()
        when (val screen = child) {
            ChildScreen.Home -> {
                val games = remember(allowedGames) {
                    installedLaunchableApps(this).filter { it.packageName in allowedGames }
                }
                ChildHome(
                    games = games,
                    hasVideos = approved.isNotEmpty(),
                    onOpenVideos = ::startVideoSession,
                    onOpenGame = ::launchGame,
                )
            }

            ChildScreen.Picker -> {
                val weekIndex = LocalDate.now().toEpochDay() / 7
                val choices = SessionRules.choicesForWeek(approved, settings.choices, weekIndex)
                VideoPicker(
                    items = choices.map { PickerItem(it.id, it.title, repo.library.thumbFile(it.id)) },
                    onPick = { id -> approved.firstOrNull { it.id == id }?.let { child = ChildScreen.Playing(it) } },
                    onBack = { child = ChildScreen.Home },
                )
            }

            is ChildScreen.Playing -> PlayerScreen(
                videoFile = repo.library.videoFile(screen.video.id),
                audioFile = if (screen.video.hasSeparateAudio) repo.library.audioFile(screen.video.id) else null,
                fallbackDurationMs = screen.video.durationMs,
                saturation = settings.saturation,
                volume = settings.volume,
                onFinished = { watched -> onVideoFinished(screen.video, watched) },
            )

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
        isLocked = lock.isLocked()
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
        isLocked = lock.isLocked()
    }

    private fun enterParentMode() {
        lastInteraction = SystemClock.uptimeMillis()
        isLocked = lock.isLocked()
        settingsVersion++
        mode = Mode.PARENT
    }

    private fun startLockIfPossible() {
        if (lock.isDeviceOwner && !lock.isLocked()) startLockTask()
    }

    private fun launchGame(packageName: String) {
        if (isResting()) {
            child = ChildScreen.Resting
            return
        }
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: return
        startActivity(intent)
    }

    private fun setGameAllowed(packageName: String, allowed: Boolean) {
        allowedGames = if (allowed) allowedGames + packageName else allowedGames - packageName
        prefs.allowedGames = allowedGames
        lock.configure(allowedGames)
    }

    private fun pauseLock() {
        if (lock.isLocked()) stopLockTask()
        isLocked = false
        Toast.makeText(this, "Lock paused until Child mode", Toast.LENGTH_SHORT).show()
    }

    private fun releaseDeviceOwner() {
        if (lock.isLocked()) stopLockTask()
        lock.releaseDeviceOwner()
        isLocked = false
        Toast.makeText(this, "Device owner removed. You can uninstall now.", Toast.LENGTH_LONG).show()
    }

    private fun next(current: Int, options: List<Int>): Int =
        options[(options.indexOf(current) + 1).mod(options.size)]
}
