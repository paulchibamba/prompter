package com.paulchibamba.teleprompter.ui.prompter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
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
 * Feeds the prompter surface: the script's paragraphs, and the three settings blocks that decide
 * how they are drawn.
 *
 * The blocks are observed rather than read once, so a change made from the quick-settings sheet
 * lands on the text the reader is looking at without leaving the screen.
 */
class PrompterViewModel(
    private val scriptId: Long,
    private val getScript: GetScript,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrompterUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadScript()
        observeSettings(settingsRepository)
    }

    private fun loadScript() {
        viewModelScope.launch {
            val script = getScript(scriptId)
            _uiState.update {
                it.copy(
                    title = script?.title.orEmpty(),
                    paragraphs = ScriptParser.paragraphs(script?.body.orEmpty()),
                    isLoading = false,
                )
            }
        }
    }

    private fun observeSettings(settingsRepository: SettingsRepository) {
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
