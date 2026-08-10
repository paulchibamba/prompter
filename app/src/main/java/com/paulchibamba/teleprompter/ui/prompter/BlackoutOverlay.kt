package com.paulchibamba.teleprompter.ui.prompter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Kills the glass between takes (docs/SPEC.md §8.4).
 *
 * Deliberately nothing but black — a hint or a button would glow on the beam splitter, which is the
 * one thing this is here to stop. It covers everything including the control bar, and a tap
 * anywhere brings the script back.
 */
@Composable
fun BlackoutOverlay(
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                // No ripple: a flash of light on a screen that exists to be dark.
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = "Restore the script",
                onClick = onRestore,
            ),
    )
}
