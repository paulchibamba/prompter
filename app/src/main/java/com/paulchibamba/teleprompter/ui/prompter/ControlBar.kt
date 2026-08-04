package com.paulchibamba.teleprompter.ui.prompter

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * The prompter's transport (docs/SPEC.md §5.3).
 *
 * It sits over the text and hides itself after a few seconds, because the point of the screen is
 * the words. Every target is at least 48dp: this gets used at arm's length, in a rig, often in
 * the dark.
 *
 * Two rows rather than one — transport on top, adjustments below — so nothing shrinks below a
 * comfortable target on a narrow phone.
 */
@Composable
fun ControlBar(
    isVisible: Boolean,
    isPlaying: Boolean,
    speedWpm: Int,
    fontSizeSp: Float,
    onPlayPause: () -> Unit,
    onRestart: () -> Unit,
    onNudgeUp: () -> Unit,
    onNudgeDown: () -> Unit,
    onSpeedUp: () -> Unit,
    onSpeedDown: () -> Unit,
    onFontUp: () -> Unit,
    onFontDown: () -> Unit,
    isMirrored: Boolean,
    onToggleMirror: () -> Unit,
    onNavigateBack: () -> Unit,
    onOpenQuickSettings: () -> Unit,
    onInteraction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Every press restarts the idle countdown. Without this the bar walks out from under a reader
    // who is in the middle of using it — three seconds is not long when you are adjusting speed.
    fun resettingIdleTimer(action: () -> Unit): () -> Unit = {
        onInteraction()
        action()
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInVertically { it },
        exit = fadeOut() + slideOutVertically { it },
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SCRIM)
                .navigationBarsPadding()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TransportRow(
                isPlaying = isPlaying,
                onPlayPause = resettingIdleTimer(onPlayPause),
                onRestart = resettingIdleTimer(onRestart),
                onNudgeUp = resettingIdleTimer(onNudgeUp),
                onNudgeDown = resettingIdleTimer(onNudgeDown),
                isMirrored = isMirrored,
                onToggleMirror = resettingIdleTimer(onToggleMirror),
                onNavigateBack = onNavigateBack,
                onOpenQuickSettings = resettingIdleTimer(onOpenQuickSettings),
            )
            AdjustmentRow(
                speedWpm = speedWpm,
                fontSizeSp = fontSizeSp,
                onSpeedUp = resettingIdleTimer(onSpeedUp),
                onSpeedDown = resettingIdleTimer(onSpeedDown),
                onFontUp = resettingIdleTimer(onFontUp),
                onFontDown = resettingIdleTimer(onFontDown),
            )
        }
    }
}

@Composable
private fun TransportRow(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onRestart: () -> Unit,
    onNudgeUp: () -> Unit,
    onNudgeDown: () -> Unit,
    isMirrored: Boolean,
    onToggleMirror: () -> Unit,
    onNavigateBack: () -> Unit,
    onOpenQuickSettings: () -> Unit,
) {
    ControlRow {
        ControlButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            description = "Close prompter",
            onClick = onNavigateBack,
        )
        ControlButton(
            icon = Icons.Filled.Refresh,
            description = "Restart from the beginning",
            onClick = onRestart,
        )
        ControlButton(
            icon = Icons.Filled.KeyboardArrowUp,
            description = "Nudge up one line",
            onClick = onNudgeUp,
        )
        PlayPauseButton(isPlaying = isPlaying, onClick = onPlayPause)
        ControlButton(
            icon = Icons.Filled.KeyboardArrowDown,
            description = "Nudge down one line",
            onClick = onNudgeDown,
        )
        MirrorButton(isMirrored = isMirrored, onClick = onToggleMirror)
        ControlButton(
            icon = Icons.Filled.Settings,
            description = "Quick settings",
            onClick = onOpenQuickSettings,
        )
    }
}

@Composable
private fun AdjustmentRow(
    speedWpm: Int,
    fontSizeSp: Float,
    onSpeedUp: () -> Unit,
    onSpeedDown: () -> Unit,
    onFontUp: () -> Unit,
    onFontDown: () -> Unit,
) {
    ControlRow {
        StepperGroup(
            label = "$speedWpm wpm",
            decreaseDescription = "Slower",
            increaseDescription = "Faster",
            onDecrease = onSpeedDown,
            onIncrease = onSpeedUp,
        )
        StepperGroup(
            label = "${fontSizeSp.toInt()} sp",
            decreaseDescription = "Smaller text",
            increaseDescription = "Larger text",
            onDecrease = onFontDown,
            onIncrease = onFontUp,
        )
    }
}

/**
 * The beam-splitter toggle: one press to flip the image for the glass and back again.
 *
 * It shows a mirrored "R" rather than an icon, because the whole question this button answers is
 * "which way round is my text", and a letter answers it at a glance.
 */
@Composable
private fun MirrorButton(isMirrored: Boolean, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(TOUCH_TARGET)
            .semantics {
                contentDescription = if (isMirrored) "Turn off mirroring" else "Mirror for beam splitter"
            },
    ) {
        Text(
            text = "R",
            color = if (isMirrored) CONTROL_TINT else CONTROL_TINT.copy(alpha = 0.55f),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.graphicsLayer { scaleX = if (isMirrored) -1f else 1f },
        )
    }
}

/**
 * A minus/readout/plus trio. The readout is live, so the reader can see what a press did without
 * looking away from the glass for long.
 */
@Composable
private fun StepperGroup(
    label: String,
    decreaseDescription: String,
    increaseDescription: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ControlButton(
            icon = Icons.Filled.KeyboardArrowDown,
            description = decreaseDescription,
            onClick = onDecrease,
        )
        Text(
            text = label,
            color = CONTROL_TINT,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 76.dp),
        )
        ControlButton(
            icon = Icons.Filled.Add,
            description = increaseDescription,
            onClick = onIncrease,
        )
    }
}

@Composable
private fun ControlRow(content: @Composable () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        content()
    }
}

@Composable
private fun PlayPauseButton(isPlaying: Boolean, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(TOUCH_TARGET)
            .semantics { contentDescription = if (isPlaying) "Pause" else "Play" },
    ) {
        if (isPlaying) {
            PauseGlyph()
        } else {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = CONTROL_TINT,
                modifier = Modifier.size(36.dp),
            )
        }
    }
}

/**
 * Drawn rather than imported: the core icon set has no pause glyph, and pulling in the extended
 * set for two rectangles is not a trade worth making.
 */
@Composable
private fun PauseGlyph() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.size(36.dp).padding(vertical = 6.dp),
    ) {
        repeat(2) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(width = 9.dp, height = 24.dp)
                    .background(CONTROL_TINT, RoundedCornerShape(2.dp)),
            )
        }
    }
}

@Composable
private fun ControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(TOUCH_TARGET),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (enabled) CONTROL_TINT else CONTROL_TINT.copy(alpha = 0.3f),
        )
    }
}

/** Deliberately not themed: the prompter's background is whatever the user chose, usually black. */
private val CONTROL_TINT = Color.White
private val SCRIM = Color.Black.copy(alpha = 0.55f)
private val TOUCH_TARGET = 48.dp
