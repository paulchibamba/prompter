package com.paulchibamba.teleprompter.ui.library

/**
 * One row of the library, already formatted. The screen renders strings and never does arithmetic —
 * word counts and durations are worked out once in the ViewModel, where they can be tested.
 */
data class ScriptRowUi(
    val id: Long,
    val title: String,
    val snippet: String,
    /** "412 words · 2:56" — the estimate at the user's current reading pace. */
    val summary: String,
    val presetId: Long?,
)

/** A preset as the "assign preset" dialog needs it: a name and an id, nothing more. */
data class PresetOptionUi(
    val id: Long,
    val name: String,
)

data class LibraryUiState(
    val rows: List<ScriptRowUi> = emptyList(),
    val presets: List<PresetOptionUi> = emptyList(),
    val searchQuery: String = "",
    val isSearchOpen: Boolean = false,
    val isLoading: Boolean = true,
) {
    /** No scripts at all — as opposed to a search that happened to match none. */
    val hasNoScripts: Boolean
        get() = !isLoading && rows.isEmpty() && searchQuery.isBlank()

    val hasNoSearchResults: Boolean
        get() = !isLoading && rows.isEmpty() && searchQuery.isNotBlank()
}

/**
 * Something that happened once and should be shown once. Kept out of [LibraryUiState] so a
 * recomposition or a rotation cannot replay a snackbar the user already dismissed.
 */
sealed interface LibraryEvent {
    data class ScriptDeleted(val title: String) : LibraryEvent
}
