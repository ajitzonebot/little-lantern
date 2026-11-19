package org.littlelantern

import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import org.littlelantern.data.Prefs
import org.littlelantern.data.installedLaunchableApps
import org.littlelantern.gate.ParentGate
import org.littlelantern.gate.PressSequenceDetector
import org.littlelantern.lock.LockController
import org.littlelantern.ui.ChildHome
import org.littlelantern.ui.LittleLanternTheme
import org.littlelantern.ui.ParentHome
import org.littlelantern.ui.PinEntryScreen
import org.littlelantern.ui.PinSetupScreen

enum class Mode { SETUP, CHILD, PIN, PARENT }

/**
 * Single activity. Boots into Child mode (or PIN setup on first run).
 * Parent mode is reached only by: volume key x5 within 3 s -> fingerprint (or PIN fallback).
 * Recreating the activity (rotation, process death) always lands back in Child mode, the safe default.
 */
class MainActivity : FragmentActivity() {

    private lateinit var prefs: Prefs
    private lateinit var lock: LockController
    private lateinit var gate: ParentGate
    private val volumeSequence = PressSequenceDetector(requiredPresses = 5, windowMillis = 3_000)

    private var mode by mutableStateOf(Mode.CHILD)
    private var allowedGames by mutableStateOf(emptySet<String>())
    private var isLocked by mutableStateOf(false)
    private var lastInteraction = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        lock = LockController(this)
        gate = ParentGate(this, onUnlocked = ::enterParentMode, onNeedPin = { mode = Mode.PIN })

        allowedGames = prefs.allowedGames
        lock.configure(allowedGames)
        mode = if (prefs.hasPin) Mode.CHILD else Mode.SETUP

        // Back never leaves the app; in parent screens it returns to Child mode.
        onBackPressedDispatcher.addCallback(this) {
            if (mode == Mode.PIN || mode == Mode.PARENT) enterChildMode()
        }

        setContent {
            LittleLanternTheme {
                when (mode) {
                    Mode.SETUP -> PinSetupScreen(onPinChosen = { pin ->
                        prefs.setPin(pin)
                        enterChildMode()
                    })

                    Mode.CHILD -> {
                        val games = remember(allowedGames) {
                            installedLaunchableApps(this@MainActivity).filter { it.packageName in allowedGames }
                        }
                        ChildHome(games = games, onOpenGame = ::launchGame)
                    }

                    Mode.PIN -> PinEntryScreen(
                        checkPin = prefs::checkPin,
                        onSuccess = ::enterParentMode,
                        onCancel = ::enterChildMode,
                    )

                    Mode.PARENT -> {
                        val apps = remember { installedLaunchableApps(this@MainActivity) }
                        ParentHome(
                            isDeviceOwner = lock.isDeviceOwner,
                            isLocked = isLocked,
                            apps = apps,
                            allowedGames = allowedGames,
                            onToggleGame = ::setGameAllowed,
                            onBackToChild = ::enterChildMode,
                            onPauseLock = ::pauseLock,
                            onReleaseDeviceOwner = ::releaseDeviceOwner,
                            lastInteraction = { lastInteraction },
                        )
                    }
                }
            }
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

    private fun enterChildMode() {
        mode = Mode.CHILD
        startLockIfPossible()
        isLocked = lock.isLocked()
    }

    private fun enterParentMode() {
        lastInteraction = SystemClock.uptimeMillis()
        isLocked = lock.isLocked()
        mode = Mode.PARENT
    }

    private fun startLockIfPossible() {
        if (lock.isDeviceOwner && !lock.isLocked()) startLockTask()
    }

    private fun launchGame(packageName: String) {
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
}
