package org.littlelantern.lock

import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import org.littlelantern.MainActivity

/**
 * Wraps the device-owner APIs that keep the child inside Little Lantern.
 * Every call is a no-op when the app is not device owner, so the app still runs (unlocked) for development.
 */
class LockController(private val context: Context) {

    private val dpm = context.getSystemService(DevicePolicyManager::class.java)
    private val admin = ComponentName(context, AdminReceiver::class.java)

    val isDeviceOwner: Boolean
        get() = dpm.isDeviceOwnerApp(context.packageName)

    /** Allows only this app plus the parent-approved game apps to run in lock task mode. */
    fun configure(allowedGamePackages: Set<String>) {
        if (!isDeviceOwner) return
        val packages = (allowedGamePackages + context.packageName).toTypedArray()
        dpm.setLockTaskPackages(admin, packages)
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

    fun isLocked(): Boolean {
        val am = context.getSystemService(ActivityManager::class.java)
        return am.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
    }

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
