package com.paulchibamba.teleprompter.ui.prompter

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.paulchibamba.teleprompter.domain.model.Preset
import com.paulchibamba.teleprompter.ui.components.PresetChoiceRow

/**
 * Presets, reachable from where the settings are actually judged (docs/SPEC.md §5.3, §5.4).
 *
 * The management screen lives under Settings, but the moment you most want to save a preset is the
 * moment you have just got the type right against real glass — which only ever happens here, with
 * the sheet open over the running script.
 */
@Composable
fun PresetShortcuts(
    presets: List<Preset>,
    assignedPresetName: String?,
    isAssignedPresetBuiltIn: Boolean,
    onApplyPreset: (Long) -> Unit,
    onSaveAsNewPreset: (String) -> Unit,
    onSaveToAssignedPreset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isPickingPreset by remember { mutableStateOf(false) }
    var isNamingPreset by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth().padding(top = 16.dp)) {
        HorizontalDivider()

        if (assignedPresetName != null) {
            SessionOnlyNotice(presetName = assignedPresetName)
        }

        TextButton(onClick = { isPickingPreset = true }) {
            Text("Apply a preset…")
        }
        TextButton(onClick = { isNamingPreset = true }) {
            Text("Save these settings as a preset…")
        }
        // Offered only for a user preset: saving over a built-in takes a copy, which would leave
        // this script pointing at a second "Studio" nobody asked for.
        if (assignedPresetName != null && !isAssignedPresetBuiltIn) {
            TextButton(onClick = onSaveToAssignedPreset) {
                Text("Save these settings to $assignedPresetName")
            }
        }
    }

    if (isPickingPreset) {
        ApplyPresetDialog(
            presets = presets,
            onConfirm = {
                onApplyPreset(it)
                isPickingPreset = false
            },
            onDismiss = { isPickingPreset = false },
        )
    }

    if (isNamingPreset) {
        NamePresetDialog(
            title = "Save as preset",
            initialName = "",
            confirmLabel = "Save",
            onConfirm = {
                onSaveAsNewPreset(it)
                isNamingPreset = false
            },
            onDismiss = { isNamingPreset = false },
        )
    }
}

/**
 * Without this the rule is invisible: an adjustment appears to work, then is gone next time the
 * script is opened, which reads as a bug rather than as the preset holding its shape.
 */
@Composable
private fun SessionOnlyNotice(presetName: String) {
    Text(
        text = "$presetName · changes apply to this session only",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp),
    )
}

/** Loads a preset into what is on screen right now — the prompter's half of "apply". */
@Composable
private fun ApplyPresetDialog(
    presets: List<Preset>,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var chosenPresetId by remember { mutableStateOf(presets.firstOrNull()?.id) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Apply a preset") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                presets.forEach { preset ->
                    PresetChoiceRow(
                        label = preset.name,
                        isSelected = chosenPresetId == preset.id,
                        onSelect = { chosenPresetId = preset.id },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { chosenPresetId?.let(onConfirm) },
                enabled = chosenPresetId != null,
            ) {
                Text("Apply")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/**
 * Names a preset, on the way to creating one or renaming one. A blank name is refused here rather
 * than silently becoming "Untitled preset" further down — the user is looking straight at the field.
 */
@Composable
fun NamePresetDialog(
    title: String,
    initialName: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.trim()) }, enabled = name.isNotBlank()) {
                Text(confirmLabel)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
