package com.paulchibamba.teleprompter.ui.prompter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import android.net.Uri
import androidx.navigation.toRoute
import com.paulchibamba.teleprompter.domain.model.LayoutSettings
import com.paulchibamba.teleprompter.domain.model.ScrollSettings
import com.paulchibamba.teleprompter.domain.model.TypographySettings
import com.paulchibamba.teleprompter.data.io.CustomFontStore
import com.paulchibamba.teleprompter.domain.repository.SettingsRepository
import com.paulchibamba.teleprompter.domain.text.ScriptParser
import com.paulchibamba.teleprompter.domain.usecase.GetScript
import com.paulchibamba.teleprompter.ui.navigation.Destination
import com.paulchibamba.teleprompter.ui.prompterContainer
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
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
    private val settingsRepository: SettingsRepository,
    private val customFontStore: CustomFontStore,
) : ViewModel() {

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
        loadScript()
        observeSettings()
        persistTypographyAfterAdjustingStops()
        persistScrollAfterAdjustingStops()
        persistLayoutAfterAdjustingStops()
    }

    /**
     * Applies a typography change to the visible text immediately and stores it a moment later.
     *
     * The stored value only changes when the write lands, so the settings flow does not emit
     * mid-drag and cannot fight the value under the reader's finger.
     */
    fun updateTypography(settings: TypographySettings) {
        val coerced = settings.coerced()
        _uiState.update { it.copy(typography = coerced) }
        pendingTypography.value = coerced
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
        pendingLayout.value = coerced
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
        pendingScroll.value = coerced
    }

    private fun loadScript() {
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
        }
    }

    private fun observeSettings() {
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

        val Factory = viewModelFactory {
            initializer {
                val container = prompterContainer()
                val route: Destination.Prompter = createSavedStateHandle().toRoute()
                PrompterViewModel(
                    scriptId = route.scriptId,
                    getScript = container.getScript,
                    settingsRepository = container.settingsRepository,
                    customFontStore = container.customFontStore,
                )
            }
        }
    }
}
