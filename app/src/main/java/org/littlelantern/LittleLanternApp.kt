package org.littlelantern

import android.app.Application
import okhttp3.OkHttpClient
import org.littlelantern.feedback.FeedbackStore
import org.littlelantern.net.GitHubIssues
import org.littlelantern.video.VideoRepository

class LittleLanternApp : Application() {
    val videos: VideoRepository by lazy { VideoRepository(this) }
    val feedback: FeedbackStore by lazy { FeedbackStore(this) }
    val github: GitHubIssues by lazy { GitHubIssues(OkHttpClient()) }
}
