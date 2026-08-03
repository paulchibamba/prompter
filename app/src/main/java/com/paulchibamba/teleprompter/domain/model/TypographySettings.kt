package com.paulchibamba.teleprompter.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class PromptAlign { LEFT, CENTER }

@Serializable
enum class CaseMode { NONE, UPPER }

/**
 * How cue markers are drawn (docs/SPEC.md §6.10). [HIDDEN] removes them from the page but not from
 * the script — they remain jump targets, because that is usually why they were written.
 */
@Serializable
enum class MarkerStyle { NORMAL, DIMMED, ACCENT, HIDDEN }

/**
 * Type settings for the prompter surface (docs/SPEC.md §4.1, §6).
 *
 * Colours are ARGB packed into a [Long] rather than a Compose `Color` so this stays a pure-Kotlin
 * model that unit tests can exercise without an Android runtime.
 *
 * `@Serializable` is what lets this block be stored verbatim in DataStore and in a preset's
 * `typographyJson` column — see `com.paulchibamba.teleprompter.data.json.SettingsCodec`. Every
 * property has a default, so a blob written by an older version decodes with the new fields at
 * their defaults instead of failing.
 */
@Serializable
data class TypographySettings(
    val fontId: String = DEFAULT_FONT_ID,
    val customFontUri: String? = null,
    val sizeSp: Float = 72f,
    val weight: Int = 500,
    val lineHeightMul: Float = 1.5f,
    val letterSpacingEm: Float = 0f,
    val paragraphSpacingEm: Float = 0.6f,
    val textAlign: PromptAlign = PromptAlign.LEFT,
    val caseTransform: CaseMode = CaseMode.NONE,
    val textColor: Long = 0xFFFFFFFF,
    val backgroundColor: Long = 0xFF000000,
    val hyphenation: Boolean = false,
    val markerStyle: MarkerStyle = MarkerStyle.NORMAL,
    val markerColor: Long = DEFAULT_MARKER_COLOR,
) {
    /**
     * Clamps every value into its supported range and rounds [weight] to the nearest 100.
     *
     * Settings arrive from DataStore, imported presets and remote actions, none of which are
     * guaranteed to be in range. Coercing beats validating: a nonsensical stored value should
     * degrade to the nearest sane one, never crash the prompter mid-read.
     */
    fun coerced(): TypographySettings = copy(
        sizeSp = sizeSp.coerceIn(MIN_SIZE_SP, MAX_SIZE_SP),
        weight = (weight.coerceIn(MIN_WEIGHT, MAX_WEIGHT) + 50) / 100 * 100,
        lineHeightMul = lineHeightMul.coerceIn(MIN_LINE_HEIGHT_MUL, MAX_LINE_HEIGHT_MUL),
        letterSpacingEm = letterSpacingEm.coerceIn(MIN_LETTER_SPACING_EM, MAX_LETTER_SPACING_EM),
        paragraphSpacingEm = paragraphSpacingEm.coerceIn(MIN_PARAGRAPH_SPACING_EM, MAX_PARAGRAPH_SPACING_EM),
    )

    companion object {
        const val DEFAULT_FONT_ID = "lexend"

        /** Bundled for low-vision reading (docs/SPEC.md §6.1); the "Bright room" preset uses it. */
        const val ATKINSON_FONT_ID = "atkinson"
        const val CUSTOM_FONT_ID = "custom"

        const val MIN_SIZE_SP = 16f
        const val MAX_SIZE_SP = 200f
        const val MIN_WEIGHT = 300
        const val MAX_WEIGHT = 900
        const val MIN_LINE_HEIGHT_MUL = 1.0f
        const val MAX_LINE_HEIGHT_MUL = 2.5f
        const val MIN_LETTER_SPACING_EM = -0.05f
        const val MAX_LETTER_SPACING_EM = 0.25f
        const val MIN_PARAGRAPH_SPACING_EM = 0f
        const val MAX_PARAGRAPH_SPACING_EM = 3f

        /** Warm amber: visible against the usual white-on-black without competing with it. */
        const val DEFAULT_MARKER_COLOR = 0xFFFFBF00

        /** The opacity [MarkerStyle.DIMMED] draws markers at. */
        const val DIMMED_MARKER_ALPHA = 0.5f
    }
}
