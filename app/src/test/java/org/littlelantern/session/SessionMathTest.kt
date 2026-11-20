package org.littlelantern.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class SessionMathTest {

    @Test
    fun calmStartsAfterFifteenMinutesAndEndsAtSixty() {
        assertEquals(0f, SessionMath.calmLevel(0), 0.001f)
        assertEquals(0f, SessionMath.calmLevel(15 * 60), 0.001f)
        assertEquals(1f / 3f, SessionMath.calmLevel(30 * 60), 0.001f)
        assertEquals(1f, SessionMath.calmLevel(60 * 60), 0.001f)
    }

    @Test
    fun leversMatchTheSpecTable() {
        assertEquals(0.65f, SessionMath.brightness(1f), 0.001f)
        assertEquals(0.5f, SessionMath.volume(1f), 0.001f)
        assertEquals(0f, SessionMath.saturation(1f), 0.001f)
        assertNull(SessionMath.choicesShown(0f))
        assertEquals(4, SessionMath.choicesShown(0.4f))
        assertEquals(2, SessionMath.choicesShown(0.8f))
    }

    @Test
    fun videoMustFinishBeforeTheHourIsUp() {
        assertTrue(SessionMath.fits(600, 50 * 60))
        assertFalse(SessionMath.fits(601, 50 * 60))
        assertTrue(SessionMath.isOver(3600))
    }

    @Test
    fun ageBandIsClampedToThreeToFive() {
        val today = LocalDate.of(2026, 11, 20)
        assertEquals(3, SessionMath.ageBand(null, today))
        assertEquals(3, SessionMath.ageBand(YearMonth.of(2024, 6), today))
        assertEquals(4, SessionMath.ageBand(YearMonth.of(2022, 8), today))
        assertEquals(5, SessionMath.ageBand(YearMonth.of(2019, 1), today))
    }
}
