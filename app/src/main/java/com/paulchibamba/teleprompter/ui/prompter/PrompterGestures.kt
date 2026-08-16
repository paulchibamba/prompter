package com.paulchibamba.teleprompter.ui.prompter

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs

/**
 * The prompter's gestures (docs/SPEC.md §10).
 *
 * Every one of them has a button equivalent in the control bar. They are accelerators for someone
 * whose hands are on the rig rather than the only way to reach anything — which is also why they
 * are allowed to be a little hard to trigger by accident.
 *
 * One-finger vertical drag is deliberately absent here: the list scrolls itself, and
 * [PauseOnManualScrub] listens to it rather than intercepting the drag. Anything else would mean
 * re-implementing fling and over-scroll to get back what `LazyColumn` already does.
 */
fun Modifier.prompterGestures(
    onTap: () -> Unit,
    onDoubleTap: () -> Unit,
    onBlackoutStart: () -> Unit,
    onBlackoutEnd: () -> Unit,
    onPinch: (zoomRatio: Float) -> Unit,
    onSpeedStep: (steps: Int) -> Unit,
): Modifier = this
    .pointerInput(Unit) {
        detectTapsAndHold(
            onTap = onTap,
            onDoubleTap = onDoubleTap,
            onHoldStart = onBlackoutStart,
            onHoldEnd = onBlackoutEnd,
        )
    }
    .pointerInput(Unit) {
        detectTwoFingerGestures(onPinch = onPinch, onSpeedStep = onSpeedStep)
    }

/**
 * Tap, double tap, and press-and-hold.
 *
 * Compose's own `detectTapGestures` reports a long press but never its release, and blackout is a
 * hold: the glass goes dark for as long as the finger is down and comes back when it lifts (§10).
 * That one requirement is why this is hand-rolled.
 *
 * A drag is not a tap. When the list underneath claims the pointer for scrolling, the wait below
 * reports a cancellation and the gesture is abandoned — so scrubbing never toggles the control bar
 * or blacks the screen out.
 */
private suspend fun PointerInputScope.detectTapsAndHold(
    onTap: () -> Unit,
    onDoubleTap: () -> Unit,
    onHoldStart: () -> Unit,
    onHoldEnd: () -> Unit,
) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)

        var wasClaimedElsewhere = false
        val firstUp = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
            waitForUpOrCancellation().also { wasClaimedElsewhere = it == null }
        }
        if (wasClaimedElsewhere) return@awaitEachGesture

        if (firstUp == null) {
            onHoldStart()
            waitForUpOrCancellation()
            onHoldEnd()
            return@awaitEachGesture
        }

        // A second touch this soon is a double tap; otherwise the first one stands on its own.
        val secondDown = withTimeoutOrNull(viewConfiguration.doubleTapTimeoutMillis) {
            awaitFirstDown(requireUnconsumed = false)
        }
        if (secondDown == null) {
            onTap()
        } else {
            waitForUpOrCancellation()
            onDoubleTap()
        }
    }
}

/**
 * Pinch for size, two-finger vertical drag for speed (§10).
 *
 * The two are **locked apart**: whichever moves first past the touch slop owns the rest of the
 * gesture. Both are driven by the same two fingers, and applying them together turns a small
 * imprecision in one into an unwanted change in the other — you reach to slow the scroll down and
 * the type jumps a size with it.
 *
 * Speed moves in whole steps rather than continuously, so it matches what the − and + buttons do
 * and stays in whichever unit the reader is working in.
 */
private suspend fun PointerInputScope.detectTwoFingerGestures(
    onPinch: (zoomRatio: Float) -> Unit,
    onSpeedStep: (steps: Int) -> Unit,
) {
    val slop = viewConfiguration.touchSlop
    val pixelsPerSpeedStep = PIXELS_PER_SPEED_STEP.toPx()

    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)

        var mode = TwoFingerMode.UNDECIDED
        var previousSpan = 0f
        var previousFocalY = 0f
        var spanTravel = 0f
        var dragTravel = 0f
        var unspentDrag = 0f

        while (true) {
            val event = awaitPointerEvent()
            val pressed = event.changes.count { it.pressed }
            if (pressed == 0) break

            if (pressed < 2) {
                // Down to one finger: hand the gesture back to the list rather than half-tracking it.
                previousSpan = 0f
                mode = TwoFingerMode.UNDECIDED
                continue
            }

            val span = event.calculateCentroidSize(useCurrent = true)
            val focalY = event.calculateCentroid(useCurrent = true).y
            if (previousSpan == 0f) {
                previousSpan = span
                previousFocalY = focalY
                spanTravel = 0f
                dragTravel = 0f
                unspentDrag = 0f
                continue
            }

            val spanChange = span - previousSpan
            val focalChange = focalY - previousFocalY

            if (mode == TwoFingerMode.UNDECIDED) {
                spanTravel += abs(spanChange)
                dragTravel += abs(focalChange)
                mode = when {
                    spanTravel > slop && spanTravel >= dragTravel -> TwoFingerMode.PINCH
                    dragTravel > slop -> TwoFingerMode.SPEED
                    else -> TwoFingerMode.UNDECIDED
                }
            }

            when (mode) {
                TwoFingerMode.PINCH -> if (previousSpan > 0f && span > 0f) onPinch(span / previousSpan)

                TwoFingerMode.SPEED -> {
                    // Up the screen is faster, so the sign flips: y grows downwards.
                    unspentDrag -= focalChange
                    val steps = (unspentDrag / pixelsPerSpeedStep).toInt()
                    if (steps != 0) {
                        onSpeedStep(steps)
                        unspentDrag -= steps * pixelsPerSpeedStep
                    }
                }

                TwoFingerMode.UNDECIDED -> Unit
            }

            if (mode != TwoFingerMode.UNDECIDED) {
                // Claim the pointers, so two fingers never scroll the list as well.
                event.changes.forEach { it.consume() }
            }
            previousSpan = span
            previousFocalY = focalY
        }
    }
}

private enum class TwoFingerMode { UNDECIDED, PINCH, SPEED }

/** A comfortable notch: a drag the height of a thumb moves the speed a few steps, not thirty. */
private val PIXELS_PER_SPEED_STEP = 48.dp
