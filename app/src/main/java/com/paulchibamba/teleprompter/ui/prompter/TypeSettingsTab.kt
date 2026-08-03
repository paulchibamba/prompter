package com.paulchibamba.teleprompter.ui.prompter

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.paulchibamba.teleprompter.domain.model.CaseMode
import com.paulchibamba.teleprompter.domain.model.PromptAlign
import com.paulchibamba.teleprompter.domain.model.TypographySettings
import com.paulchibamba.teleprompter.ui.components.LabelledSlider
import com.paulchibamba.teleprompter.ui.components.SegmentedOptionRow
import kotlin.math.roundToInt

/**
 * The typography controls (docs/SPEC.md §6).
 *
 * Everything here previews on the actual script behind the sheet rather than on a sample, because
 * the only question that matters is whether *this* text is readable through *your* glass.
 */
@Composable
fun TypeSettingsTab(
    typography: TypographySettings,
    onTypographyChanged: (TypographySettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SizeSlider(typography, onTypographyChanged)
        LineHeightSlider(typography, onTypographyChanged)
        TrackingSlider(typography, onTypographyChanged)
        ParagraphSpacingSlider(typography, onTypographyChanged)
        WeightPicker(typography, onTypographyChanged)
        AlignmentPicker(typography, onTypographyChanged)
        CasePicker(typography, onTypographyChanged)
    }
}

@Composable
private fun SizeSlider(
    typography: TypographySettings,
    onChanged: (TypographySettings) -> Unit,
) {
    LabelledSlider(
        label = "Size",
        value = typography.sizeSp,
        range = TypographySettings.MIN_SIZE_SP..TypographySettings.MAX_SIZE_SP,
        step = 2f,
        valueLabel = "${typography.sizeSp.roundToInt()} sp",
        onValueChange = { onChanged(typography.copy(sizeSp = it)) },
    )
}

/**
 * The single most impactful setting after size, and the one most teleprompter apps omit entirely.
 */
@Composable
private fun LineHeightSlider(
    typography: TypographySettings,
    onChanged: (TypographySettings) -> Unit,
) {
    LabelledSlider(
        label = "Line height",
        value = typography.lineHeightMul,
        range = TypographySettings.MIN_LINE_HEIGHT_MUL..TypographySettings.MAX_LINE_HEIGHT_MUL,
        step = 0.05f,
        valueLabel = String.format("%.2f×", typography.lineHeightMul),
        onValueChange = { onChanged(typography.copy(lineHeightMul = it)) },
    )
}

@Composable
private fun TrackingSlider(
    typography: TypographySettings,
    onChanged: (TypographySettings) -> Unit,
) {
    LabelledSlider(
        label = "Letter spacing",
        value = typography.letterSpacingEm,
        range = TypographySettings.MIN_LETTER_SPACING_EM..TypographySettings.MAX_LETTER_SPACING_EM,
        step = 0.005f,
        valueLabel = String.format("%+.3f em", typography.letterSpacingEm),
        onValueChange = { onChanged(typography.copy(letterSpacingEm = it)) },
        supportingText = "A little positive spacing helps at distance, and in mirrored text.",
    )
}

@Composable
private fun ParagraphSpacingSlider(
    typography: TypographySettings,
    onChanged: (TypographySettings) -> Unit,
) {
    LabelledSlider(
        label = "Paragraph spacing",
        value = typography.paragraphSpacingEm,
        range = TypographySettings.MIN_PARAGRAPH_SPACING_EM..TypographySettings.MAX_PARAGRAPH_SPACING_EM,
        step = 0.1f,
        valueLabel = String.format("%.1f em", typography.paragraphSpacingEm),
        onValueChange = { onChanged(typography.copy(paragraphSpacingEm = it)) },
    )
}

/**
 * Each name is drawn in the weight it selects, so the choice is made by eye rather than by
 * remembering what "500" looks like.
 *
 * Chips rather than a segmented row: five options with real words in them do not fit across a
 * narrow phone, and shrinking the labels would defeat the point of showing the specimen.
 */
@Composable
private fun WeightPicker(
    typography: TypographySettings,
    onChanged: (TypographySettings) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(text = "Weight", style = MaterialTheme.typography.titleSmall)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(top = 4.dp),
        ) {
            NAMED_WEIGHTS.forEach { (weight, name) ->
                FilterChip(
                    selected = typography.weight == weight,
                    onClick = { onChanged(typography.copy(weight = weight)) },
                    label = { Text(text = name, fontWeight = FontWeight(weight)) },
                )
            }
        }
        Text(
            text = "400 tends to thin out on a beam splitter; 700 blooms.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun AlignmentPicker(
    typography: TypographySettings,
    onChanged: (TypographySettings) -> Unit,
) {
    SegmentedOptionRow(
        label = "Alignment",
        options = listOf(PromptAlign.LEFT, PromptAlign.CENTER),
        selectedOption = typography.textAlign,
        onOptionSelected = { onChanged(typography.copy(textAlign = it)) },
    ) { align ->
        Text(if (align == PromptAlign.LEFT) "Left" else "Centre")
    }
}

@Composable
private fun CasePicker(
    typography: TypographySettings,
    onChanged: (TypographySettings) -> Unit,
) {
    SegmentedOptionRow(
        label = "Case",
        options = listOf(CaseMode.NONE, CaseMode.UPPER),
        selectedOption = typography.caseTransform,
        onOptionSelected = { onChanged(typography.copy(caseTransform = it)) },
        // The option stays available; the tradeoff is stated rather than decided for the user.
        supportingText = "All caps is traditional but slower to read — try bold instead.",
    ) { case ->
        Text(if (case == CaseMode.NONE) "Normal" else "UPPERCASE")
    }
}

/**
 * The five weights worth naming. The stored value is a plain number, so a preset written by hand
 * can still carry 600 or 800 — these are the ones offered by eye.
 */
private val NAMED_WEIGHTS = listOf(
    300 to "Light",
    400 to "Regular",
    500 to "Medium",
    700 to "Bold",
    900 to "Black",
)
