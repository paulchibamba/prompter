package com.paulchibamba.teleprompter.ui.settings

import androidx.compose.runtime.Composable
import com.paulchibamba.teleprompter.ui.components.PlaceholderScaffold

/**
 * Placeholder. The real screen lists every prompt action with its bound key, and binds new ones by
 * asking the user to press a button on the remote and reporting back which device sent it.
 */
@Composable
fun RemoteMappingScreen(onNavigateBack: () -> Unit) {
    PlaceholderScaffold(
        title = "Remote & buttons",
        description = "Bind remote buttons to prompter actions.",
        onNavigateBack = onNavigateBack,
    )
}
