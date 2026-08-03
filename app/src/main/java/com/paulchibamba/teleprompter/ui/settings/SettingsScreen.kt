package com.paulchibamba.teleprompter.ui.settings

import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.paulchibamba.teleprompter.ui.components.PlaceholderScaffold

/**
 * Placeholder. The real settings screen holds the global typography, layout and scroll defaults,
 * preset management, backup, and the About section — including the line stating that the app makes
 * no network connections.
 */
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onOpenRemoteMapping: () -> Unit,
    onOpenKeySniffer: () -> Unit,
) {
    PlaceholderScaffold(
        title = "Settings",
        description = "Global defaults, presets, and remote setup.",
        onNavigateBack = onNavigateBack,
    ) {
        OutlinedButton(onClick = onOpenRemoteMapping) {
            Text("Remote & buttons")
        }
        TextButton(onClick = onOpenKeySniffer) {
            Text("Key sniffer")
        }
    }
}
