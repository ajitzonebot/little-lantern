package org.littlelantern.feedback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset

class FeedbackFormatTest {

    private val f = Feedback(
        id = "1",
        createdAt = 0,
        screen = "Child: Pause",
        kind = FeedbackKind.BUG,
        text = "\nButtons appear too late\nMy kid waited and got upset",
    )

    @Test
    fun titleUsesKindScreenAndFirstLine() {
        assertEquals("[Bug] Child: Pause: Buttons appear too late", FeedbackFormat.title(f))
    }

    @Test
    fun longTitlesAreShortened() {
        val long = f.copy(text = "x".repeat(200))
        assertTrue(FeedbackFormat.title(long).endsWith("..."))
        assertTrue(FeedbackFormat.title(long).length < 110)
    }

    @Test
    fun bodyHasContext() {
        val body = FeedbackFormat.body(f, "0.2.0", "Pixel 6, Android 14", ZoneOffset.UTC)
        assertTrue(body.contains("My kid waited and got upset"))
        assertTrue(body.contains("- Screen: Child: Pause"))
        assertTrue(body.contains("- Written: 1970-01-01 00:00"))
        assertTrue(body.contains("- App version: 0.2.0"))
    }

    @Test
    fun labels() {
        assertEquals(listOf("feedback", "bug"), FeedbackFormat.labels(f))
    }
}
