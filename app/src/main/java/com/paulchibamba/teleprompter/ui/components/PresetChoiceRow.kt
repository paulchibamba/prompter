package com.paulchibamba.teleprompter.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * One preset in a pick-one list. Shared by the library's "assign a preset to this script" dialog
 * and the prompter's "apply a preset to what I am reading now" dialog — two different questions
 * that offer the same list of answers.
 */
@Composable
fun PresetChoiceRow(
    label: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .selectable(selected = isSelected, onClick = onSelect)
            .padding(vertical = 8.dp),
    ) {
        // Null callback: the whole row is the target, so the button must not be separately clickable.
        RadioButton(selected = isSelected, onClick = null)
        Text(text = label, modifier = Modifier.padding(start = 12.dp))
    }
}
