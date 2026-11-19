package org.littlelantern.data

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable

data class AppEntry(val packageName: String, val label: String, val icon: Drawable)

/** Lists launchable apps on the device, excluding Little Lantern itself. */
fun installedLaunchableApps(context: Context): List<AppEntry> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(intent, 0)
        .filter { it.activityInfo.packageName != context.packageName }
        .map { AppEntry(it.activityInfo.packageName, it.loadLabel(pm).toString(), it.loadIcon(pm)) }
        .distinctBy { it.packageName }
        .sortedBy { it.label.lowercase() }
}
