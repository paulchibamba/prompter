package com.paulchibamba.teleprompter.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.paulchibamba.teleprompter.domain.model.Script
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Exercises the real SQL against an in-memory SQLite database. Robolectric lets this run in the
 * normal unit-test task, so CI covers the queries without an emulator — worth it, because the DAO's
 * ordering and `LIKE` behaviour is the sort of thing that is only wrong once it ships.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScriptDaoTest {

    private lateinit var database: PrompterDatabase
    private lateinit var dao: ScriptDao
    private lateinit var repository: RoomScriptRepository

    @Before
    fun openDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            PrompterDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.scriptDao()
        repository = RoomScriptRepository(dao)
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `upsert assigns an id and round-trips every field`() = runTest {
        val id = repository.upsert(script(title = "Keynote", body = "Line one\nLine two", updatedAt = 10L))

        val loaded = repository.byId(id)!!
        assertEquals(id, loaded.id)
        assertEquals("Keynote", loaded.title)
        assertEquals("Line one\nLine two", loaded.body) // newlines survive storage (docs/SPEC.md §3.1)
        assertEquals(10L, loaded.updatedAt)
    }

    @Test
    fun `upsert on an existing id updates in place and returns that id`() = runTest {
        val id = repository.upsert(script(title = "Draft"))

        val returned = repository.upsert(repository.byId(id)!!.copy(title = "Final"))

        assertEquals(id, returned)
        assertEquals(1, dao.count())
        assertEquals("Final", repository.byId(id)!!.title)
    }

    @Test
    fun `observeAll orders by sortIndex then most recently updated`() = runTest {
        repository.upsert(script(title = "Older", sortIndex = 1, updatedAt = 100L))
        repository.upsert(script(title = "Newer", sortIndex = 1, updatedAt = 200L))
        repository.upsert(script(title = "Pinned", sortIndex = 0, updatedAt = 1L))

        assertEquals(
            listOf("Pinned", "Newer", "Older"),
            repository.observeAll().first().map { it.title },
        )
    }

    @Test
    fun `search matches title and body and ignores case`() = runTest {
        repository.upsert(script(title = "Keynote", body = "nothing relevant"))
        repository.upsert(script(title = "Other", body = "mentions the KEYNOTE in passing"))
        repository.upsert(script(title = "Third", body = "unrelated"))

        val titles = repository.search("keynote").first().map { it.title }
        assertEquals(listOf("Keynote", "Other"), titles)
    }

    @Test
    fun `a blank search returns everything`() = runTest {
        repository.upsert(script(title = "A"))
        repository.upsert(script(title = "B"))

        assertEquals(2, repository.search("   ").first().size)
    }

    @Test
    fun `savePosition writes only the resume point`() = runTest {
        val id = repository.upsert(script(title = "Keynote", updatedAt = 10L))

        repository.savePosition(id, 812.5f)

        val loaded = repository.byId(id)!!
        assertEquals(812.5f, loaded.lastPositionPx, 0f)
        assertEquals(10L, loaded.updatedAt)
    }

    @Test
    fun `setOrder rewrites every sort index`() = runTest {
        val a = repository.upsert(script(title = "A", sortIndex = 0))
        val b = repository.upsert(script(title = "B", sortIndex = 1))
        val c = repository.upsert(script(title = "C", sortIndex = 2))

        repository.setOrder(listOf(c, a, b))

        assertEquals(listOf("C", "A", "B"), repository.observeAll().first().map { it.title })
    }

    @Test
    fun `delete removes the row`() = runTest {
        val id = repository.upsert(script(title = "Temporary"))

        repository.delete(id)

        assertNull(repository.byId(id))
        assertEquals(0, dao.count())
    }

    private fun script(
        title: String = "Script",
        body: String = "body",
        updatedAt: Long = 0L,
        sortIndex: Int = 0,
    ) = Script(
        title = title,
        body = body,
        createdAt = 0L,
        updatedAt = updatedAt,
        wordCount = 0,
        sortIndex = sortIndex,
    )
}
