package com.paulchibamba.teleprompter.ui.prompter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import com.paulchibamba.teleprompter.domain.model.ScrollSettings
import com.paulchibamba.teleprompter.domain.repository.SettingsRepository
import com.paulchibamba.teleprompter.domain.text.ScriptParser
import com.paulchibamba.teleprompter.domain.usecase.GetScript
import com.paulchibamba.teleprompter.ui.navigation.Destination
import com.paulchibamba.teleprompter.ui.prompterContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
class PrompterViewModel(
    private val scriptId: Long,
    private val getScript: GetScript,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrompterUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadScript()
        observeSettings()
    }

    fun togglePlayPause() {
        _uiState.update { it.copy(isPlaying = !it.isPlaying) }
    }

    fun pause() {
        _uiState.update { it.copy(isPlaying = false) }
    }

    fun play() {
        _uiState.update { it.copy(isPlaying = true) }
    }

    fun increaseSpeed() = stepSpeed(steps = 1)

    fun decreaseSpeed() = stepSpeed(steps = -1)

    fun increaseFontSize() = stepFontSize(stepSp = FONT_STEP_SP)

    fun decreaseFontSize() = stepFontSize(stepSp = -FONT_STEP_SP)

    private fun stepSpeed(steps: Int) {
        val current = _uiState.value.scroll
        updateScroll(current.copy(speedWpm = current.steppedWpm(steps)))
    }

    private fun stepFontSize(stepSp: Float) {
        val current = _uiState.value.typography
        val stepped = current.copy(sizeSp = current.sizeSp + stepSp).coerced()
        viewModelScope.launch { settingsRepository.setTypography(stepped) }
    }

    private fun updateScroll(settings: ScrollSettings) {
        viewModelScope.launch { settingsRepository.setScroll(settings.coerced()) }
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

        val Factory = viewModelFactory {
            initializer {
                val container = prompterContainer()
                val route: Destination.Prompter = createSavedStateHandle().toRoute()
                PrompterViewModel(
                    scriptId = route.scriptId,
                    getScript = container.getScript,
                    settingsRepository = container.settingsRepository,
                )
            }
        }
    }
}
