package org.littlelantern.gate

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PressSequenceDetectorTest {

    @Test
    fun fivePressesWithinWindowTriggers() {
        val d = PressSequenceDetector(requiredPresses = 5, windowMillis = 3_000)
        repeat(4) { assertFalse(d.onPress(1_000L + it * 200)) }
        assertTrue(d.onPress(1_800L))
    }

    @Test
    fun slowPressesNeverTrigger() {
        val d = PressSequenceDetector(requiredPresses = 5, windowMillis = 3_000)
        for (i in 0 until 20) assertFalse(d.onPress(i * 1_000L))
    }

    @Test
    fun resetsAfterTrigger() {
        val d = PressSequenceDetector(requiredPresses = 5, windowMillis = 3_000)
        repeat(4) { d.onPress(it * 100L) }
        assertTrue(d.onPress(400L))
        assertFalse(d.onPress(500L))
    }
}
