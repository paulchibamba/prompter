package com.paulchibamba.teleprompter.data.io

import android.net.Uri
import com.paulchibamba.teleprompter.data.prefs.BackupPreferences
import com.paulchibamba.teleprompter.domain.repository.ScriptRepository
import com.paulchibamba.teleprompter.domain.usecase.CreateBackupSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Writes a snapshot whenever the scripts change, without anyone asking (docs/BUILD_PLAN.md, "Data
 * safety").
 *
 * A backup the user has to remember is a backup that is not there on the day it matters. This runs
 * for the life of the process and needs no thought after the folder is chosen once.
 *
 * It watches the **scripts** rather than everything: settings and presets ride along in whatever
 * snapshot the next script edit triggers, and a slider drag should not write a file. The debounce
 * is long for the same reason — typing in the editor autosaves every 500ms, and one snapshot per
 * keystroke would fill the folder with near-identical files and churn the user's storage.
 */
@OptIn(FlowPreview::class)
class AutomaticBackup(
    private val scripts: ScriptRepository,
    private val createSnapshot: CreateBackupSnapshot,
    private val backupStore: BackupStore,
    private val preferences: BackupPreferences,
    private val now: () -> Long = System::currentTimeMillis,
) {

    fun start(scope: CoroutineScope) {
        scope.launch {
            combine(
                // Only the shape of the library matters here, not every keystroke inside a body.
                scripts.observeAll().map { list -> list.map { it.id to it.updatedAt } }.distinctUntilChanged(),
                preferences.state.map { it.folderUri }.distinctUntilChanged(),
            ) { _, folderUri -> folderUri }
                .filterNotNull()
                .debounce(QUIET_PERIOD_MILLIS)
                .collect { folderUri -> writeSnapshot(Uri.parse(folderUri)) }
        }
    }

    /** Writes one now, for the "Back up now" button. @return when it was written, or null if it failed. */
    suspend fun writeSnapshot(treeUri: Uri): Long? {
        if (!backupStore.canStillWriteTo(treeUri)) return null
        val timestamp = now()
        val written = backupStore.writeSnapshot(
            treeUri = treeUri,
            snapshot = createSnapshot(timestamp),
            fileName = fileNameFor(timestamp),
            keepAtMost = SNAPSHOTS_KEPT,
        ) ?: return null
        preferences.setLastBackupAt(timestamp)
        return timestamp.takeIf { written != Uri.EMPTY }
    }

    /** Sortable and readable, so the folder reads as a history rather than a pile. */
    private fun fileNameFor(epochMillis: Long): String =
        "prompter-" + FILE_TIMESTAMP.format(Date(epochMillis)) + BackupStore.BACKUP_EXTENSION

    private companion object {
        /**
         * Long enough that a burst of edits — the editor's own autosave fires every 500ms — settles
         * into one snapshot rather than a folder full of them.
         */
        const val QUIET_PERIOD_MILLIS = 5_000L

        /**
         * Enough history to survive the mistake being noticed late. One file would mean a backup
         * taken straight after an accidental delete overwrites the copy that still had the script.
         */
        const val SNAPSHOTS_KEPT = 10

        val FILE_TIMESTAMP = SimpleDateFormat("yyyy-MM-dd-HHmmss", Locale.US)
    }
}
