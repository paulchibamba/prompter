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
    /** Denormalised on the script; the scroll pace is derived from it. */
    val wordCount: Int = 0,
    val isPlaying: Boolean = false,
    /** Seconds still to show in the pre-roll; 0 when no countdown is running (docs/SPEC.md §8.4). */
    val countdownRemaining: Int = 0,
    val isBlackedOut: Boolean = false,
    /**
     * Playback stopped because the reader dragged the text, not because they pressed pause. It is
     * tracked separately so the prompter can say why it stopped and offer a way back (§8.4).
     */
    val isPausedByScrub: Boolean = false,
    val typography: TypographySettings = TypographySettings(),
    val layout: LayoutSettings = LayoutSettings(),
    val scroll: ScrollSettings = ScrollSettings(),
    /**
     * The preset this script is assigned, if any (docs/SPEC.md §3.1, §4). A name here means the
     * settings above came from that preset rather than from the global defaults — and that
     * adjusting them changes this session only.
     */
    val presetId: Long? = null,
    val presetName: String? = null,
    /** Built-ins are read-only, so the "save back to this preset" action is not offered for them. */
    val isPresetBuiltIn: Boolean = false,
    val isLoading: Boolean = true,
) {
    val hasContent: Boolean
        get() = paragraphs.any { it.isNotBlank() }

    val isCountingDown: Boolean
        get() = countdownRemaining > 0
}
