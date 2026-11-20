package org.littlelantern.ui

import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import kotlinx.coroutines.delay
import org.littlelantern.data.Video
import org.littlelantern.video.Net
import org.littlelantern.video.Streams

/**
 * Plays one approved video by streaming it. Nothing is saved to the phone.
 * No next video, no suggestions. At the end a still "all done" card shows, then home.
 * SponsorBlock segments ("like and subscribe", sponsors) are skipped.
 */
@Composable
fun PlayerScreen(video: Video, volume: Float, onHome: () -> Unit) {
    val context = LocalContext.current
    val currentVolume by rememberUpdatedState(volume)
    var state by remember { mutableStateOf("loading") }
    var aspect by remember { mutableFloatStateOf(16f / 9f) }
    var playing by remember { mutableStateOf(true) }
    var progress by remember { mutableFloatStateOf(0f) }

    val player = remember { ExoPlayer.Builder(context).build() }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) state = "ready"
                if (playbackState == Player.STATE_ENDED) state = "ended"
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    aspect = videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
            }

            override fun onPlayerError(error: PlaybackException) {
                state = "error"
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    // Resolve fresh stream links every time: they expire, and nothing is stored.
    LaunchedEffect(video.id) {
        val resolved = runCatching { Streams.resolve(video.url) }.getOrNull()
        if (resolved == null) {
            state = "error"
            return@LaunchedEffect
        }
        val http = DefaultHttpDataSource.Factory()
            .setUserAgent(Net.USER_AGENT)
            .setAllowCrossProtocolRedirects(true)
        val factory = ProgressiveMediaSource.Factory(http)
        fun source(url: String): MediaSource = factory.createMediaSource(MediaItem.fromUri(url))
        val media: MediaSource? = when {
            resolved.muxedUrl != null -> source(resolved.muxedUrl)
            resolved.playVideoUrl != null && resolved.playAudioUrl != null ->
                MergingMediaSource(source(resolved.playVideoUrl), source(resolved.playAudioUrl))
            else -> null
        }
        if (media == null) {
            state = "error"
            return@LaunchedEffect
        }
        player.setMediaSource(media)
        player.volume = currentVolume
        player.prepare()
        player.playWhenReady = true
    }

    // Skip marked segments, keep the volume in step with the session, track progress.
    LaunchedEffect(player) {
        while (true) {
            player.volume = currentVolume
            val pos = player.currentPosition / 1000.0
            video.skipSegments.firstOrNull { pos >= it.startSeconds && pos < it.endSeconds - 0.5 }?.let {
                player.seekTo((it.endSeconds * 1000).toLong())
            }
            val dur = player.duration
            if (dur > 0) progress = (player.currentPosition.toFloat() / dur).coerceIn(0f, 1f)
            delay(500)
        }
    }

    LaunchedEffect(state) {
        if (state == "ended") {
            delay(4000)
            onHome()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Palette.Ink)
            .padding(18.dp),
    ) {
        RoundButton(Glyph.HOME, "Back to home", onHome, dark = true)
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            when (state) {
                "ended" -> AllDoneCard()
                "error" -> Text(
                    "This video can't play right now.",
                    color = Palette.Bg,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                )
                else -> Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(aspect)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center,
                ) {
                    AndroidView(
                        factory = { ctx -> TextureView(ctx).also { player.setVideoTextureView(it) } },
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (state == "loading") CircularProgressIndicator(color = Palette.Border)
                }
            }
        }
        if (state != "ended") {
            Column(
                Modifier.fillMaxWidth().padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                Box(
                    Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(Palette.Bg)
                        .clickable(role = Role.Button) { if (player.isPlaying) player.pause() else player.play() }
                        .semantics { contentDescription = if (playing) "Pause" else "Play" },
                    contentAlignment = Alignment.Center,
                ) {
                    GlyphIcon(if (playing) Glyph.PAUSE else Glyph.PLAY, Palette.Ink, 40.dp)
                }
                Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF4A4A44))) {
                    Box(Modifier.fillMaxWidth(progress).height(6.dp).clip(RoundedCornerShape(3.dp)).background(Palette.Border))
                }
            }
        }
    }
}

@Composable
private fun AllDoneCard() {
    Column(
        Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Palette.Surface)
            .padding(horizontal = 40.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GlyphIcon(Glyph.LANTERN, Palette.Sand, 56.dp)
        Spacer(Modifier.height(12.dp))
        Text("All done", color = Palette.Ink, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
    }
}
