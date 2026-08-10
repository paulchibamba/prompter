package com.paulchibamba.teleprompter.ui.prompter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The 3-2-1 before the text starts moving (docs/SPEC.md §5.3, §8.4).
 *
 * It is drawn **inside the mirror**, unlike the control bar. The countdown is for the person
 * reading through the glass — it is the cue to draw breath — so a mirrored rig has to show it the
 * right way round to them, not to whoever is holding the phone.
 *
 * The numeral is huge and the scrim is light: the point is to be unmissable in peripheral vision
 * while the reader is still looking at the first line.
 */
@Composable
fun CountdownOverlay(
    secondsRemaining: Int,
    textColor: Color,
    mirrorHorizontal: Boolean,
    mirrorVertical: Boolean,
    modifier: Modifier = Modifier,
) {
    if (secondsRemaining <= 0) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SCRIM)
            .semantics { contentDescription = "Starting in $secondsRemaining" },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = secondsRemaining.toString(),
            color = textColor,
            fontSize = NUMERAL_SIZE,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.mirrored(
                horizontally = mirrorHorizontal,
                vertically = mirrorVertical,
            ),
        )
    }
}

/** Dark enough to separate the numeral from the script, light enough to still read the first line. */
private val SCRIM = Color.Black.copy(alpha = 0.45f)

private val NUMERAL_SIZE = 160.sp
