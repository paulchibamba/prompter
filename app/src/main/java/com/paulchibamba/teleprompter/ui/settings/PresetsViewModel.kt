package com.paulchibamba.teleprompter.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.paulchibamba.teleprompter.domain.model.Preset
import com.paulchibamba.teleprompter.domain.usecase.ApplyPreset
import com.paulchibamba.teleprompter.domain.usecase.DeletePreset
import com.paulchibamba.teleprompter.domain.usecase.DuplicatePreset
import com.paulchibamba.teleprompter.domain.usecase.GetPreset
import com.paulchibamba.teleprompter.domain.usecase.ObservePresets
import com.paulchibamba.teleprompter.domain.usecase.SaveCurrentSettingsAsPreset
import com.paulchibamba.teleprompter.domain.usecase.SavePreset
import com.paulchibamba.teleprompter.domain.usecase.UpdatePresetFromCurrentSettings
import com.paulchibamba.teleprompter.ui.prompter.COLOUR_PRESETS
import com.paulchibamba.teleprompter.ui.prompterContainer
import com.paulchibamba.teleprompter.ui.theme.PrompterFonts
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Drives preset management (docs/SPEC.md §5.4).
 *
 * Applying writes to the *global defaults*, which is what a preset is for: a named set of values
 * you can put back on at any time. It has no effect on a script that has been assigned a preset of
 * its own — that script reads through its own assignment either way.
 */
class PresetsViewModel(
    private val getPreset: GetPreset,
    private val savePreset: SavePreset,
    private val deletePreset: DeletePreset,
    private val duplicatePreset: DuplicatePreset,
    private val applyPreset: ApplyPreset,
    private val saveCurrentSettingsAsPreset: SaveCurrentSettingsAsPreset,
    private val updatePresetFromCurrentSettings: UpdatePresetFromCurrentSettings,
    observePresets: ObservePresets,
) : ViewModel() {

    private val events = Channel<PresetsEvent>(Channel.BUFFERED)
    val eventStream: Flow<PresetsEvent> = events.receiveAsFlow()

    val uiState: StateFlow<PresetsUiState> = observePresets()
        .map { presets -> PresetsUiState(rows = presets.map { it.toRow() }, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = PresetsUiState(),
        )

    fun applyPreset(id: Long) = withPreset(id) { preset ->
        applyPreset.invoke(id)
        events.send(PresetsEvent.PresetApplied(preset.name))
    }

    fun saveCurrentSettingsAsPreset(name: String) {
        viewModelScope.launch { saveCurrentSettingsAsPreset.invoke(name) }
    }

    fun renamePreset(id: Long, name: String) = withPreset(id) { preset ->
        savePreset(preset.copy(name = name))
    }

    fun duplicatePreset(id: Long) {
        viewModelScope.launch { duplicatePreset.invoke(id) }
    }

    fun updateFromCurrentSettings(id: Long) {
        viewModelScope.launch { updatePresetFromCurrentSettings.invoke(id) }
    }

    /** Deletes immediately and offers an undo, the same bargain the library strikes (§5.1). */
    fun deletePreset(id: Long) = withPreset(id) { preset ->
        if (!deletePreset.invoke(id)) return@withPreset
        events.send(PresetsEvent.PresetDeleted(preset))
    }

    /** Writes the row back with its original id, so any script still assigned to it reconnects. */
    fun undoDeletion(preset: Preset) {
        viewModelScope.launch { savePreset(preset) }
    }

    private fun withPreset(id: Long, action: suspend (Preset) -> Unit) {
        viewModelScope.launch {
            val preset = getPreset(id) ?: return@launch
            action(preset)
        }
    }

    private fun Preset.toRow() = PresetRowUi(
        id = id,
        name = name,
        summary = summaryOf(this),
        isBuiltIn = isBuiltIn,
    )

    /**
     * The three things that identify a preset at a glance: what it looks like, how big, and how
     * fast. A colour pairing with a name uses it; anything else falls back to describing it as
     * custom rather than reciting two hex values at the user.
     */
    private fun summaryOf(preset: Preset): String {
        val typography = preset.typography
        val face = PrompterFonts.displayNameFor(typography.fontId)
        val colours = COLOUR_PRESETS
            .firstOrNull {
                it.textColor == typography.textColor && it.backgroundColor == typography.backgroundColor
            }
            ?.name
            ?.lowercase()
            ?: CUSTOM_COLOURS
        return "$face ${typography.sizeSp.roundToInt()}sp · $colours · ${preset.scroll.speedWpm} wpm"
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L
        private const val CUSTOM_COLOURS = "custom colours"

        val Factory = viewModelFactory {
            initializer {
                val container = prompterContainer()
                PresetsViewModel(
                    getPreset = container.getPreset,
                    savePreset = container.savePreset,
                    deletePreset = container.deletePreset,
                    duplicatePreset = container.duplicatePreset,
                    applyPreset = container.applyPreset,
                    saveCurrentSettingsAsPreset = container.saveCurrentSettingsAsPreset,
                    updatePresetFromCurrentSettings = container.updatePresetFromCurrentSettings,
                    observePresets = container.observePresets,
                )
            }
        }
    }
}
