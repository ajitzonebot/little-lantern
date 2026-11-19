package org.littlelantern.video

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.littlelantern.check.KidCheck
import org.littlelantern.check.Metrics
import org.littlelantern.check.VideoAnalyzer
import org.littlelantern.net.OkHttpDownloader
import org.littlelantern.net.USER_AGENT
import org.schabi.newpipe.extractor.NewPipe
import java.util.concurrent.TimeUnit

/**
 * Adds videos by link: read the link with NewPipeExtractor, stream short samples to run the kid check,
 * then wait for the parent. Videos are never downloaded; they stream when played.
 */
class VideoRepository(context: Context) {

    val library = VideoLibrary(context)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = OkHttpClient.Builder().readTimeout(30, TimeUnit.SECONDS).build()
    private val queue = Channel<String>(Channel.UNLIMITED)

    private val _videos = MutableStateFlow(library.list())
    val videos: StateFlow<List<VideoEntry>> = _videos

    /** Live progress text per video id, e.g. "Checking samples 40%". */
    private val _progress = MutableStateFlow<Map<String, String>>(emptyMap())
    val progress: StateFlow<Map<String, String>> = _progress

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    init {
        NewPipe.init(OkHttpDownloader(client))
        scope.launch { for (id in queue) process(id) }
        library.list()
            .filter { it.status == VideoStatus.QUEUED || it.status == VideoStatus.CHECKING }
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

    /** Fresh stream links for playback (they expire, so resolve at play time). */
    suspend fun resolveForPlayback(entry: VideoEntry): ResolvedVideo = withContext(Dispatchers.IO) {
        YouTubeSource.resolve(entry.url)
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
            change(id) { it.copy(status = VideoStatus.CHECKING, error = null) }
            val resolved = YouTubeSource.resolve(entry.url)
            change(id) { it.copy(title = resolved.title, channel = resolved.channel, durationMs = resolved.durationMs) }
            resolved.thumbnailUrl?.let { saveThumbnail(id, it) }

            val measured = VideoAnalyzer.analyze(
                videoUrl = resolved.videoUrl,
                audioUrl = resolved.audioUrl,
                durationMs = resolved.durationMs,
                headers = mapOf("User-Agent" to USER_AGENT),
            ) { p -> setProgress(id, "Checking samples ${(p * 100).toInt()}%") }

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

    /** A small picture for the picker (a few KB), not the video. */
    private fun saveThumbnail(id: String, url: String) {
        runCatching {
            val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
            client.newCall(request).execute().use { response ->
                val bytes = response.body?.bytes()
                if (response.isSuccessful && bytes != null && bytes.size < 1_000_000) {
                    library.thumbFile(id).writeBytes(bytes)
                }
            }
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
        /** Pulls the first YouTube link out of shared text such as "Watch this: https://youtu.be/...". */
        fun extractUrl(text: String): String? =
            Regex("https?://\\S+").find(text)?.value
                ?.takeIf { it.contains("youtube.com") || it.contains("youtu.be") }
    }
}
