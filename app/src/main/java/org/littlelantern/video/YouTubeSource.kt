package org.littlelantern.video

import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.StreamInfo

/** What we need to download and check one video. */
data class ResolvedVideo(
    val title: String,
    val channel: String,
    val description: String,
    val durationMs: Long,
    val videoUrl: String,
    val audioUrl: String?,
)

/**
 * Reads YouTube through NewPipeExtractor: direct media streams, no ads, no recommendations.
 * Blocking network calls: use off the main thread.
 */
object YouTubeSource {

    private const val MAX_PLAYLIST_ITEMS = 50
    private const val MAX_HEIGHT = 480

    fun isPlaylist(url: String): Boolean =
        url.contains("/playlist") || (url.contains("list=") && !url.contains("v="))

    /** A video link gives itself; a playlist link gives up to 50 video links. */
    fun expand(url: String): List<String> {
        if (!isPlaylist(url)) return listOf(url.trim())
        val info = PlaylistInfo.getInfo(url.trim())
        val urls = info.relatedItems.map { it.url }.toMutableList()
        var page = info.nextPage
        while (page != null && urls.size < MAX_PLAYLIST_ITEMS) {
            val more = PlaylistInfo.getMoreItems(ServiceList.YouTube, url.trim(), page)
            urls += more.items.map { it.url }
            page = if (more.hasNextPage()) more.nextPage else null
        }
        return urls.take(MAX_PLAYLIST_ITEMS)
    }

    fun resolve(url: String): ResolvedVideo {
        val info = StreamInfo.getInfo(url)

        // Prefer a small MP4 video-only stream (H.264), at most 480p, plus a separate audio stream.
        val videoOnly = info.videoOnlyStreams.filter {
            it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP && it.isUrl
        }
        val pickedVideo = videoOnly
            .filter { it.format == MediaFormat.MPEG_4 && height(it.resolution) in 1..MAX_HEIGHT }
            .maxByOrNull { height(it.resolution) }
            ?: videoOnly.minByOrNull { height(it.resolution) }

        val audio = info.audioStreams
            .filter { it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP && it.isUrl }
            .sortedWith(compareBy({ it.format != MediaFormat.M4A }, { it.averageBitrate }))
            .firstOrNull()

        val videoUrl: String
        val audioUrl: String?
        if (pickedVideo != null) {
            videoUrl = pickedVideo.content
            audioUrl = audio?.content
        } else {
            // Fall back to a muxed stream (video and audio together).
            val muxed = info.videoStreams
                .filter { it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP && it.isUrl }
                .minByOrNull { height(it.resolution) }
                ?: error("No downloadable stream for this video")
            videoUrl = muxed.content
            audioUrl = null
        }

        return ResolvedVideo(
            title = info.name,
            channel = info.uploaderName ?: "",
            description = info.description?.content ?: "",
            durationMs = info.duration * 1000,
            videoUrl = videoUrl,
            audioUrl = audioUrl,
        )
    }

    private fun height(resolution: String?): Int =
        resolution?.substringBefore('p')?.toIntOrNull() ?: 0
}
