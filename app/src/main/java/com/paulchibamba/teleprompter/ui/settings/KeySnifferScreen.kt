package com.paulchibamba.teleprompter.ui.settings

import androidx.compose.runtime.Composable
import com.paulchibamba.teleprompter.ui.components.PlaceholderScaffold

/**
 * Placeholder. The real screen logs every incoming key event — keycode, name, scancode, device name
 * and descriptor, source flags, repeat count — so an unknown remote can be identified before any
 * bindings are written for it.
 */
@Composable
fun KeySnifferScreen(onNavigateBack: () -> Unit) {
    PlaceholderScaffold(
        title = "Key sniffer",
        description = "Every key event, and the device that sent it.",
        onNavigateBack = onNavigateBack,
    )
}
