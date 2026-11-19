package org.littlelantern.data

import android.content.Context
import org.littlelantern.gate.PinHasher
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

    private companion object {
        const val KEY_PIN_HASH = "pin_hash"
        const val KEY_PIN_SALT = "pin_salt"
        const val KEY_ALLOWED_GAMES = "allowed_games"
    }
}
