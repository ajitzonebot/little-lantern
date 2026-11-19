package org.littlelantern.feedback

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Turns a feedback note into a GitHub issue title and body. Pure, unit tested. */
object FeedbackFormat {

    private val time = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    fun title(f: Feedback): String {
        val firstLine = f.text.lineSequence().firstOrNull { it.isNotBlank() }?.trim() ?: "Feedback"
        val short = if (firstLine.length > 70) firstLine.take(67).trimEnd() + "..." else firstLine
        return "[${f.kind.label}] ${f.screen}: $short"
    }

    fun body(f: Feedback, appVersion: String, device: String, zone: ZoneId = ZoneId.systemDefault()): String = buildString {
        appendLine(f.text.trim())
        appendLine()
        appendLine("---")
        appendLine("- Screen: ${f.screen}")
        appendLine("- Type: ${f.kind.label}")
        appendLine("- Written: ${time.format(Instant.ofEpochMilli(f.createdAt).atZone(zone))}")
        appendLine("- App version: $appVersion")
        appendLine("- Device: $device")
        appendLine()
        append("_Sent from the in-app feedback button._")
    }

    fun labels(f: Feedback): List<String> = listOf("feedback", f.kind.name.lowercase())

    /** All unsent notes as plain text, for copying when GitHub sending is not set up. */
    fun asText(items: List<Feedback>, appVersion: String, device: String): String =
        items.joinToString("\n\n") { "## ${title(it)}\n\n${body(it, appVersion, device)}" }
}
