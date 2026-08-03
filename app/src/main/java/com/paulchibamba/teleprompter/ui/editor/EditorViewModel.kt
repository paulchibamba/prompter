package com.paulchibamba.teleprompter.ui.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.paulchibamba.teleprompter.domain.model.Script
import com.paulchibamba.teleprompter.domain.model.ScrollSettings
import com.paulchibamba.teleprompter.domain.scroll.WpmCalculator
import com.paulchibamba.teleprompter.domain.text.MarkerInsertion
import com.paulchibamba.teleprompter.domain.text.ScriptParser
import com.paulchibamba.teleprompter.domain.usecase.GetScript
import com.paulchibamba.teleprompter.domain.usecase.SaveScript
import com.paulchibamba.teleprompter.ui.navigation.Destination
import com.paulchibamba.teleprompter.ui.prompterContainer
import androidx.navigation.toRoute
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Drives the editor (docs/SPEC.md §5.2).
 *
 * Saving is something the user should never have to think about: edits are written on a short
 * debounce and again when the screen goes away. The Save button stays anyway, because "did that
 * save?" is a real anxiety and a button that answers it costs nothing.
 */
@OptIn(FlowPreview::class)
class EditorViewModel(
    private val scriptId: Long,
    private val getScript: GetScript,
    private val saveScript: SaveScript,
    private val scrollSettings: Flow<ScrollSettings>,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState = _uiState.asStateFlow()

    /**
     * The script as last written, so a save that changes nothing can be skipped and so fields we
     * do not edit here — the resume position, the assigned preset — survive a round trip.
     */
    private var persistedScript: Script? = null
    private var lastSavedContent: EditedContent? = null

    init {
        loadScript()
        recomputeSummaryWhenBodyOrPaceChanges()
        autosaveAfterTypingStops()
    }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun updateBody(body: TextFieldValue) {
        _uiState.update { it.copy(body = body) }
    }

    /** Turns the caret's line into a cue marker, leaving the caret on the same character. */
    fun insertMarker() {
        val current = _uiState.value.body
        val edit = MarkerInsertion.insertMarkerAtCaretLine(current.text, current.selection.start)
        _uiState.update {
            it.copy(body = TextFieldValue(text = edit.text, selection = TextRange(edit.caret)))
        }
    }

    /**
     * Saves now and reports the id, which the caller needs because previewing a brand-new script
     * requires it to exist first. Returns null only if there is nothing worth saving.
     */
    fun saveThen(onSaved: (Long) -> Unit) {
        viewModelScope.launch {
            saveIfChanged()?.let(onSaved)
        }
    }

    /** Called when the screen stops, so an edit is never lost to a swipe away or a phone call. */
    fun saveNow() {
        viewModelScope.launch { saveIfChanged() }
    }

    private fun loadScript() {
        viewModelScope.launch {
            val existing = if (scriptId == Destination.NEW_SCRIPT) null else getScript(scriptId)
            persistedScript = existing
            lastSavedContent = existing?.let { EditedContent(it.title, it.body) }
            _uiState.update {
                it.copy(
                    title = existing?.title.orEmpty(),
                    body = TextFieldValue(existing?.body.orEmpty()),
                    savedScriptId = existing?.id,
                    isLoading = false,
                )
            }
        }
    }

    /**
     * Word count and duration lag typing by a moment on purpose: recounting a ten-thousand-word
     * script on every keystroke would be felt, and a counter that settles a beat later is not.
     */
    private fun recomputeSummaryWhenBodyOrPaceChanges() {
        viewModelScope.launch {
            _uiState
                .map { it.body.text }
                .distinctUntilChanged()
                .debounce(SUMMARY_DEBOUNCE_MILLIS)
                .collect { body -> updateSummary(body) }
        }
        viewModelScope.launch {
            scrollSettings.map { it.speedWpm }.distinctUntilChanged().collect {
                updateSummary(_uiState.value.body.text)
            }
        }
    }

    private suspend fun updateSummary(body: String) {
        val wordCount = ScriptParser.wordCount(body)
        val speedWpm = scrollSettings.first().speedWpm
        val duration = WpmCalculator.formatDuration(WpmCalculator.estimatedSeconds(wordCount, speedWpm))
        val words = if (wordCount == 1) "1 word" else "$wordCount words"
        _uiState.update { it.copy(wordCount = wordCount, summary = "$words · $duration") }
    }

    private fun autosaveAfterTypingStops() {
        viewModelScope.launch {
            _uiState
                .map { EditedContent(it.title, it.body.text) }
                .distinctUntilChanged()
                .drop(1) // the initial load is not an edit
                .debounce(AUTOSAVE_DEBOUNCE_MILLIS)
                .collect { saveIfChanged() }
        }
    }

    /**
     * Writes the script if anything actually changed, returning its id.
     *
     * A brand-new script with nothing in it is not saved at all — opening the editor and changing
     * your mind should not litter the library with empty rows.
     */
    private suspend fun saveIfChanged(): Long? {
        val state = _uiState.value
        if (state.isLoading) return state.savedScriptId

        val content = EditedContent(state.title, state.body.text)
        if (content == lastSavedContent) return state.savedScriptId
        if (state.isEmpty && persistedScript == null) return null

        val existing = persistedScript
        val id = saveScript(
            existing?.copy(title = content.title, body = content.body)
                ?: Script(
                    title = content.title,
                    body = content.body,
                    createdAt = 0L,
                    updatedAt = 0L,
                    wordCount = 0,
                ),
        )

        lastSavedContent = content
        persistedScript = getScript(id)
        _uiState.update { it.copy(savedScriptId = id) }
        return id
    }

    /** Just the parts of the state a save cares about, so an unrelated change cannot trigger one. */
    private data class EditedContent(val title: String, val body: String)

    companion object {
        private const val AUTOSAVE_DEBOUNCE_MILLIS = 500L
        private const val SUMMARY_DEBOUNCE_MILLIS = 300L

        val Factory = viewModelFactory {
            initializer {
                val container = prompterContainer()
                val route: Destination.Editor = createSavedStateHandle().toRoute()
                EditorViewModel(
                    scriptId = route.scriptId,
                    getScript = container.getScript,
                    saveScript = container.saveScript,
                    scrollSettings = container.settingsRepository.scroll,
                )
            }
        }
    }
}
