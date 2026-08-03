package com.paulchibamba.teleprompter.domain.color

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContrastRatioTest {

    private val white = 0xFFFFFFFF
    private val black = 0xFF000000
    private val amber = 0xFFFFBF00
    private val midGrey = 0xFF767676

    @Test
    fun `white on black is the maximum ratio`() {
        assertEquals(21.0f, ContrastRatio.between(white, black), TOLERANCE)
    }

    @Test
    fun `the ratio does not depend on which colour is the foreground`() {
        assertEquals(
            ContrastRatio.between(white, black),
            ContrastRatio.between(black, white),
            TOLERANCE,
        )
    }

    @Test
    fun `a colour against itself has no contrast at all`() {
        assertEquals(1.0f, ContrastRatio.between(amber, amber), TOLERANCE)
    }

    /** The canonical WCAG worked example: #767676 is the darkest grey passing AA on white. */
    @Test
    fun `mid grey on white matches the published WCAG value`() {
        assertEquals(4.54f, ContrastRatio.between(midGrey, white), 0.02f)
    }

    @Test
    fun `amber on black clears the enhanced threshold`() {
        val ratio = ContrastRatio.between(amber, black)

        assertTrue(ratio > 7.0f)
        assertTrue(ContrastRatio.meetsEnhancedContrast(ratio))
    }

    @Test
    fun `mid grey on white does not clear the enhanced threshold`() {
        assertFalse(ContrastRatio.meetsEnhancedContrast(ContrastRatio.between(midGrey, white)))
    }

    @Test
    fun `exactly seven to one counts as meeting the threshold`() {
        assertTrue(ContrastRatio.meetsEnhancedContrast(7.0f))
        assertFalse(ContrastRatio.meetsEnhancedContrast(6.99f))
    }

    @Test
    fun `green contributes more than blue at the same channel value`() {
        val green = 0xFF00FF00
        val blue = 0xFF0000FF

        // Both are maxed on one channel, but the eye is far more sensitive to green, so green
        // sits closer to white and therefore contrasts less with it.
        assertTrue(ContrastRatio.between(green, white) < ContrastRatio.between(blue, white))
    }

    @Test
    fun `alpha is ignored so a translucent colour rates like its opaque form`() {
        val translucentWhite = 0x00FFFFFFL

        assertEquals(
            ContrastRatio.between(white, black),
            ContrastRatio.between(translucentWhite, black),
            TOLERANCE,
        )
    }

    @Test
    fun `formats as a ratio against one`() {
        assertEquals("21.0:1", ContrastRatio.format(21.0f))
        assertEquals("4.5:1", ContrastRatio.format(4.54f))
    }

    private companion object {
        const val TOLERANCE = 0.01f
    }
}
