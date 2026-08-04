package com.paulchibamba.teleprompter.ui.prompter

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.paulchibamba.teleprompter.domain.model.LineStyle

/**
 * The fixed point on screen where the current line sits (docs/SPEC.md §7.3).
 *
 * It is drawn at exactly the percentage the user chose, and the scrolling text is offset by half a
 * line so the *middle* of the current line lands on it. Marking the top of a line box instead
 * would put the indicator visibly above the words it is pointing at.
 */
@Composable
fun ReadingLineIndicator(
    style: LineStyle,
    colour: Color,
    readingLineFromTop: Dp,
    lineHeight: Dp,
    marginLeft: Dp,
    marginRight: Dp,
    modifier: Modifier = Modifier,
) {
    if (style == LineStyle.OFF) return

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centreY = readingLineFromTop.toPx()
            when (style) {
                LineStyle.LINE -> drawThinLine(centreY, colour)
                LineStyle.BAND -> drawBand(centreY, lineHeight.toPx(), colour)
                LineStyle.ARROWS -> drawArrows(
                    centreY = centreY,
                    colour = colour,
                    marginLeftPx = marginLeft.toPx(),
                    marginRightPx = marginRight.toPx(),
                )
                LineStyle.OFF -> Unit
            }
        }
    }
}

private fun DrawScope.drawThinLine(centreY: Float, colour: Color) {
    val thickness = LINE_THICKNESS.toPx()
    drawRect(
        color = colour,
        topLeft = Offset(0f, centreY - thickness / 2f),
        size = Size(size.width, thickness),
    )
}

/** A bar the height of one line, so the current line sits inside it rather than beside it. */
private fun DrawScope.drawBand(centreY: Float, lineHeightPx: Float, colour: Color) {
    drawRect(
        color = colour,
        topLeft = Offset(0f, centreY - lineHeightPx / 2f),
        size = Size(size.width, lineHeightPx),
    )
}

/**
 * Arrows sit in the margins, pointing inward — never over the text.
 *
 * That placement is the point: an indicator that overlaps words costs you the word it covers, and
 * it will always be the word you were about to read.
 */
private fun DrawScope.drawArrows(
    centreY: Float,
    colour: Color,
    marginLeftPx: Float,
    marginRightPx: Float,
) {
    val height = ARROW_HEIGHT.toPx()
    val width = ARROW_WIDTH.toPx()
    val inset = ARROW_EDGE_INSET.toPx()

    // Never wider than the margin it lives in, so a narrow margin shrinks the arrow rather than
    // letting it stray across the first character.
    val leftWidth = minOf(width, (marginLeftPx - inset).coerceAtLeast(0f))
    val rightWidth = minOf(width, (marginRightPx - inset).coerceAtLeast(0f))

    if (leftWidth > 0f) {
        drawPath(
            path = trianglePointingRight(inset, centreY, leftWidth, height),
            color = colour,
        )
    }
    if (rightWidth > 0f) {
        drawPath(
            path = trianglePointingLeft(size.width - inset, centreY, rightWidth, height),
            color = colour,
        )
    }
}

private fun trianglePointingRight(left: Float, centreY: Float, width: Float, height: Float) =
    Path().apply {
        moveTo(left, centreY - height / 2f)
        lineTo(left + width, centreY)
        lineTo(left, centreY + height / 2f)
        close()
    }

private fun trianglePointingLeft(right: Float, centreY: Float, width: Float, height: Float) =
    Path().apply {
        moveTo(right, centreY - height / 2f)
        lineTo(right - width, centreY)
        lineTo(right, centreY + height / 2f)
        close()
    }

private val LINE_THICKNESS = 2.dp
private val ARROW_WIDTH = 18.dp
private val ARROW_HEIGHT = 28.dp
private val ARROW_EDGE_INSET = 4.dp
