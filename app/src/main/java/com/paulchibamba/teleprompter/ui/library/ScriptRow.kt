package com.paulchibamba.teleprompter.ui.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * One script in the library.
 *
 * Tapping opens the prompter rather than the editor, because reading is the common case (§5.1);
 * editing is a long-press or the pencil.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScriptRow(
    row: ScriptRowUi,
    onOpenPrompter: () -> Unit,
    onOpenEditor: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onAssignPreset: () -> Unit,
    onDelete: () -> Unit,
    onDragStarted: () -> Unit,
    onDragged: (Float) -> Unit,
    onDragStopped: () -> Unit,
    isBeingDragged: Boolean,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = {
            Text(
                text = row.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = { ScriptRowSupportingText(row) },
        leadingContent = {
            ReorderHandle(
                onDragStarted = onDragStarted,
                onDragged = onDragged,
                onDragStopped = onDragStopped,
            )
        },
        trailingContent = {
            Row {
                EditScriptButton(onOpenEditor)
                ScriptRowMenuButton(
                    onRename = onRename,
                    onDuplicate = onDuplicate,
                    onAssignPreset = onAssignPreset,
                    onDelete = onDelete,
                )
            }
        },
        colors = if (isBeingDragged) {
            ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        } else {
            ListItemDefaults.colors()
        },
        modifier = modifier.combinedClickable(
            onClick = onOpenPrompter,
            onLongClick = onOpenEditor,
            onLongClickLabel = "Edit script",
        ),
    )
}

@Composable
private fun ScriptRowSupportingText(row: ScriptRowUi) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = row.snippet,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = row.summary,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EditScriptButton(onOpenEditor: () -> Unit) {
    IconButton(onClick = onOpenEditor) {
        Icon(imageVector = Icons.Filled.Edit, contentDescription = "Edit script")
    }
}

/**
 * The drag affordance. It carries its own [contentDescription] because a handle that only responds
 * to dragging is invisible to anyone driving the app with TalkBack — reordering by other means
 * arrives with the accessibility pass.
 */
@Composable
private fun ReorderHandle(
    onDragStarted: () -> Unit,
    onDragged: (Float) -> Unit,
    onDragStopped: () -> Unit,
) {
    Icon(
        imageVector = Icons.Filled.Menu,
        contentDescription = null,
        modifier = Modifier
            .size(24.dp)
            .semantics { contentDescription = "Reorder script" }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { onDragStarted() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDragged(dragAmount.y)
                    },
                    onDragEnd = onDragStopped,
                    onDragCancel = onDragStopped,
                )
            },
    )
}

@Composable
private fun ScriptRowMenuButton(
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onAssignPreset: () -> Unit,
    onDelete: () -> Unit,
) {
    var isMenuOpen by remember { mutableStateOf(false) }

    IconButton(onClick = { isMenuOpen = true }) {
        Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "More options")
    }

    DropdownMenu(expanded = isMenuOpen, onDismissRequest = { isMenuOpen = false }) {
        ScriptRowMenuItem("Rename") { isMenuOpen = false; onRename() }
        ScriptRowMenuItem("Duplicate") { isMenuOpen = false; onDuplicate() }
        ScriptRowMenuItem("Assign preset") { isMenuOpen = false; onAssignPreset() }
        ScriptRowMenuItem("Delete") { isMenuOpen = false; onDelete() }
        // Export arrives with import/export in a later step; shown disabled so the menu's final
        // shape is visible rather than shifting under the user once it is wired up.
        ScriptRowMenuItem("Export .txt", enabled = false) {}
    }
}

@Composable
private fun ScriptRowMenuItem(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(label) },
        enabled = enabled,
        onClick = onClick,
    )
}
