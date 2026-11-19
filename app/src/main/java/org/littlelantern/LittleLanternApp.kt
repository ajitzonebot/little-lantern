package org.littlelantern

import android.app.Application
import org.littlelantern.video.VideoRepository

class LittleLanternApp : Application() {
    val videos: VideoRepository by lazy { VideoRepository(this) }
}
