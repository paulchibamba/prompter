package com.paulchibamba.teleprompter.ui.prompter

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Stops the scroll when the reader takes hold of the text (docs/SPEC.md §8.4).
 *
 * This watches the list's own drag interactions rather than intercepting pointer events, which is
 * what keeps fling and over-scroll behaving exactly as `LazyColumn` intends — and, more usefully,
 * distinguishes a *user* drag from the scroll engine's own `scrollBy`. Reading the scroll position
 * instead would fire on every frame of ordinary playback.
 */
@Composable
fun PauseOnManualScrub(listState: LazyListState, onScrubbed: () -> Unit) {
    LaunchedEffect(listState) {
        listState.interactionSource.interactions.collect { interaction ->
            if (interaction is DragInteraction.Start) onScrubbed()
        }
    }
}

/**
 * Says the scroll stopped because you grabbed it, and offers the way back.
 *
 * Playback deliberately does **not** pick up again on its own when the finger lifts: the reader has
 * moved to a different line and needs a moment to find it, and text that started moving again by
 * itself would be worse than one press.
 */
@Composable
fun ScrubPauseChip(
    isVisible: Boolean,
    onResume: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .padding(16.dp)
                .background(CHIP_BACKGROUND, RoundedCornerShape(50))
                .padding(start = 16.dp),
        ) {
            Text(
                text = "Paused",
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
            )
            TextButton(onClick = onResume) {
                Text(text = "Resume", color = RESUME_TINT)
            }
        }
    }
}

/** Not themed: it sits over whatever background the reader chose, which is usually black. */
private val CHIP_BACKGROUND = Color.Black.copy(alpha = 0.75f)
private val RESUME_TINT = Color(0xFF9FC7FF)
