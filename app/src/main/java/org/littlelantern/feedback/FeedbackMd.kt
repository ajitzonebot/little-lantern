package org.littlelantern.feedback

/** Where a note belongs in feedback.md: a heading, and optionally a "- item:" line under it. */
data class FeedbackTarget(val section: String, val item: String? = null) {
    val label: String get() = if (item == null) section else "$section: $item"
}

/** Adds notes to feedback.md under the matching heading. Pure, unit tested. */
object FeedbackMd {

    const val EMPTY = "# Feedback\n\nNotes sent from the app's Feedback button.\n"

    /**
     * Inserts one bullet.
     * - Heading found (any level, text starts with [FeedbackTarget.section]): the note goes at the end of that
     *   heading's own text, before the next heading.
     * - Item line "- <item>:" found inside it: the note goes as a sub-bullet under that line.
     * - Item missing: a new "- <item>: note" line at the end of the section.
     * - Heading missing: a new "### <section>" at the end of the file.
     */
    fun insert(markdown: String, target: FeedbackTarget, note: String): String {
        val text = note.lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" ")
        val lines = markdown.trimEnd('\n').lines().toMutableList()

        val head = lines.indexOfFirst { isHeading(it) && headingText(it).startsWith(target.section, ignoreCase = true) }
        if (head < 0) {
            lines += ""
            lines += "### ${target.section}"
            lines += if (target.item != null) "- ${target.item}: $text" else "- $text"
            return lines.joinToString("\n") + "\n"
        }
        // The section's own text runs to the next heading of any level, so a note for "## Parent mode"
        // lands above its "###" sub-sections rather than inside the last one.
        val end = (head + 1 until lines.size).firstOrNull { isHeading(lines[it]) } ?: lines.size

        if (target.item != null) {
            val prefix = "- ${target.item}:"
            val itemLine = (head + 1 until end).firstOrNull { lines[it].trim().startsWith(prefix, ignoreCase = true) }
            if (itemLine != null) {
                var at = itemLine + 1
                while (at < end && lines[at].startsWith("  ")) at++
                lines.add(at, "  - $text")
                return lines.joinToString("\n") + "\n"
            }
            return insertAtSectionEnd(lines, head, end, "- ${target.item}: $text")
        }
        return insertAtSectionEnd(lines, head, end, "- $text")
    }

    private fun insertAtSectionEnd(lines: MutableList<String>, head: Int, end: Int, bullet: String): String {
        var at = end
        while (at - 1 > head && lines[at - 1].isBlank()) at--
        lines.add(at, bullet)
        // Keep a blank line before the next heading.
        if (at + 1 < lines.size && isHeading(lines[at + 1])) lines.add(at + 1, "")
        return lines.joinToString("\n") + "\n"
    }

    private fun isHeading(line: String) = line.startsWith("#") && line.trimStart('#').startsWith(" ")
    private fun headingText(line: String) = line.trimStart('#').trim()
}
