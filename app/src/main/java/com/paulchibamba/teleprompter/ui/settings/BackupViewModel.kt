package com.paulchibamba.teleprompter.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.paulchibamba.teleprompter.data.io.AutomaticBackup
import com.paulchibamba.teleprompter.data.io.BackupOutcome
import com.paulchibamba.teleprompter.data.io.BackupStore
import com.paulchibamba.teleprompter.data.io.StoredBackup
import com.paulchibamba.teleprompter.data.prefs.BackupPreferences
import com.paulchibamba.teleprompter.domain.usecase.CreateBackupSnapshot
import com.paulchibamba.teleprompter.domain.usecase.RestoreBackupSnapshot
import com.paulchibamba.teleprompter.domain.usecase.RestoreMode
import com.paulchibamba.teleprompter.ui.prompterContainer
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BackupUiState(
    val folderUri: String? = null,
    val folderLabel: String? = null,
    val lastBackupAt: Long = 0L,
    /** Snapshots found in the chosen folder, newest first. */
    val snapshots: List<StoredBackup> = emptyList(),
    val isBusy: Boolean = false,
) {
    val isAutomaticBackupOn: Boolean get() = folderUri != null
}

sealed interface BackupEvent {
    data class BackupWritten(val at: Long) : BackupEvent

    /** Asked for a backup with an empty library — most likely straight after a reinstall. */
    data object NothingToBackUp : BackupEvent

    data class Restored(val scripts: Int, val presets: Int) : BackupEvent

    data class Failed(val reason: String) : BackupEvent
}

/**
 * Choosing where snapshots go, and getting them back (docs/BUILD_PLAN.md, "Data safety").
 *
 * Restore defaults to merging rather than replacing. Someone restoring has usually just lost
 * something, and the destructive reading of "restore" would let a stale backup delete the work that
 * survived — the failure this whole step exists to prevent, arriving through the recovery path.
 */
class BackupViewModel(
    private val backupStore: BackupStore,
    private val preferences: BackupPreferences,
    private val automaticBackup: AutomaticBackup,
    private val createSnapshot: CreateBackupSnapshot,
    private val restoreSnapshot: RestoreBackupSnapshot,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState = _uiState.asStateFlow()

    private val events = Channel<BackupEvent>(Channel.BUFFERED)
    val eventStream: Flow<BackupEvent> = events.receiveAsFlow()

    init {
        observePreferences()
    }

    /** The user picked a folder in the system picker. */
    fun chooseFolder(treeUri: Uri) {
        viewModelScope.launch {
            if (!backupStore.rememberFolder(treeUri)) {
                events.send(BackupEvent.Failed("That folder could not be used."))
                return@launch
            }
            preferences.setFolder(treeUri.toString())
            // Written against the folder just granted rather than the one in state, which arrives
            // back asynchronously through preferences and may not be there yet. Choosing the folder
            // is the moment the user decided their work matters, so a snapshot now honours that —
            // and with an empty library nothing is written, which is what leaves the snapshots
            // already in the folder as the newest for restore to offer.
            backUpTo(treeUri)
        }
    }

    fun forgetFolder() {
        viewModelScope.launch { preferences.setFolder(null) }
    }

    fun backUpNow() {
        val folderUri = _uiState.value.folderUri ?: return
        viewModelScope.launch { backUpTo(Uri.parse(folderUri)) }
    }

    private suspend fun backUpTo(treeUri: Uri) {
        _uiState.update { it.copy(isBusy = true) }
        val outcome = automaticBackup.writeSnapshot(treeUri)
        _uiState.update { it.copy(isBusy = false) }
        when (outcome) {
            is BackupOutcome.Written -> {
                events.send(BackupEvent.BackupWritten(outcome.at))
                refreshSnapshots(treeUri)
            }
            // The folder is still listed: this is the reinstall path, and what is in there is
            // precisely what the user came to the screen for.
            BackupOutcome.NothingToBackUp -> {
                refreshSnapshots(treeUri)
                events.send(BackupEvent.NothingToBackUp)
            }
            BackupOutcome.Failed ->
                events.send(BackupEvent.Failed("Could not write to the backup folder."))
        }
    }

    /** Restores a file the user picked, or one of the snapshots listed from the folder. */
    fun restoreFrom(fileUri: Uri, mode: RestoreMode = RestoreMode.MERGE) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val snapshot = backupStore.readSnapshot(fileUri)
            if (snapshot == null) {
                _uiState.update { it.copy(isBusy = false) }
                events.send(BackupEvent.Failed("That file is not a Prompter backup this version can read."))
                return@launch
            }
            val result = restoreSnapshot(snapshot, mode)
            _uiState.update { it.copy(isBusy = false) }
            events.send(BackupEvent.Restored(result.scriptsRestored, result.presetsRestored))
        }
    }

    /** Writes a snapshot to a single file the user named, for keeping a copy somewhere else. */
    fun exportTo(fileUri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val written = backupStore.writeSnapshotTo(fileUri, createSnapshot(System.currentTimeMillis()))
            _uiState.update { it.copy(isBusy = false) }
            if (written) {
                events.send(BackupEvent.BackupWritten(System.currentTimeMillis()))
            } else {
                events.send(BackupEvent.Failed("Could not write that file."))
            }
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferences.state.collect { stored ->
                _uiState.update {
                    it.copy(
                        folderUri = stored.folderUri,
                        folderLabel = stored.folderUri?.let(::readableFolderName),
                        lastBackupAt = stored.lastBackupAt,
                    )
                }
                stored.folderUri?.let { refreshSnapshots(Uri.parse(it)) }
            }
        }
    }

    private suspend fun refreshSnapshots(treeUri: Uri) {
        val found = backupStore.listSnapshots(treeUri)
        _uiState.update { it.copy(snapshots = found) }
    }

    /**
     * A tree URI is unreadable to a human. The last path segment is the closest thing to the folder
     * name the user picked, so it beats showing them `content://com.android.externalstorage…`.
     */
    private fun readableFolderName(treeUri: String): String =
        Uri.decode(treeUri).substringAfterLast('/').substringAfterLast(':').ifBlank { "Chosen folder" }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = prompterContainer()
                BackupViewModel(
                    backupStore = container.backupStore,
                    preferences = container.backupPreferences,
                    automaticBackup = container.automaticBackup,
                    createSnapshot = container.createBackupSnapshot,
                    restoreSnapshot = container.restoreBackupSnapshot,
                )
            }
        }
    }
}
