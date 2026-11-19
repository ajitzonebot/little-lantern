package org.littlelantern.video

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import org.littlelantern.check.Metrics
import java.io.File
import java.util.UUID

enum class VideoStatus { QUEUED, CHECKING, REVIEW, APPROVED, FAILED }

/** A video the parent added by link. Only the link, the check result and a small thumbnail are kept. */
data class VideoEntry(
    val id: String,
    val url: String,
    val title: String = "",
    val channel: String = "",
    val durationMs: Long = 0,
    val status: VideoStatus = VideoStatus.QUEUED,
    val error: String? = null,
    val metrics: Metrics? = null,
)

/** library.json plus thumbs/<id>.jpg. Videos themselves are never stored; they stream when played. */
class VideoLibrary(context: Context) {

    private val thumbDir = File(context.filesDir, "thumbs").apply { mkdirs() }
    private val index = File(context.filesDir, "library.json")

    init {
        // v0.2 downloaded videos; remove any left over.
        File(context.filesDir, "media").deleteRecursively()
    }

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
        val status = when (val raw = o.optString("status")) {
            "DOWNLOADING" -> VideoStatus.QUEUED // v0.2 state: check again by streaming
            else -> runCatching { VideoStatus.valueOf(raw) }.getOrDefault(VideoStatus.FAILED)
        }
        return VideoEntry(
            id = o.getString("id"),
            url = o.getString("url"),
            title = o.optString("title"),
            channel = o.optString("channel"),
            durationMs = o.optLong("durationMs"),
            status = status,
            error = if (o.isNull("error")) null else o.optString("error"),
            metrics = m,
        )
    }

    private fun JSONObject.optDoubleOrNull(key: String): Double? = if (!has(key) || isNull(key)) null else optDouble(key)
}
