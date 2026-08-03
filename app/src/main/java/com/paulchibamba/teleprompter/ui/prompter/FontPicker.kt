package com.paulchibamba.teleprompter.ui.prompter

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paulchibamba.teleprompter.domain.model.TypographySettings
import com.paulchibamba.teleprompter.ui.theme.PrompterFonts
import java.io.File

/**
 * The current face, shown in itself, opening the picker on tap.
 */
@Composable
fun FontPickerRow(
    fontId: String,
    customFontFile: File?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Font",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = PrompterFonts.displayNameFor(fontId),
            fontFamily = PrompterFonts.familyFor(fontId, customFontFile),
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/**
 * Every face, each drawn in itself (docs/SPEC.md §6.1).
 *
 * Names alone would be useless here — nobody chooses a reading face from the word "Newsreader".
 * The specimen is the control.
 */
@Composable
fun FontPickerDialog(
    selectedFontId: String,
    customFontFile: File?,
    onFontSelected: (String) -> Unit,
    onCustomFontImported: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { onCustomFontImported(it.toString()) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reading face") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                PrompterFonts.bundledChoices.forEach { choice ->
                    FontSpecimenRow(
                        displayName = choice.displayName,
                        fontFamily = PrompterFonts.familyFor(choice.id),
                        isSelected = choice.id == selectedFontId,
                        onSelect = { onFontSelected(choice.id) },
                    )
                }
                if (customFontFile != null) {
                    FontSpecimenRow(
                        displayName = "Imported font",
                        fontFamily = PrompterFonts.familyFor(
                            TypographySettings.CUSTOM_FONT_ID,
                            customFontFile,
                        ),
                        isSelected = selectedFontId == TypographySettings.CUSTOM_FONT_ID,
                        onSelect = { onFontSelected(TypographySettings.CUSTOM_FONT_ID) },
                    )
                }
                TextButton(
                    // Anything, because providers routinely report a .ttf as an octet stream and
                    // filtering on font MIME types hides the file the user is looking straight at.
                    onClick = { importLauncher.launch(arrayOf("*/*")) },
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Text("Import a font…")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@Composable
private fun FontSpecimenRow(
    displayName: String,
    fontFamily: FontFamily,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = isSelected, onClick = null)
        Text(
            text = displayName,
            fontFamily = fontFamily,
            fontSize = SPECIMEN_SIZE_SP.sp,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

/** Large enough that the letterforms are actually distinguishable from one another. */
private const val SPECIMEN_SIZE_SP = 28
