package com.paulchibamba.teleprompter.ui.library

import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.paulchibamba.teleprompter.ui.components.PlaceholderScaffold

/**
 * Placeholder. The real library lists scripts with word counts and estimated durations, searches,
 * reorders, and deletes with an undo snackbar.
 *
 * The sample id below stands in for a real row until there are rows to tap.
 */
@Composable
fun LibraryScreen(
    onOpenPrompter: (Long) -> Unit,
    onOpenEditor: (Long) -> Unit,
    onOpenSettings: () -> Unit,
) {
    PlaceholderScaffold(
        title = "Prompter",
        description = "Your scripts will live here.",
    ) {
        Button(onClick = { onOpenPrompter(SAMPLE_SCRIPT_ID) }) {
            Text("Open prompter")
        }
        OutlinedButton(onClick = { onOpenEditor(SAMPLE_SCRIPT_ID) }) {
            Text("Edit a script")
        }
        OutlinedButton(onClick = { onOpenEditor(NEW_SCRIPT) }) {
            Text("New script")
        }
        TextButton(onClick = onOpenSettings) {
            Text("Settings")
        }
    }
}

private const val SAMPLE_SCRIPT_ID = 1L
private const val NEW_SCRIPT = 0L
