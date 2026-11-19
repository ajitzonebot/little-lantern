package org.littlelantern.gate

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {

    @Test
    fun validatesSixDigits() {
        assertTrue(PinHasher.isValidPin("123456"))
        assertFalse(PinHasher.isValidPin("12345"))
        assertFalse(PinHasher.isValidPin("12345a"))
    }

    @Test
    fun verifiesCorrectPinOnly() {
        val salt = PinHasher.newSalt()
        val hash = PinHasher.hash("246810", salt)
        assertTrue(PinHasher.verify("246810", salt, hash))
        assertFalse(PinHasher.verify("246811", salt, hash))
    }

    @Test
    fun differentSaltsGiveDifferentHashes() {
        val a = PinHasher.hash("111111", PinHasher.newSalt())
        val b = PinHasher.hash("111111", PinHasher.newSalt())
        assertFalse(a.contentEquals(b))
    }
}
