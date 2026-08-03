package com.paulchibamba.teleprompter.ui.prompter

import androidx.compose.runtime.Composable
import com.paulchibamba.teleprompter.ui.components.PlaceholderScaffold

/**
 * Placeholder. The real prompter is full-screen and immersive: paragraphs scrolling off the frame
 * clock at a words-per-minute pace, a reading-line indicator, mirroring for beam-splitter glass, and
 * an auto-hiding control bar.
 */
@Composable
fun PrompterScreen(
    scriptId: Long,
    onNavigateBack: () -> Unit,
) {
    PlaceholderScaffold(
        title = "Prompter",
        description = "Prompting script $scriptId.",
        onNavigateBack = onNavigateBack,
    )
}
