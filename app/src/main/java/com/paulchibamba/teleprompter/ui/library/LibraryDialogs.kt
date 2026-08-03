package com.paulchibamba.teleprompter.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Renames a script in place. A blank title is refused rather than silently derived from the body. */
@Composable
fun RenameScriptDialog(
    initialTitle: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember { mutableStateOf(initialTitle) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename script") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(title.trim()) }, enabled = title.isNotBlank()) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

/**
 * Chooses which preset a script prompts with. "Global default" is a first-class option rather than
 * an absence — a script with no preset follows the settings screen, and that should be sayable.
 */
@Composable
fun AssignPresetDialog(
    presets: List<PresetOptionUi>,
    selectedPresetId: Long?,
    onConfirm: (Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    var chosenPresetId by remember { mutableStateOf(selectedPresetId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign preset") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                PresetChoice(
                    label = "Global default",
                    isSelected = chosenPresetId == null,
                    onSelect = { chosenPresetId = null },
                )
                presets.forEach { preset ->
                    PresetChoice(
                        label = preset.name,
                        isSelected = chosenPresetId == preset.id,
                        onSelect = { chosenPresetId = preset.id },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(chosenPresetId) }) { Text("Assign") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun PresetChoice(
    label: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = isSelected, onClick = onSelect)
            .padding(vertical = 8.dp),
    ) {
        // Null callback: the whole row is the target, so the button must not be separately clickable.
        RadioButton(selected = isSelected, onClick = null)
        Text(text = label, modifier = Modifier.padding(start = 12.dp))
    }
}
