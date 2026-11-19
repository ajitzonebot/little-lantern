package org.littlelantern.net

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/**
 * Creates GitHub issues with a personal access token the parent enters on the phone.
 * The token needs only "Issues: read and write" on this one repository.
 */
class GitHubIssues(private val client: OkHttpClient) {

    /** Returns the new issue's web URL. */
    fun create(repo: String, token: String, title: String, body: String, labels: List<String>): String {
        require(Regex("^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$").matches(repo)) { "Repository must look like owner/name" }
        val json = JSONObject()
            .put("title", title)
            .put("body", body)
            .put("labels", JSONArray(labels))
        val request = Request.Builder()
            .url("https://api.github.com/repos/$repo/issues")
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "little-lantern-feedback")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val reason = runCatching { JSONObject(text).optString("message") }.getOrNull().orEmpty()
                throw IOException("GitHub said ${response.code}${if (reason.isNotEmpty()) ": $reason" else ""}")
            }
            return JSONObject(text).getString("html_url")
        }
    }
}
