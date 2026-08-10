package com.paulchibamba.teleprompter.ui.prompter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.paulchibamba.teleprompter.domain.model.EndBehaviour
import com.paulchibamba.teleprompter.domain.model.ScrollSettings
import com.paulchibamba.teleprompter.domain.model.SpeedMode
import com.paulchibamba.teleprompter.domain.scroll.WpmCalculator
import com.paulchibamba.teleprompter.ui.components.LabelledSlider
import com.paulchibamba.teleprompter.ui.components.SegmentedOptionRow
import com.paulchibamba.teleprompter.ui.components.SwitchRow
import kotlin.math.roundToInt

/**
 * How fast the text moves, how it starts, and what happens when it runs out (docs/SPEC.md §8).
 *
 * Whichever unit is selected, the *other* one is shown underneath it. Words per minute is the
 * meaningful unit and the default, but someone who has calibrated a rig in pixels per second should
 * be able to see how the two relate on their own script rather than having to trust the switch.
 */
@Composable
fun ScrollSettingsTab(
    scroll: ScrollSettings,
    onScrollChanged: (ScrollSettings) -> Unit,
    wordCount: Int,
    contentHeightPx: Float,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SpeedModePicker(scroll, onScrollChanged)
        SpeedSlider(scroll, onScrollChanged, wordCount, contentHeightPx)
        EstimatedDurationRow(scroll, wordCount, contentHeightPx)
        if (scroll.speedMode == SpeedMode.WPM) {
            SpeedStepSlider(scroll, onScrollChanged)
        }
        CountdownSlider(scroll, onScrollChanged)
        RampSlider(scroll, onScrollChanged)
        KeepScreenOnToggle(scroll, onScrollChanged)
        EndBehaviourPicker(scroll, onScrollChanged)
    }
}

@Composable
private fun SpeedModePicker(
    scroll: ScrollSettings,
    onChanged: (ScrollSettings) -> Unit,
) {
    SegmentedOptionRow(
        label = "Speed unit",
        options = SpeedMode.entries,
        selectedOption = scroll.speedMode,
        onOptionSelected = { onChanged(scroll.copy(speedMode = it)) },
        supportingText = "Words per minute holds the pace when the font size changes; pixels do not.",
    ) { mode ->
        Text(if (mode == SpeedMode.WPM) "Words/min" else "Pixels/sec")
    }
}

/**
 * One slider, two units. The subtitle carries the conversion for the script actually loaded, which
 * is the only place the two units can be compared honestly — the mapping depends on how tall this
 * script lays out at this size.
 */
@Composable
private fun SpeedSlider(
    scroll: ScrollSettings,
    onChanged: (ScrollSettings) -> Unit,
    wordCount: Int,
    contentHeightPx: Float,
) {
    when (scroll.speedMode) {
        SpeedMode.WPM -> LabelledSlider(
            label = "Speed",
            value = scroll.speedWpm.toFloat(),
            range = ScrollSettings.MIN_WPM.toFloat()..ScrollSettings.MAX_WPM.toFloat(),
            step = WPM_SLIDER_STEP,
            valueLabel = "${scroll.speedWpm} wpm",
            onValueChange = { onChanged(scroll.copy(speedWpm = it.roundToInt())) },
            supportingText = otherUnitFor(scroll, wordCount, contentHeightPx),
        )

        SpeedMode.PIXELS -> LabelledSlider(
            label = "Speed",
            value = scroll.speedPxPerSec,
            // Narrower than the storable range: the useful band is at the bottom of it, and a
            // slider that has to cross 2000px/s cannot be dragged to 60. Typing still reaches the
            // rest, and anything out of range is clamped when it is stored.
            range = MIN_USEFUL_PX_PER_SEC..MAX_USEFUL_PX_PER_SEC,
            step = ScrollSettings.SPEED_STEP_PX_PER_SEC,
            valueLabel = "${scroll.speedPxPerSec.roundToInt()} px/s",
            onValueChange = { onChanged(scroll.copy(speedPxPerSec = it)) },
            supportingText = otherUnitFor(scroll, wordCount, contentHeightPx),
        )
    }
}

/**
 * The same speed expressed in the unit that is *not* selected. Null until the script has been
 * measured — quoting a conversion of zero would read as a broken setting rather than a pending one.
 */
private fun otherUnitFor(
    scroll: ScrollSettings,
    wordCount: Int,
    contentHeightPx: Float,
): String? {
    if (wordCount <= 0 || contentHeightPx <= 0f) return null

    return when (scroll.speedMode) {
        SpeedMode.WPM -> {
            val pxPerSecond = WpmCalculator.pxPerSecond(scroll.speedWpm, contentHeightPx, wordCount)
            "About ${pxPerSecond.roundToInt()} px/s on this script."
        }

        SpeedMode.PIXELS -> {
            val wpm = WpmCalculator.wpmFor(scroll.speedPxPerSec, contentHeightPx, wordCount)
            "About $wpm wpm on this script. − and + move " +
                "${ScrollSettings.SPEED_STEP_PX_PER_SEC.roundToInt()} px/s."
        }
    }
}

/** What the script comes to at this pace, in the same "412 words · 2:56" form as the library. */
@Composable
private fun EstimatedDurationRow(
    scroll: ScrollSettings,
    wordCount: Int,
    contentHeightPx: Float,
) {
    val wpm = WpmCalculator.effectiveWpm(scroll, contentHeightPx, wordCount)
    val duration = WpmCalculator.formatDuration(WpmCalculator.estimatedSeconds(wordCount, wpm))

    Text(
        text = "$wordCount words · $duration at this speed",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

/**
 * How far one press of slower or faster moves. It is here rather than buried in the remote settings
 * because it is judged while reading: too small and the remote does nothing, too large and one
 * press overshoots the pace you were hunting for.
 */
@Composable
private fun SpeedStepSlider(
    scroll: ScrollSettings,
    onChanged: (ScrollSettings) -> Unit,
) {
    LabelledSlider(
        label = "Speed step",
        value = scroll.speedStepWpm.toFloat(),
        range = ScrollSettings.MIN_STEP_WPM.toFloat()..ScrollSettings.MAX_STEP_WPM.toFloat(),
        step = 1f,
        valueLabel = "${scroll.speedStepWpm} wpm",
        onValueChange = { onChanged(scroll.copy(speedStepWpm = it.roundToInt())) },
        supportingText = "How much one press of − or + changes the speed, here and on the remote.",
    )
}

/**
 * The pause between pressing play and the text moving, so the talent can draw breath rather than
 * being chased by a line that started without them.
 */
@Composable
private fun CountdownSlider(
    scroll: ScrollSettings,
    onChanged: (ScrollSettings) -> Unit,
) {
    LabelledSlider(
        label = "Countdown",
        value = scroll.countdownSeconds.toFloat(),
        range = ScrollSettings.MIN_COUNTDOWN_SECONDS.toFloat()..ScrollSettings.MAX_COUNTDOWN_SECONDS.toFloat(),
        step = 1f,
        valueLabel = if (scroll.countdownSeconds <= 0) "Off" else "${scroll.countdownSeconds} s",
        onValueChange = { onChanged(scroll.copy(countdownSeconds = it.roundToInt())) },
    )
}

/**
 * How long the text takes to reach full speed. Starting at full speed is a visible jerk that pulls
 * the eye off the line it was reading.
 */
@Composable
private fun RampSlider(
    scroll: ScrollSettings,
    onChanged: (ScrollSettings) -> Unit,
) {
    LabelledSlider(
        label = "Ease in",
        value = scroll.rampMillis.toFloat(),
        range = ScrollSettings.MIN_RAMP_MILLIS.toFloat()..ScrollSettings.MAX_RAMP_MILLIS.toFloat(),
        step = RAMP_SLIDER_STEP_MILLIS,
        valueLabel = if (scroll.rampMillis <= 0) "Off" else "${scroll.rampMillis} ms",
        onValueChange = { onChanged(scroll.copy(rampMillis = it.roundToInt())) },
        supportingText = "The text accelerates up to speed rather than snapping into motion.",
    )
}

@Composable
private fun KeepScreenOnToggle(
    scroll: ScrollSettings,
    onChanged: (ScrollSettings) -> Unit,
) {
    SwitchRow(
        title = "Keep screen on",
        subtitle = "The prompter never dims mid-take. It only applies on this screen.",
        checked = scroll.keepScreenOn,
        onCheckedChange = { onChanged(scroll.copy(keepScreenOn = it)) },
    )
}

@Composable
private fun EndBehaviourPicker(
    scroll: ScrollSettings,
    onChanged: (ScrollSettings) -> Unit,
) {
    SegmentedOptionRow(
        label = "At the end",
        options = EndBehaviour.entries,
        selectedOption = scroll.endBehaviour,
        onOptionSelected = { onChanged(scroll.copy(endBehaviour = it)) },
        supportingText = when (scroll.endBehaviour) {
            EndBehaviour.HOLD -> "The last line stays on the mark."
            EndBehaviour.LOOP -> "Jumps back to the top and keeps reading."
            EndBehaviour.EXIT -> "Closes the prompter and returns to the library."
        },
    ) { behaviour ->
        Text(
            text = when (behaviour) {
                EndBehaviour.HOLD -> "Hold"
                EndBehaviour.LOOP -> "Loop"
                EndBehaviour.EXIT -> "Exit"
            },
        )
    }
}

/** Fine enough to hunt a pace by eye, coarse enough that the slider is not a thousand positions. */
private const val WPM_SLIDER_STEP = 5f
private const val RAMP_SLIDER_STEP_MILLIS = 50f

/** The band of pixel speeds that is actually readable; the model still stores anything up to 2000. */
private const val MIN_USEFUL_PX_PER_SEC = 5f
private const val MAX_USEFUL_PX_PER_SEC = 400f
