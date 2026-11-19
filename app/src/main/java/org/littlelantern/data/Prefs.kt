package org.littlelantern.data

import android.content.Context
import org.littlelantern.gate.PinHasher
import org.littlelantern.session.SessionSettings
import java.time.LocalDate
import java.util.Base64

/** Small local settings store. Nothing leaves the device. */
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

    var allowedGames: Set<String>
        get() = prefs.getStringSet(KEY_ALLOWED_GAMES, emptySet())?.toSet() ?: emptySet()
        set(value) {
            prefs.edit().putStringSet(KEY_ALLOWED_GAMES, value).apply()
        }

    var dailyLimitMin: Int
        get() = prefs.getInt(KEY_DAILY_LIMIT, 60)
        set(value) = prefs.edit().putInt(KEY_DAILY_LIMIT, value).apply()

    var sessionMin: Int
        get() = prefs.getInt(KEY_SESSION, 15)
        set(value) = prefs.edit().putInt(KEY_SESSION, value).apply()

    var restMin: Int
        get() = prefs.getInt(KEY_REST, 60)
        set(value) = prefs.edit().putInt(KEY_REST, value).apply()

    fun sessionSettings() = SessionSettings(
        dailyLimitMs = dailyLimitMin * SessionSettings.MINUTE,
        sessionLengthMs = sessionMin * SessionSettings.MINUTE,
        restAfterSessionMs = restMin * SessionSettings.MINUTE,
    )

    /** Video time watched today; resets at midnight. */
    val todayWatchedMs: Long
        get() = if (prefs.getString(KEY_USAGE_DAY, null) == LocalDate.now().toString()) prefs.getLong(KEY_USAGE_MS, 0) else 0

    fun addWatched(ms: Long) {
        prefs.edit()
            .putString(KEY_USAGE_DAY, LocalDate.now().toString())
            .putLong(KEY_USAGE_MS, todayWatchedMs + ms)
            .apply()
    }

    /** Wall-clock time (ms) before which Child mode rests. */
    var restUntil: Long
        get() = prefs.getLong(KEY_REST_UNTIL, 0)
        set(value) = prefs.edit().putLong(KEY_REST_UNTIL, value).apply()

    /** Times the child chose "All done" (a validation metric). */
    var voluntaryStops: Int
        get() = prefs.getInt(KEY_VOLUNTARY_STOPS, 0)
        set(value) = prefs.edit().putInt(KEY_VOLUNTARY_STOPS, value).apply()

    /** Feedback button on every screen; on while testing. */
    var showFeedbackButton: Boolean
        get() = prefs.getBoolean(KEY_SHOW_FEEDBACK, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_FEEDBACK, value).apply()

    var githubRepo: String
        get() = prefs.getString(KEY_GITHUB_REPO, null) ?: "ajitzonebot/little-lantern"
        set(value) = prefs.edit().putString(KEY_GITHUB_REPO, value.trim()).apply()

    /** Personal access token, stored only on this phone (app-private storage). */
    var githubToken: String
        get() = prefs.getString(KEY_GITHUB_TOKEN, null) ?: ""
        set(value) = prefs.edit().putString(KEY_GITHUB_TOKEN, value.trim()).apply()

    private companion object {
        const val KEY_SHOW_FEEDBACK = "show_feedback"
        const val KEY_GITHUB_REPO = "github_repo"
        const val KEY_GITHUB_TOKEN = "github_token"
        const val KEY_DAILY_LIMIT = "daily_limit_min"
        const val KEY_SESSION = "session_min"
        const val KEY_REST = "rest_min"
        const val KEY_USAGE_DAY = "usage_day"
        const val KEY_USAGE_MS = "usage_ms"
        const val KEY_REST_UNTIL = "rest_until"
        const val KEY_VOLUNTARY_STOPS = "voluntary_stops"
        const val KEY_PIN_HASH = "pin_hash"
        const val KEY_PIN_SALT = "pin_salt"
        const val KEY_ALLOWED_GAMES = "allowed_games"
    }
}
