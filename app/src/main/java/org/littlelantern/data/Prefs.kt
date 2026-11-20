package org.littlelantern.data

import android.content.Context
import org.littlelantern.gate.PinHasher
import org.littlelantern.session.SessionMath
import java.time.LocalDate
import java.time.YearMonth
import java.util.Base64

/** Small local settings store. Only feedback notes leave the device, and only to the GitHub repository the parent sets. */
class Prefs(context: Context) {

    private val prefs = context.getSharedPreferences("little_lantern", Context.MODE_PRIVATE)
    private val encoder = Base64.getEncoder()
    private val decoder = Base64.getDecoder()

    val hasPin: Boolean
        get() = prefs.contains(KEY_PIN_HASH) && prefs.contains(KEY_PIN_SALT)

    fun setPin(pin: String) {
        require(PinHasher.isValidPin(pin)) { "PIN must be ${PinHasher.PIN_LENGTH} digits" }
        val salt = PinHasher.newSalt()
        prefs.edit()
            .putString(KEY_PIN_SALT, encoder.encodeToString(salt))
            .putString(KEY_PIN_HASH, encoder.encodeToString(PinHasher.hash(pin, salt)))
            .apply()
    }

    fun checkPin(pin: String): Boolean {
        val salt = prefs.getString(KEY_PIN_SALT, null) ?: return false
        val hash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        return PinHasher.verify(pin, decoder.decode(salt), decoder.decode(hash))
    }

    /** Child's birth month, used to pick the games for their age. Null until the parent sets it. */
    var birthMonth: YearMonth?
        get() = prefs.getString(KEY_BIRTH_MONTH, null)?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
        set(value) {
            prefs.edit().putString(KEY_BIRTH_MONTH, value?.toString()).apply()
        }

    /** Highest playback volume the parent allows, 0..1. The session lowers it further toward the end. */
    var volumeCap: Float
        get() = prefs.getFloat(KEY_VOLUME_CAP, 0.7f)
        set(value) {
            prefs.edit().putFloat(KEY_VOLUME_CAP, value.coerceIn(0.1f, 1f)).apply()
        }

    /** When on, colour-only games are hidden and colour sorting uses shapes. */
    var colourBlindMode: Boolean
        get() = prefs.getBoolean(KEY_COLOUR_BLIND, false)
        set(value) {
            prefs.edit().putBoolean(KEY_COLOUR_BLIND, value).apply()
        }

    /** Optional YouTube Data API key for the paid-placement and made-for-kids checks. */
    var youtubeApiKey: String
        get() = prefs.getString(KEY_YT_API, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_YT_API, value.trim()).apply()
        }

    /** Games the parent switched off. */
    var disabledGames: Set<String>
        get() = prefs.getStringSet(KEY_DISABLED_GAMES, emptySet())?.toSet() ?: emptySet()
        set(value) {
            prefs.edit().putStringSet(KEY_DISABLED_GAMES, value).apply()
        }

    /** Small Feedback button on every screen (for testing). */
    var showFeedback: Boolean
        get() = prefs.getBoolean(KEY_SHOW_FEEDBACK, true)
        set(value) {
            prefs.edit().putBoolean(KEY_SHOW_FEEDBACK, value).apply()
        }

    /** GitHub repository (owner/name) whose feedback.md receives notes. */
    var feedbackRepo: String
        get() = prefs.getString(KEY_FB_REPO, DEFAULT_FEEDBACK_REPO) ?: DEFAULT_FEEDBACK_REPO
        set(value) {
            prefs.edit().putString(KEY_FB_REPO, value.trim()).apply()
        }

    /** Branch for feedback.md. Blank means the repository's default branch. */
    var feedbackBranch: String
        get() = prefs.getString(KEY_FB_BRANCH, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_FB_BRANCH, value.trim()).apply()
        }

    /** Fine-grained token with Contents: read and write on that repository only. Stored in app-private storage. */
    var feedbackToken: String
        get() = prefs.getString(KEY_FB_TOKEN, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_FB_TOKEN, value.trim()).apply()
        }

    /** Seconds of Child mode used today. Resets when the date changes. */
    fun usedSecondsToday(today: LocalDate = LocalDate.now()): Int {
        if (prefs.getString(KEY_USAGE_DATE, null) != today.toString()) return 0
        return prefs.getInt(KEY_USAGE_SECONDS, 0)
    }

    fun addUsage(seconds: Int, today: LocalDate = LocalDate.now()): Int {
        val total = (usedSecondsToday(today) + seconds).coerceAtMost(SessionMath.LIMIT_SECONDS)
        prefs.edit()
            .putString(KEY_USAGE_DATE, today.toString())
            .putInt(KEY_USAGE_SECONDS, total)
            .apply()
        return total
    }

    private companion object {
        const val KEY_PIN_HASH = "pin_hash"
        const val KEY_PIN_SALT = "pin_salt"
        const val KEY_BIRTH_MONTH = "birth_month"
        const val KEY_VOLUME_CAP = "volume_cap"
        const val KEY_COLOUR_BLIND = "colour_blind"
        const val KEY_YT_API = "youtube_api_key"
        const val KEY_DISABLED_GAMES = "disabled_games"
        const val KEY_USAGE_DATE = "usage_date"
        const val KEY_USAGE_SECONDS = "usage_seconds"
        const val KEY_SHOW_FEEDBACK = "show_feedback"
        const val KEY_FB_REPO = "feedback_repo"
        const val KEY_FB_BRANCH = "feedback_branch"
        const val KEY_FB_TOKEN = "feedback_token"
        const val DEFAULT_FEEDBACK_REPO = "ajitzonebot/little-lantern"
    }
}
