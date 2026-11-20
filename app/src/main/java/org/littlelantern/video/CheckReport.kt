package org.littlelantern.video

import org.json.JSONArray
import org.json.JSONObject

/** A part of a video to skip during playback (SponsorBlock "like and subscribe", sponsor, self-promo). */
data class SkipSegment(val category: String, val startSeconds: Double, val endSeconds: Double) {
    fun toJson(): JSONObject = JSONObject().put("c", category).put("s", startSeconds).put("e", endSeconds)

    companion object {
        fun fromJson(o: JSONObject) = SkipSegment(o.optString("c"), o.optDouble("s"), o.optDouble("e"))
    }
}

/** Three-way result: a check can pass, flag, or be unknown. Unknown is never shown as a pass. */
enum class Verdict { PASS, FLAG, UNKNOWN }

/**
 * Everything the add-video check found. Null numbers mean the measure could not run.
 * Pace thresholds lean on Lillard and Peterson 2011: the slow show cut about every 34 s (1.8 per minute),
 * the fast one about every 11 s (5.5 per minute). They are not validated cut-offs.
 */
data class CheckReport(
    val cutsPerMinute: Double? = null,
    val flashesPerSecondMax: Int? = null,
    val redFlashesPerSecondMax: Int? = null,
    val saturation: Double? = null,
    val loudnessJumps: Int? = null,
    val analysisError: String? = null,
    val interactionSegments: Int? = null,
    val sponsorSegments: Int? = null,
    val paidPlacement: Boolean? = null,
    val madeForKids: Boolean? = null,
    val keywordHits: List<String>? = null,
    val parentFantasy: Boolean = false,
    val parentUrging: Boolean = false,
) {
    val paceLabel: String
        get() {
            val c = cutsPerMinute ?: return "Unknown"
            return if (c <= SLOW_MAX) "Slow" else if (c <= FAST_MIN) "Medium" else "Fast"
        }

    val paceVerdict: Verdict
        get() = when (val c = cutsPerMinute) {
            null -> Verdict.UNKNOWN
            else -> if (c > FAST_MIN) Verdict.FLAG else Verdict.PASS
        }

    val flashVerdict: Verdict
        get() = when (val f = flashesPerSecondMax) {
            null -> Verdict.UNKNOWN
            else -> if (f > FLASH_LIMIT) Verdict.FLAG else Verdict.PASS
        }

    val redFlashVerdict: Verdict
        get() = when (val f = redFlashesPerSecondMax) {
            null -> Verdict.UNKNOWN
            else -> if (f > FLASH_LIMIT) Verdict.FLAG else Verdict.PASS
        }

    val saturationLabel: String
        get() = when (val s = saturation) {
            null -> "Unknown"
            else -> if (s < 0.12) "Low" else if (s < 0.25) "Medium" else "High"
        }

    fun toJson(): JSONObject = JSONObject().apply {
        put("cpm", cutsPerMinute ?: JSONObject.NULL)
        put("fps", flashesPerSecondMax ?: JSONObject.NULL)
        put("rfps", redFlashesPerSecondMax ?: JSONObject.NULL)
        put("sat", saturation ?: JSONObject.NULL)
        put("loud", loudnessJumps ?: JSONObject.NULL)
        put("err", analysisError ?: JSONObject.NULL)
        put("sbi", interactionSegments ?: JSONObject.NULL)
        put("sbs", sponsorSegments ?: JSONObject.NULL)
        put("paid", paidPlacement ?: JSONObject.NULL)
        put("kids", madeForKids ?: JSONObject.NULL)
        put("kw", keywordHits?.let { JSONArray(it) } ?: JSONObject.NULL)
        put("fantasy", parentFantasy)
        put("urging", parentUrging)
    }

    companion object {
        const val SLOW_MAX = 2.5
        const val FAST_MIN = 5.0
        /** More than 3 flashes in any one second is the broadcast and WCAG limit. */
        const val FLASH_LIMIT = 3

        fun fromJson(o: JSONObject): CheckReport {
            fun d(k: String) = if (o.isNull(k) || !o.has(k)) null else o.optDouble(k)
            fun i(k: String) = if (o.isNull(k) || !o.has(k)) null else o.optInt(k)
            fun b(k: String) = if (o.isNull(k) || !o.has(k)) null else o.optBoolean(k)
            val kw = o.optJSONArray("kw")?.let { a -> (0 until a.length()).map { a.getString(it) } }
            return CheckReport(
                cutsPerMinute = d("cpm"),
                flashesPerSecondMax = i("fps"),
                redFlashesPerSecondMax = i("rfps"),
                saturation = d("sat"),
                loudnessJumps = i("loud"),
                analysisError = if (o.isNull("err") || !o.has("err")) null else o.optString("err"),
                interactionSegments = i("sbi"),
                sponsorSegments = i("sbs"),
                paidPlacement = b("paid"),
                madeForKids = b("kids"),
                keywordHits = kw,
                parentFantasy = o.optBoolean("fantasy"),
                parentUrging = o.optBoolean("urging"),
            )
        }
    }
}
