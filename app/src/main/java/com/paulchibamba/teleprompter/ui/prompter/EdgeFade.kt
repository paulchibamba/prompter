package com.paulchibamba.teleprompter.ui.prompter

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Fades the text out at the top and bottom of the frame (docs/SPEC.md §7.4).
 *
 * Lines appearing and vanishing at a hard edge "pop", and the eye follows the pop instead of the
 * reading line. Softening the entry and exit costs nothing and removes a real distraction.
 *
 * This erases alpha rather than painting a colour over the text — `DstOut` against an offscreen
 * layer — so it works whatever background the user has chosen. Drawing a black gradient instead
 * would look wrong the moment somebody picks black-on-white.
 */
fun Modifier.edgeFade(edgeFadePercent: Float): Modifier {
    if (edgeFadePercent <= 0f) return this

    return this
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()

            val fadeHeight = size.height * (edgeFadePercent / 100f)
            if (fadeHeight <= 0f) return@drawWithContent
            val fadeFraction = (fadeHeight / size.height).coerceIn(0f, 0.5f)

            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.Black,
                    fadeFraction to Color.Transparent,
                    1f - fadeFraction to Color.Transparent,
                    1f to Color.Black,
                ),
                blendMode = BlendMode.DstOut,
            )
        }
}
