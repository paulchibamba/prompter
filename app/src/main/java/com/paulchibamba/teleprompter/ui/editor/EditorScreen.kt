package com.paulchibamba.teleprompter.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.paulchibamba.teleprompter.ui.navigation.Destination
import kotlinx.coroutines.launch

/**
 * Writing and editing a script (docs/SPEC.md §5.2).
 *
 * The editing face is fixed at a comfortable reading-on-your-hand size and ignores the prompter's
 * typography entirely. Editing legibility and prompting legibility are different problems: nobody
 * wants to type into 72sp all-caps amber.
 */
@Composable
fun EditorScreen(
    scriptId: Long,
    onNavigateBack: () -> Unit,
    onPreview: (Long) -> Unit,
    viewModel: EditorViewModel = viewModel(
        key = "editor-$scriptId",
        factory = EditorViewModel.Factory,
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    SaveWhenScreenStops(viewModel)

    EditorScaffold(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        isNewScript = scriptId == Destination.NEW_SCRIPT,
        onTitleChanged = viewModel::updateTitle,
        onBodyChanged = viewModel::updateBody,
        onInsertMarker = viewModel::insertMarker,
        onNavigateBack = {
            viewModel.saveNow()
            onNavigateBack()
        },
        onPreview = { viewModel.saveThen(onPreview) },
        onSave = {
            viewModel.saveThen {
                coroutineScope.launch { snackbarHostState.showSnackbar("Saved") }
            }
        },
    )
}

/** Autosave covers typing pauses; this covers the screen going away mid-sentence. */
@Composable
private fun SaveWhenScreenStops(viewModel: EditorViewModel) {
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.saveNow() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorScaffold(
    uiState: EditorUiState,
    snackbarHostState: SnackbarHostState,
    isNewScript: Boolean,
    onTitleChanged: (String) -> Unit,
    onBodyChanged: (TextFieldValue) -> Unit,
    onInsertMarker: () -> Unit,
    onNavigateBack: () -> Unit,
    onPreview: () -> Unit,
    onSave: () -> Unit,
) {
    Scaffold(
        topBar = {
            EditorTopBar(
                isNewScript = isNewScript,
                onNavigateBack = onNavigateBack,
                onSave = onSave,
            )
        },
        bottomBar = {
            EditorBottomBar(
                summary = uiState.summary,
                onInsertMarker = onInsertMarker,
                onPreview = onPreview,
                canPreview = uiState.body.text.isNotBlank(),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // On the Scaffold rather than the content, so the bottom bar rides above the keyboard.
        // Marker and Preview are most wanted mid-sentence, which is exactly when the IME is up.
        modifier = Modifier.imePadding(),
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            TitleField(title = uiState.title, onTitleChanged = onTitleChanged)
            BodyField(
                body = uiState.body,
                onBodyChanged = onBodyChanged,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorTopBar(
    isNewScript: Boolean,
    onNavigateBack: () -> Unit,
    onSave: () -> Unit,
) {
    TopAppBar(
        title = { Text(if (isNewScript) "New script" else "Edit script") },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            IconButton(onClick = onSave) {
                Icon(Icons.Filled.Check, contentDescription = "Save script")
            }
        },
    )
}

@Composable
private fun TitleField(title: String, onTitleChanged: (String) -> Unit) {
    OutlinedTextField(
        value = title,
        onValueChange = onTitleChanged,
        label = { Text("Title") },
        placeholder = { Text("Untitled") },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

/**
 * The script body. Fixed at [EDITOR_TEXT_SIZE_SP] regardless of the prompter's settings, and
 * unlike the prompter it *does* respect the system font scale — this is ordinary app chrome.
 */
@Composable
private fun BodyField(
    body: TextFieldValue,
    onBodyChanged: (TextFieldValue) -> Unit,
    modifier: Modifier,
) {
    val editorTextStyle = LocalTextStyle.current.merge(
        TextStyle(
            fontSize = EDITOR_TEXT_SIZE_SP.sp,
            lineHeight = (EDITOR_TEXT_SIZE_SP * EDITOR_LINE_HEIGHT).sp,
            color = MaterialTheme.colorScheme.onSurface,
        ),
    )

    Box(modifier = modifier.padding(horizontal = 16.dp)) {
        if (body.text.isEmpty()) {
            Text(
                text = "Write your script here.\n\nStart a line with ## to mark a section.",
                style = editorTextStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        BasicTextField(
            value = body,
            onValueChange = onBodyChanged,
            textStyle = editorTextStyle,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun EditorBottomBar(
    summary: String,
    onInsertMarker: () -> Unit,
    onPreview: () -> Unit,
    canPreview: Boolean,
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    BottomAppBar {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = summary,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onInsertMarker) { Text("Marker") }
            // Import lands with the import/export step; shown disabled so the bar keeps its shape.
            TextButton(onClick = {}, enabled = false) { Text("Import") }
            IconButton(
                onClick = {
                    keyboardController?.hide()
                    onPreview()
                },
                enabled = canPreview,
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Preview in prompter")
            }
        }
    }
}

private const val EDITOR_TEXT_SIZE_SP = 18
private const val EDITOR_LINE_HEIGHT = 1.5f
