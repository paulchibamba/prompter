package com.paulchibamba.teleprompter.ui.prompter

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import com.paulchibamba.teleprompter.domain.model.ScrollSettings

/**
 * Overrides screen brightness for the prompter only (docs/SPEC.md §6.9).
 *
 * A beam splitter throws away most of the light, so the phone often needs to be brighter than
 * anyone would want it elsewhere — and the room is frequently lit for the camera rather than for
 * reading. The override is scoped to this window and released on the way out, so the phone goes
 * back to whatever the system was doing.
 */
@Composable
fun ScreenBrightnessOverride(brightness: Float) {
    val view = LocalView.current
    val window = view.context.findHostActivity()?.window ?: return

    DisposableEffect(window, brightness) {
        window.applyBrightness(brightness)
        onDispose { window.applyBrightness(ScrollSettings.BRIGHTNESS_SYSTEM) }
    }
}

private fun android.view.Window.applyBrightness(brightness: Float) {
    attributes = attributes.apply {
        screenBrightness = if (brightness < 0f) {
            WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        } else {
            brightness.coerceIn(0f, 1f)
        }
    }
}

private tailrec fun Context.findHostActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findHostActivity()
    else -> null
}
