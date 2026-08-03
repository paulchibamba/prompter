package com.paulchibamba.teleprompter.ui.prompter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paulchibamba.teleprompter.domain.color.ContrastRatio
import com.paulchibamba.teleprompter.domain.model.TypographySettings

/** A text-and-background pairing that is known to read well (docs/SPEC.md §6.9). */
data class ColourPreset(
    val name: String,
    val textColor: Long,
    val backgroundColor: Long,
)

val COLOUR_PRESETS = listOf(
    ColourPreset("White on black", 0xFFFFFFFF, 0xFF000000),
    ColourPreset("Black on white", 0xFF000000, 0xFFFFFFFF),
    ColourPreset("Amber on black", 0xFFFFBF00, 0xFF000000),
    ColourPreset("Yellow on black", 0xFFFFF200, 0xFF000000),
    ColourPreset("Grey on black", 0xFFBDBDBD, 0xFF000000),
)

/** Enough range to build a low-contrast pairing on purpose, and to recover from one. */
private val TEXT_COLOURS = listOf(
    0xFFFFFFFF, 0xFFE0E0E0, 0xFFBDBDBD, 0xFF9E9E9E, 0xFF757575,
    0xFFFFBF00, 0xFFFFF200, 0xFF000000,
)

private val BACKGROUND_COLOURS = listOf(
    0xFF000000, 0xFF121212, 0xFF303030, 0xFF616161,
    0xFFFFFFFF, 0xFFFFF8E1,
)

@Composable
fun ColourSettings(
    typography: TypographySettings,
    onTypographyChanged: (TypographySettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(text = "Colour", style = MaterialTheme.typography.titleSmall)

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(top = 8.dp),
        ) {
            COLOUR_PRESETS.forEach { preset ->
                ColourPresetSwatch(
                    preset = preset,
                    isSelected = preset.matches(typography),
                    onSelect = {
                        onTypographyChanged(
                            typography.copy(
                                textColor = preset.textColor,
                                backgroundColor = preset.backgroundColor,
                            ),
                        )
                    },
                )
            }
        }

        ColourSwatchRow(
            label = "Text",
            colours = TEXT_COLOURS,
            selectedColour = typography.textColor,
            onSelect = { onTypographyChanged(typography.copy(textColor = it)) },
        )
        ColourSwatchRow(
            label = "Background",
            colours = BACKGROUND_COLOURS,
            selectedColour = typography.backgroundColor,
            onSelect = { onTypographyChanged(typography.copy(backgroundColor = it)) },
        )

        ContrastReadout(
            textColor = typography.textColor,
            backgroundColor = typography.backgroundColor,
        )
    }
}

/**
 * A specimen of the pairing itself rather than a name — the same reasoning as the font picker.
 * "Amber on black" tells you less in a glance than amber on black does.
 */
@Composable
private fun ColourPresetSwatch(
    preset: ColourPreset,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(width = 84.dp, height = 56.dp)
            .background(Color(preset.backgroundColor), RoundedCornerShape(8.dp))
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                shape = RoundedCornerShape(8.dp),
            )
            .clickable(onClick = onSelect),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Aa",
            color = Color(preset.textColor),
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

/**
 * Text and background chosen independently, so a pairing outside the presets is reachable — which
 * is the whole reason the contrast readout below is worth having.
 */
@Composable
private fun ColourSwatchRow(
    label: String,
    colours: List<Long>,
    selectedColour: Long,
    onSelect: (Long) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(88.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            colours.forEach { colour ->
                ColourSwatch(
                    colour = colour,
                    isSelected = colour == selectedColour,
                    onSelect = { onSelect(colour) },
                )
            }
        }
    }
}

@Composable
private fun ColourSwatch(colour: Long, isSelected: Boolean, onSelect: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(Color(colour), CircleShape)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                shape = CircleShape,
            )
            .clickable(onClick = onSelect),
    )
}

/**
 * The live contrast number, with a warning below 7:1.
 *
 * Stated rather than enforced: a user who has measured their own rig may know something the
 * formula does not, and refusing their choice would be presumptuous.
 */
@Composable
private fun ContrastReadout(textColor: Long, backgroundColor: Long) {
    val ratio = ContrastRatio.between(textColor, backgroundColor)
    val isComfortable = ContrastRatio.meetsEnhancedContrast(ratio)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(
                    color = if (isComfortable) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    shape = CircleShape,
                ),
        )
        Text(
            text = buildContrastMessage(ratio, isComfortable),
            style = MaterialTheme.typography.bodySmall,
            color = if (isComfortable) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.error
            },
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

private fun buildContrastMessage(ratio: Float, isComfortable: Boolean): String {
    val formatted = "Contrast ${ContrastRatio.format(ratio)}"
    return if (isComfortable) formatted else "$formatted — low for reading at distance"
}

private fun ColourPreset.matches(typography: TypographySettings): Boolean =
    typography.textColor == textColor && typography.backgroundColor == backgroundColor
