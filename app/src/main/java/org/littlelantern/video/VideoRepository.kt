package org.littlelantern.video

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.littlelantern.check.KidCheck
import org.littlelantern.check.Metrics
import org.littlelantern.check.VideoAnalyzer
import org.littlelantern.net.OkHttpDownloader
import org.littlelantern.net.downloadInChunks
import org.schabi.newpipe.extractor.NewPipe
import java.util.concurrent.TimeUnit

/**
 * Adds videos by link: resolve with NewPipeExtractor, download, run the kid check, then wait for the parent.
 * One video is processed at a time. Network is used only here, only when the parent adds links.
 */
class VideoRepository(context: Context) {

    val library = VideoLibrary(context)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = OkHttpClient.Builder()
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    private val queue = Channel<String>(Channel.UNLIMITED)

    private val _videos = MutableStateFlow(library.list())
    val videos: StateFlow<List<VideoEntry>> = _videos

    /** Live progress text per video id, e.g. "Downloading 40%". */
    private val _progress = MutableStateFlow<Map<String, String>>(emptyMap())
    val progress: StateFlow<Map<String, String>> = _progress

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    init {
        NewPipe.init(OkHttpDownloader(client))
        scope.launch { for (id in queue) process(id) }
        // Resume anything interrupted by an app restart.
        library.list()
            .filter { it.status in setOf(VideoStatus.QUEUED, VideoStatus.DOWNLOADING, VideoStatus.CHECKING) }
            .forEach { queue.trySend(it.id) }
    }

    fun addLink(raw: String) {
        val url = extractUrl(raw)
        if (url == null) {
            _message.value = "That doesn't look like a YouTube link."
            return
        }
        _message.value = if (YouTubeSource.isPlaylist(url)) "Reading playlist..." else "Adding video..."
        scope.launch {
            try {
                val known = library.list().map { it.url }.toSet()
                val urls = YouTubeSource.expand(url).filter { it !in known }
                urls.forEach { queue.send(library.addPending(it).id) }
                refresh()
                _message.value = when (urls.size) {
                    0 -> "Already in the library."
                    1 -> "Added. Checking it now."
                    else -> "Added ${urls.size} videos. Checking them one by one."
                }
            } catch (e: Exception) {
                _message.value = "Could not read that link: ${e.message ?: e.javaClass.simpleName}"
            }
        }
    }

    fun approve(id: String) = change(id) { it.copy(status = VideoStatus.APPROVED) }

    fun reject(id: String) {
        library.remove(id)
        refresh()
    }

    fun retry(id: String) {
        change(id) { it.copy(status = VideoStatus.QUEUED, error = null) }
        queue.trySend(id)
    }

    fun setParentFantastical(id: String, marked: Boolean) = change(id) { entry ->
        entry.copy(metrics = entry.metrics?.copy(parentMarkedFantastical = marked))
    }

    fun clearMessage() {
        _message.value = null
    }

    private fun process(id: String) {
        val entry = library.list().firstOrNull { it.id == id } ?: return
        try {
            setProgress(id, "Reading link")
            change(id) { it.copy(status = VideoStatus.DOWNLOADING, error = null) }
            val resolved = YouTubeSource.resolve(entry.url)
            if (resolved.durationMs > MAX_DURATION_MS) {
                change(id) {
                    it.copy(title = resolved.title, channel = resolved.channel, durationMs = resolved.durationMs,
                        status = VideoStatus.FAILED, error = "Longer than 30 minutes, not downloaded.")
                }
                return
            }
            change(id) { it.copy(title = resolved.title, channel = resolved.channel, durationMs = resolved.durationMs) }

            downloadInChunks(client, resolved.videoUrl, library.videoFile(id)) { p ->
                setProgress(id, if (p < 0) "Downloading video" else "Downloading video ${(p * 100).toInt()}%")
            }
            resolved.audioUrl?.let { audioUrl ->
                downloadInChunks(client, audioUrl, library.audioFile(id)) { p ->
                    setProgress(id, if (p < 0) "Downloading sound" else "Downloading sound ${(p * 100).toInt()}%")
                }
            }
            saveThumbnail(id)

            change(id) { it.copy(status = VideoStatus.CHECKING, hasSeparateAudio = resolved.audioUrl != null) }
            val audio = if (resolved.audioUrl != null) library.audioFile(id) else library.videoFile(id)
            val measured = VideoAnalyzer.analyze(library.videoFile(id), audio) { p ->
                setProgress(id, "Checking ${(p * 100).toInt()}%")
            }
            val metrics = Metrics(
                cutsPerMinute = measured.cutsPerMinute,
                flashesPerMinute = measured.flashesPerMinute,
                meanSaturation = measured.meanSaturation,
                loudSpikesPerMinute = measured.loudSpikesPerMinute,
                durationMs = resolved.durationMs,
                fantasyHints = KidCheck.fantasyHints(resolved.title, resolved.description),
                parentMarkedFantastical = false,
            )
            change(id) { it.copy(status = VideoStatus.REVIEW, metrics = metrics) }
        } catch (e: Exception) {
            change(id) { it.copy(status = VideoStatus.FAILED, error = e.message ?: e.javaClass.simpleName) }
        } finally {
            setProgress(id, null)
        }
    }

    private fun saveThumbnail(id: String) {
        val r = MediaMetadataRetriever()
        try {
            r.setDataSource(library.videoFile(id).path)
            val durationMs = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val frame = r.getFrameAtTime(durationMs * 1000 / 3, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) ?: return
            library.thumbFile(id).outputStream().use { frame.compress(Bitmap.CompressFormat.JPEG, 80, it) }
            frame.recycle()
        } catch (_: RuntimeException) {
            // No thumbnail; the picker shows a plain tile.
        } finally {
            r.release()
        }
    }

    private fun change(id: String, update: (VideoEntry) -> VideoEntry) {
        library.update(id, update)
        refresh()
    }

    private fun refresh() {
        _videos.value = library.list()
    }

    private fun setProgress(id: String, text: String?) {
        _progress.value = if (text == null) _progress.value - id else _progress.value + (id to text)
    }

    companion object {
        private const val MAX_DURATION_MS = 30 * 60_000L

        /** Pulls the first http(s) link out of shared text such as "Watch this: https://youtu.be/...". */
        fun extractUrl(text: String): String? =
            Regex("https?://\\S+").find(text)?.value
                ?.takeIf { it.contains("youtube.com") || it.contains("youtu.be") }
    }
}
