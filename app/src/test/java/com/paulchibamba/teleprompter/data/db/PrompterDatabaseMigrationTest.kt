package com.paulchibamba.teleprompter.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.paulchibamba.teleprompter.domain.model.BuiltInPresets
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Upgrades a real version-1 database file to version 2 and checks that nothing was lost.
 *
 * This is worth the setup below because Room *validates the schema when it opens*: if
 * [PrompterDatabase.MIGRATION_1_2]'s `CREATE TABLE` differs from the table Room would generate — a
 * missing `NOT NULL`, a column in a different order — opening the migrated file throws
 * `IllegalStateException`. So the assertion that the script survived is almost incidental; the test
 * mostly exists to make that open happen at all.
 *
 * The version-1 file is built by hand rather than with `MigrationTestHelper`, which reads its
 * schemas from instrumentation assets and so needs an emulator. Room recognises a database as
 * version 1 by three things: the `user_version` pragma, the tables themselves, and the identity
 * hash it stores in `room_master_table`. All three come from `app/schemas/…/1.json` — if a future
 * step changes the version-1 schema (it should not; that schema is shipped), take the new hash from
 * there.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PrompterDatabaseMigrationTest {

    private lateinit var databaseFile: File

    @Before
    fun removeAnyExistingFile() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        databaseFile = context.getDatabasePath(TEST_DB_NAME)
        databaseFile.parentFile?.mkdirs()
        context.deleteDatabase(TEST_DB_NAME)
    }

    @After
    fun removeFile() {
        ApplicationProvider.getApplicationContext<Context>().deleteDatabase(TEST_DB_NAME)
    }

    @Test
    fun `migrating from version 1 keeps existing scripts and adds the presets table`() = runTest {
        createVersion1DatabaseWithOneScript()

        val database = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            PrompterDatabase::class.java,
            TEST_DB_NAME,
        ).addMigrations(*PrompterDatabase.MIGRATIONS).allowMainThreadQueries().build()

        try {
            val scripts = database.scriptDao().observeAll().first()
            assertEquals(1, scripts.size)
            assertEquals("Keynote", scripts.single().title)
            assertEquals("Line one\nLine two", scripts.single().body)

            // The new table is not just present but usable, seeding included.
            val presets = RoomPresetRepository(database.presetDao()).observeAll().first()
            assertEquals(BuiltInPresets.all.map { it.name }, presets.map { it.name })
        } finally {
            database.close()
        }
    }

    /** The `scripts` table exactly as version 1 shipped it, plus the metadata Room looks for. */
    private fun createVersion1DatabaseWithOneScript() {
        val database = SQLiteDatabase.openOrCreateDatabase(databaseFile, null)
        try {
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `scripts` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`title` TEXT NOT NULL, " +
                    "`body` TEXT NOT NULL, " +
                    "`createdAt` INTEGER NOT NULL, " +
                    "`updatedAt` INTEGER NOT NULL, " +
                    "`wordCount` INTEGER NOT NULL, " +
                    "`lastPositionPx` REAL NOT NULL, " +
                    "`presetId` INTEGER, " +
                    "`sortIndex` INTEGER NOT NULL)",
            )
            database.execSQL(
                "INSERT INTO scripts (title, body, createdAt, updatedAt, wordCount, lastPositionPx, sortIndex) " +
                    "VALUES ('Keynote', 'Line one\nLine two', 1, 1, 4, 0.0, 0)",
            )
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS room_master_table " +
                    "(id INTEGER PRIMARY KEY, identity_hash TEXT)",
            )
            database.execSQL(
                "INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (42, '$VERSION_1_IDENTITY_HASH')",
            )
            database.version = 1
        } finally {
            database.close()
        }
    }

    private companion object {
        const val TEST_DB_NAME = "migration-test.db"

        /** `database.identityHash` from `app/schemas/…PrompterDatabase/1.json`. */
        const val VERSION_1_IDENTITY_HASH = "406306b61fae208d7c484353068272bc"
    }
}
