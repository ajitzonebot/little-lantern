package org.littlelantern.gate

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Salted PBKDF2 hashing for the 6-digit parent PIN. The PIN itself is never stored. */
object PinHasher {
    const val PIN_LENGTH = 6
    private const val ITERATIONS = 120_000
    private const val KEY_BITS = 256

    fun isValidPin(pin: String): Boolean = pin.length == PIN_LENGTH && pin.all { it in '0'..'9' }

    fun newSalt(): ByteArray = ByteArray(16).also { SecureRandom().nextBytes(it) }

    fun hash(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    fun verify(pin: String, salt: ByteArray, expectedHash: ByteArray): Boolean =
        MessageDigest.isEqual(hash(pin, salt), expectedHash)
}
