package com.paulchibamba.teleprompter.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.paulchibamba.teleprompter.domain.model.BuiltInPresets
import com.paulchibamba.teleprompter.domain.model.Preset
import com.paulchibamba.teleprompter.domain.model.TypographySettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The preset half of the database, against real SQLite under Robolectric — same reasoning as
 * `ScriptDaoTest`. What matters here beyond the SQL is that seeding is genuinely idempotent and
 * that a built-in cannot be deleted, because both are load-bearing: the app assumes there is always
 * at least one preset to fall back to.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PresetDaoTest {

    private lateinit var database: PrompterDatabase
    private lateinit var dao: PresetDao
    private lateinit var repository: RoomPresetRepository

    @Before
    fun openDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            PrompterDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.presetDao()
        repository = RoomPresetRepository(dao)
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `the three built-ins are seeded on first read`() = runTest {
        val names = repository.observeAll().first().map { it.name }

        assertEquals(listOf("Studio", "Bright room", "Tight glass"), names)
    }

    @Test
    fun `seeding twice does not duplicate or reset anything`() = runTest {
        repository.ensureBuiltIns()
        repository.upsert(BuiltInPresets.studio.copy(name = "Studio (edited)"))

        repository.ensureBuiltIns()

        assertEquals(BuiltInPresets.all.size, dao.count())
        assertEquals("Studio (edited)", repository.byId(BuiltInPresets.STUDIO_ID)!!.name)
    }

    @Test
    fun `a built-in round-trips its settings through the JSON columns`() = runTest {
        val brightRoom = repository.byId(BuiltInPresets.BRIGHT_ROOM_ID)!!

        assertEquals(BuiltInPresets.brightRoom, brightRoom)
        assertEquals(TypographySettings.ATKINSON_FONT_ID, brightRoom.typography.fontId)
        assertEquals(88f, brightRoom.typography.sizeSp, 0f)
        assertEquals(700, brightRoom.typography.weight)
        assertEquals(0.02f, brightRoom.typography.letterSpacingEm, 1e-6f)
    }

    @Test
    fun `a user preset gets an id above the built-ins and sorts after them by name`() = runTest {
        repository.ensureBuiltIns()

        val id = repository.upsert(Preset(name = "Album commentary"))

        assertTrue("user ids must not collide with built-ins, got $id", id >= BuiltInPresets.FIRST_USER_ID)
        assertEquals(
            listOf("Studio", "Bright room", "Tight glass", "Album commentary"),
            repository.observeAll().first().map { it.name },
        )
    }

    @Test
    fun `user presets sort alphabetically regardless of case`() = runTest {
        repository.upsert(Preset(name = "zoom call"))
        repository.upsert(Preset(name = "Album commentary"))
        repository.upsert(Preset(name = "backyard"))

        val userNames = repository.observeAll().first().filterNot { it.isBuiltIn }.map { it.name }
        assertEquals(listOf("Album commentary", "backyard", "zoom call"), userNames)
    }

    @Test
    fun `upsert on an existing id updates in place and returns that id`() = runTest {
        val id = repository.upsert(Preset(name = "Draft"))

        val returned = repository.upsert(repository.byId(id)!!.copy(name = "Final"))

        assertEquals(id, returned)
        assertEquals("Final", repository.byId(id)!!.name)
    }

    @Test
    fun `an out-of-range setting is coerced before it is stored`() = runTest {
        val id = repository.upsert(
            Preset(name = "Enormous", typography = TypographySettings(sizeSp = 9000f)),
        )

        assertEquals(TypographySettings.MAX_SIZE_SP, repository.byId(id)!!.typography.sizeSp, 0f)
    }

    @Test
    fun `deleting a user preset removes it`() = runTest {
        val id = repository.upsert(Preset(name = "Temporary"))

        assertTrue(repository.delete(id))
        assertNull(repository.byId(id))
    }

    @Test
    fun `deleting a built-in is refused and leaves the row alone`() = runTest {
        repository.ensureBuiltIns()

        assertFalse(repository.delete(BuiltInPresets.STUDIO_ID))
        assertNotNull(repository.byId(BuiltInPresets.STUDIO_ID))
    }
}
