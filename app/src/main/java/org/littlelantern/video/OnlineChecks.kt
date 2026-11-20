package org.littlelantern.video

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

/** Dark-pattern checks that need only the internet, no video data. */
object OnlineChecks {

    data class SponsorResult(val interaction: Int, val sponsor: Int, val segments: List<SkipSegment>)

    private val categories = listOf("sponsor", "selfpromo", "interaction")

    /**
     * SponsorBlock crowd-marked segments. Uses the hash-prefix lookup, so the video id is not sent.
     * Returns null when the service could not be reached (unknown, not a pass).
     */
    suspend fun sponsorBlock(videoId: String): SponsorResult? = withContext(Dispatchers.IO) {
        runCatching {
            val prefix = sha256Hex(videoId).take(4)
            val url = "https://sponsor.ajay.app/api/skipSegments/$prefix".toHttpUrl().newBuilder()
                .addQueryParameter("categories", JSONArray(categories).toString())
                .build()
            Net.client.newCall(Request.Builder().url(url).header("User-Agent", "LittleLantern").build()).execute().use { r ->
                when (r.code) {
                    404 -> SponsorResult(0, 0, emptyList())
                    200 -> parseSponsorBlock(r.body?.string() ?: "[]", videoId)
                    else -> null
                }
            }
        }.getOrNull()
    }

    fun parseSponsorBlock(body: String, videoId: String): SponsorResult {
        val arr = JSONArray(body)
        val entry = (0 until arr.length()).map { arr.getJSONObject(it) }.firstOrNull { it.optString("videoID") == videoId }
            ?: return SponsorResult(0, 0, emptyList())
        val segs = entry.optJSONArray("segments") ?: JSONArray()
        val list = (0 until segs.length()).mapNotNull { i ->
            val s = segs.getJSONObject(i)
            val range = s.optJSONArray("segment") ?: return@mapNotNull null
            SkipSegment(s.optString("category"), range.optDouble(0), range.optDouble(1))
        }
        return SponsorResult(
            interaction = list.count { it.category == "interaction" },
            sponsor = list.count { it.category == "sponsor" || it.category == "selfpromo" },
            segments = list,
        )
    }

    data class YouTubeFlags(val madeForKids: Boolean?, val paidPlacement: Boolean?)

    /** YouTube Data API flags. Needs the parent's API key; without one both are unknown. */
    suspend fun youtubeFlags(videoId: String, apiKey: String): YouTubeFlags = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext YouTubeFlags(null, null)
        runCatching {
            val url = "https://www.googleapis.com/youtube/v3/videos".toHttpUrl().newBuilder()
                .addQueryParameter("part", "status,paidProductPlacementDetails")
                .addQueryParameter("id", videoId)
                .addQueryParameter("key", apiKey)
                .build()
            Net.client.newCall(Request.Builder().url(url).build()).execute().use { r ->
                if (r.code != 200) return@use YouTubeFlags(null, null)
                val item = JSONObject(r.body?.string() ?: "{}").optJSONArray("items")?.optJSONObject(0)
                    ?: return@use YouTubeFlags(null, null)
                val status = item.optJSONObject("status")
                val paid = item.optJSONObject("paidProductPlacementDetails")
                YouTubeFlags(
                    madeForKids = status?.takeIf { it.has("madeForKids") }?.optBoolean("madeForKids"),
                    paidPlacement = paid?.takeIf { it.has("hasPaidProductPlacement") }?.optBoolean("hasPaidProductPlacement"),
                )
            }
        }.getOrDefault(YouTubeFlags(null, null))
    }

    private fun sha256Hex(s: String): String =
        MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
}
