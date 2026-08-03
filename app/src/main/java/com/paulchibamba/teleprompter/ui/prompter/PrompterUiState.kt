package com.paulchibamba.teleprompter.ui.prompter

import com.paulchibamba.teleprompter.domain.model.LayoutSettings
import com.paulchibamba.teleprompter.domain.model.ScrollSettings
import com.paulchibamba.teleprompter.domain.model.TypographySettings

data class PrompterUiState(
    val title: String = "",
    /**
     * The body split one entry per line, empty lines kept as spacers. The surface renders one
     * `LazyColumn` item per entry rather than one giant text node, so measurement stays
     * incremental on a long script (docs/SPEC.md §8.3).
     */
    val paragraphs: List<String> = emptyList(),
    val typography: TypographySettings = TypographySettings(),
    val layout: LayoutSettings = LayoutSettings(),
    val scroll: ScrollSettings = ScrollSettings(),
    val isLoading: Boolean = true,
) {
    val hasContent: Boolean
        get() = paragraphs.any { it.isNotBlank() }
}
