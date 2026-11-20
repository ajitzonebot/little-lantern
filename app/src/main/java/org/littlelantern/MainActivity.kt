package org.littlelantern

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.littlelantern.data.Prefs
import org.littlelantern.data.Video
import org.littlelantern.data.VideoStore
import org.littlelantern.games.Catalog
import org.littlelantern.games.GamesMenu
import org.littlelantern.games.LocalColourBlind
import org.littlelantern.games.LocalSpeaker
import org.littlelantern.games.TtsSpeaker
import org.littlelantern.gate.ParentGate
import org.littlelantern.gate.PressSequenceDetector
import org.littlelantern.lock.LockController
import org.littlelantern.session.SessionMath
import org.littlelantern.ui.AddVideoScreen
import org.littlelantern.ui.ChildHome
import org.littlelantern.ui.EndScreen
import org.littlelantern.ui.LittleLanternTheme
import org.littlelantern.ui.ParentActions
import org.littlelantern.ui.ParentHome
import org.littlelantern.ui.ParentSettings
import org.littlelantern.ui.PinEntryScreen
import org.littlelantern.ui.PinSetupScreen
import org.littlelantern.ui.PlayerScreen

enum class Mode { SETUP, CHILD, PIN, PARENT }

sealed interface ChildScreen {
    data object Home : ChildScreen
    data class Watch(val videoId: String) : ChildScreen
    data object Games : ChildScreen
    data class Game(val id: String) : ChildScreen
    data object Ended : ChildScreen
}

/**
 * Single activity. Boots into Child mode (or PIN setup on first run).
 * Parent mode is reached only by: volume key x5 within 3 s -> fingerprint (or PIN fallback).
 * Child mode is limited to 60 minutes a day and calms down (colour, brightness, volume, choices) from minute 15.
 */
class MainActivity : FragmentActivity() {

    private lateinit var prefs: Prefs
    private lateinit var videoStore: VideoStore
    private lateinit var lock: LockController
    private lateinit var gate: ParentGate
    private lateinit var speaker: TtsSpeaker
    private val volumeSequence = PressSequenceDetector(requiredPresses = 5, windowMillis = 3_000)

    private var mode by mutableStateOf(Mode.CHILD)
    private var child by mutableStateOf<ChildScreen>(ChildScreen.Home)
    private var addingVideo by mutableStateOf(false)
    private var videos by mutableStateOf(emptyList<Video>())
    private var usedSeconds by mutableIntStateOf(0)
    private var settingsVersion by mutableIntStateOf(0)
    private var isLocked by mutableStateOf(false)
    private var lastInteraction = 0L
    private var appliedLevel = -1f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        videoStore = VideoStore(this)
        lock = LockController(this)
        speaker = TtsSpeaker(this)
        gate = ParentGate(this, onUnlocked = ::enterParentMode, onNeedPin = { mode = Mode.PIN })

        lock.configure()
        videos = videoStore.all()
        usedSeconds = prefs.usedSecondsToday()
        mode = if (prefs.hasPin) Mode.CHILD else Mode.SETUP
        if (SessionMath.isOver(usedSeconds)) child = ChildScreen.Ended

        // Back never leaves the app; in parent screens it returns to Child mode.
        onBackPressedDispatcher.addCallback(this) {
            when {
                mode == Mode.PARENT && addingVideo -> addingVideo = false
                mode == Mode.PIN || mode == Mode.PARENT -> enterChildMode()
            }
        }

        // The daily clock: counts Child mode time while the app is on screen.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    delay(1_000)
                    tick()
                }
            }
        }

        setContent {
            LittleLanternTheme {
                CompositionLocalProvider(
                    LocalSpeaker provides speaker,
                    LocalColourBlind provides prefs.colourBlindMode.also { settingsVersion },
                ) {
                    when (mode) {
                        Mode.SETUP -> PinSetupScreen(onPinChosen = { pin ->
                            prefs.setPin(pin)
                            enterChildMode()
                        })

                        Mode.CHILD -> ChildContent()

                        Mode.PIN -> PinEntryScreen(
                            checkPin = prefs::checkPin,
                            onSuccess = ::enterParentMode,
                            onCancel = ::enterChildMode,
                        )

                        Mode.PARENT -> ParentContent()
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun ChildContent() {
        val level = SessionMath.calmLevel(usedSeconds)
        when (val screen = child) {
            ChildScreen.Home -> {
                val limit = SessionMath.choicesShown(level)
                val shown = if (limit == null) videos else videos.take(limit)
                ChildHome(
                    videos = shown,
                    canStart = { SessionMath.fits(it.durationSeconds, usedSeconds) },
                    onWatch = { child = ChildScreen.Watch(it.id) },
                    onGames = { child = ChildScreen.Games },
                )
            }
            is ChildScreen.Watch -> {
                val video = videos.firstOrNull { it.id == screen.videoId }
                if (video == null) {
                    child = ChildScreen.Home
                } else {
                    PlayerScreen(
                        video = video,
                        volume = prefs.volumeCap * SessionMath.volume(level),
                        onHome = { child = ChildScreen.Home },
                    )
                }
            }
            ChildScreen.Games -> GamesMenu(
                games = Catalog.visible(ageBand(), prefs.disabledGames.also { settingsVersion }, prefs.colourBlindMode),
                onPlay = { child = ChildScreen.Game(it.id) },
                onHome = { child = ChildScreen.Home },
            )
            is ChildScreen.Game -> {
                val game = Catalog.byId(screen.id)
                if (game == null) child = ChildScreen.Games else game.play { child = ChildScreen.Games }
            }
            ChildScreen.Ended -> EndScreen()
        }
    }

    @androidx.compose.runtime.Composable
    private fun ParentContent() {
        @Suppress("UNUSED_VARIABLE") val v = settingsVersion
        if (addingVideo) {
            AddVideoScreen(
                apiKey = prefs.youtubeApiKey,
                onApprove = { video ->
                    videoStore.add(video)
                    videos = videoStore.all()
                    addingVideo = false
                },
                onClose = { addingVideo = false },
            )
            return
        }
        ParentHome(
            isDeviceOwner = lock.isDeviceOwner,
            isLocked = isLocked,
            videos = videos,
            games = Catalog.forParent(ageBand()),
            settings = ParentSettings(
                birthMonth = prefs.birthMonth,
                volumeCap = prefs.volumeCap,
                colourBlind = prefs.colourBlindMode,
                apiKey = prefs.youtubeApiKey,
                disabledGames = prefs.disabledGames,
                ageBand = ageBand(),
                usedMinutesToday = usedSeconds / 60,
            ),
            actions = ParentActions(
                onBackToChild = ::enterChildMode,
                onAddVideo = { addingVideo = true },
                onRemoveVideo = { id -> videoStore.remove(id); videos = videoStore.all() },
                onBirthMonth = { prefs.birthMonth = it; settingsVersion++ },
                onVolumeCap = { prefs.volumeCap = it; settingsVersion++ },
                onColourBlind = { prefs.colourBlindMode = it; settingsVersion++ },
                onApiKey = { prefs.youtubeApiKey = it },
                onToggleGame = { id, on ->
                    prefs.disabledGames = if (on) prefs.disabledGames - id else prefs.disabledGames + id
                    settingsVersion++
                },
                onPauseLock = ::pauseLock,
                onReleaseDeviceOwner = ::releaseDeviceOwner,
            ),
            lastInteraction = { lastInteraction },
        )
    }

    private fun ageBand(): Int = SessionMath.ageBand(prefs.birthMonth)

    private fun tick() {
        if (mode != Mode.CHILD) return
        if (child == ChildScreen.Ended) {
            // A new day starts fresh.
            usedSeconds = prefs.usedSecondsToday()
            if (!SessionMath.isOver(usedSeconds)) child = ChildScreen.Home
            return
        }
        usedSeconds = prefs.addUsage(1)
        if (SessionMath.isOver(usedSeconds)) child = ChildScreen.Ended
        applyCalm(SessionMath.calmLevel(usedSeconds))
    }

    /** Dims the real screen brightness and drains colour (Android 12+) as the session goes on. */
    private fun applyCalm(level: Float) {
        val rounded = (level * 50).toInt() / 50f
        if (rounded == appliedLevel) return
        appliedLevel = rounded
        val attrs = window.attributes
        attrs.screenBrightness = if (rounded <= 0f) {
            WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        } else {
            val system = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128) / 255f
            (system.coerceIn(0.05f, 1f) * SessionMath.brightness(rounded)).coerceIn(0.03f, 1f)
        }
        window.attributes = attrs
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.decorView.setRenderEffect(
                if (rounded <= 0f) {
                    null
                } else {
                    RenderEffect.createColorFilterEffect(
                        ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(SessionMath.saturation(rounded)) }),
                    )
                },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (mode == Mode.CHILD) {
            startLockIfPossible()
            usedSeconds = prefs.usedSecondsToday()
            appliedLevel = -1f
            applyCalm(SessionMath.calmLevel(usedSeconds))
        }
        isLocked = lock.isLocked()
    }

    override fun onDestroy() {
        speaker.shutdown()
        super.onDestroy()
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

    private fun enterChildMode() {
        mode = Mode.CHILD
        addingVideo = false
        usedSeconds = prefs.usedSecondsToday()
        child = if (SessionMath.isOver(usedSeconds)) ChildScreen.Ended else ChildScreen.Home
        appliedLevel = -1f
        applyCalm(SessionMath.calmLevel(usedSeconds))
        startLockIfPossible()
        isLocked = lock.isLocked()
    }

    private fun enterParentMode() {
        lastInteraction = SystemClock.uptimeMillis()
        isLocked = lock.isLocked()
        appliedLevel = -1f
        applyCalm(0f)
        mode = Mode.PARENT
    }

    private fun startLockIfPossible() {
        if (lock.isDeviceOwner && !lock.isLocked()) startLockTask()
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
}
