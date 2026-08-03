package com.paulchibamba.teleprompter.domain.model

enum class SpeedMode { WPM, PIXELS }

enum class EndBehaviour { HOLD, LOOP, EXIT }

/**
 * How fast the text moves and what happens at the end (docs/SPEC.md §4.3, §8).
 *
 * Speed is expressed in words per minute by default because it is the only unit that stays
 * meaningful when the font size changes — see [com.paulchibamba.teleprompter.domain.scroll.WpmCalculator].
 */
data class ScrollSettings(
    val speedWpm: Int = 140,
    val speedMode: SpeedMode = SpeedMode.WPM,
    val speedPxPerSec: Float = 60f,
    val speedStepWpm: Int = 10,
    val rampMillis: Int = 350,
    val countdownSeconds: Int = 3,
    val keepScreenOn: Boolean = true,
    val brightnessOverride: Float = BRIGHTNESS_SYSTEM,
    val endBehaviour: EndBehaviour = EndBehaviour.HOLD,
) {
    /**
     * Clamps every value into its supported range. [brightnessOverride] keeps its
     * [BRIGHTNESS_SYSTEM] sentinel rather than being dragged up into the 0..1 range.
     */
    fun coerced(): ScrollSettings = copy(
        speedWpm = speedWpm.coerceIn(MIN_WPM, MAX_WPM),
        speedPxPerSec = speedPxPerSec.coerceIn(MIN_PX_PER_SEC, MAX_PX_PER_SEC),
        speedStepWpm = speedStepWpm.coerceIn(MIN_STEP_WPM, MAX_STEP_WPM),
        rampMillis = rampMillis.coerceIn(MIN_RAMP_MILLIS, MAX_RAMP_MILLIS),
        countdownSeconds = countdownSeconds.coerceIn(MIN_COUNTDOWN_SECONDS, MAX_COUNTDOWN_SECONDS),
        brightnessOverride = if (brightnessOverride < 0f) BRIGHTNESS_SYSTEM else brightnessOverride.coerceAtMost(1f),
    )

    /** The speed one [speedStepWpm] press up or down, clamped — used by the remote's speed actions. */
    fun steppedWpm(steps: Int): Int = (speedWpm + steps * speedStepWpm).coerceIn(MIN_WPM, MAX_WPM)

    companion object {
        /** [brightnessOverride] sentinel meaning "leave the system brightness alone". */
        const val BRIGHTNESS_SYSTEM = -1f

        const val MIN_WPM = 40
        const val MAX_WPM = 400
        const val MIN_PX_PER_SEC = 1f
        const val MAX_PX_PER_SEC = 2000f
        const val MIN_STEP_WPM = 1
        const val MAX_STEP_WPM = 50
        const val MIN_RAMP_MILLIS = 0
        const val MAX_RAMP_MILLIS = 3000
        const val MIN_COUNTDOWN_SECONDS = 0
        const val MAX_COUNTDOWN_SECONDS = 10
    }
}
