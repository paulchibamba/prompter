package com.paulchibamba.teleprompter.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.paulchibamba.teleprompter.ui.components.ReorderState
import com.paulchibamba.teleprompter.ui.components.rememberReorderState
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The library: every script, with a word count and how long it will take to read at the current
 * pace (docs/SPEC.md §5.1).
 */
@Composable
fun LibraryScreen(
    onOpenPrompter: (Long) -> Unit,
    onOpenEditor: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: LibraryViewModel = viewModel(factory = LibraryViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    ObserveDeletionEvents(
        viewModel = viewModel,
        snackbarHostState = snackbarHostState,
    )

    LibraryScaffold(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onOpenPrompter = onOpenPrompter,
        onOpenEditor = onOpenEditor,
        onOpenSettings = onOpenSettings,
        onOpenSearch = viewModel::openSearch,
        onCloseSearch = viewModel::closeSearch,
        onSearchQueryChanged = viewModel::updateSearchQuery,
        onRename = viewModel::renameScript,
        onDuplicate = viewModel::duplicateScript,
        onAssignPreset = viewModel::assignPreset,
        onDelete = viewModel::deleteScript,
        onReorder = viewModel::reorderScripts,
    )
}

/**
 * Shows the undo snackbar for exactly five seconds (§5.1). Material's own Short duration is four
 * and Long is ten, so the timeout is driven here rather than approximated.
 */
@Composable
private fun ObserveDeletionEvents(
    viewModel: LibraryViewModel,
    snackbarHostState: SnackbarHostState,
) {
    LaunchedEffect(viewModel) {
        viewModel.eventStream.collect { event ->
            when (event) {
                is LibraryEvent.ScriptDeleted -> {
                    val result = withTimeoutOrNull(UNDO_WINDOW_MILLIS) {
                        snackbarHostState.showSnackbar(
                            message = "Deleted \"${event.title}\"",
                            actionLabel = "Undo",
                            withDismissAction = false,
                            duration = SnackbarDuration.Indefinite,
                        )
                    }
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoDeletion()
                    } else {
                        viewModel.forgetDeletion()
                    }
                    snackbarHostState.currentSnackbarData?.dismiss()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryScaffold(
    uiState: LibraryUiState,
    snackbarHostState: SnackbarHostState,
    onOpenPrompter: (Long) -> Unit,
    onOpenEditor: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onRename: (Long, String) -> Unit,
    onDuplicate: (Long) -> Unit,
    onAssignPreset: (Long, Long?) -> Unit,
    onDelete: (Long) -> Unit,
    onReorder: (List<Long>) -> Unit,
) {
    Scaffold(
        topBar = {
            LibraryTopBar(
                uiState = uiState,
                onOpenSearch = onOpenSearch,
                onCloseSearch = onCloseSearch,
                onSearchQueryChanged = onSearchQueryChanged,
                onOpenSettings = onOpenSettings,
            )
        },
        floatingActionButton = { NewScriptButton(onClick = { onOpenEditor(NEW_SCRIPT) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { contentPadding ->
        LibraryContent(
            uiState = uiState,
            modifier = Modifier.padding(contentPadding),
            onOpenPrompter = onOpenPrompter,
            onOpenEditor = onOpenEditor,
            onRename = onRename,
            onDuplicate = onDuplicate,
            onAssignPreset = onAssignPreset,
            onDelete = onDelete,
            onReorder = onReorder,
        )
    }
}

@Composable
private fun LibraryContent(
    uiState: LibraryUiState,
    modifier: Modifier,
    onOpenPrompter: (Long) -> Unit,
    onOpenEditor: (Long) -> Unit,
    onRename: (Long, String) -> Unit,
    onDuplicate: (Long) -> Unit,
    onAssignPreset: (Long, Long?) -> Unit,
    onDelete: (Long) -> Unit,
    onReorder: (List<Long>) -> Unit,
) {
    when {
        uiState.hasNoScripts -> EmptyLibraryMessage(
            modifier = modifier,
            onCreateScript = { onOpenEditor(NEW_SCRIPT) },
        )

        uiState.hasNoSearchResults -> NoSearchResultsMessage(uiState.searchQuery, modifier)

        else -> ScriptList(
            uiState = uiState,
            modifier = modifier,
            onOpenPrompter = onOpenPrompter,
            onOpenEditor = onOpenEditor,
            onRename = onRename,
            onDuplicate = onDuplicate,
            onAssignPreset = onAssignPreset,
            onDelete = onDelete,
            onReorder = onReorder,
        )
    }
}

@Composable
private fun ScriptList(
    uiState: LibraryUiState,
    modifier: Modifier,
    onOpenPrompter: (Long) -> Unit,
    onOpenEditor: (Long) -> Unit,
    onRename: (Long, String) -> Unit,
    onDuplicate: (Long) -> Unit,
    onAssignPreset: (Long, Long?) -> Unit,
    onDelete: (Long) -> Unit,
    onReorder: (List<Long>) -> Unit,
) {
    val listState = rememberLazyListState()
    val reorderState = rememberReorderState(
        listState = listState,
        items = uiState.rows,
        keyOf = ScriptRowUi::id,
        onOrderCommitted = onReorder,
    )

    var rowBeingRenamed by remember { mutableStateOf<ScriptRowUi?>(null) }
    var rowChoosingPreset by remember { mutableStateOf<ScriptRowUi?>(null) }

    LazyColumn(state = listState, modifier = modifier.fillMaxSize()) {
        items(reorderState.orderedItems, key = ScriptRowUi::id) { row ->
            ScriptRow(
                row = row,
                onOpenPrompter = { onOpenPrompter(row.id) },
                onOpenEditor = { onOpenEditor(row.id) },
                onRename = { rowBeingRenamed = row },
                onDuplicate = { onDuplicate(row.id) },
                onAssignPreset = { rowChoosingPreset = row },
                onDelete = { onDelete(row.id) },
                onDragStarted = { reorderState.onDragStarted(row.id) },
                onDragged = reorderState::onDragged,
                onDragStopped = reorderState::onDragStopped,
                isBeingDragged = reorderState.isDragging(row.id),
                modifier = Modifier.draggedRowModifier(reorderState, row.id),
            )
        }
    }

    rowBeingRenamed?.let { row ->
        RenameScriptDialog(
            initialTitle = row.title,
            onConfirm = { newTitle ->
                onRename(row.id, newTitle)
                rowBeingRenamed = null
            },
            onDismiss = { rowBeingRenamed = null },
        )
    }

    rowChoosingPreset?.let { row ->
        AssignPresetDialog(
            presets = uiState.presets,
            selectedPresetId = row.presetId,
            onConfirm = { presetId ->
                onAssignPreset(row.id, presetId)
                rowChoosingPreset = null
            },
            onDismiss = { rowChoosingPreset = null },
        )
    }
}

/** Lifts the dragged row above its neighbours and lets it follow the finger. */
private fun Modifier.draggedRowModifier(reorderState: ReorderState<ScriptRowUi>, id: Long): Modifier =
    this
        .zIndex(if (reorderState.isDragging(id)) 1f else 0f)
        .offset { IntOffset(x = 0, y = reorderState.offsetFor(id).toInt()) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryTopBar(
    uiState: LibraryUiState,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    TopAppBar(
        title = {
            if (uiState.isSearchOpen) {
                SearchField(query = uiState.searchQuery, onQueryChanged = onSearchQueryChanged)
            } else {
                Text("Prompter")
            }
        },
        actions = {
            if (uiState.isSearchOpen) {
                IconButton(onClick = onCloseSearch) {
                    Icon(Icons.Filled.Close, contentDescription = "Close search")
                }
            } else {
                IconButton(onClick = onOpenSearch) {
                    Icon(Icons.Filled.Search, contentDescription = "Search scripts")
                }
                LibraryOverflowMenu(onOpenSettings = onOpenSettings)
            }
        },
    )
}

@Composable
private fun SearchField(query: String, onQueryChanged: (String) -> Unit) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    TextField(
        value = query,
        onValueChange = onQueryChanged,
        placeholder = { Text("Search scripts") },
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
    )
}

@Composable
private fun LibraryOverflowMenu(onOpenSettings: () -> Unit) {
    var isMenuOpen by remember { mutableStateOf(false) }

    IconButton(onClick = { isMenuOpen = true }) {
        Icon(Icons.Filled.MoreVert, contentDescription = "More options")
    }

    DropdownMenu(expanded = isMenuOpen, onDismissRequest = { isMenuOpen = false }) {
        DropdownMenuItem(
            text = { Text("Settings") },
            onClick = { isMenuOpen = false; onOpenSettings() },
        )
        // Import arrives with the import/export step; shown disabled so the menu's final shape is
        // visible rather than shifting under the user once it is wired up.
        DropdownMenuItem(text = { Text("Import .txt") }, enabled = false, onClick = {})
    }
}

@Composable
private fun NewScriptButton(onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
        text = { Text("New script") },
    )
}

@Composable
private fun EmptyLibraryMessage(modifier: Modifier, onCreateScript: () -> Unit) {
    CentredMessage(modifier) {
        Text(
            text = "No scripts yet",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = "Write one, or import a .txt file.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        OutlinedButton(onClick = onCreateScript) { Text("New script") }
        OutlinedButton(onClick = {}, enabled = false) { Text("Import .txt") }
    }
}

@Composable
private fun NoSearchResultsMessage(query: String, modifier: Modifier) {
    CentredMessage(modifier) {
        Text(
            text = "Nothing matches \"$query\"",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CentredMessage(modifier: Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
    }
}

private const val NEW_SCRIPT = 0L
private const val UNDO_WINDOW_MILLIS = 5_000L
