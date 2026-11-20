package org.littlelantern.video

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.VideoStream
import java.util.concurrent.TimeUnit

/** Shared HTTP client and user agent for NewPipeExtractor, Media3 and the online checks. */
object Net {
    const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0"

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    @Volatile private var initialised = false

    fun initNewPipe() {
        if (initialised) return
        synchronized(this) {
            if (!initialised) {
                NewPipe.init(OkHttpDownloader(client))
                initialised = true
            }
        }
    }
}

/** NewPipeExtractor's network layer, backed by OkHttp. */
private class OkHttpDownloader(private val client: OkHttpClient) : Downloader() {
    override fun execute(request: Request): Response {
        val method = request.httpMethod()
        val data = request.dataToSend()
        val body = when {
            data != null -> data.toRequestBody(null)
            method == "POST" -> ByteArray(0).toRequestBody(null)
            else -> null
        }
        val builder = okhttp3.Request.Builder()
            .url(request.url())
            .method(method, body)
            .header("User-Agent", Net.USER_AGENT)
        request.headers().forEach { (name, values) ->
            builder.removeHeader(name)
            values.forEach { builder.addHeader(name, it) }
        }
        client.newCall(builder.build()).execute().use { r ->
            if (r.code == 429) throw ReCaptchaException("reCaptcha challenge requested", request.url())
            return Response(r.code, r.message, r.headers.toMultimap(), r.body?.string(), r.request.url.toString())
        }
    }
}

/** What playback and the check need from a YouTube link. */
data class ResolvedVideo(
    val id: String,
    val title: String,
    val channel: String,
    val durationSeconds: Long,
    val thumbnailUrl: String?,
    val description: String,
    /** Muxed video+audio stream, when one exists (simplest to play). */
    val muxedUrl: String?,
    /** Separate video and audio streams for playback. */
    val playVideoUrl: String?,
    val playAudioUrl: String?,
    /** Lowest-quality streams for the check: least data. */
    val checkVideoUrl: String?,
    val checkAudioUrl: String?,
)

object Streams {

    suspend fun resolve(url: String): ResolvedVideo = withContext(Dispatchers.IO) {
        Net.initNewPipe()
        val info = StreamInfo.getInfo(ServiceList.YouTube, url)

        val muxed = info.videoStreams
            .filter { it.isPlayable() && it.format == MediaFormat.MPEG_4 }
            .maxByOrNull { it.height() }
        val videoOnly = info.videoOnlyStreams.filter { it.isPlayable() && it.format == MediaFormat.MPEG_4 }
        val playVideo = videoOnly.filter { it.height() in 1..480 }.maxByOrNull { it.height() }
            ?: videoOnly.minByOrNull { it.height() }
        val checkVideo = videoOnly.filter { it.height() > 0 }.minByOrNull { it.height() }
        val audio = info.audioStreams.filter { it.isPlayable() && it.format == MediaFormat.M4A }
        val playAudio = audio.maxByOrNull { it.averageBitrate }
        val checkAudio = audio.minByOrNull { it.averageBitrate.takeIf { b -> b > 0 } ?: Int.MAX_VALUE }

        ResolvedVideo(
            id = info.id,
            title = info.name,
            channel = info.uploaderName ?: "",
            durationSeconds = info.duration,
            thumbnailUrl = info.thumbnails.maxByOrNull { it.width }?.url,
            description = info.description?.content ?: "",
            muxedUrl = muxed?.content,
            playVideoUrl = playVideo?.content,
            playAudioUrl = playAudio?.content,
            checkVideoUrl = checkVideo?.content ?: muxed?.content,
            checkAudioUrl = checkAudio?.content ?: muxed?.content,
        )
    }

    private fun VideoStream.height(): Int =
        resolution.takeWhile { it.isDigit() }.toIntOrNull() ?: 0

    private fun VideoStream.isPlayable() = isUrl && deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP

    private fun AudioStream.isPlayable() = isUrl && deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP
}
