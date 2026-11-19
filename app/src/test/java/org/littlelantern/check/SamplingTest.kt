package org.littlelantern.check

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SamplingTest {

    @Test
    fun longVideoIsSampledAcrossItsWholeLength() {
        val d = 23 * 60_000L
        val segs = Sampling.segments(d)
        assertEquals(6, segs.size)
        assertTrue(segs.all { it.lengthMs == 20_000L })
        assertTrue(segs.first().startMs < 3 * 60_000L)
        assertTrue(segs.last().endMs > 19 * 60_000L)
        assertTrue(segs.zipWithNext().all { (a, b) -> a.endMs <= b.startMs })
    }

    @Test
    fun shortVideoUsesShorterSamplesInsideIt() {
        val segs = Sampling.segments(60_000L)
        assertTrue(segs.all { it.lengthMs == 10_000L && it.startMs >= 0 && it.endMs <= 60_000L })
    }

    @Test
    fun unknownDurationGivesNoSamples() {
        assertTrue(Sampling.segments(0).isEmpty())
    }
}
