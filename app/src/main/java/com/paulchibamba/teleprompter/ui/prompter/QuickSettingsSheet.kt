package com.paulchibamba.teleprompter.ui.prompter

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.paulchibamba.teleprompter.domain.model.TypographySettings

/**
 * The prompter's primary settings surface (docs/SPEC.md §5.3).
 *
 * Two decisions make it useful rather than merely present. It occupies a little over half the
 * screen, so the script stays visible above it — and the scrim is transparent, so the text behind
 * is not dimmed while you are judging whether it is readable. Every change lands on the real
 * script immediately; there is no sample text and no apply button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickSettingsSheet(
    typography: TypographySettings,
    onTypographyChanged: (TypographySettings) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedTab by remember { mutableStateOf(QuickSettingsTab.TYPE) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        // Dimming the script would defeat the purpose: the point is to judge the real text while
        // adjusting it.
        scrimColor = Color.Transparent,
    ) {
        // The fraction goes on the *content*, not the sheet. Sizing the sheet container itself
        // anchors it to the top of the screen and the script ends up below it, which is backwards.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(SHEET_HEIGHT_FRACTION)
                .navigationBarsPadding(),
        ) {
            QuickSettingsTabRow(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                when (selectedTab) {
                    QuickSettingsTab.TYPE -> TypeSettingsTab(
                        typography = typography,
                        onTypographyChanged = onTypographyChanged,
                    )

                    QuickSettingsTab.LAYOUT -> NotYetBuiltMessage(
                        "Margins, reading line, edge fade and mirroring arrive with the layout step.",
                    )

                    QuickSettingsTab.SCROLL -> NotYetBuiltMessage(
                        "Speed mode, countdown and end behaviour arrive with the scroll step.",
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickSettingsTabRow(
    selectedTab: QuickSettingsTab,
    onTabSelected: (QuickSettingsTab) -> Unit,
) {
    TabRow(selectedTabIndex = selectedTab.ordinal) {
        QuickSettingsTab.entries.forEach { tab ->
            Tab(
                selected = tab == selectedTab,
                onClick = { onTabSelected(tab) },
                text = { Text(tab.label) },
            )
        }
    }
}

@Composable
private fun NotYetBuiltMessage(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 24.dp),
    )
}

enum class QuickSettingsTab(val label: String) {
    TYPE("Type"),
    LAYOUT("Layout"),
    SCROLL("Scroll"),
}

/** A little over half, so the script above stays legible while it is being adjusted. */
private const val SHEET_HEIGHT_FRACTION = 0.55f
