package com.paulchibamba.teleprompter.domain.usecase

import com.paulchibamba.teleprompter.data.json.BackupCodec
import com.paulchibamba.teleprompter.domain.model.BuiltInPresets
import com.paulchibamba.teleprompter.domain.model.Preset
import com.paulchibamba.teleprompter.domain.model.Script
import com.paulchibamba.teleprompter.domain.model.ScrollSettings
import com.paulchibamba.teleprompter.domain.model.TypographySettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A backup is the only copy of a user's writing that survives an uninstall, so these tests are
 * about one question: does everything come back, exactly as it went in.
 */
class BackupUseCasesTest {

    @Test
    fun `a backup restores every script byte for byte, newlines included`() = runTest {
        val body = "First line\n\nSecond paragraph\n## A marker\nTrailing line\n"
        val (scripts, presets, settings) = repositories()
        scripts.upsert(scriptOf(id = 1L, title = "Keynote", body = body))

        val snapshot = CreateBackupSnapshot(scripts, presets, settings)(nowMillis = 1_000L)
        val restored = intoEmptyDevice(snapshot)

        assertEquals(body, restored.byId(1L)!!.body)
        assertEquals("Keynote", restored.byId(1L)!!.title)
    }

    @Test
    fun `a backup survives being written to a file and read back`() = runTest {
        val (scripts, presets, settings) = repositories()
        scripts.upsert(scriptOf(id = 7L, title = "Documentary", body = "The river\nhas carved"))
        presets.upsert(Preset(id = 9L, name = "Podcast", scroll = ScrollSettings(speedWpm = 200)))
        settings.setTypography(TypographySettings(sizeSp = 64f))

        val snapshot = CreateBackupSnapshot(scripts, presets, settings)(nowMillis = 42L)
        val reopened = BackupCodec.decode(BackupCodec.encode(snapshot))

        assertNotNull(reopened)
        assertEquals(snapshot, reopened)
    }

    @Test
    fun `restoring keeps the preset a script was assigned`() = runTest {
        val (scripts, presets, settings) = repositories()
        presets.upsert(Preset(id = 9L, name = "Podcast"))
        scripts.upsert(scriptOf(id = 1L, title = "Assigned", body = "x").copy(presetId = 9L))

        val snapshot = CreateBackupSnapshot(scripts, presets, settings)(nowMillis = 0L)
        val restored = intoEmptyDevice(snapshot)

        assertEquals(9L, restored.byId(1L)!!.presetId)
    }

    @Test
    fun `word counts are recomputed on restore rather than trusted from the file`() = runTest {
        val (scripts, presets, settings) = repositories()
        // A count that is plainly wrong, as a build with different counting rules might have written.
        scripts.upsert(scriptOf(id = 1L, title = "Wrong", body = "one two three").copy(wordCount = 999))

        val snapshot = CreateBackupSnapshot(scripts, presets, settings)(nowMillis = 0L)
        val restored = intoEmptyDevice(snapshot)

        assertEquals(3, restored.byId(1L)!!.wordCount)
    }

    @Test
    fun `restoring merges by default, so an old backup cannot delete newer work`() = runTest {
        val (scripts, presets, settings) = repositories()
        scripts.upsert(scriptOf(id = 1L, title = "In the backup", body = "old"))
        val snapshot = CreateBackupSnapshot(scripts, presets, settings)(nowMillis = 0L)

        // The device has moved on since: one script gone, another written that the backup never saw.
        scripts.delete(1L)
        scripts.upsert(scriptOf(id = 2L, title = "Written since", body = "new"))

        RestoreBackupSnapshot(scripts, presets, settings)(snapshot)

        val titles = scripts.observeAll().first().map { it.title }
        assertTrue("the backup's script should come back", "In the backup" in titles)
        assertTrue("newer work must survive a restore", "Written since" in titles)
    }

    @Test
    fun `replacing leaves the device holding exactly what the backup held`() = runTest {
        val (scripts, presets, settings) = repositories()
        scripts.upsert(scriptOf(id = 1L, title = "In the backup", body = "old"))
        val snapshot = CreateBackupSnapshot(scripts, presets, settings)(nowMillis = 0L)
        scripts.upsert(scriptOf(id = 2L, title = "Written since", body = "new"))

        RestoreBackupSnapshot(scripts, presets, settings)(snapshot, RestoreMode.REPLACE)

        assertEquals(listOf("In the backup"), scripts.observeAll().first().map { it.title })
    }

    @Test
    fun `built-in presets are not duplicated by a restore`() = runTest {
        val (scripts, presets, settings) = repositories()
        presets.ensureBuiltIns()
        val snapshot = CreateBackupSnapshot(scripts, presets, settings)(nowMillis = 0L)

        RestoreBackupSnapshot(scripts, presets, settings)(snapshot)

        assertEquals(BuiltInPresets.all.size, presets.observeAll().first().size)
    }

    /**
     * The predicate the automatic backup checks before writing. A snapshot of a freshly reinstalled
     * device holds nothing to recover, and writing it would make it the newest file in the folder —
     * so "restore the newest backup" would restore nothing while the real snapshots sat behind it.
     */
    @Test
    fun `a snapshot of a device with no scripts holds nothing worth writing`() = runTest {
        val (scripts, presets, settings) = repositories()
        presets.ensureBuiltIns()

        val snapshot = CreateBackupSnapshot(scripts, presets, settings)(nowMillis = 0L)

        assertTrue(snapshot.isEmpty)
    }

    @Test
    fun `a snapshot is worth writing as soon as there is one script`() = runTest {
        val (scripts, presets, settings) = repositories()
        scripts.upsert(scriptOf(id = 1L, title = "Only one", body = "a"))

        val snapshot = CreateBackupSnapshot(scripts, presets, settings)(nowMillis = 0L)

        assertFalse(snapshot.isEmpty)
    }

    /** A preset the user made is their work too, even with no scripts written yet. */
    @Test
    fun `a snapshot is worth writing for a preset the user made`() = runTest {
        val (scripts, presets, settings) = repositories()
        presets.ensureBuiltIns()
        presets.upsert(Preset(id = 9L, name = "Podcast"))

        val snapshot = CreateBackupSnapshot(scripts, presets, settings)(nowMillis = 0L)

        assertFalse(snapshot.isEmpty)
    }

    @Test
    fun `a file that is not a backup is refused rather than half-restored`() {
        assertNull(BackupCodec.decode("not json at all"))
        assertNull(BackupCodec.decode("""{"unrelated":"object"}"""))
    }

    @Test
    fun `a backup from a newer version is refused rather than silently losing its fields`() {
        val fromTheFuture = """{"version":9999,"createdAt":0,"scripts":[],"presets":[]}"""

        assertNull(BackupCodec.decode(fromTheFuture))
    }

    private fun repositories() =
        Triple(FakeScriptRepository(), FakePresetRepository(), FakeSettingsRepository())

    /** Restores into a device that holds nothing, which is the situation after a reinstall. */
    private suspend fun intoEmptyDevice(
        snapshot: com.paulchibamba.teleprompter.domain.backup.BackupSnapshot,
    ): FakeScriptRepository {
        val fresh = FakeScriptRepository()
        val freshPresets = FakePresetRepository()
        RestoreBackupSnapshot(fresh, freshPresets, FakeSettingsRepository())(snapshot)
        return fresh
    }

    private fun scriptOf(id: Long, title: String, body: String) = Script(
        id = id,
        title = title,
        body = body,
        createdAt = 1L,
        updatedAt = 2L,
        wordCount = body.split(" ").size,
    )
}
