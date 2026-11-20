package org.littlelantern.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import org.littlelantern.video.CheckReport
import org.littlelantern.video.SkipSegment
import java.io.File

/** A parent-approved video. Only the link and details are kept; the video itself always streams. */
data class Video(
    val id: String,
    val url: String,
    val title: String,
    val channel: String,
    val durationSeconds: Long,
    val thumbnailUrl: String?,
    val skipSegments: List<SkipSegment>,
    val report: CheckReport,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("url", url)
        .put("title", title)
        .put("channel", channel)
        .put("duration", durationSeconds)
        .put("thumbnail", thumbnailUrl ?: JSONObject.NULL)
        .put("skip", JSONArray().apply { skipSegments.forEach { put(it.toJson()) } })
        .put("report", report.toJson())

    companion object {
        fun fromJson(o: JSONObject): Video {
            val skip = o.optJSONArray("skip") ?: JSONArray()
            return Video(
                id = o.getString("id"),
                url = o.getString("url"),
                title = o.optString("title"),
                channel = o.optString("channel"),
                durationSeconds = o.optLong("duration"),
                thumbnailUrl = if (o.isNull("thumbnail")) null else o.optString("thumbnail"),
                skipSegments = (0 until skip.length()).map { SkipSegment.fromJson(skip.getJSONObject(it)) },
                report = CheckReport.fromJson(o.optJSONObject("report") ?: JSONObject()),
            )
        }
    }
}

/** Approved videos, stored as a small JSON file in app storage. */
class VideoStore(context: Context) {
    private val file = File(context.filesDir, "videos.json")

    fun all(): List<Video> {
        if (!file.exists()) return emptyList()
        return runCatching {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).map { Video.fromJson(arr.getJSONObject(it)) }
        }.getOrDefault(emptyList())
    }

    fun add(video: Video) = save(all().filterNot { it.id == video.id } + video)

    fun remove(id: String) = save(all().filterNot { it.id == id })

    private fun save(videos: List<Video>) {
        val arr = JSONArray().apply { videos.forEach { put(it.toJson()) } }
        val tmp = File(file.parentFile, "videos.json.tmp")
        tmp.writeText(arr.toString())
        tmp.renameTo(file)
    }
}
