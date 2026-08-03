package com.paulchibamba.teleprompter.domain.scroll

import kotlin.math.roundToInt

/**
 * Converts a reading pace in words per minute into a scroll speed in pixels per second, and back
 * (docs/SPEC.md §8.1).
 *
 * The conversion is deliberately driven by the *laid-out* content height rather than by the font
 * size: raise the size and the content gets taller, so px/s rises by exactly the same factor and
 * the reading pace does not change. That property is the whole reason speed is expressed in WPM,
 * and it is what the Phase C milestone tests.
 */
object WpmCalculator {

    /**
     * Scroll speed for [speedWpm] over a script of [wordCount] words that lays out to
     * [contentHeightPx] tall at the current style and column width.
     *
     * Returns 0 for an empty or unmeasured script — the caller has nothing to scroll, and a
     * division by zero here would poison the frame loop with NaN.
     */
    fun pxPerSecond(speedWpm: Int, contentHeightPx: Float, wordCount: Int): Float {
        if (wordCount <= 0 || contentHeightPx <= 0f) return 0f
        return (speedWpm / SECONDS_PER_MINUTE) * (contentHeightPx / wordCount)
    }

    /**
     * The inverse of [pxPerSecond]: the pace an explicit pixels/second speed corresponds to. Used
     * to show the other unit as a subtitle in pixels mode, so the mapping is learnable.
     */
    fun wpmFor(pxPerSecond: Float, contentHeightPx: Float, wordCount: Int): Int {
        if (wordCount <= 0 || contentHeightPx <= 0f) return 0
        return (pxPerSecond * SECONDS_PER_MINUTE * wordCount / contentHeightPx).roundToInt()
    }

    /** How long [wordCount] words take to read at [speedWpm], in seconds. */
    fun estimatedSeconds(wordCount: Int, speedWpm: Int): Int {
        if (wordCount <= 0 || speedWpm <= 0) return 0
        return (wordCount * SECONDS_PER_MINUTE / speedWpm).roundToInt()
    }

    /**
     * Seconds left from a scroll position of [scrolledPx] through [scrollableHeightPx] of travel.
     * Clamped at both ends so an over-scroll bounce cannot show a negative time.
     */
    fun remainingSeconds(totalSeconds: Int, scrolledPx: Float, scrollableHeightPx: Float): Int {
        if (scrollableHeightPx <= 0f) return totalSeconds
        val progress = (scrolledPx / scrollableHeightPx).coerceIn(0f, 1f)
        return (totalSeconds * (1f - progress)).roundToInt()
    }

    /**
     * Formats a duration as `m:ss`, or `h:mm:ss` past an hour. Used for the library's
     * "412 words · 2:56" subtitle and the prompter's time-remaining readout.
     */
    fun formatDuration(seconds: Int): String {
        val safe = seconds.coerceAtLeast(0)
        val hours = safe / 3600
        val minutes = (safe % 3600) / 60
        val secs = safe % 60
        return if (hours > 0) {
            "$hours:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}"
        } else {
            "$minutes:${secs.toString().padStart(2, '0')}"
        }
    }

    private const val SECONDS_PER_MINUTE = 60f
}
