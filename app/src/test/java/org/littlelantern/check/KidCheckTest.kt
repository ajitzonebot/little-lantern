package org.littlelantern.check

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KidCheckTest {

    private fun metrics(
        cuts: Double = 4.0,
        flashes: Double = 0.5,
        sat: Double = 0.3,
        loud: Double = 0.5,
        minutes: Long = 5,
        hints: List<String> = emptyList(),
        parentFantastical: Boolean = false,
    ) = Metrics(cuts, flashes, sat, loud, minutes * 60_000, hints, parentFantastical)

    @Test
    fun slowShortVideoIsCalm() {
        assertEquals(Verdict.CALM, KidCheck.evaluate(metrics()).verdict)
    }

    @Test
    fun oneHighFactorNeedsChecking() {
        assertEquals(Verdict.CHECK, KidCheck.evaluate(metrics(cuts = 25.0)).verdict)
    }

    @Test
    fun fastLoudVideoIsNotRecommended() {
        assertEquals(Verdict.NOT_RECOMMENDED, KidCheck.evaluate(metrics(cuts = 25.0, loud = 6.0)).verdict)
    }

    @Test
    fun parentMarkIsHigh() {
        val report = KidCheck.evaluate(metrics(parentFantastical = true))
        assertEquals(Level.HIGH, report.items.last().level)
    }

    @Test
    fun findsHintWords() {
        val hints = KidCheck.fantasyHints("SURPRISE EGGS compilation!", "Superhero magic + more")
        assertTrue(hints.containsAll(listOf("surprise", "eggs", "compilation", "superhero", "magic", "+ more")))
        assertTrue(KidCheck.fantasyHints("Ducks at the pond", "A calm walk").isEmpty())
    }

    @Test
    fun unmeasuredIsCaution() {
        val report = KidCheck.evaluate(Metrics(null, null, null, null, 60_000, emptyList(), false))
        assertEquals(Level.CAUTION, report.items.first().level)
    }
}
