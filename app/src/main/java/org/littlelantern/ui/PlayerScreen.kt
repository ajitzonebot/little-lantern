package org.littlelantern.ui

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import android.view.TextureView
import android.view.View
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import java.io.File

/**
 * Full-screen calm playback: no controls, no seeking, no autoplay, reduced colour and volume.
 * [onFinished] fires once, when the video reaches its natural end.
 */
@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    videoFile: File,
    audioFile: File?,
    fallbackDurationMs: Long,
    saturation: Float,
    volume: Float,
    onFinished: (watchedMs: Long) -> Unit,
) {
    val context = LocalContext.current
    val finished by rememberUpdatedState(onFinished)
    var aspect by remember { mutableFloatStateOf(16f / 9f) }

    val player = remember(videoFile) {
        val factory = ProgressiveMediaSource.Factory(DefaultDataSource.Factory(context))
        val video = factory.createMediaSource(MediaItem.fromUri(Uri.fromFile(videoFile)))
        val source = if (audioFile != null) {
            MergingMediaSource(video, factory.createMediaSource(MediaItem.fromUri(Uri.fromFile(audioFile))))
        } else {
            video
        }
        ExoPlayer.Builder(context).build().apply {
            setMediaSource(source)
            repeatMode = Player.REPEAT_MODE_OFF
            this.volume = volume
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(player) {
        var done = false
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED && !done) {
                    done = true
                    val duration = player.duration.takeIf { it > 0 } ?: fallbackDurationMs
                    finished(duration)
                }
            }

            override fun onVideoSizeChanged(size: VideoSize) {
                if (size.width > 0 && size.height > 0) {
                    aspect = size.width * size.pixelWidthHeightRatio / size.height
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    // Pause when the screen turns off or the app leaves the foreground.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, player) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> player.pause()
                Lifecycle.Event.ON_RESUME -> player.play()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF1E211F)), contentAlignment = Alignment.Center) {
        AndroidView(
            factory = { ctx ->
                TextureView(ctx).also { view ->
                    // Desaturate the picture itself, not an overlay.
                    val matrix = ColorMatrix().apply { setSaturation(saturation) }
                    view.setLayerType(View.LAYER_TYPE_HARDWARE, Paint().apply { colorFilter = ColorMatrixColorFilter(matrix) })
                    player.setVideoTextureView(view)
                }
            },
            modifier = Modifier.fillMaxWidth().aspectRatio(aspect),
        )
    }
}
