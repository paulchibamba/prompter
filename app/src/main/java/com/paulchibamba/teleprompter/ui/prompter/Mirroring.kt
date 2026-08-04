package com.paulchibamba.teleprompter.ui.prompter

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Pre-flips the image so the beam splitter flips it back (docs/SPEC.md §7.5).
 *
 * This is applied to the text **and** the reading-line indicator together. Mirroring one without
 * the other would put the mark somewhere the current line is not — on a vertical flip the two
 * would end up on opposite sides of centre.
 *
 * Pointer input passes through the same transform, so a drag still moves the text the way the
 * finger physically moves. The controls are deliberately outside this: they are read by whoever is
 * operating the phone, not through the glass.
 */
fun Modifier.mirrored(horizontally: Boolean, vertically: Boolean): Modifier {
    if (!horizontally && !vertically) return this

    return graphicsLayer {
        scaleX = if (horizontally) -1f else 1f
        scaleY = if (vertically) -1f else 1f
    }
}
