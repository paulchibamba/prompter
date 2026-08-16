package com.paulchibamba.teleprompter.ui.prompter

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

/**
 * The two-finger gestures (docs/SPEC.md §10).
 *
 * These are instrumented rather than unit tests because there is no other way to drive them: the
 * `adb shell input` command has no multi-touch, so pinch and two-finger drag cannot be exercised
 * from a script the way every other gesture in this step was. CI has no emulator, so these are run
 * against a connected device by hand.
 */
@RunWith(AndroidJUnit4::class)
class PrompterGesturesTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun pinchingOutwardsEnlargesTheTextAndDoesNotTouchTheSpeed() {
        val recorded = GestureRecorder()
        composeTestRule.setContent { GestureSurface(recorded) }

        composeTestRule.onNodeWithTag(SURFACE).performTouchInput {
            pinch(
                start0 = center + Offset(0f, -SMALL_SPAN),
                end0 = center + Offset(0f, -LARGE_SPAN),
                start1 = center + Offset(0f, SMALL_SPAN),
                end1 = center + Offset(0f, LARGE_SPAN),
            )
        }

        // Fingers moving apart is a bigger font, so the ratios multiply out to more than one.
        assertTrue("expected a zoom, got ${recorded.cumulativeZoom}", recorded.cumulativeZoom > 1.1f)
        assertEquals("pinching must not change the speed", 0, recorded.speedSteps)
    }

    @Test
    fun twoFingersDraggingUpwardsRaisesTheSpeedAndDoesNotResizeTheText() {
        val recorded = GestureRecorder()
        composeTestRule.setContent { GestureSurface(recorded) }

        composeTestRule.onNodeWithTag(SURFACE).performTouchInput {
            val left = center + Offset(-SMALL_SPAN, 0f)
            val right = center + Offset(SMALL_SPAN, 0f)
            down(0, left)
            down(1, right)
            repeat(DRAG_STEPS) {
                updatePointerBy(0, Offset(0f, -DRAG_STEP_PIXELS))
                updatePointerBy(1, Offset(0f, -DRAG_STEP_PIXELS))
                move()
            }
            up(0)
            up(1)
        }

        assertTrue("expected the speed to rise, got ${recorded.speedSteps}", recorded.speedSteps > 0)
        assertTrue(
            "dragging must not resize the text, got ${recorded.cumulativeZoom}",
            abs(recorded.cumulativeZoom - 1f) < 0.01f,
        )
    }

    @Test
    fun oneFingerDraggingLeavesBothTwoFingerGesturesAlone() {
        val recorded = GestureRecorder()
        composeTestRule.setContent { GestureSurface(recorded) }

        composeTestRule.onNodeWithTag(SURFACE).performTouchInput {
            swipeUp()
        }

        assertEquals(0, recorded.speedSteps)
        assertEquals(1f, recorded.cumulativeZoom, 0.001f)
    }

    private class GestureRecorder {
        var cumulativeZoom = 1f
        var speedSteps = 0
    }

    @Composable
    private fun GestureSurface(recorded: GestureRecorder) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag(SURFACE)
                .prompterGestures(
                    onTap = {},
                    onDoubleTap = {},
                    onBlackoutStart = {},
                    onBlackoutEnd = {},
                    onPinch = { recorded.cumulativeZoom *= it },
                    onSpeedStep = { recorded.speedSteps += it },
                ),
        )
    }

    private companion object {
        const val SURFACE = "gesture-surface"
        const val SMALL_SPAN = 100f
        const val LARGE_SPAN = 300f

        /** Comfortably past the 48dp a speed step costs, in several events so the lock engages. */
        const val DRAG_STEPS = 20
        const val DRAG_STEP_PIXELS = 30f
    }
}
