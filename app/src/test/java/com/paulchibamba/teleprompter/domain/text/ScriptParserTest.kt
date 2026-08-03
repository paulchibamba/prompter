package com.paulchibamba.teleprompter.domain.text

import com.paulchibamba.teleprompter.domain.model.Marker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScriptParserTest {

    private val script = """
        # Cold open
        Good evening, and welcome.

        ## The first beat
        Here is a line of copy.
        ---
        ### A sub-beat
        And the last thing we say.
    """.trimIndent()

    @Test
    fun `markers are parsed with level and label`() {
        assertEquals(
            listOf("Cold open" to 1, "The first beat" to 2, "A sub-beat" to 3),
            ScriptParser.markers(script).map { it.label to it.level },
        )
    }

    @Test
    fun `marker offset points at the first character of its own line`() {
        val markers = ScriptParser.markers(script)
        markers.forEach { marker ->
            assertTrue(
                "marker '${marker.label}' should start at a line boundary",
                marker.charOffset == 0 || script[marker.charOffset - 1] == '\n',
            )
            assertTrue(script.startsWith("#".repeat(marker.level) + " " + marker.label, marker.charOffset))
        }
    }

    @Test
    fun `indented headings are markers but four hashes are not`() {
        val body = "   ## Indented\n#### Too deep\n#NoSpace\n# \n"
        assertEquals(listOf("Indented"), ScriptParser.markers(body).map { it.label })
    }

    @Test
    fun `section breaks are found and are not markers`() {
        assertEquals(1, ScriptParser.sectionBreaks(script).size)
        assertTrue(ScriptParser.markers(script).none { it.label == "---" })
    }

    @Test
    fun `word count counts marker labels but not hashes or section breaks`() {
        // 2 + 4 + 3 + 6 + 2 + 6 words, with the hashes and the `---` contributing nothing.
        assertEquals(23, ScriptParser.wordCount(script))
    }

    @Test
    fun `word count ignores runs of whitespace and empty bodies`() {
        assertEquals(0, ScriptParser.wordCount(""))
        assertEquals(0, ScriptParser.wordCount("   \n\n\t "))
        assertEquals(3, ScriptParser.wordCount("  one   two\t\tthree  "))
    }

    @Test
    fun `paragraphs preserve blank lines as spacers`() {
        // The reference app's defining bug was joining lines without newlines (docs/SPEC.md §3.1).
        val paragraphs = ScriptParser.paragraphs("one\n\ntwo")
        assertEquals(listOf("one", "", "two"), paragraphs)
    }

    @Test
    fun `next and previous marker navigate from an offset`() {
        val markers = ScriptParser.markers(script)
        val middle = markers[1]

        assertEquals(markers[2], markers.nextAfter(middle.charOffset))
        assertEquals(markers[0], markers.previousBefore(middle.charOffset))
        assertNull(markers.nextAfter(markers.last().charOffset))
        assertNull(markers.previousBefore(0))
    }

    @Test
    fun `navigation on a script with no markers returns null`() {
        val none = emptyList<Marker>()
        assertNull(none.nextAfter(0))
        assertNull(none.previousBefore(100))
    }
}
