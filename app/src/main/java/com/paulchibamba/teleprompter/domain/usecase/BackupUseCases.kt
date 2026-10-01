package com.paulchibamba.teleprompter.domain.usecase

import com.paulchibamba.teleprompter.domain.backup.BackupSnapshot
import com.paulchibamba.teleprompter.domain.backup.toBackup
import com.paulchibamba.teleprompter.domain.backup.toPreset
import com.paulchibamba.teleprompter.domain.backup.toScript
import com.paulchibamba.teleprompter.domain.repository.PresetRepository
import com.paulchibamba.teleprompter.domain.repository.ScriptRepository
import com.paulchibamba.teleprompter.domain.repository.SettingsRepository
import com.paulchibamba.teleprompter.domain.text.ScriptParser
import kotlinx.coroutines.flow.first

/** How a restore treats what is already on the device. */
enum class RestoreMode {
    /**
     * Adds what the backup holds without touching anything already here. A script whose id is in
     * both wins from the backup — it is the same script, restored — but nothing is removed.
     */
    MERGE,

    /** The device ends up holding exactly what the backup holds, and nothing else. */
    REPLACE,
}

data class RestoreResult(
    val scriptsRestored: Int,
    val presetsRestored: Int,
)

/**
 * Gathers everything worth keeping into one snapshot (docs/SPEC.md §11.2).
 *
 * Built-in presets are captured too. They cost a few hundred bytes and it means a snapshot is a
 * complete description of the app's state rather than one that depends on the reading build
 * shipping the same three built-ins.
 */
class CreateBackupSnapshot(
    private val scripts: ScriptRepository,
    private val presets: PresetRepository,
    private val settings: SettingsRepository,
) {
    suspend operator fun invoke(nowMillis: Long): BackupSnapshot = BackupSnapshot(
        createdAt = nowMillis,
        scripts = scripts.observeAll().first().map { it.toBackup() },
        presets = presets.observeAll().first().map { it.toBackup() },
        typography = settings.typography.first(),
        layout = settings.layout.first(),
        scroll = settings.scroll.first(),
    )
}

/**
 * Puts a snapshot back.
 *
 * Word counts are **recomputed** from the restored body rather than read from the file. The stored
 * count is derived data, and one written by a build whose counting rules differed would quietly put
 * every duration estimate out.
 *
 * Ids are preserved, which is what reconnects a script to the preset it was assigned.
 */
class RestoreBackupSnapshot(
    private val scripts: ScriptRepository,
    private val presets: PresetRepository,
    private val settings: SettingsRepository,
) {
    suspend operator fun invoke(
        snapshot: BackupSnapshot,
        mode: RestoreMode = RestoreMode.MERGE,
    ): RestoreResult {
        if (mode == RestoreMode.REPLACE) removeEverythingRestorable()

        // Presets first: a script carries a presetId, and restoring it against a preset that is not
        // there yet would leave the assignment dangling for however long the write takes.
        snapshot.presets.filterNot { it.isBuiltIn }.forEach { presets.upsert(it.toPreset()) }
        snapshot.scripts.forEach { backed ->
            scripts.upsert(backed.toScript(wordCount = ScriptParser.wordCount(backed.body)))
        }
        settings.setAll(snapshot.typography, snapshot.layout, snapshot.scroll)

        return RestoreResult(
            scriptsRestored = snapshot.scripts.size,
            presetsRestored = snapshot.presets.count { !it.isBuiltIn },
        )
    }

    /** Built-ins are left alone: they are not the user's to delete, and deleting one is refused. */
    private suspend fun removeEverythingRestorable() {
        scripts.observeAll().first().forEach { scripts.delete(it.id) }
        presets.observeAll().first().filterNot { it.isBuiltIn }.forEach { presets.delete(it.id) }
    }
}
