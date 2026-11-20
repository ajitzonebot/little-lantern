package org.littlelantern.lock

import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import org.littlelantern.MainActivity

enum class LockState {
    /** The child can swipe out. */
    NONE,

    /** Android App pinning: gestures blocked; leaving needs the phone's PIN if that setting is on. Ends on restart. */
    PINNED,

    /** Device owner lock task: blocks everything and survives restarts. */
    FULL,
}

/**
 * Keeps the child inside Little Lantern.
 * Any phone: Android App pinning (startLockTask without device owner; the phone asks to confirm).
 * Spare phone set up as device owner: full lock task mode, no confirmation.
 */
class LockController(private val context: Context) {

    private val dpm = context.getSystemService(DevicePolicyManager::class.java)
    private val admin = ComponentName(context, AdminReceiver::class.java)

    val isDeviceOwner: Boolean
        get() = dpm.isDeviceOwnerApp(context.packageName)

    /** Allows only this app to run in lock task mode. All games are built in. */
    fun configure() {
        if (!isDeviceOwner) return
        dpm.setLockTaskPackages(admin, arrayOf(context.packageName))
        // No home, recents, notifications, status bar info or power menu while locked.
        dpm.setLockTaskFeatures(admin, DevicePolicyManager.LOCK_TASK_FEATURE_NONE)
        dpm.setStatusBarDisabled(admin, true)
        // Make Little Lantern the permanent home screen so a reboot lands here.
        val home = IntentFilter(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addCategory(Intent.CATEGORY_DEFAULT)
        }
        dpm.addPersistentPreferredActivity(admin, home, ComponentName(context, MainActivity::class.java))
    }

    fun state(): LockState {
        val am = context.getSystemService(ActivityManager::class.java)
        return when (am.lockTaskModeState) {
            ActivityManager.LOCK_TASK_MODE_LOCKED -> LockState.FULL
            ActivityManager.LOCK_TASK_MODE_PINNED -> LockState.PINNED
            else -> LockState.NONE
        }
    }

    fun isLocked(): Boolean = state() != LockState.NONE

    /** Undoes everything so the app can be uninstalled. Parent mode only. */
    fun releaseDeviceOwner() {
        if (!isDeviceOwner) return
        dpm.setStatusBarDisabled(admin, false)
        dpm.clearPackagePersistentPreferredActivities(admin, context.packageName)
        dpm.setLockTaskPackages(admin, emptyArray())
        @Suppress("DEPRECATION")
        dpm.clearDeviceOwnerApp(context.packageName)
    }
}
