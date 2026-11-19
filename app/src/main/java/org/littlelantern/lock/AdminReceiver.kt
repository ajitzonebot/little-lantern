package org.littlelantern.lock

import android.app.admin.DeviceAdminReceiver

/** Device admin receiver. Set as device owner once with adb (see docs/SETUP.md). */
class AdminReceiver : DeviceAdminReceiver()
