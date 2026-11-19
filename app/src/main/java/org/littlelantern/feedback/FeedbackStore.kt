package org.littlelantern.feedback

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

enum class FeedbackKind(val label: String) { BUG("Bug"), IDEA("Idea"), OTHER("Other") }

data class Feedback(
    val id: String,
    val createdAt: Long,
    val screen: String,
    val kind: FeedbackKind,
    val text: String,
    val issueUrl: String? = null,
) {
    val sent: Boolean get() = issueUrl != null
}

/** Feedback notes, saved on the phone until the parent sends them to GitHub. */
class FeedbackStore(context: Context) {

    private val file = File(context.filesDir, "feedback.json")

    @Synchronized
    fun list(): List<Feedback> {
        if (!file.exists()) return emptyList()
        val arr = JSONArray(file.readText())
        return List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            Feedback(
                id = o.getString("id"),
                createdAt = o.getLong("createdAt"),
                screen = o.getString("screen"),
                kind = runCatching { FeedbackKind.valueOf(o.getString("kind")) }.getOrDefault(FeedbackKind.OTHER),
                text = o.getString("text"),
                issueUrl = if (o.isNull("issueUrl")) null else o.optString("issueUrl"),
            )
        }
    }

    @Synchronized
    fun add(screen: String, kind: FeedbackKind, text: String): Feedback {
        val item = Feedback(UUID.randomUUID().toString(), System.currentTimeMillis(), screen, kind, text.trim())
        save(list() + item)
        return item
    }

    @Synchronized
    fun markSent(id: String, issueUrl: String) {
        save(list().map { if (it.id == id) it.copy(issueUrl = issueUrl) else it })
    }

    @Synchronized
    fun delete(id: String) {
        save(list().filter { it.id != id })
    }

    private fun save(items: List<Feedback>) {
        val arr = JSONArray()
        items.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("createdAt", it.createdAt)
                    .put("screen", it.screen)
                    .put("kind", it.kind.name)
                    .put("text", it.text)
                    .put("issueUrl", it.issueUrl ?: JSONObject.NULL),
            )
        }
        val tmp = File(file.path + ".tmp")
        tmp.writeText(arr.toString())
        tmp.renameTo(file)
    }
}
