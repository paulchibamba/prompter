package com.paulchibamba.teleprompter.domain.text

import com.paulchibamba.teleprompter.domain.model.Marker

/**
 * Derives everything structural about a script from its body text. Markers are never stored, so
 * editing a heading updates the cue list on the next parse with nothing to keep in sync
 * (docs/SPEC.md §3.2).
 *
 * All of this is pure and cheap enough to run on save and on import; nothing here touches Android.
 */
object ScriptParser {

    /** Markdown-style heading: up to three hashes, whitespace, then a non-empty label. */
    private val MARKER_REGEX = Regex("""^\s*(#{1,3})\s+(\S.*)$""")

    /** A line that is exactly three hyphens (ignoring surrounding whitespace) is a section break. */
    private const val SECTION_BREAK = "---"

    /**
     * Every cue marker in [body], in reading order. `charOffset` points at the first character of
     * the marker's line, so scrolling to a marker lands on the heading itself rather than after it.
     */
    fun markers(body: String): List<Marker> = buildList {
        forEachLine(body) { line, offset ->
            val match = MARKER_REGEX.find(line) ?: return@forEachLine
            val (hashes, label) = match.destructured
            add(Marker(charOffset = offset, label = label.trim(), level = hashes.length))
        }
    }

    /** Character offsets of the hard section breaks in [body], in reading order. */
    fun sectionBreaks(body: String): List<Int> = buildList {
        forEachLine(body) { line, offset ->
            if (line.trim() == SECTION_BREAK) add(offset)
        }
    }

    /**
     * Words in [body], as used for the library's duration estimate and the WPM scroll maths.
     *
     * A word is any run of non-whitespace. Section-break lines contribute nothing — they are
     * punctuation for the eye, not something anyone reads aloud. Marker headings *do* count their
     * label (they render as normal text by default, §3.2) but not their hashes.
     */
    fun wordCount(body: String): Int {
        var count = 0
        forEachLine(body) { line, _ ->
            val trimmed = line.trim()
            if (trimmed == SECTION_BREAK) return@forEachLine
            val spoken = MARKER_REGEX.find(line)?.destructured?.component2() ?: trimmed
            if (spoken.isNotEmpty()) count += spoken.split(WHITESPACE).count { it.isNotEmpty() }
        }
        return count
    }

    /**
     * Whether [line] is a cue marker, for deciding how to draw it. The same regex that finds
     * markers for jumping decides how they look, so the two can never disagree.
     */
    fun isMarkerLine(line: String): Boolean = MARKER_REGEX.containsMatchIn(line)

    /**
     * The body split for rendering: one entry per line, empty lines preserved as spacers. The
     * prompter renders one `LazyColumn` item per entry rather than one giant text node (§8.3).
     */
    fun paragraphs(body: String): List<String> = body.split("\n")

    private val WHITESPACE = Regex("""\s+""")

    /** Walks [body] line by line, handing each line and the offset of its first character to [action]. */
    private inline fun forEachLine(body: String, action: (line: String, offset: Int) -> Unit) {
        var offset = 0
        while (offset <= body.length) {
            val newline = body.indexOf('\n', offset)
            val end = if (newline == -1) body.length else newline
            action(body.substring(offset, end), offset)
            if (newline == -1) return
            offset = newline + 1
        }
    }
}

/** The first marker starting strictly after [charOffset], or null at the end of the script. */
fun List<Marker>.nextAfter(charOffset: Int): Marker? = firstOrNull { it.charOffset > charOffset }

/** The last marker starting strictly before [charOffset], or null at the start of the script. */
fun List<Marker>.previousBefore(charOffset: Int): Marker? = lastOrNull { it.charOffset < charOffset }
