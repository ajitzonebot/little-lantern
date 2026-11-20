package org.littlelantern.feedback

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Base64

/**
 * Appends notes to feedback.md through the GitHub contents API: read the file, add the notes, write it back as one commit.
 * The token needs only "Contents: read and write" on the one repository.
 */
class GitHubFeedback(private val client: OkHttpClient) {

    private val day = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /** Sends [notes] in one commit. Returns the commit's web URL. */
    fun send(repo: String, branch: String, token: String, notes: List<FeedbackNote>, appVersion: String): String {
        require(Regex("^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$").matches(repo)) { "Repository must look like owner/name" }
        require(token.isNotBlank()) { "No GitHub token saved" }
        // One retry if someone else changed the file between read and write.
        repeat(2) { attempt ->
            val (current, sha) = read(repo, branch, token)
            var md = current ?: FeedbackMd.EMPTY
            notes.sortedBy { it.createdAt }.forEach { n ->
                val date = day.format(Instant.ofEpochMilli(n.createdAt).atZone(ZoneId.systemDefault()))
                md = FeedbackMd.insert(md, n.target, "$date (app $appVersion): ${n.text}")
            }
            val result = write(repo, branch, token, md, sha, "Feedback from the app: ${notes.size} note(s)")
            if (result != null) return result
            if (attempt == 1) throw IOException("feedback.md kept changing; try again")
        }
        error("unreachable")
    }

    private fun url(repo: String) = "https://api.github.com/repos/$repo/contents/feedback.md"

    private fun Request.Builder.auth(token: String) = this
        .header("Authorization", "Bearer $token")
        .header("Accept", "application/vnd.github+json")
        .header("X-GitHub-Api-Version", "2022-11-28")
        .header("User-Agent", "little-lantern-feedback")

    /** Returns the file text and sha, or (null, null) when the file does not exist yet. */
    private fun read(repo: String, branch: String, token: String): Pair<String?, String?> {
        val ref = if (branch.isBlank()) "" else "?ref=" + URLEncoder.encode(branch, "UTF-8")
        val request = Request.Builder().url(url(repo) + ref).auth(token).get().build()
        client.newCall(request).execute().use { r ->
            val body = r.body?.string().orEmpty()
            if (r.code == 404) return null to null
            if (!r.isSuccessful) throw IOException(reason(r.code, body))
            val o = JSONObject(body)
            val text = String(Base64.getMimeDecoder().decode(o.getString("content")), Charsets.UTF_8)
            return text to o.getString("sha")
        }
    }

    /** Returns the commit URL, or null on a sha conflict. */
    private fun write(repo: String, branch: String, token: String, md: String, sha: String?, message: String): String? {
        val json = JSONObject()
            .put("message", message)
            .put("content", Base64.getEncoder().encodeToString(md.toByteArray(Charsets.UTF_8)))
        if (sha != null) json.put("sha", sha)
        if (branch.isNotBlank()) json.put("branch", branch)
        val request = Request.Builder().url(url(repo)).auth(token)
            .put(json.toString().toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(request).execute().use { r ->
            val body = r.body?.string().orEmpty()
            if (r.code == 409) return null
            if (!r.isSuccessful) throw IOException(reason(r.code, body))
            return JSONObject(body).getJSONObject("commit").optString("html_url")
        }
    }

    private fun reason(code: Int, body: String): String {
        val msg = runCatching { JSONObject(body).optString("message") }.getOrNull().orEmpty()
        val hint = when (code) {
            401 -> " (token wrong or expired)"
            403, 404 -> " (token needs Contents: read and write on this repository)"
            else -> ""
        }
        return "GitHub said $code${if (msg.isNotEmpty()) ": $msg" else ""}$hint"
    }
}
