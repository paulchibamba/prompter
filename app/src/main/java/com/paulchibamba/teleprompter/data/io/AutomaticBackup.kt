package com.paulchibamba.teleprompter.data.io

import android.net.Uri
import com.paulchibamba.teleprompter.data.prefs.BackupPreferences
import com.paulchibamba.teleprompter.domain.repository.ScriptRepository
import com.paulchibamba.teleprompter.domain.usecase.CreateBackupSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** What came of asking for a snapshot. */
sealed interface BackupOutcome {

    data class Written(val at: Long) : BackupOutcome

    /**
     * Nothing was written, because there is nothing on the device to lose. Distinct from [Failed]:
     * telling the user a write failed when the truth is that their library is empty would send them
     * looking for a problem with the folder.
     */
    data object NothingToBackUp : BackupOutcome

    data object Failed : BackupOutcome
}

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
            scripts.observeAll()
                // Only the shape of the library matters here, not every keystroke inside a body.
                .map { list -> list.map { it.id to it.updatedAt } }
                .distinctUntilChanged()
                // The first emission is the library as it already is, not a change to it. Writing on
                // every launch would spend the ten-snapshot history on ten copies of one state, and
                // push out the older ones that are the reason for keeping a history at all.
                .drop(1)
                .debounce(QUIET_PERIOD_MILLIS)
                .collect { backUpToChosenFolder() }
        }
    }

    /**
     * The folder is read here rather than being a second trigger alongside the scripts. Combining
     * the two meant *choosing* a folder also scheduled a snapshot, which on a fresh install wrote an
     * empty one — see [writeSnapshot].
     */
    private suspend fun backUpToChosenFolder() {
        val folderUri = preferences.state.first().folderUri ?: return
        writeSnapshot(Uri.parse(folderUri))
    }

    /** Writes one now, for the "Back up now" button and for choosing a folder. */
    suspend fun writeSnapshot(treeUri: Uri): BackupOutcome {
        if (!backupStore.canStillWriteTo(treeUri)) return BackupOutcome.Failed

        val timestamp = now()
        val snapshot = createSnapshot(timestamp)
        // An empty snapshot is not a backup: there is nothing in it to recover, and being the newest
        // file in the folder it is what "restore the newest backup" would offer. Writing one is at
        // its most tempting exactly when it does the most harm — straight after a reinstall, library
        // empty, with the real snapshots sitting right there — so recovery would be broken by the
        // feature meant to provide it.
        if (snapshot.isEmpty) return BackupOutcome.NothingToBackUp

        val written = backupStore.writeSnapshot(
            treeUri = treeUri,
            snapshot = snapshot,
            fileName = fileNameFor(timestamp),
            keepAtMost = SNAPSHOTS_KEPT,
        )
        if (written == null || written == Uri.EMPTY) return BackupOutcome.Failed

        // Only once the file is actually there. A "Last backup" time the user can read, standing for
        // a write that did not happen, is worse than admitting there has been no backup yet.
        preferences.setLastBackupAt(timestamp)
        return BackupOutcome.Written(timestamp)
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
