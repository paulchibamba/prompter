package com.paulchibamba.teleprompter.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.paulchibamba.teleprompter.domain.model.Preset
import com.paulchibamba.teleprompter.ui.prompter.NamePresetDialog

/**
 * Create, rename, apply and delete presets (docs/SPEC.md §5.4).
 *
 * Tapping a row applies it — that is the common action by a wide margin, and burying it in an
 * overflow menu would be the wrong way round. Everything that changes a preset rather than uses one
 * lives in the menu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetsScreen(
    onNavigateBack: () -> Unit,
    viewModel: PresetsViewModel = viewModel(factory = PresetsViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var isNamingNewPreset by remember { mutableStateOf(false) }
    var presetBeingRenamed by remember { mutableStateOf<PresetRowUi?>(null) }

    PresetsSnackbars(
        events = viewModel,
        snackbarHostState = snackbarHostState,
        onUndoDeletion = viewModel::undoDeletion,
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Presets") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { isNamingNewPreset = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Save current settings as preset")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { contentPadding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
            items(uiState.rows, key = { it.id }) { row ->
                PresetRow(
                    row = row,
                    onApply = { viewModel.applyPreset(row.id) },
                    onRename = { presetBeingRenamed = row },
                    onDuplicate = { viewModel.duplicatePreset(row.id) },
                    onUpdateFromCurrentSettings = { viewModel.updateFromCurrentSettings(row.id) },
                    onDelete = { viewModel.deletePreset(row.id) },
                )
                HorizontalDivider()
            }
        }
    }

    if (isNamingNewPreset) {
        NamePresetDialog(
            title = "Save current settings",
            initialName = "",
            confirmLabel = "Save",
            onConfirm = {
                viewModel.saveCurrentSettingsAsPreset(it)
                isNamingNewPreset = false
            },
            onDismiss = { isNamingNewPreset = false },
        )
    }

    presetBeingRenamed?.let { row ->
        NamePresetDialog(
            title = "Rename preset",
            initialName = row.name,
            confirmLabel = "Rename",
            onConfirm = {
                viewModel.renamePreset(row.id, it)
                presetBeingRenamed = null
            },
            onDismiss = { presetBeingRenamed = null },
        )
    }
}

@Composable
private fun PresetRow(
    row: PresetRowUi,
    onApply: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onUpdateFromCurrentSettings: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onApply)
            .padding(start = 20.dp, top = 12.dp, bottom = 12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = row.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = row.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (row.isBuiltIn) {
                Text(
                    text = "Built in",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        PresetRowMenu(
            isBuiltIn = row.isBuiltIn,
            onRename = onRename,
            onDuplicate = onDuplicate,
            onUpdateFromCurrentSettings = onUpdateFromCurrentSettings,
            onDelete = onDelete,
        )
    }
}

/**
 * A built-in can only be duplicated. The three shipped presets are the app's known starting points
 * — editing one in place would take away the thing you go back to when a rig stops working.
 */
@Composable
private fun PresetRowMenu(
    isBuiltIn: Boolean,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onUpdateFromCurrentSettings: () -> Unit,
    onDelete: () -> Unit,
) {
    var isMenuOpen by remember { mutableStateOf(false) }

    fun closingMenu(action: () -> Unit): () -> Unit = {
        isMenuOpen = false
        action()
    }

    Box {
        IconButton(onClick = { isMenuOpen = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "More options")
        }
        DropdownMenu(expanded = isMenuOpen, onDismissRequest = { isMenuOpen = false }) {
            DropdownMenuItem(
                text = { Text("Rename") },
                enabled = !isBuiltIn,
                onClick = closingMenu(onRename),
            )
            DropdownMenuItem(
                text = { Text("Duplicate") },
                onClick = closingMenu(onDuplicate),
            )
            DropdownMenuItem(
                text = { Text("Update from current settings") },
                enabled = !isBuiltIn,
                onClick = closingMenu(onUpdateFromCurrentSettings),
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                enabled = !isBuiltIn,
                onClick = closingMenu(onDelete),
            )
        }
    }
}

/**
 * Applying is silent otherwise: the screen you are looking at does not change, because what changed
 * is the global defaults behind it.
 */
@Composable
private fun PresetsSnackbars(
    events: PresetsViewModel,
    snackbarHostState: SnackbarHostState,
    onUndoDeletion: (Preset) -> Unit,
) {
    LaunchedEffect(events) {
        events.eventStream.collect { event ->
            when (event) {
                is PresetsEvent.PresetApplied -> {
                    snackbarHostState.showSnackbar("Applied ${event.name}")
                }

                is PresetsEvent.PresetDeleted -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "Deleted ${event.preset.name}",
                        actionLabel = "Undo",
                    )
                    // The preset came with the event, so Undo restores the one this snackbar names
                    // even if another has been deleted while it was on screen.
                    if (result == SnackbarResult.ActionPerformed) onUndoDeletion(event.preset)
                }
            }
        }
    }
}
