package org.littlelantern.feedback

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class FeedbackNote(
    val id: String,
    val createdAt: Long,
    val target: FeedbackTarget,
    val text: String,
    val sent: Boolean = false,
)

/** Notes kept on the phone until they reach feedback.md on GitHub. */
class FeedbackStore(context: Context) {

    private val file = File(context.filesDir, "feedback.json")

    @Synchronized
    fun list(): List<FeedbackNote> {
        if (!file.exists()) return emptyList()
        val arr = runCatching { JSONArray(file.readText()) }.getOrElse { return emptyList() }
        return List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            FeedbackNote(
                id = o.getString("id"),
                createdAt = o.getLong("createdAt"),
                target = FeedbackTarget(o.getString("section"), if (o.isNull("item")) null else o.getString("item")),
                text = o.getString("text"),
                sent = o.optBoolean("sent", false),
            )
        }
    }

    @Synchronized
    fun add(target: FeedbackTarget, text: String): FeedbackNote {
        val note = FeedbackNote(UUID.randomUUID().toString(), System.currentTimeMillis(), target, text.trim())
        save(list() + note)
        return note
    }

    @Synchronized
    fun markSent(ids: Set<String>) = save(list().map { if (it.id in ids) it.copy(sent = true) else it })

    @Synchronized
    fun delete(id: String) = save(list().filter { it.id != id })

    private fun save(items: List<FeedbackNote>) {
        val arr = JSONArray()
        items.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("createdAt", it.createdAt)
                    .put("section", it.target.section)
                    .put("item", it.target.item ?: JSONObject.NULL)
                    .put("text", it.text)
                    .put("sent", it.sent),
            )
        }
        val tmp = File(file.path + ".tmp")
        tmp.writeText(arr.toString())
        tmp.renameTo(file)
    }
}
