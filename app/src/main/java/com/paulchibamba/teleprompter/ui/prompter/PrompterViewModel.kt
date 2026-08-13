package com.paulchibamba.teleprompter.ui.prompter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import android.net.Uri
import androidx.navigation.toRoute
import com.paulchibamba.teleprompter.domain.model.LayoutSettings
import com.paulchibamba.teleprompter.domain.model.Preset
import com.paulchibamba.teleprompter.domain.model.ScrollSettings
import com.paulchibamba.teleprompter.domain.model.TypographySettings
import com.paulchibamba.teleprompter.data.io.CustomFontStore
import com.paulchibamba.teleprompter.domain.repository.SettingsRepository
import com.paulchibamba.teleprompter.domain.text.ScriptParser
import com.paulchibamba.teleprompter.domain.usecase.ApplyPreset
import com.paulchibamba.teleprompter.domain.usecase.GetPreset
import com.paulchibamba.teleprompter.domain.usecase.GetScript
import com.paulchibamba.teleprompter.domain.usecase.ObservePresets
import com.paulchibamba.teleprompter.domain.usecase.SavePreset
import com.paulchibamba.teleprompter.ui.navigation.Destination
import com.paulchibamba.teleprompter.ui.prompterContainer
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Feeds the prompter surface and owns the transport state.
 *
 * The settings blocks are observed rather than read once, so a change made from the control bar
 * lands on the text the reader is looking at without leaving the screen. Speed and size changes
 * are written straight to the global settings — they are adjustments a reader makes mid-take and
 * expects to still be there next time.
 */
@OptIn(FlowPreview::class)
class PrompterViewModel(
    private val scriptId: Long,
    private val getScript: GetScript,
    private val getPreset: GetPreset,
    private val savePreset: SavePreset,
    private val applyPreset: ApplyPreset,
    observePresets: ObservePresets,
    private val settingsRepository: SettingsRepository,
    private val customFontStore: CustomFontStore,
) : ViewModel() {

    /** The presets the sheet's "apply a preset" dialog offers. */
    val presets = observePresets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(PRESETS_STOP_TIMEOUT_MILLIS), emptyList())

    private val _uiState = MutableStateFlow(PrompterUiState())
    val uiState = _uiState.asStateFlow()

    /**
     * The most recent typography edit awaiting a write. Dragging a slider produces a change every
     * few milliseconds; the surface should follow the finger, but the database should not.
     */
    private val pendingTypography = MutableStateFlow<TypographySettings?>(null)
    private val pendingScroll = MutableStateFlow<ScrollSettings?>(null)
    private val pendingLayout = MutableStateFlow<LayoutSettings?>(null)

    /** The countdown in progress, if any. Held so pausing can abandon it rather than outrun it. */
    private var countdownJob: Job? = null

    init {
        loadScriptAndResolveSettings()
        persistTypographyAfterAdjustingStops()
        persistScrollAfterAdjustingStops()
        persistLayoutAfterAdjustingStops()
    }

    /**
     * Whether an adjustment made here is allowed to reach storage.
     *
     * A script with an assigned preset reads through a named, shared setting that may well be one
     * of the read-only built-ins, so a tweak made to cope with today's light stays in this session
     * — it never rewrites the preset, and it never touches the global defaults. Saving it is an
     * explicit action instead (see [saveSessionAsPreset]).
     */
    private val isFollowingGlobalDefaults: Boolean
        get() = _uiState.value.presetName == null

    /**
     * Applies a typography change to the visible text immediately and stores it a moment later.
     *
     * The stored value only changes when the write lands, so the settings flow does not emit
     * mid-drag and cannot fight the value under the reader's finger.
     */
    fun updateTypography(settings: TypographySettings) {
        val coerced = settings.coerced()
        _uiState.update { it.copy(typography = coerced) }
        if (isFollowingGlobalDefaults) pendingTypography.value = coerced
    }

    /**
     * The one-press beam-splitter flip. Turning it on sets the horizontal mirror, which is what
     * almost every rig needs; turning it off clears both, so a vertical flip set for an
     * upside-down mount does not survive as a surprise.
     */
    fun toggleBeamSplitterMirror() {
        val layout = _uiState.value.layout
        val isMirrored = layout.mirrorHorizontal || layout.mirrorVertical
        updateLayout(
            layout.copy(
                mirrorHorizontal = !isMirrored,
                mirrorVertical = false,
            ),
        )
    }

    /** Applies a layout change to the live prompter and stores it a moment later. */
    fun updateLayout(settings: LayoutSettings) {
        val coerced = settings.coerced()
        _uiState.update { it.copy(layout = coerced) }
        if (isFollowingGlobalDefaults) pendingLayout.value = coerced
    }

    private fun persistLayoutAfterAdjustingStops() {
        viewModelScope.launch {
            pendingLayout
                .filterNotNull()
                .debounce(SETTINGS_WRITE_DEBOUNCE_MILLIS)
                .collect { settings -> settingsRepository.setLayout(settings) }
        }
    }

    private fun persistScrollAfterAdjustingStops() {
        viewModelScope.launch {
            pendingScroll
                .filterNotNull()
                .debounce(SETTINGS_WRITE_DEBOUNCE_MILLIS)
                .collect { settings -> settingsRepository.setScroll(settings) }
        }
    }

    private fun persistTypographyAfterAdjustingStops() {
        viewModelScope.launch {
            pendingTypography
                .filterNotNull()
                .debounce(SETTINGS_WRITE_DEBOUNCE_MILLIS)
                .collect { settings -> settingsRepository.setTypography(settings) }
        }
    }

    fun togglePlayPause() {
        val state = _uiState.value
        if (state.isPlaying || state.isCountingDown) pause() else play()
    }

    /** Stops the text, and abandons a countdown that has not finished. */
    fun pause() {
        countdownJob?.cancel()
        _uiState.update { it.copy(isPlaying = false, countdownRemaining = 0) }
    }

    /** Starts the text, after the countdown if one is configured (docs/SPEC.md §8.4). */
    fun play() {
        countdownJob?.cancel()
        val countdownSeconds = _uiState.value.scroll.countdownSeconds
        if (countdownSeconds <= 0) {
            _uiState.update { it.copy(isPlaying = true) }
            return
        }
        countdownJob = viewModelScope.launch { countDownThenPlay(countdownSeconds) }
    }

    private suspend fun countDownThenPlay(seconds: Int) {
        for (remaining in seconds downTo 1) {
            _uiState.update { it.copy(countdownRemaining = remaining, isPlaying = false) }
            delay(ONE_SECOND_MILLIS)
        }
        _uiState.update { it.copy(countdownRemaining = 0, isPlaying = true) }
    }

    /**
     * Fills the screen black and stops the text (docs/SPEC.md §8.4).
     *
     * Restoring does not resume playing. That follows the same rule as a manual scrub: the reader
     * decides when the take starts again, and text that begins moving on its own the moment the
     * glass lights up is worse than one extra press.
     */
    fun toggleBlackout() {
        if (_uiState.value.isBlackedOut) {
            _uiState.update { it.copy(isBlackedOut = false) }
            return
        }
        pause()
        _uiState.update { it.copy(isBlackedOut = true) }
    }

    fun increaseSpeed() = stepSpeed(steps = 1)

    fun decreaseSpeed() = stepSpeed(steps = -1)

    fun increaseFontSize() = stepFontSize(stepSp = FONT_STEP_SP)

    fun decreaseFontSize() = stepFontSize(stepSp = -FONT_STEP_SP)

    /** The imported face, if there is one and it is still readable. */
    fun importedFontFile() = customFontStore.importedFont()

    /**
     * Copies the picked font into app storage and switches to it. A file that cannot be read is
     * ignored rather than selected — better to stay on a working face than show nothing.
     */
    fun importCustomFont(uri: String) {
        viewModelScope.launch {
            val stored = customFontStore.importFrom(Uri.parse(uri)) ?: return@launch
            updateTypography(
                _uiState.value.typography.copy(
                    fontId = TypographySettings.CUSTOM_FONT_ID,
                    customFontUri = stored.path,
                ),
            )
        }
    }

    /** Moves whichever unit the reader is working in — words per minute, or pixels per second. */
    private fun stepSpeed(steps: Int) {
        updateScrollSettings(_uiState.value.scroll.steppedSpeed(steps))
    }

    private fun stepFontSize(stepSp: Float) {
        val current = _uiState.value.typography
        updateTypography(current.copy(sizeSp = current.sizeSp + stepSp))
    }

    /** Applies a scroll-settings change to the live prompter and stores it a moment later. */
    fun updateScrollSettings(settings: ScrollSettings) {
        val coerced = settings.coerced()
        _uiState.update { it.copy(scroll = coerced) }
        if (isFollowingGlobalDefaults) pendingScroll.value = coerced
    }

    /**
     * Loads a preset into what is on screen right now.
     *
     * When this script follows the global defaults the preset is written through to them as one
     * atomic edit — three debounced writes could leave the surface rendering a new type size
     * against the old margins for a frame. When it has a preset of its own, this is a session
     * change like any other and nothing is stored.
     */
    fun applyPresetToSession(presetId: Long) {
        viewModelScope.launch {
            val preset = getPreset(presetId) ?: return@launch
            _uiState.update {
                it.copy(
                    typography = preset.typography,
                    layout = preset.layout,
                    scroll = preset.scroll,
                )
            }
            if (isFollowingGlobalDefaults) applyPreset(presetId)
        }
    }

    /**
     * Saves what is on screen as a new preset.
     *
     * Built from the live state rather than from [settingsRepository], because for a script with
     * an assigned preset the values being looked at are deliberately not in storage —
     * `SaveCurrentSettingsAsPreset` would quietly save the wrong thing.
     */
    fun saveSessionAsPreset(name: String) {
        viewModelScope.launch { savePreset(sessionAsPreset(name)) }
    }

    /**
     * Writes what is on screen back to the preset this script is assigned, keeping its name.
     *
     * Only offered for a user preset. On a built-in [SavePreset] would take a copy, and the script
     * would end up pointing at a second "Studio" it never asked for.
     */
    fun saveSessionToAssignedPreset() {
        val state = _uiState.value
        val presetId = state.presetId ?: return
        if (state.isPresetBuiltIn) return
        val name = state.presetName ?: return
        viewModelScope.launch { savePreset(sessionAsPreset(name).copy(id = presetId)) }
    }

    private fun sessionAsPreset(name: String): Preset {
        val state = _uiState.value
        return Preset(
            name = name,
            typography = state.typography,
            layout = state.layout,
            scroll = state.scroll,
        )
    }

    /**
     * Loads the script, then decides where its settings come from.
     *
     * These are one operation rather than two because the answer to the second depends on the
     * first: a script with an assigned preset reads through that preset, and the global observer
     * must never be started for it — left running, its first emission would overwrite the preset
     * with the defaults a fraction of a second after the screen opened.
     */
    private fun loadScriptAndResolveSettings() {
        viewModelScope.launch {
            val script = getScript(scriptId)
            _uiState.update {
                it.copy(
                    title = script?.title.orEmpty(),
                    paragraphs = ScriptParser.paragraphs(script?.body.orEmpty()),
                    wordCount = script?.wordCount ?: 0,
                    isLoading = false,
                )
            }

            val preset = script?.presetId?.let { getPreset(it) }
            if (preset == null) {
                observeGlobalSettings()
            } else {
                seedFromPreset(preset)
            }
        }
    }

    /**
     * All three blocks in one update, so the surface never lays out a new type size against the
     * old margins.
     */
    private fun seedFromPreset(preset: Preset) {
        _uiState.update {
            it.copy(
                typography = preset.typography,
                layout = preset.layout,
                scroll = preset.scroll,
                presetId = preset.id,
                presetName = preset.name,
                isPresetBuiltIn = preset.isBuiltIn,
            )
        }
    }

    private fun observeGlobalSettings() {
        viewModelScope.launch {
            combine(
                settingsRepository.typography,
                settingsRepository.layout,
                settingsRepository.scroll,
            ) { typography, layout, scroll -> Triple(typography, layout, scroll) }
                .collect { (typography, layout, scroll) ->
                    _uiState.update {
                        it.copy(typography = typography, layout = layout, scroll = scroll)
                    }
                }
        }
    }

    companion object {
        /** Matches the spec's stepper granularity for size (docs/SPEC.md §6.2). */
        private const val FONT_STEP_SP = 2f
        private const val SETTINGS_WRITE_DEBOUNCE_MILLIS = 200L
        private const val ONE_SECOND_MILLIS = 1_000L
        private const val PRESETS_STOP_TIMEOUT_MILLIS = 5_000L

        val Factory = viewModelFactory {
            initializer {
                val container = prompterContainer()
                val route: Destination.Prompter = createSavedStateHandle().toRoute()
                PrompterViewModel(
                    scriptId = route.scriptId,
                    getScript = container.getScript,
                    getPreset = container.getPreset,
                    savePreset = container.savePreset,
                    applyPreset = container.applyPreset,
                    observePresets = container.observePresets,
                    settingsRepository = container.settingsRepository,
                    customFontStore = container.customFontStore,
                )
            }
        }
    }
}
