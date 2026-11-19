package org.littlelantern.lock

import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import org.littlelantern.MainActivity

enum class LockState {
    /** Child can swipe out. */
    NONE,

    /** App pinning: gestures blocked, leaving needs the phone PIN. Ends on restart. */
    PINNED,

    /** Device owner lock task: blocks everything, survives restarts. */
    FULL,
}

/**
 * Keeps the child inside Little Lantern.
 * Normal phone: Android App pinning (startLockTask without device owner).
 * Spare phone set up as device owner: full lock task mode.
 */
class LockController(private val context: Context) {

    private val dpm = context.getSystemService(DevicePolicyManager::class.java)
    private val admin = ComponentName(context, AdminReceiver::class.java)

    val isDeviceOwner: Boolean
        get() = dpm.isDeviceOwnerApp(context.packageName)

    /** Device owner only: allow just this app, hide system UI, make it the home screen. */
    fun configure() {
        if (!isDeviceOwner) return
        dpm.setLockTaskPackages(admin, arrayOf(context.packageName))
        dpm.setLockTaskFeatures(admin, DevicePolicyManager.LOCK_TASK_FEATURE_NONE)
        dpm.setStatusBarDisabled(admin, true)
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

    /** Undoes the device owner setup so the app can be uninstalled. Parent mode only. */
    fun releaseDeviceOwner() {
        if (!isDeviceOwner) return
        dpm.setStatusBarDisabled(admin, false)
        dpm.clearPackagePersistentPreferredActivities(admin, context.packageName)
        dpm.setLockTaskPackages(admin, emptyArray())
        @Suppress("DEPRECATION")
        dpm.clearDeviceOwnerApp(context.packageName)
    }
}
