package org.littlelantern.video

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectorsTest {

    private fun hist(bin: Int) = DoubleArray(16).also { it[bin] = 1.0 }

    @Test
    fun countsHardCutsButNotSteadyFrames() {
        val c = CutCounter()
        var t = 0.0
        repeat(30) { c.onFrame(hist(2), t); t += 1 / 30.0 }
        c.onFrame(hist(12), t) // cut
        t += 1 / 30.0
        repeat(30) { c.onFrame(hist(12), t); t += 1 / 30.0 }
        assertEquals(1, c.cuts)
        assertEquals(30.0, c.perMinute(2.0)!!, 0.001)
    }

    @Test
    fun flagsFastFlashing() {
        val f = FlashCounter()
        var t = 0.0
        // 5 dark-bright cycles in one second.
        repeat(10) { i -> f.onFrame(if (i % 2 == 0) 0.1 else 0.6, t); t += 0.1 }
        assertTrue(f.maxFlashesPerSecond > CheckReport.FLASH_LIMIT)
    }

    @Test
    fun ignoresSlowFades() {
        val f = FlashCounter()
        var t = 0.0
        repeat(300) { i -> f.onFrame(0.2 + 0.4 * i / 300.0, t); t += 1 / 30.0 }
        assertEquals(0, f.maxFlashesPerSecond)
    }

    @Test
    fun countsLoudnessJumps() {
        val l = LoudnessJumpCounter()
        repeat(40) { l.onWindow(-40.0) }
        l.onWindow(-10.0)
        repeat(40) { l.onWindow(-40.0) }
        assertEquals(1, l.jumps)
    }

    @Test
    fun keywordScanFindsLures() {
        assertEquals(listOf("subscribe", "surprise"), KeywordScan.hits("SURPRISE eggs! Please subscribe"))
    }

    @Test
    fun parsesSponsorBlockResponse() {
        val body = """[{"videoID":"other","segments":[]},
            {"videoID":"abc","segments":[
              {"category":"interaction","segment":[10.0,15.5]},
              {"category":"sponsor","segment":[30.0,60.0]}]}]"""
        val r = OnlineChecks.parseSponsorBlock(body, "abc")
        assertEquals(1, r.interaction)
        assertEquals(1, r.sponsor)
        assertEquals(15.5, r.segments.first().endSeconds, 0.001)
    }
}
