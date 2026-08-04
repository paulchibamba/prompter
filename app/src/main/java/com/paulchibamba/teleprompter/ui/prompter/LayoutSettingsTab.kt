package com.paulchibamba.teleprompter.ui.prompter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.paulchibamba.teleprompter.domain.model.LayoutSettings
import com.paulchibamba.teleprompter.domain.model.LineStyle
import com.paulchibamba.teleprompter.ui.components.LabelledSlider
import com.paulchibamba.teleprompter.ui.components.SegmentedOptionRow
import kotlin.math.roundToInt

/**
 * Where the text sits on the glass (docs/SPEC.md §7.1, §7.2, §6.7).
 *
 * Margins are percentages rather than dp because the constraint is the beam splitter's crop, and
 * that is proportional — a setting found on one phone should mean the same thing on another.
 */
@Composable
fun LayoutSettingsTab(
    layout: LayoutSettings,
    onLayoutChanged: (LayoutSettings) -> Unit,
    isSafeAreaVisible: Boolean,
    onSafeAreaVisibilityChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SafeAreaToggle(isSafeAreaVisible, onSafeAreaVisibilityChanged)
        SideMarginControls(layout, onLayoutChanged)
        VerticalMarginControls(layout, onLayoutChanged)
        LineLengthControls(layout, onLayoutChanged)
        ReadingLineControls(layout, onLayoutChanged)
        EdgeFadeSlider(layout, onLayoutChanged)
    }
}

/**
 * Where the current line sits, and how it is marked (docs/SPEC.md §7.3).
 *
 * Position is a percentage from the top because that is how it is judged in a rig — some people
 * read best with the line high, so most of the visible text is what is coming next; others want it
 * lower so they can see what they have just said.
 */
@Composable
private fun ReadingLineControls(
    layout: LayoutSettings,
    onChanged: (LayoutSettings) -> Unit,
) {
    LabelledSlider(
        label = "Reading line",
        value = layout.readingLinePct,
        range = LayoutSettings.MIN_READING_LINE_PCT..LayoutSettings.MAX_READING_LINE_PCT,
        step = 1f,
        valueLabel = "${layout.readingLinePct.roundToInt()}% from top",
        onValueChange = { onChanged(layout.copy(readingLinePct = it)) },
    )

    SegmentedOptionRow(
        label = "Marker style",
        options = LineStyle.entries,
        selectedOption = layout.readingLineStyle,
        onOptionSelected = { onChanged(layout.copy(readingLineStyle = it)) },
        supportingText = "Arrows sit in the margins, so they never cover a word.",
    ) { style ->
        Text(
            text = when (style) {
                LineStyle.OFF -> "Off"
                LineStyle.LINE -> "Line"
                LineStyle.ARROWS -> "Arrows"
                LineStyle.BAND -> "Band"
            },
        )
    }

    if (layout.readingLineStyle != LineStyle.OFF) {
        ReadingLineColourRow(layout, onChanged)
        ReadingLineOpacitySlider(layout, onChanged)
    }
}

@Composable
private fun ReadingLineColourRow(
    layout: LayoutSettings,
    onChanged: (LayoutSettings) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
        Text(
            text = "Marker colour",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(120.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            READING_LINE_COLOURS.forEach { rgb ->
                val withCurrentOpacity = rgb or (layout.readingLineColor and ALPHA_MASK)
                ReadingLineSwatch(
                    colour = Color(rgb or OPAQUE_ALPHA),
                    isSelected = layout.readingLineColor.rgbOnly() == rgb,
                    onSelect = { onChanged(layout.copy(readingLineColor = withCurrentOpacity)) },
                )
            }
        }
    }
}

@Composable
private fun ReadingLineSwatch(colour: Color, isSelected: Boolean, onSelect: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(colour, CircleShape)
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
 * The marker is translucent by default and stays adjustable, because how much it should assert
 * itself depends on the glass — a bright rig washes out a subtle line, a dark one makes a strong
 * one shout.
 */
@Composable
private fun ReadingLineOpacitySlider(
    layout: LayoutSettings,
    onChanged: (LayoutSettings) -> Unit,
) {
    val opacity = layout.readingLineColor.alphaFraction()

    LabelledSlider(
        label = "Marker opacity",
        value = opacity,
        range = 0f..1f,
        step = 0.05f,
        valueLabel = "${(opacity * 100).roundToInt()}%",
        onValueChange = { fraction ->
            onChanged(layout.copy(readingLineColor = layout.readingLineColor.withAlpha(fraction)))
        },
    )
}

@Composable
private fun EdgeFadeSlider(
    layout: LayoutSettings,
    onChanged: (LayoutSettings) -> Unit,
) {
    LabelledSlider(
        label = "Edge fade",
        value = layout.edgeFadePct,
        range = LayoutSettings.MIN_EDGE_FADE_PCT..LayoutSettings.MAX_EDGE_FADE_PCT,
        step = 1f,
        valueLabel = if (layout.edgeFadePct <= 0f) "Off" else "${layout.edgeFadePct.roundToInt()}%",
        onValueChange = { onChanged(layout.copy(edgeFadePct = it)) },
        supportingText = "Softens lines entering and leaving the frame.",
    )
}

private fun Long.rgbOnly(): Long = this and RGB_MASK

private fun Long.alphaFraction(): Float = ((this shr 24) and 0xFF).toFloat() / 255f

private fun Long.withAlpha(fraction: Float): Long {
    val alpha = (fraction.coerceIn(0f, 1f) * 255f).roundToInt().toLong()
    return (alpha shl 24) or rgbOnly()
}

/** Colours that read as "this is the mark", not as part of the script. */
private val READING_LINE_COLOURS = listOf(
    0xFF3B30L, // red
    0xFFBF00L, // amber
    0x00E5FFL, // cyan
    0x7CFC00L, // green
    0xFFFFFFL, // white
)

private const val RGB_MASK = 0x00FFFFFFL
private const val ALPHA_MASK = -0x1000000L
private const val OPAQUE_ALPHA = -0x1000000L

/**
 * The calibration loop: turn it on, hold the phone in the rig, adjust until the dashed box matches
 * the glass. It is deliberately the first thing in the tab.
 */
@Composable
private fun SafeAreaToggle(isVisible: Boolean, onChanged: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Show safe area", style = MaterialTheme.typography.titleSmall)
            Text(
                text = "Hold the phone in the rig and match the box to the glass.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = isVisible, onCheckedChange = onChanged)
    }
}

/**
 * Left and right, linked by default. Unlinking is for glass that is not centred over the phone,
 * which is common enough in cheaper rigs to be worth the toggle.
 */
@Composable
private fun SideMarginControls(
    layout: LayoutSettings,
    onChanged: (LayoutSettings) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Link side margins",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = layout.linkLeftRight,
            onCheckedChange = { onChanged(layout.copy(linkLeftRight = it)) },
        )
    }

    if (layout.linkLeftRight) {
        MarginSlider(
            label = "Side margins",
            value = layout.marginLeftPct,
            onValueChange = {
                onChanged(layout.copy(marginLeftPct = it, marginRightPct = it))
            },
        )
    } else {
        MarginSlider(
            label = "Left margin",
            value = layout.marginLeftPct,
            onValueChange = { onChanged(layout.copy(marginLeftPct = it)) },
        )
        MarginSlider(
            label = "Right margin",
            value = layout.marginRightPct,
            onValueChange = { onChanged(layout.copy(marginRightPct = it)) },
        )
    }
}

@Composable
private fun VerticalMarginControls(
    layout: LayoutSettings,
    onChanged: (LayoutSettings) -> Unit,
) {
    MarginSlider(
        label = "Top margin",
        value = layout.marginTopPct,
        onValueChange = { onChanged(layout.copy(marginTopPct = it)) },
    )
    MarginSlider(
        label = "Bottom margin",
        value = layout.marginBottomPct,
        onValueChange = { onChanged(layout.copy(marginBottomPct = it)) },
    )
}

@Composable
private fun MarginSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
) {
    LabelledSlider(
        label = label,
        value = value,
        range = LayoutSettings.MIN_MARGIN_PCT..LayoutSettings.MAX_MARGIN_PCT,
        step = 1f,
        valueLabel = "${value.roundToInt()}%",
        onValueChange = onValueChange,
    )
}

/**
 * Capping the measure is off by default, because it only helps once the text is wide enough to
 * lose the return sweep — which depends on the rig, not on the app.
 */
@Composable
private fun LineLengthControls(
    layout: LayoutSettings,
    onChanged: (LayoutSettings) -> Unit,
) {
    val isCapped = layout.maxMeasureCh > LayoutSettings.MEASURE_OFF

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Limit line length", style = MaterialTheme.typography.titleSmall)
            Text(
                text = "Long lines are where the eye loses its place on the way back.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = isCapped,
            onCheckedChange = { enabled ->
                onChanged(
                    layout.copy(
                        maxMeasureCh = if (enabled) DEFAULT_MEASURE_CH else LayoutSettings.MEASURE_OFF,
                    ),
                )
            },
        )
    }

    if (isCapped) {
        LabelledSlider(
            label = "Characters per line",
            value = layout.maxMeasureCh.toFloat(),
            range = MIN_USEFUL_MEASURE_CH..MAX_USEFUL_MEASURE_CH,
            step = 1f,
            valueLabel = "${layout.maxMeasureCh} ch",
            onValueChange = { onChanged(layout.copy(maxMeasureCh = it.roundToInt())) },
        )
    }
}

/** A comfortable starting measure for prose, and the range either side of it that is worth having. */
private const val DEFAULT_MEASURE_CH = 45
private const val MIN_USEFUL_MEASURE_CH = 20f
private const val MAX_USEFUL_MEASURE_CH = 90f
