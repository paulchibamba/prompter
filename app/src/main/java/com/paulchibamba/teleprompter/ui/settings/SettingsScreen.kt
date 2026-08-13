package com.paulchibamba.teleprompter.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The way in to everything that is not adjusted while reading (docs/SPEC.md §5.4).
 *
 * The quick-settings sheet is the primary settings surface — it previews on the real script, which
 * is the only way to judge type through glass — so this screen deliberately does not duplicate it.
 * It holds what cannot be decided mid-take: presets, and the remote.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onOpenPresets: () -> Unit,
    onOpenRemoteMapping: () -> Unit,
    onOpenKeySniffer: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            SettingsEntry(
                title = "Presets",
                subtitle = "Save, rename and apply named settings.",
                onClick = onOpenPresets,
            )
            HorizontalDivider()
            SettingsEntry(
                title = "Remote & buttons",
                subtitle = "Bind a Bluetooth remote to prompter actions.",
                onClick = onOpenRemoteMapping,
            )
            HorizontalDivider()
            SettingsEntry(
                title = "Key sniffer",
                subtitle = "See exactly what a remote sends, and from which device.",
                onClick = onOpenKeySniffer,
            )
        }
    }
}

@Composable
private fun SettingsEntry(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
