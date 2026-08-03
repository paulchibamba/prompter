package com.paulchibamba.teleprompter.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class LineStyle { OFF, LINE, ARROWS, BAND }

@Serializable
enum class OrientLock { FOLLOW_SENSOR, PORTRAIT, LANDSCAPE }

/**
 * Where the text sits on the glass (docs/SPEC.md §4.2, §7). Margins are percentages so a preset
 * carries between devices of different sizes without re-tuning.
 *
 * Serialised into DataStore and into a preset's `layoutJson` column — see
 * [TypographySettings] for why every property carries a default.
 */
@Serializable
data class LayoutSettings(
    val marginLeftPct: Float = 10f,
    val marginRightPct: Float = 10f,
    val marginTopPct: Float = 8f,
    val marginBottomPct: Float = 8f,
    val linkLeftRight: Boolean = true,
    val maxMeasureCh: Int = 0,
    val readingLinePct: Float = 40f,
    val readingLineStyle: LineStyle = LineStyle.ARROWS,
    val readingLineColor: Long = 0x66FF3B30,
    val edgeFadePct: Float = 8f,
    val mirrorHorizontal: Boolean = false,
    val mirrorVertical: Boolean = false,
    val orientationLock: OrientLock = OrientLock.FOLLOW_SENSOR,
) {
    /**
     * Clamps every value into its supported range, and mirrors the left margin onto the right when
     * [linkLeftRight] is set so the linked pair can never drift apart.
     */
    fun coerced(): LayoutSettings {
        val left = marginLeftPct.coerceIn(MIN_MARGIN_PCT, MAX_MARGIN_PCT)
        return copy(
            marginLeftPct = left,
            marginRightPct = if (linkLeftRight) left else marginRightPct.coerceIn(MIN_MARGIN_PCT, MAX_MARGIN_PCT),
            marginTopPct = marginTopPct.coerceIn(MIN_MARGIN_PCT, MAX_MARGIN_PCT),
            marginBottomPct = marginBottomPct.coerceIn(MIN_MARGIN_PCT, MAX_MARGIN_PCT),
            maxMeasureCh = maxMeasureCh.coerceIn(MEASURE_OFF, MAX_MEASURE_CH),
            readingLinePct = readingLinePct.coerceIn(MIN_READING_LINE_PCT, MAX_READING_LINE_PCT),
            edgeFadePct = edgeFadePct.coerceIn(MIN_EDGE_FADE_PCT, MAX_EDGE_FADE_PCT),
        )
    }

    companion object {
        const val MIN_MARGIN_PCT = 0f
        const val MAX_MARGIN_PCT = 40f

        /** [maxMeasureCh] of zero means "no line-length cap". */
        const val MEASURE_OFF = 0
        const val MAX_MEASURE_CH = 120

        const val MIN_READING_LINE_PCT = 0f
        const val MAX_READING_LINE_PCT = 90f
        const val MIN_EDGE_FADE_PCT = 0f
        const val MAX_EDGE_FADE_PCT = 25f
    }
}
