package com.paulchibamba.teleprompter.domain.color

import kotlin.math.pow

/**
 * WCAG contrast between two opaque colours (docs/SPEC.md §6.9).
 *
 * Contrast is not a matter of taste on a teleprompter. The text is read at two metres, through a
 * half-silvered mirror that throws away much of the light, often in a room lit for the camera
 * rather than for reading. A number the user can see while choosing colours is worth more than any
 * amount of advice.
 *
 * Colours are ARGB packed into a [Long], matching the settings model, so this stays pure Kotlin.
 * Alpha is ignored: the prompter draws opaque text on an opaque background.
 */
object ContrastRatio {

    /** The threshold the readout warns below — WCAG's AAA level for body text. */
    const val ENHANCED_CONTRAST = 7.0f

    /** Black on white, the highest ratio the formula can produce. */
    const val MAXIMUM_RATIO = 21.0f

    /**
     * The contrast ratio between [foreground] and [background], from 1.0 (identical) to 21.0.
     *
     * Order does not matter: the formula puts the lighter colour on top either way.
     */
    fun between(foreground: Long, background: Long): Float {
        val foregroundLuminance = relativeLuminance(foreground)
        val backgroundLuminance = relativeLuminance(background)
        val lighter = maxOf(foregroundLuminance, backgroundLuminance)
        val darker = minOf(foregroundLuminance, backgroundLuminance)
        return (lighter + LUMINANCE_OFFSET) / (darker + LUMINANCE_OFFSET)
    }

    fun meetsEnhancedContrast(ratio: Float): Boolean = ratio >= ENHANCED_CONTRAST

    /** Formats as the familiar "21.0:1". */
    fun format(ratio: Float): String = "${"%.1f".format(ratio)}:1"

    /**
     * Perceived brightness, weighted for how sensitive the eye is to each primary — green far more
     * than blue, which is why yellow on black reads so much better than blue on black.
     */
    private fun relativeLuminance(argb: Long): Float {
        val red = channelValue(argb, RED_SHIFT)
        val green = channelValue(argb, GREEN_SHIFT)
        val blue = channelValue(argb, BLUE_SHIFT)
        return RED_WEIGHT * red + GREEN_WEIGHT * green + BLUE_WEIGHT * blue
    }

    /** One channel, converted from sRGB's encoding back to linear light. */
    private fun channelValue(argb: Long, shift: Int): Float {
        val encoded = ((argb shr shift) and 0xFF).toFloat() / 255f
        return if (encoded <= SRGB_KNEE) {
            encoded / SRGB_LOW_SLOPE
        } else {
            ((encoded + SRGB_OFFSET) / SRGB_SCALE).toDouble().pow(SRGB_EXPONENT).toFloat()
        }
    }

    private const val RED_SHIFT = 16
    private const val GREEN_SHIFT = 8
    private const val BLUE_SHIFT = 0

    private const val RED_WEIGHT = 0.2126f
    private const val GREEN_WEIGHT = 0.7152f
    private const val BLUE_WEIGHT = 0.0722f

    private const val LUMINANCE_OFFSET = 0.05f
    private const val SRGB_KNEE = 0.03928f
    private const val SRGB_LOW_SLOPE = 12.92f
    private const val SRGB_OFFSET = 0.055f
    private const val SRGB_SCALE = 1.055f
    private const val SRGB_EXPONENT = 2.4
}
