package com.paulchibamba.teleprompter.domain.usecase

import com.paulchibamba.teleprompter.domain.model.Script
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScriptUseCasesTest {

    private val repository = FakeScriptRepository()
    private var clock = 1_000L
    private val saveScript = SaveScript(repository, now = { clock })

    @Test
    fun `saving a new script stamps both timestamps and counts words`() = runTest {
        val id = saveScript(draft(body = "One two three\n\n## A cue\nfour five"))

        val saved = repository.byId(id)!!
        assertEquals(1_000L, saved.createdAt)
        assertEquals(1_000L, saved.updatedAt)
        assertEquals(7, saved.wordCount) // the hashes do not count, the label does
        assertNotEquals(0L, saved.id)
    }

    @Test
    fun `saving an existing script keeps createdAt and moves updatedAt`() = runTest {
        val id = saveScript(draft(body = "first"))
        clock = 5_000L

        saveScript(repository.byId(id)!!.copy(body = "first and second"))

        val saved = repository.byId(id)!!
        assertEquals(1_000L, saved.createdAt)
        assertEquals(5_000L, saved.updatedAt)
        assertEquals(3, saved.wordCount)
    }

    @Test
    fun `a blank title is derived from the first line of the body`() = runTest {
        val id = saveScript(draft(title = "   ", body = "\n---\n## Cold open\nGood evening."))
        assertEquals("Cold open", repository.byId(id)!!.title)
    }

    @Test
    fun `a derived title is truncated and an empty body falls back to untitled`() = runTest {
        val long = "word ".repeat(40)
        val truncated = repository.byId(saveScript(draft(title = "", body = long)))!!.title
        assertTrue(truncated.endsWith("…"))
        assertTrue("'$truncated' is longer than the cap", truncated.length <= SaveScript.MAX_DERIVED_TITLE + 1)

        val empty = repository.byId(saveScript(draft(title = "", body = "  \n\n")))!!.title
        assertEquals(SaveScript.UNTITLED, empty)
    }

    @Test
    fun `an explicit title is trimmed but kept`() = runTest {
        val id = saveScript(draft(title = "  Opening monologue  ", body = "anything"))
        assertEquals("Opening monologue", repository.byId(id)!!.title)
    }

    @Test
    fun `delete then restore brings the script back with the same id`() = runTest {
        val id = saveScript(draft(title = "Keep me", body = "body"))
        val script = repository.byId(id)!!

        DeleteScript(repository)(script)
        assertNull(repository.byId(id))

        RestoreScript(repository)(script)
        assertEquals(script, repository.byId(id))
    }

    @Test
    fun `duplicating copies the body under a new id and clears the resume point`() = runTest {
        val id = saveScript(draft(title = "Original", body = "the body"))
        SaveScrollPosition(repository)(id, 4_200f)
        clock = 9_000L

        val copyId = DuplicateScript(repository, saveScript)(id)!!

        val original = repository.byId(id)!!
        val copy = repository.byId(copyId)!!
        assertNotEquals(id, copyId)
        assertEquals("Original ${DuplicateScript.COPY_SUFFIX}", copy.title)
        assertEquals(original.body, copy.body)
        assertEquals(0f, copy.lastPositionPx, 0f)
        assertEquals(9_000L, copy.createdAt)
        assertEquals(4_200f, original.lastPositionPx, 0f) // the original is untouched
    }

    @Test
    fun `duplicating a script that no longer exists returns null`() = runTest {
        assertNull(DuplicateScript(repository, saveScript)(404L))
    }

    @Test
    fun `saving a scroll position does not move updatedAt`() = runTest {
        val id = saveScript(draft(body = "body"))
        clock = 7_000L

        SaveScrollPosition(repository)(id, 1_234f)

        val saved = repository.byId(id)!!
        assertEquals(1_000L, saved.updatedAt)
        assertEquals(1_234f, saved.lastPositionPx, 0f)
    }

    @Test
    fun `a negative scroll position is clamped to the top`() = runTest {
        val id = saveScript(draft(body = "body"))
        SaveScrollPosition(repository)(id, -50f)
        assertEquals(0f, repository.byId(id)!!.lastPositionPx, 0f)
    }

    @Test
    fun `reordering assigns sort indices in the order given`() = runTest {
        val first = saveScript(draft(title = "A", body = "a"))
        val second = saveScript(draft(title = "B", body = "b"))
        val third = saveScript(draft(title = "C", body = "c"))

        ReorderScripts(repository)(listOf(third, first, second))

        assertEquals(listOf("C", "A", "B"), repository.current.map { it.title })
    }

    @Test
    fun `observing with a blank query returns everything`() = runTest {
        saveScript(draft(title = "A", body = "alpha"))
        saveScript(draft(title = "B", body = "beta"))

        assertEquals(2, ObserveScripts(repository)().first().size)
        assertEquals(2, ObserveScripts(repository)("   ").first().size)
    }

    @Test
    fun `observing with a query matches title and body`() = runTest {
        saveScript(draft(title = "Keynote", body = "nothing relevant"))
        saveScript(draft(title = "Other", body = "mentions the keynote in passing"))
        saveScript(draft(title = "Third", body = "unrelated"))

        val matches = ObserveScripts(repository)("keynote").first()
        assertEquals(listOf("Keynote", "Other"), matches.map { it.title })
    }

    @Test
    fun `getting a missing script returns null`() = runTest {
        assertNull(GetScript(repository)(404L))
    }

    private fun draft(title: String = "Draft", body: String = ""): Script = Script(
        title = title,
        body = body,
        createdAt = 0L,
        updatedAt = 0L,
        wordCount = 0,
    )
}
