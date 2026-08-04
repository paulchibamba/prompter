package com.paulchibamba.teleprompter.ui.prompter

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.unit.dp
import com.paulchibamba.teleprompter.domain.model.LayoutSettings
import com.paulchibamba.teleprompter.ui.components.LabelledSlider
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
    }
}

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
