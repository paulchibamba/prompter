package com.paulchibamba.teleprompter.ui.prompter

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import com.paulchibamba.teleprompter.domain.model.OrientLock

/**
 * Holds the prompter to one orientation while it is on screen (docs/SPEC.md §7.6).
 *
 * A rig is usually landscape, and a phone that rotates itself mid-take because someone tilted the
 * mount is a ruined take. The lock is released on the way out, so the rest of the app keeps
 * following the sensor.
 */
@Composable
fun OrientationLock(lock: OrientLock) {
    val activity = LocalView.current.context.findHostingActivity() ?: return

    DisposableEffect(activity, lock) {
        activity.requestedOrientation = lock.toActivityInfo()
        onDispose {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
}

/**
 * The sensor variants rather than the fixed ones, so a locked orientation still lets the phone
 * flip 180° — which is exactly what happens when a rig is mounted the other way up.
 */
private fun OrientLock.toActivityInfo(): Int = when (this) {
    OrientLock.FOLLOW_SENSOR -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    OrientLock.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    OrientLock.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
}

private tailrec fun Context.findHostingActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findHostingActivity()
    else -> null
}
