package org.littlelantern.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionRulesTest {
    private val s = SessionSettings(dailyLimitMs = 60_000, sessionLengthMs = 20_000, restAfterSessionMs = 5_000)

    @Test
    fun pauseUntilSessionLengthReached() {
        assertEquals(AfterVideo.PAUSE, SessionRules.afterVideo(10_000, 10_000, s))
        assertEquals(AfterVideo.END_SESSION, SessionRules.afterVideo(20_000, 30_000, s))
    }

    @Test
    fun dailyLimitWins() {
        assertEquals(AfterVideo.END_DAY, SessionRules.afterVideo(5_000, 60_000, s))
    }

    @Test
    fun restingBlocksNewSession() {
        assertFalse(SessionRules.canStartSession(0, nowMs = 100, restUntilMs = 200, s = s))
        assertTrue(SessionRules.canStartSession(0, nowMs = 200, restUntilMs = 200, s = s))
        assertFalse(SessionRules.canStartSession(60_000, nowMs = 300, restUntilMs = 0, s = s))
    }

    @Test
    fun restUntilTomorrowAfterDailyLimit() {
        assertEquals(999L, SessionRules.restUntil(AfterVideo.END_DAY, 100, 999, s))
        assertEquals(5_100L, SessionRules.restUntil(AfterVideo.END_SESSION, 100, 999, s))
    }

    @Test
    fun choicesRotateWeeklyAndWrap() {
        val lib = listOf("a", "b", "c", "d", "e")
        assertEquals(listOf("a", "b", "c"), SessionRules.choicesForWeek(lib, 3, 0))
        assertEquals(listOf("d", "e", "a"), SessionRules.choicesForWeek(lib, 3, 1))
        assertEquals(listOf("a", "b"), SessionRules.choicesForWeek(listOf("a", "b"), 3, 7))
    }
}
