package com.paulchibamba.teleprompter.ui.prompter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * How tall the script lays out at the current style and column width.
 *
 * This is what makes words-per-minute self-calibrating (docs/SPEC.md §8.1). Speed is derived from
 * the *laid-out* height rather than the font size, so raising the size makes the content taller,
 * raises pixels-per-second by the same factor, and leaves the reading pace exactly where it was.
 * Get this wrong and every font-size change silently becomes a speed change.
 *
 * Measurement happens off the main thread and only when the body, the style or the width actually
 * changes — a ten-thousand-word script is not something to measure during a frame.
 */
@Composable
fun rememberContentHeightPx(
    paragraphs: List<String>,
    textStyle: TextStyle,
    columnWidthPx: Int,
    paragraphSpacingPx: Float,
    density: Density,
): Float {
    val textMeasurer = rememberTextMeasurer()
    var contentHeightPx by remember { mutableFloatStateOf(0f) }

    // Hashing the inputs keeps the effect from re-running when an unrelated setting changes, and
    // keeps the key cheap: the body itself can be megabytes.
    val measurementKey = remember(paragraphs, textStyle, columnWidthPx, paragraphSpacingPx, density) {
        MeasurementKey(
            bodyHash = paragraphs.hashCode(),
            styleHash = textStyle.hashCode(),
            widthPx = columnWidthPx,
            paragraphSpacingPx = paragraphSpacingPx,
        )
    }

    LaunchedEffect(measurementKey) {
        if (columnWidthPx <= 0 || paragraphs.isEmpty()) {
            contentHeightPx = 0f
            return@LaunchedEffect
        }
        contentHeightPx = withContext(Dispatchers.Default) {
            val textHeight = textMeasurer.measure(
                text = AnnotatedString(paragraphs.joinToString("\n")),
                style = textStyle,
                constraints = Constraints(maxWidth = columnWidthPx),
                // Explicitly the surface's density, with the font scale pinned to 1 exactly as the
                // surface renders it. Measuring at the system font scale instead makes the pace
                // wrong by however much the user has adjusted their text size — silently, and only
                // on their device.
                density = density,
            ).size.height.toFloat()

            // Paragraph spacing is padding on each item, so it is not part of the measured text.
            textHeight + paragraphs.size * paragraphSpacingPx
        }
    }

    return contentHeightPx
}

private data class MeasurementKey(
    val bodyHash: Int,
    val styleHash: Int,
    val widthPx: Int,
    val paragraphSpacingPx: Float,
)
