package com.paulchibamba.teleprompter.ui.editor

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.paulchibamba.teleprompter.ui.components.PlaceholderScaffold

/**
 * Placeholder. The real editor is a title field and a full-height text field that autosaves on a
 * debounce, with word count, estimated duration, and a button that inserts a `## ` cue marker.
 */
@Composable
fun EditorScreen(
    scriptId: Long,
    onNavigateBack: () -> Unit,
    onPreview: (Long) -> Unit,
) {
    PlaceholderScaffold(
        title = if (scriptId == NEW_SCRIPT) "New script" else "Edit script",
        description = "Editing script $scriptId.",
        onNavigateBack = onNavigateBack,
    ) {
        Button(onClick = { onPreview(scriptId) }) {
            Text("Preview in prompter")
        }
    }
}

private const val NEW_SCRIPT = 0L
