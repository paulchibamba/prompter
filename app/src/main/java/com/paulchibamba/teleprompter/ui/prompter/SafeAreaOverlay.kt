package com.paulchibamba.teleprompter.ui.prompter

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * A dashed rectangle showing exactly where the text can go (docs/SPEC.md §7.2).
 *
 * This is the fastest way to set margins for a beam splitter, and it is what makes them usable at
 * all: hold the phone in the rig, turn this on, and adjust until the box matches the glass you can
 * actually see. Without it, margins are trial and error against a moving script.
 *
 * The corner labels give the resulting dp, so a setting that worked can be written down and
 * reproduced rather than re-hunted.
 */
@Composable
fun SafeAreaOverlay(
    marginLeft: Dp,
    marginRight: Dp,
    marginTop: Dp,
    marginBottom: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                val left = marginLeft.toPx()
                val top = marginTop.toPx()
                val width = size.width - left - marginRight.toPx()
                val height = size.height - top - marginBottom.toPx()
                if (width <= 0f || height <= 0f) return@drawBehind

                drawRect(
                    color = SAFE_AREA_COLOUR,
                    topLeft = Offset(left, top),
                    size = Size(width, height),
                    style = Stroke(
                        width = STROKE_WIDTH_PX,
                        pathEffect = PathEffect.dashPathEffect(DASH_PATTERN, phase = 0f),
                    ),
                )
            },
    ) {
        MarginLabel("${marginLeft.value.roundToInt()}dp", Alignment.CenterStart, marginLeft)
        MarginLabel("${marginRight.value.roundToInt()}dp", Alignment.CenterEnd, marginRight)
        MarginLabel("${marginTop.value.roundToInt()}dp", Alignment.TopCenter, marginTop)
        MarginLabel("${marginBottom.value.roundToInt()}dp", Alignment.BottomCenter, marginBottom)
    }
}

/**
 * Sits just inside its edge of the frame, so each number is next to the margin it describes rather
 * than in a legend the reader has to decode.
 */
@Composable
private fun BoxScope.MarginLabel(
    text: String,
    alignment: Alignment,
    margin: Dp,
) {
    Text(
        text = text,
        color = SAFE_AREA_COLOUR,
        fontSize = LABEL_SIZE_SP.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier
            .align(alignment)
            .padding(inwardPaddingFor(alignment, margin)),
    )
}

private fun inwardPaddingFor(alignment: Alignment, margin: Dp) = when (alignment) {
    Alignment.CenterStart -> PaddingValues(start = margin + LABEL_GAP)
    Alignment.CenterEnd -> PaddingValues(end = margin + LABEL_GAP)
    Alignment.TopCenter -> PaddingValues(top = margin + LABEL_GAP)
    else -> PaddingValues(bottom = margin + LABEL_GAP)
}

/** Cyan: unlikely to be mistaken for the reading line, the text, or anything in the script. */
private val SAFE_AREA_COLOUR = Color(0xFF00E5FF)
private val DASH_PATTERN = floatArrayOf(18f, 14f)
private val LABEL_GAP = 6.dp
private const val STROKE_WIDTH_PX = 3f
private const val LABEL_SIZE_SP = 12
