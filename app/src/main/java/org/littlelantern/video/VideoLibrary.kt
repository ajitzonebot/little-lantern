package org.littlelantern.video

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import org.littlelantern.check.Metrics
import java.io.File
import java.util.UUID

enum class VideoStatus { QUEUED, DOWNLOADING, CHECKING, REVIEW, APPROVED, FAILED }

data class VideoEntry(
    val id: String,
    val url: String,
    val title: String = "",
    val channel: String = "",
    val durationMs: Long = 0,
    val status: VideoStatus = VideoStatus.QUEUED,
    val error: String? = null,
    val metrics: Metrics? = null,
    val hasSeparateAudio: Boolean = false,
)

/**
 * Videos added by link, stored offline in app-private storage:
 * media/<id>.video, media/<id>.audio, thumbs/<id>.jpg and library.json.
 */
class VideoLibrary(context: Context) {

    private val mediaDir = File(context.filesDir, "media").apply { mkdirs() }
    private val thumbDir = File(context.filesDir, "thumbs").apply { mkdirs() }
    private val index = File(context.filesDir, "library.json")

    fun videoFile(id: String) = File(mediaDir, "$id.video")
    fun audioFile(id: String) = File(mediaDir, "$id.audio")
    fun thumbFile(id: String) = File(thumbDir, "$id.jpg")

    @Synchronized
    fun list(): List<VideoEntry> {
        if (!index.exists()) return emptyList()
        val arr = JSONArray(index.readText())
        return List(arr.length()) { fromJson(arr.getJSONObject(it)) }
    }

    @Synchronized
    fun addPending(url: String): VideoEntry {
        val entry = VideoEntry(id = UUID.randomUUID().toString(), url = url)
        save(list() + entry)
        return entry
    }

    @Synchronized
    fun update(id: String, change: (VideoEntry) -> VideoEntry) {
        save(list().map { if (it.id == id) change(it) else it })
    }

    @Synchronized
    fun remove(id: String) {
        videoFile(id).delete()
        audioFile(id).delete()
        thumbFile(id).delete()
        save(list().filter { it.id != id })
    }

    @Synchronized
    private fun save(entries: List<VideoEntry>) {
        val arr = JSONArray()
        entries.forEach { arr.put(toJson(it)) }
        val tmp = File(index.path + ".tmp")
        tmp.writeText(arr.toString())
        tmp.renameTo(index)
    }

    private fun toJson(e: VideoEntry) = JSONObject().apply {
        put("id", e.id)
        put("url", e.url)
        put("title", e.title)
        put("channel", e.channel)
        put("durationMs", e.durationMs)
        put("status", e.status.name)
        put("error", e.error ?: JSONObject.NULL)
        put("hasSeparateAudio", e.hasSeparateAudio)
        e.metrics?.let { m ->
            put(
                "metrics",
                JSONObject().apply {
                    put("cuts", m.cutsPerMinute ?: JSONObject.NULL)
                    put("flashes", m.flashesPerMinute ?: JSONObject.NULL)
                    put("saturation", m.meanSaturation ?: JSONObject.NULL)
                    put("loud", m.loudSpikesPerMinute ?: JSONObject.NULL)
                    put("durationMs", m.durationMs)
                    put("hints", JSONArray(m.fantasyHints))
                    put("parentFantastical", m.parentMarkedFantastical)
                },
            )
        }
    }

    private fun fromJson(o: JSONObject): VideoEntry {
        val m = o.optJSONObject("metrics")?.let { j ->
            val hints = j.optJSONArray("hints") ?: JSONArray()
            Metrics(
                cutsPerMinute = j.optDoubleOrNull("cuts"),
                flashesPerMinute = j.optDoubleOrNull("flashes"),
                meanSaturation = j.optDoubleOrNull("saturation"),
                loudSpikesPerMinute = j.optDoubleOrNull("loud"),
                durationMs = j.optLong("durationMs"),
                fantasyHints = List(hints.length()) { hints.getString(it) },
                parentMarkedFantastical = j.optBoolean("parentFantastical"),
            )
        }
        return VideoEntry(
            id = o.getString("id"),
            url = o.getString("url"),
            title = o.optString("title"),
            channel = o.optString("channel"),
            durationMs = o.optLong("durationMs"),
            status = runCatching { VideoStatus.valueOf(o.getString("status")) }.getOrDefault(VideoStatus.FAILED),
            error = if (o.isNull("error")) null else o.optString("error"),
            metrics = m,
            hasSeparateAudio = o.optBoolean("hasSeparateAudio"),
        )
    }

    private fun JSONObject.optDoubleOrNull(key: String): Double? = if (isNull(key) || !has(key)) null else optDouble(key)
}
