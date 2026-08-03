package com.paulchibamba.teleprompter.ui.prompter

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.isActive

/**
 * Scrolls the surface off the frame clock (docs/SPEC.md §8.2).
 *
 * Smoothness is the whole product, and two details decide it:
 *
 * **Sub-pixel carry.** `LazyListState` quantises scrolling to whole pixels. At a typical reading
 * pace a frame's travel is a fraction of a pixel, so rounding every frame independently would
 * throw that fraction away sixty times a second — the text would visibly stutter and, worse, run
 * slow. The remainder is carried into the next frame instead.
 *
 * **Ease-in.** Starting at full speed is a jerk that pulls the eye. The first few hundred
 * milliseconds ramp up on a smoothstep curve.
 *
 * The loop allocates nothing per frame: the carry lives in a holder that outlives it, which is
 * also what lets a pause resume exactly where it left off rather than dropping the fraction.
 */
@Composable
fun AutoScrollEffect(
    listState: LazyListState,
    isPlaying: Boolean,
    pixelsPerSecond: Float,
    rampMillis: Int,
    direction: Int = FORWARD,
    onReachedEnd: () -> Unit,
) {
    val carry = remember { SubPixelCarry() }

    LaunchedEffect(isPlaying, pixelsPerSecond, rampMillis, direction) {
        if (!isPlaying || pixelsPerSecond <= 0f) return@LaunchedEffect

        var previousFrameNanos = withFrameNanos { it }
        val rampStartNanos = previousFrameNanos

        while (isActive) {
            val frameNanos = withFrameNanos { it }
            val elapsedSeconds = secondsBetween(previousFrameNanos, frameNanos)
            previousFrameNanos = frameNanos

            val ramp = rampProgress(rampStartNanos, frameNanos, rampMillis)
            val travel = pixelsPerSecond * ramp * elapsedSeconds * direction + carry.value

            val wholePixels = travel.toInt()
            carry.value = travel - wholePixels

            if (wholePixels != 0) {
                val consumed = listState.scrollBy(wholePixels.toFloat())
                if (hasHitTheEnd(consumed, wholePixels)) {
                    carry.value = 0f
                    onReachedEnd()
                    break
                }
            }
        }
    }
}

/** Survives pause and resume, so the fraction of a pixel owed is never lost. */
private class SubPixelCarry {
    var value: Float = 0f
}

/**
 * Guarded against pathological frames: a dropped frame or a stall would otherwise produce one huge
 * jump, which reads far worse than a moment of running slightly slow.
 */
private fun secondsBetween(fromNanos: Long, toNanos: Long): Float =
    ((toNanos - fromNanos) / NANOS_PER_SECOND).coerceIn(0f, MAX_FRAME_SECONDS)

/** Smoothstep, so the start eases in rather than snapping to full speed. */
private fun rampProgress(startNanos: Long, nowNanos: Long, rampMillis: Int): Float {
    if (rampMillis <= 0) return 1f
    val progress = ((nowNanos - startNanos) / NANOS_PER_MILLISECOND / rampMillis).coerceIn(0f, 1f)
    return progress * progress * (3f - 2f * progress)
}

/**
 * The list refused to move in the direction we asked, which means there is nothing left to scroll.
 * Only meaningful when we actually asked it to move.
 */
private fun hasHitTheEnd(consumed: Float, requested: Int): Boolean =
    consumed == 0f && requested != 0

const val FORWARD = 1
const val BACKWARD = -1

private const val NANOS_PER_SECOND = 1_000_000_000f
private const val NANOS_PER_MILLISECOND = 1_000_000f

/** 50ms — anything slower than 20fps is treated as 20fps rather than teleporting the text. */
private const val MAX_FRAME_SECONDS = 0.05f
