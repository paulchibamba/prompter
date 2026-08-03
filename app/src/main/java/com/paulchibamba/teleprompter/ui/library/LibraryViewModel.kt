package com.paulchibamba.teleprompter.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.paulchibamba.teleprompter.domain.model.Preset
import com.paulchibamba.teleprompter.domain.model.Script
import com.paulchibamba.teleprompter.domain.model.ScrollSettings
import com.paulchibamba.teleprompter.domain.scroll.WpmCalculator
import com.paulchibamba.teleprompter.domain.usecase.DeleteScript
import com.paulchibamba.teleprompter.domain.usecase.DuplicateScript
import com.paulchibamba.teleprompter.domain.usecase.GetScript
import com.paulchibamba.teleprompter.domain.usecase.ObservePresets
import com.paulchibamba.teleprompter.domain.usecase.ObserveScripts
import com.paulchibamba.teleprompter.domain.usecase.ReorderScripts
import com.paulchibamba.teleprompter.domain.usecase.RestoreScript
import com.paulchibamba.teleprompter.domain.usecase.SaveScript
import com.paulchibamba.teleprompter.ui.prompterContainer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Drives the library list (docs/SPEC.md §5.1).
 *
 * Rows are formatted here rather than in the composable so the screen stays declarative and the
 * interesting part — how a duration estimate is derived, what a snippet looks like — is reachable
 * without an emulator.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModel(
    private val observeScripts: ObserveScripts,
    private val getScript: GetScript,
    private val saveScript: SaveScript,
    private val deleteScript: DeleteScript,
    private val restoreScript: RestoreScript,
    private val duplicateScript: DuplicateScript,
    private val reorderScripts: ReorderScripts,
    observePresets: ObservePresets,
    scrollSettings: Flow<ScrollSettings>,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val isSearchOpen = MutableStateFlow(false)

    /**
     * The script the user just deleted, held only for as long as the undo snackbar is up. It is
     * the whole [Script] rather than an id because the row is already gone from the database —
     * restoring means writing it back, id and all.
     */
    private var undoableDeletion: Script? = null

    private val events = Channel<LibraryEvent>(Channel.BUFFERED)
    val eventStream: Flow<LibraryEvent> = events.receiveAsFlow()

    val uiState: StateFlow<LibraryUiState> = combine(
        searchQuery.flatMapLatest { query -> observeScripts(query) },
        scrollSettings.map { it.speedWpm }.distinctUntilChanged(),
        observePresets(),
        searchQuery,
        isSearchOpen,
    ) { scripts, speedWpm, presets, query, searchOpen ->
        LibraryUiState(
            rows = scripts.map { script -> script.toRow(speedWpm) },
            presets = presets.map { preset -> preset.toOption() },
            searchQuery = query,
            isSearchOpen = searchOpen,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = LibraryUiState(),
    )

    fun openSearch() {
        isSearchOpen.value = true
    }

    /** Closing search clears the query too — a hidden filter silently hiding scripts is a trap. */
    fun closeSearch() {
        isSearchOpen.value = false
        searchQuery.value = ""
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun renameScript(id: Long, title: String) = withScript(id) { script ->
        saveScript(script.copy(title = title))
    }

    fun duplicateScript(id: Long) {
        viewModelScope.launch { duplicateScript.invoke(id) }
    }

    fun assignPreset(id: Long, presetId: Long?) = withScript(id) { script ->
        saveScript(script.copy(presetId = presetId))
    }

    /**
     * Deletes immediately and offers an undo, rather than asking first. Faster in the common case,
     * and more forgiving in the rare one (§5.1).
     */
    fun deleteScript(id: Long) = withScript(id) { script ->
        undoableDeletion = script
        deleteScript.invoke(script)
        events.send(LibraryEvent.ScriptDeleted(script.title))
    }

    fun undoDeletion() {
        val deleted = undoableDeletion ?: return
        undoableDeletion = null
        viewModelScope.launch { restoreScript(deleted) }
    }

    /** The snackbar timed out, so the deletion is now permanent. */
    fun forgetDeletion() {
        undoableDeletion = null
    }

    /** [idsInOrder] is the list exactly as it now reads on screen, after the drag settled. */
    fun reorderScripts(idsInOrder: List<Long>) {
        viewModelScope.launch { reorderScripts.invoke(idsInOrder) }
    }

    private fun withScript(id: Long, action: suspend (Script) -> Unit) {
        viewModelScope.launch {
            val script = getScript(id) ?: return@launch
            action(script)
        }
    }

    private fun Script.toRow(speedWpm: Int): ScriptRowUi = ScriptRowUi(
        id = id,
        title = title,
        snippet = snippetOf(body),
        summary = summaryOf(wordCount, speedWpm),
        presetId = presetId,
    )

    /**
     * The opening of the script on one line. Newlines collapse to spaces here and only here — the
     * stored body keeps every one of them, which is the whole point of §3.1.
     */
    private fun snippetOf(body: String): String {
        val flattened = body.replace(WHITESPACE_RUN, " ").trim()
        return when {
            flattened.isEmpty() -> EMPTY_BODY_PLACEHOLDER
            flattened.length <= SNIPPET_LENGTH -> flattened
            else -> flattened.take(SNIPPET_LENGTH).trimEnd() + "…"
        }
    }

    private fun summaryOf(wordCount: Int, speedWpm: Int): String {
        val words = if (wordCount == 1) "1 word" else "$wordCount words"
        val duration = WpmCalculator.formatDuration(WpmCalculator.estimatedSeconds(wordCount, speedWpm))
        return "$words · $duration"
    }

    private fun Preset.toOption() = PresetOptionUi(id = id, name = name)

    companion object {
        private const val SNIPPET_LENGTH = 80
        private const val EMPTY_BODY_PLACEHOLDER = "Empty script"
        private const val STOP_TIMEOUT_MILLIS = 5_000L
        private val WHITESPACE_RUN = Regex("""\s+""")

        val Factory = viewModelFactory {
            initializer {
                val container = prompterContainer()
                LibraryViewModel(
                    observeScripts = container.observeScripts,
                    getScript = container.getScript,
                    saveScript = container.saveScript,
                    deleteScript = container.deleteScript,
                    restoreScript = container.restoreScript,
                    duplicateScript = container.duplicateScript,
                    reorderScripts = container.reorderScripts,
                    observePresets = container.observePresets,
                    scrollSettings = container.settingsRepository.scroll,
                )
            }
        }
    }
}
