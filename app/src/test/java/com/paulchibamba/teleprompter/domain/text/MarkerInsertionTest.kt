package com.paulchibamba.teleprompter.domain.text

import org.junit.Assert.assertEquals
import org.junit.Test

class MarkerInsertionTest {

    @Test
    fun `inserts marker at the start of a single line`() {
        val result = MarkerInsertion.insertMarkerAtCaretLine("Opening remarks", caret = 7)

        assertEquals("## Opening remarks", result.text)
    }

    @Test
    fun `moves the caret so it stays on the same character`() {
        val result = MarkerInsertion.insertMarkerAtCaretLine("Opening remarks", caret = 7)

        assertEquals(10, result.caret)
        assertEquals(result.text[result.caret], "## Opening remarks"[10])
    }

    @Test
    fun `marks only the line the caret is on`() {
        val body = "First line\nSecond line\nThird line"
        val caretInSecondLine = body.indexOf("Second") + 3

        val result = MarkerInsertion.insertMarkerAtCaretLine(body, caretInSecondLine)

        assertEquals("First line\n## Second line\nThird line", result.text)
    }

    @Test
    fun `marks the first line when the caret is at the very start`() {
        val result = MarkerInsertion.insertMarkerAtCaretLine("First line\nSecond", caret = 0)

        assertEquals("## First line\nSecond", result.text)
    }

    @Test
    fun `marks the last line when the caret is at the very end`() {
        val body = "First\nLast"

        val result = MarkerInsertion.insertMarkerAtCaretLine(body, caret = body.length)

        assertEquals("First\n## Last", result.text)
    }

    @Test
    fun `marks an empty line`() {
        val result = MarkerInsertion.insertMarkerAtCaretLine("First\n\nThird", caret = 6)

        assertEquals("First\n## \nThird", result.text)
    }

    @Test
    fun `leaves an existing marker alone rather than nesting hashes`() {
        val body = "## Already a marker"

        val result = MarkerInsertion.insertMarkerAtCaretLine(body, caret = 5)

        assertEquals(body, result.text)
        assertEquals(5, result.caret)
    }

    @Test
    fun `leaves markers of every heading level alone`() {
        listOf("# One", "## Two", "### Three").forEach { line ->
            assertEquals(line, MarkerInsertion.insertMarkerAtCaretLine(line, caret = 2).text)
        }
    }

    @Test
    fun `treats a hash without a following space as ordinary text`() {
        val result = MarkerInsertion.insertMarkerAtCaretLine("#hashtag", caret = 0)

        assertEquals("## #hashtag", result.text)
    }

    @Test
    fun `clamps a caret outside the text rather than throwing`() {
        assertEquals("## Body", MarkerInsertion.insertMarkerAtCaretLine("Body", caret = 99).text)
        assertEquals("## Body", MarkerInsertion.insertMarkerAtCaretLine("Body", caret = -5).text)
    }

    @Test
    fun `the result parses back as a marker`() {
        val result = MarkerInsertion.insertMarkerAtCaretLine("Opening remarks", caret = 0)

        val markers = ScriptParser.markers(result.text)

        assertEquals(1, markers.size)
        assertEquals("Opening remarks", markers.first().label)
    }
}
