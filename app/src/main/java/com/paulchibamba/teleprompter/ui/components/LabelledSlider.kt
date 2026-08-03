package com.paulchibamba.teleprompter.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * A settings control with three ways to reach the same value: drag the slider for a feel, press
 * the steppers for one increment, or tap the readout to type an exact number.
 *
 * All three matter here. Sliders are good for hunting a value by eye against live text, steppers
 * are good for "one more", and typing is the only way to reproduce a number someone wrote down
 * after calibrating against their rig.
 */
@Composable
fun LabelledSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    step: Float,
    valueLabel: String,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
) {
    var isTypingValue by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        SliderHeader(
            label = label,
            valueLabel = valueLabel,
            onReadoutClick = { isTypingValue = true },
        )
        SliderWithSteppers(
            label = label,
            value = value,
            range = range,
            step = step,
            onValueChange = onValueChange,
        )
        if (supportingText != null) {
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (isTypingValue) {
        ExactValueDialog(
            label = label,
            currentValue = value,
            range = range,
            onConfirm = {
                onValueChange(it)
                isTypingValue = false
            },
            onDismiss = { isTypingValue = false },
        )
    }
}

@Composable
private fun SliderHeader(label: String, valueLabel: String, onReadoutClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = valueLabel,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.End,
            modifier = Modifier
                .clickable(onClick = onReadoutClick)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun SliderWithSteppers(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    step: Float,
    onValueChange: (Float) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        StepperButton(
            icon = Icons.Filled.KeyboardArrowDown,
            description = "Decrease $label",
            onClick = { onValueChange((value - step).coerceIn(range)) },
        )
        Slider(
            value = value.coerceIn(range),
            onValueChange = { onValueChange(snapToStep(it, range, step)) },
            valueRange = range,
            steps = discreteStepCount(range, step),
            modifier = Modifier.weight(1f),
        )
        StepperButton(
            icon = Icons.Filled.Add,
            description = "Increase $label",
            onClick = { onValueChange((value + step).coerceIn(range)) },
        )
    }
}

@Composable
private fun StepperButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Icon(imageVector = icon, contentDescription = description)
    }
}

/** Lets someone reproduce an exact value they calibrated earlier, rather than hunting for it. */
@Composable
private fun ExactValueDialog(
    label: String,
    currentValue: Float,
    range: ClosedFloatingPointRange<Float>,
    onConfirm: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    var typed by remember { mutableStateOf(formatForEditing(currentValue)) }
    val parsed = typed.toFloatOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(label) },
        text = {
            Column {
                OutlinedTextField(
                    value = typed,
                    onValueChange = { typed = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.widthIn(min = 160.dp),
                )
                Text(
                    text = "${formatForEditing(range.start)} to ${formatForEditing(range.endInclusive)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            // Out-of-range input is clamped rather than refused: the user's intent is clear, and
            // an error message here would be pedantry.
            TextButton(
                onClick = { parsed?.let { onConfirm(it.coerceIn(range)) } },
                enabled = parsed != null,
            ) {
                Text("Set")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun formatForEditing(value: Float): String =
    if (value == value.roundToInt().toFloat()) value.roundToInt().toString() else value.toString()

private fun snapToStep(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    step: Float,
): Float {
    if (step <= 0f) return value
    val stepsFromStart = ((value - range.start) / step).roundToInt()
    return (range.start + stepsFromStart * step).coerceIn(range)
}

/** Compose counts the gaps *between* discrete positions, so it is one fewer than the positions. */
private fun discreteStepCount(range: ClosedFloatingPointRange<Float>, step: Float): Int {
    if (step <= 0f) return 0
    return ((range.endInclusive - range.start) / step).roundToInt() - 1
}
