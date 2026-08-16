package com.paulchibamba.teleprompter.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.paulchibamba.teleprompter.data.io.BackupStore
import java.text.DateFormat
import java.util.Date

/**
 * Backup and restore (docs/BUILD_PLAN.md, "Data safety").
 *
 * The screen leads with the one thing that matters — whether anything is protecting this library
 * right now — because a backup feature nobody switched on is the same as no backup feature.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onNavigateBack: () -> Unit,
    viewModel: BackupViewModel = viewModel(factory = BackupViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val chooseFolder = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { treeUri -> treeUri?.let(viewModel::chooseFolder) }

    val chooseFileToRestore = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { fileUri -> fileUri?.let { viewModel.restoreFrom(it) } }

    val chooseWhereToExport = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BackupStore.BACKUP_MIME_TYPE),
    ) { fileUri -> fileUri?.let(viewModel::exportTo) }

    BackupSnackbars(viewModel, snackbarHostState)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (uiState.isBusy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            AutomaticBackupSection(
                uiState = uiState,
                onChooseFolder = { chooseFolder.launch(null) },
                onBackUpNow = viewModel::backUpNow,
                onForgetFolder = viewModel::forgetFolder,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            RestoreSection(
                snapshotCount = uiState.snapshots.size,
                onRestoreNewest = {
                    uiState.snapshots.firstOrNull()?.let { viewModel.restoreFrom(it.uri) }
                },
                onRestoreFromFile = { chooseFileToRestore.launch(arrayOf("*/*")) },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SaveACopySection(onExport = { chooseWhereToExport.launch(suggestedExportName()) })
        }
    }
}

@Composable
private fun AutomaticBackupSection(
    uiState: BackupUiState,
    onChooseFolder: () -> Unit,
    onBackUpNow: () -> Unit,
    onForgetFolder: () -> Unit,
) {
    Text(text = "Automatic backup", style = MaterialTheme.typography.titleMedium)

    if (!uiState.isAutomaticBackupOn) {
        // Stated plainly rather than softened: this is the state in which work gets lost.
        Text(
            text = "Off. Nothing on this phone is protected — uninstalling the app would take every " +
                "script with it. Choose a folder and Prompter will keep a copy there whenever you " +
                "change a script.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        Button(onClick = onChooseFolder) { Text("Choose a backup folder…") }
        return
    }

    Text(
        text = "Saving to ${uiState.folderLabel}",
        style = MaterialTheme.typography.bodyMedium,
    )
    Text(
        text = uiState.lastBackupAt.asLastBackupLine(),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        text = "Keeps the last 10 copies. They stay in that folder if the app is uninstalled — " +
            "after reinstalling, come back here and choose the same folder to get them back.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onBackUpNow) { Text("Back up now") }
        OutlinedButton(onClick = onChooseFolder) { Text("Change folder") }
    }
    TextButton(onClick = onForgetFolder) { Text("Turn off automatic backup") }
}

@Composable
private fun RestoreSection(
    snapshotCount: Int,
    onRestoreNewest: () -> Unit,
    onRestoreFromFile: () -> Unit,
) {
    Text(text = "Restore", style = MaterialTheme.typography.titleMedium)
    Text(
        text = "Restoring adds what the backup holds. Nothing already on this phone is deleted, so " +
            "restoring an old copy can never cost you newer work.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (snapshotCount > 0) {
        Button(onClick = onRestoreNewest) { Text("Restore the newest backup") }
    }
    OutlinedButton(onClick = onRestoreFromFile) { Text("Restore from a file…") }
}

@Composable
private fun SaveACopySection(onExport: () -> Unit) {
    Text(text = "Save a copy", style = MaterialTheme.typography.titleMedium)
    Text(
        text = "Writes everything to a single file you choose — for keeping somewhere else, or " +
            "moving to another phone.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OutlinedButton(onClick = onExport) { Text("Save a backup file…") }
}

@Composable
private fun BackupSnackbars(viewModel: BackupViewModel, snackbarHostState: SnackbarHostState) {
    LaunchedEffect(viewModel) {
        viewModel.eventStream.collect { event ->
            val message = when (event) {
                is BackupEvent.BackupWritten -> "Backed up"
                is BackupEvent.Restored -> restoredMessage(event)
                is BackupEvent.Failed -> event.reason
            }
            snackbarHostState.showSnackbar(message)
        }
    }
}

private fun restoredMessage(event: BackupEvent.Restored): String {
    val scripts = if (event.scripts == 1) "1 script" else "${event.scripts} scripts"
    return if (event.presets == 0) {
        "Restored $scripts"
    } else {
        val presets = if (event.presets == 1) "1 preset" else "${event.presets} presets"
        "Restored $scripts and $presets"
    }
}

private fun Long.asLastBackupLine(): String =
    if (this <= 0L) {
        "No backup written yet."
    } else {
        "Last backup " + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(this))
    }

private fun suggestedExportName(): String =
    "prompter-backup" + BackupStore.BACKUP_EXTENSION
