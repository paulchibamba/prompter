package com.paulchibamba.teleprompter.ui.prompter

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Puts the window into the state a teleprompter needs, and puts it back on the way out.
 *
 * System bars are hidden but recoverable with a swipe from the edge — never permanently, because a
 * reader who cannot get back to the clock or the battery indicator mid-take is being punished for
 * using the app.
 */
@Composable
fun ImmersiveScreen(keepScreenOn: Boolean) {
    val view = LocalView.current
    val window = view.context.findActivity()?.window ?: return

    HideSystemBarsWhileVisible(window, view)
    KeepScreenOnWhileVisible(window, keepScreenOn)
}

@Composable
private fun HideSystemBarsWhileVisible(window: android.view.Window, view: android.view.View) {
    DisposableEffect(window, view) {
        val controller = WindowCompat.getInsetsController(window, view)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())

        onDispose { controller.show(WindowInsetsCompat.Type.systemBars()) }
    }
}

/**
 * A script long enough to be worth prompting is longer than any screen timeout, and the reader's
 * hands are usually not on the phone.
 */
@Composable
private fun KeepScreenOnWhileVisible(window: android.view.Window, keepScreenOn: Boolean) {
    DisposableEffect(window, keepScreenOn) {
        if (keepScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose { window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
