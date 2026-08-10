package com.paulchibamba.teleprompter.domain.scroll

import com.paulchibamba.teleprompter.domain.model.ScrollSettings
import com.paulchibamba.teleprompter.domain.model.SpeedMode
import org.junit.Assert.assertEquals
import org.junit.Test

class WpmCalculatorTest {

    @Test
    fun `px per second follows the spec formula`() {
        // 100 words laid out 6000px tall = 60px per word; 120wpm = 2 words/sec = 120px/sec.
        assertEquals(120f, WpmCalculator.pxPerSecond(speedWpm = 120, contentHeightPx = 6000f, wordCount = 100), TOLERANCE)
    }

    @Test
    fun `doubling the font size leaves the reading pace unchanged`() {
        // This is the property the whole WPM design exists for (docs/SPEC.md §8.1): taller content
        // at the same word count means proportionally faster pixels, so words per second is fixed.
        val words = 250
        val small = WpmCalculator.pxPerSecond(140, contentHeightPx = 8000f, wordCount = words)
        val large = WpmCalculator.pxPerSecond(140, contentHeightPx = 16000f, wordCount = words)

        assertEquals(2f, large / small, TOLERANCE)
        assertEquals(8000f / small, 16000f / large, TOLERANCE) // identical seconds to read
    }

    @Test
    fun `px per second is zero for an unmeasured or empty script`() {
        assertEquals(0f, WpmCalculator.pxPerSecond(140, contentHeightPx = 0f, wordCount = 100), TOLERANCE)
        assertEquals(0f, WpmCalculator.pxPerSecond(140, contentHeightPx = 6000f, wordCount = 0), TOLERANCE)
    }

    @Test
    fun `wpm and px per second round-trip`() {
        val pxPerSecond = WpmCalculator.pxPerSecond(180, contentHeightPx = 9500f, wordCount = 412)
        assertEquals(180, WpmCalculator.wpmFor(pxPerSecond, contentHeightPx = 9500f, wordCount = 412))
    }

    @Test
    fun `wpm for degenerate input is zero rather than infinite`() {
        assertEquals(0, WpmCalculator.wpmFor(60f, contentHeightPx = 0f, wordCount = 100))
        assertEquals(0, WpmCalculator.wpmFor(60f, contentHeightPx = 6000f, wordCount = 0))
    }

    @Test
    fun `estimated duration matches the library subtitle example`() {
        // docs/SPEC.md §5.1 shows "412 words · 2:56" — at 140wpm that is 176.6s.
        val seconds = WpmCalculator.estimatedSeconds(wordCount = 412, speedWpm = 140)
        assertEquals(177, seconds)
        assertEquals("2:57", WpmCalculator.formatDuration(seconds))
    }

    @Test
    fun `estimated duration is zero for degenerate input`() {
        assertEquals(0, WpmCalculator.estimatedSeconds(wordCount = 0, speedWpm = 140))
        assertEquals(0, WpmCalculator.estimatedSeconds(wordCount = 412, speedWpm = 0))
    }

    @Test
    fun `remaining time tracks progress and clamps at both ends`() {
        assertEquals(180, WpmCalculator.remainingSeconds(180, scrolledPx = 0f, scrollableHeightPx = 1000f))
        assertEquals(90, WpmCalculator.remainingSeconds(180, scrolledPx = 500f, scrollableHeightPx = 1000f))
        assertEquals(0, WpmCalculator.remainingSeconds(180, scrolledPx = 1200f, scrollableHeightPx = 1000f))
        assertEquals(180, WpmCalculator.remainingSeconds(180, scrolledPx = -50f, scrollableHeightPx = 1000f))
        assertEquals(180, WpmCalculator.remainingSeconds(180, scrolledPx = 0f, scrollableHeightPx = 0f))
    }

    @Test
    fun `words per minute mode derives its pixel speed from the laid-out content`() {
        val scroll = ScrollSettings(speedMode = SpeedMode.WPM, speedWpm = 120, speedPxPerSec = 999f)

        assertEquals(
            120f,
            WpmCalculator.effectivePxPerSecond(scroll, contentHeightPx = 6000f, wordCount = 100),
            TOLERANCE,
        )
        assertEquals(120, WpmCalculator.effectiveWpm(scroll, contentHeightPx = 6000f, wordCount = 100))
    }

    @Test
    fun `pixels mode uses the stored speed and reports the pace it works out to`() {
        val scroll = ScrollSettings(speedMode = SpeedMode.PIXELS, speedWpm = 999, speedPxPerSec = 120f)

        assertEquals(
            120f,
            WpmCalculator.effectivePxPerSecond(scroll, contentHeightPx = 6000f, wordCount = 100),
            TOLERANCE,
        )
        assertEquals(120, WpmCalculator.effectiveWpm(scroll, contentHeightPx = 6000f, wordCount = 100))
    }

    @Test
    fun `pixels mode holds its speed when the font size changes, and the pace moves with it`() {
        // The cost of the mode, stated as a test: the same px per second over taller content is a
        // slower read. Words-per-minute mode is the default precisely because it does not do this.
        val scroll = ScrollSettings(speedMode = SpeedMode.PIXELS, speedPxPerSec = 120f)

        assertEquals(120, WpmCalculator.effectiveWpm(scroll, contentHeightPx = 6000f, wordCount = 100))
        assertEquals(60, WpmCalculator.effectiveWpm(scroll, contentHeightPx = 12000f, wordCount = 100))
    }

    @Test
    fun `durations format as m ss and h mm ss`() {
        assertEquals("0:00", WpmCalculator.formatDuration(0))
        assertEquals("0:09", WpmCalculator.formatDuration(9))
        assertEquals("2:56", WpmCalculator.formatDuration(176))
        assertEquals("59:59", WpmCalculator.formatDuration(3599))
        assertEquals("1:00:00", WpmCalculator.formatDuration(3600))
        assertEquals("1:02:03", WpmCalculator.formatDuration(3723))
        assertEquals("0:00", WpmCalculator.formatDuration(-5))
    }

    private companion object {
        const val TOLERANCE = 0.001f
    }
}
