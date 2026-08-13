package com.paulchibamba.teleprompter.ui.settings

import com.paulchibamba.teleprompter.domain.model.Preset

/**
 * One preset as the management screen needs it, already formatted. Like [ScriptRowUi] in the
 * library, the arithmetic and the string building happen in the ViewModel where they are reachable
 * without an emulator.
 */
data class PresetRowUi(
    val id: Long,
    val name: String,
    /** "Lexend 72sp · white on black · 140 wpm" — enough to recognise a preset without applying it. */
    val summary: String,
    /** Built-ins are read-only: they cannot be renamed, overwritten or deleted. */
    val isBuiltIn: Boolean,
)

data class PresetsUiState(
    val rows: List<PresetRowUi> = emptyList(),
    val isLoading: Boolean = true,
)

/**
 * Something that happened once and should be shown once, kept out of [PresetsUiState] so a
 * recomposition cannot replay a snackbar the user already dismissed — the same split the library
 * makes with `LibraryEvent`.
 */
sealed interface PresetsEvent {
    data class PresetApplied(val name: String) : PresetsEvent

    /**
     * Carries the whole deleted [Preset] rather than leaving it in a field on the ViewModel.
     * Snackbars queue, so deleting a second preset while the first one's snackbar is still up would
     * otherwise overwrite the field, and Undo would restore something other than the name on screen.
     */
    data class PresetDeleted(val preset: Preset) : PresetsEvent
}
