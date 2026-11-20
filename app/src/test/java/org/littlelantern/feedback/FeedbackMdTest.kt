package org.littlelantern.feedback

import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbackMdTest {

    private val doc = """
        # Mockup feedback

        ## Child: videos and session

        ### Child home

        ### Video player

        ## Child: games

        ### Games menu (age 4)

        ### Age 3
        - Shape fit:
        - Colour sort:

        ## Parent mode

        ### Add a video and check

        ## General
    """.trimIndent() + "\n"

    @Test
    fun noteGoesUnderMatchingHeading() {
        val out = FeedbackMd.insert(doc, FeedbackTarget("Child home"), "too bright")
        assertTrue(out.contains("### Child home\n- too bright\n\n### Video player"))
    }

    @Test
    fun headingMatchesByPrefix() {
        val out = FeedbackMd.insert(doc, FeedbackTarget("Games menu"), "tiles small")
        assertTrue(out.contains("### Games menu (age 4)\n- tiles small\n\n### Age 3"))
    }

    @Test
    fun gameNoteGoesUnderItsLineAndKeepsOrder() {
        var out = FeedbackMd.insert(doc, FeedbackTarget("Age 3", "Shape fit"), "first")
        out = FeedbackMd.insert(out, FeedbackTarget("Age 3", "Shape fit"), "second")
        assertTrue(out.contains("- Shape fit:\n  - first\n  - second\n- Colour sort:"))
    }

    @Test
    fun missingItemIsAddedToItsSection() {
        val out = FeedbackMd.insert(doc, FeedbackTarget("Age 3", "Trace letters"), "nice")
        assertTrue(out.contains("- Colour sort:\n- Trace letters: nice\n\n## Parent mode"))
    }

    @Test
    fun parentHeadingNoteStaysAboveSubSections() {
        val out = FeedbackMd.insert(doc, FeedbackTarget("Parent mode"), "PIN slow")
        assertTrue(out.contains("## Parent mode\n- PIN slow\n\n### Add a video and check"))
    }

    @Test
    fun missingHeadingIsAppended() {
        val out = FeedbackMd.insert(doc, FeedbackTarget("PIN entry"), "ok")
        assertTrue(out.endsWith("## General\n\n### PIN entry\n- ok\n"))
    }

    @Test
    fun lastSectionAndMultilineNote() {
        val out = FeedbackMd.insert(doc, FeedbackTarget("General"), "line one\n\n  line two ")
        assertTrue(out.endsWith("## General\n- line one line two\n"))
    }

    @Test
    fun emptyFileGetsHeading() {
        val out = FeedbackMd.insert(FeedbackMd.EMPTY, FeedbackTarget("Child home"), "hi")
        assertTrue(out.endsWith("\n### Child home\n- hi\n"))
    }
}
