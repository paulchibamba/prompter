package com.paulchibamba.teleprompter.domain.text

/**
 * The result of an edit: the new text, and where the caret should end up.
 *
 * Returning the caret alongside the text matters — an edit that inserts characters before the
 * caret and leaves it where it was would silently move it backwards through the user's own words.
 */
data class TextEdit(val text: String, val caret: Int)

/**
 * Turns the line the caret is on into a cue marker (docs/SPEC.md §5.2).
 *
 * Markers are ordinary Markdown-style headings in the body text — nothing is stored separately —
 * so "insert marker" is literally putting `## ` at the start of a line.
 */
object MarkerInsertion {

    private const val MARKER_PREFIX = "## "

    /** A line that already opens with one to three hashes and a space is already a marker. */
    private val EXISTING_MARKER = Regex("""^\s*#{1,3}\s""")

    /**
     * Inserts [MARKER_PREFIX] at the start of the line containing [caret].
     *
     * Pressing the button twice on the same line is a no-op rather than producing `## ## `, because
     * the second press is far more likely to be a mis-tap than a request for a deeper heading.
     */
    fun insertMarkerAtCaretLine(text: String, caret: Int): TextEdit {
        val safeCaret = caret.coerceIn(0, text.length)
        val lineStart = startOfLineContaining(text, safeCaret)

        if (isAlreadyMarker(text, lineStart)) return TextEdit(text, safeCaret)

        return TextEdit(
            text = text.substring(0, lineStart) + MARKER_PREFIX + text.substring(lineStart),
            caret = safeCaret + MARKER_PREFIX.length,
        )
    }

    private fun startOfLineContaining(text: String, caret: Int): Int =
        text.lastIndexOf('\n', (caret - 1).coerceAtLeast(0))
            .let { newline -> if (newline == -1 || caret == 0) 0 else newline + 1 }

    private fun isAlreadyMarker(text: String, lineStart: Int): Boolean {
        val lineEnd = text.indexOf('\n', lineStart).let { if (it == -1) text.length else it }
        return EXISTING_MARKER.containsMatchIn(text.substring(lineStart, lineEnd))
    }
}
