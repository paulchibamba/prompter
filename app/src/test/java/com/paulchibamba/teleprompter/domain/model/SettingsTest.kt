package com.paulchibamba.teleprompter.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The defaults are asserted deliberately: they are the app's out-of-the-box reading experience and
 * docs/SPEC.md §4 pins them. Changing one should be a conscious edit here too.
 */
class SettingsTest {

    @Test
    fun `typography defaults match the spec`() {
        val defaults = TypographySettings()
        assertEquals("lexend", defaults.fontId)
        assertEquals(72f, defaults.sizeSp, TOLERANCE)
        assertEquals(500, defaults.weight)
        assertEquals(1.5f, defaults.lineHeightMul, TOLERANCE)
        assertEquals(PromptAlign.LEFT, defaults.textAlign)
        assertEquals(CaseMode.NONE, defaults.caseTransform)
        assertEquals(0xFFFFFFFF, defaults.textColor)
        assertEquals(0xFF000000, defaults.backgroundColor)
        assertEquals(defaults, defaults.coerced())
    }

    @Test
    fun `typography out of range values are clamped`() {
        val coerced = TypographySettings(
            sizeSp = 9000f,
            lineHeightMul = 0.1f,
            letterSpacingEm = -1f,
            paragraphSpacingEm = 99f,
        ).coerced()

        assertEquals(TypographySettings.MAX_SIZE_SP, coerced.sizeSp, TOLERANCE)
        assertEquals(TypographySettings.MIN_LINE_HEIGHT_MUL, coerced.lineHeightMul, TOLERANCE)
        assertEquals(TypographySettings.MIN_LETTER_SPACING_EM, coerced.letterSpacingEm, TOLERANCE)
        assertEquals(TypographySettings.MAX_PARAGRAPH_SPACING_EM, coerced.paragraphSpacingEm, TOLERANCE)
    }

    @Test
    fun `typography weight snaps to the nearest hundred inside the variable range`() {
        assertEquals(500, TypographySettings(weight = 540).coerced().weight)
        assertEquals(600, TypographySettings(weight = 560).coerced().weight)
        assertEquals(300, TypographySettings(weight = 100).coerced().weight)
        assertEquals(900, TypographySettings(weight = 1000).coerced().weight)
    }

    @Test
    fun `layout defaults match the spec`() {
        val defaults = LayoutSettings()
        assertEquals(10f, defaults.marginLeftPct, TOLERANCE)
        assertEquals(8f, defaults.marginTopPct, TOLERANCE)
        assertEquals(40f, defaults.readingLinePct, TOLERANCE)
        assertEquals(LineStyle.ARROWS, defaults.readingLineStyle)
        assertEquals(OrientLock.FOLLOW_SENSOR, defaults.orientationLock)
        assertEquals(LayoutSettings.MEASURE_OFF, defaults.maxMeasureCh)
        assertEquals(defaults, defaults.coerced())
    }

    @Test
    fun `linked margins mirror the left onto the right`() {
        val linked = LayoutSettings(marginLeftPct = 22f, marginRightPct = 3f, linkLeftRight = true).coerced()
        assertEquals(22f, linked.marginRightPct, TOLERANCE)

        val unlinked = LayoutSettings(marginLeftPct = 22f, marginRightPct = 3f, linkLeftRight = false).coerced()
        assertEquals(3f, unlinked.marginRightPct, TOLERANCE)
    }

    @Test
    fun `layout out of range values are clamped`() {
        val coerced = LayoutSettings(
            marginLeftPct = -5f,
            marginTopPct = 90f,
            readingLinePct = 99f,
            edgeFadePct = 80f,
            maxMeasureCh = -3,
        ).coerced()

        assertEquals(LayoutSettings.MIN_MARGIN_PCT, coerced.marginLeftPct, TOLERANCE)
        assertEquals(LayoutSettings.MAX_MARGIN_PCT, coerced.marginTopPct, TOLERANCE)
        assertEquals(LayoutSettings.MAX_READING_LINE_PCT, coerced.readingLinePct, TOLERANCE)
        assertEquals(LayoutSettings.MAX_EDGE_FADE_PCT, coerced.edgeFadePct, TOLERANCE)
        assertEquals(LayoutSettings.MEASURE_OFF, coerced.maxMeasureCh)
    }

    @Test
    fun `scroll defaults match the spec`() {
        val defaults = ScrollSettings()
        assertEquals(140, defaults.speedWpm)
        assertEquals(SpeedMode.WPM, defaults.speedMode)
        assertEquals(10, defaults.speedStepWpm)
        assertEquals(350, defaults.rampMillis)
        assertEquals(3, defaults.countdownSeconds)
        assertEquals(EndBehaviour.HOLD, defaults.endBehaviour)
        assertEquals(ScrollSettings.BRIGHTNESS_SYSTEM, defaults.brightnessOverride, TOLERANCE)
        assertEquals(defaults, defaults.coerced())
    }

    @Test
    fun `scroll out of range values are clamped`() {
        val coerced = ScrollSettings(
            speedWpm = 5000,
            speedStepWpm = 0,
            rampMillis = -1,
            countdownSeconds = 60,
        ).coerced()

        assertEquals(ScrollSettings.MAX_WPM, coerced.speedWpm)
        assertEquals(ScrollSettings.MIN_STEP_WPM, coerced.speedStepWpm)
        assertEquals(ScrollSettings.MIN_RAMP_MILLIS, coerced.rampMillis)
        assertEquals(ScrollSettings.MAX_COUNTDOWN_SECONDS, coerced.countdownSeconds)
    }

    @Test
    fun `brightness keeps its system sentinel and clamps a real override`() {
        assertEquals(
            ScrollSettings.BRIGHTNESS_SYSTEM,
            ScrollSettings(brightnessOverride = -0.5f).coerced().brightnessOverride,
            TOLERANCE,
        )
        assertEquals(1f, ScrollSettings(brightnessOverride = 4f).coerced().brightnessOverride, TOLERANCE)
        assertEquals(0.4f, ScrollSettings(brightnessOverride = 0.4f).coerced().brightnessOverride, TOLERANCE)
    }

    @Test
    fun `stepping the speed moves by the step size and stops at the limits`() {
        val settings = ScrollSettings(speedWpm = 140, speedStepWpm = 10)
        assertEquals(150, settings.steppedWpm(1))
        assertEquals(120, settings.steppedWpm(-2))
        assertEquals(ScrollSettings.MIN_WPM, settings.steppedWpm(-100))
        assertEquals(ScrollSettings.MAX_WPM, settings.steppedWpm(100))
    }

    @Test
    fun `a preset coerces every block it holds`() {
        val preset = Preset(
            name = "Broken import",
            typography = TypographySettings(sizeSp = 9000f),
            layout = LayoutSettings(readingLinePct = 200f),
            scroll = ScrollSettings(speedWpm = 5000),
        ).coerced()

        assertEquals(TypographySettings.MAX_SIZE_SP, preset.typography.sizeSp, TOLERANCE)
        assertEquals(LayoutSettings.MAX_READING_LINE_PCT, preset.layout.readingLinePct, TOLERANCE)
        assertEquals(ScrollSettings.MAX_WPM, preset.scroll.speedWpm)
    }

    private companion object {
        const val TOLERANCE = 0.0001f
    }
}
