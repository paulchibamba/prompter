package com.paulchibamba.teleprompter.data.json

import com.paulchibamba.teleprompter.domain.model.CaseMode
import com.paulchibamba.teleprompter.domain.model.EndBehaviour
import com.paulchibamba.teleprompter.domain.model.LayoutSettings
import com.paulchibamba.teleprompter.domain.model.LineStyle
import com.paulchibamba.teleprompter.domain.model.OrientLock
import com.paulchibamba.teleprompter.domain.model.PromptAlign
import com.paulchibamba.teleprompter.domain.model.ScrollSettings
import com.paulchibamba.teleprompter.domain.model.SpeedMode
import com.paulchibamba.teleprompter.domain.model.TypographySettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The codec is the only thing standing between a user's tuning and losing it, so these tests care
 * less about the happy path than about every way a stored blob can be wrong: written by another
 * version, half-truncated, or holding a value the current build's ranges no longer allow.
 */
class SettingsCodecTest {

    @Test
    fun `typography survives a round trip with every field changed from its default`() {
        val settings = TypographySettings(
            fontId = "newsreader",
            customFontUri = "content://fonts/1",
            sizeSp = 96f,
            weight = 700,
            lineHeightMul = 1.8f,
            letterSpacingEm = 0.02f,
            paragraphSpacingEm = 1.2f,
            textAlign = PromptAlign.CENTER,
            caseTransform = CaseMode.UPPER,
            textColor = 0xFFFFCC00,
            backgroundColor = 0xFF101010,
            hyphenation = true,
        )

        assertEquals(settings, SettingsCodec.decodeTypography(SettingsCodec.encode(settings)))
    }

    @Test
    fun `layout survives a round trip with every field changed from its default`() {
        val settings = LayoutSettings(
            marginLeftPct = 18f,
            marginRightPct = 4f,
            marginTopPct = 12f,
            marginBottomPct = 14f,
            linkLeftRight = false,
            maxMeasureCh = 48,
            readingLinePct = 55f,
            readingLineStyle = LineStyle.BAND,
            readingLineColor = 0x8800FF00,
            edgeFadePct = 16f,
            mirrorHorizontal = true,
            mirrorVertical = true,
            orientationLock = OrientLock.LANDSCAPE,
        )

        assertEquals(settings, SettingsCodec.decodeLayout(SettingsCodec.encode(settings)))
    }

    @Test
    fun `scroll survives a round trip with every field changed from its default`() {
        val settings = ScrollSettings(
            speedWpm = 220,
            speedMode = SpeedMode.PIXELS,
            speedPxPerSec = 145f,
            speedStepWpm = 25,
            rampMillis = 800,
            countdownSeconds = 7,
            keepScreenOn = false,
            brightnessOverride = 0.65f,
            endBehaviour = EndBehaviour.LOOP,
        )

        assertEquals(settings, SettingsCodec.decodeScroll(SettingsCodec.encode(settings)))
    }

    @Test
    fun `an absent blob decodes to the defaults`() {
        assertEquals(TypographySettings(), SettingsCodec.decodeTypography(null))
        assertEquals(LayoutSettings(), SettingsCodec.decodeLayout(""))
        assertEquals(ScrollSettings(), SettingsCodec.decodeScroll("   "))
    }

    @Test
    fun `a truncated blob decodes to the defaults rather than throwing`() {
        assertEquals(TypographySettings(), SettingsCodec.decodeTypography("""{"sizeSp":96.0,"weig"""))
        assertEquals(LayoutSettings(), SettingsCodec.decodeLayout("not json at all"))
    }

    @Test
    fun `a field this build has never heard of is ignored, and the rest still decodes`() {
        // What a blob written by a newer version looks like after a downgrade.
        val fromTheFuture = """{"sizeSp":90.0,"opticalSizing":true}"""

        assertEquals(90f, SettingsCodec.decodeTypography(fromTheFuture).sizeSp, 0f)
    }

    @Test
    fun `a missing field takes its default instead of failing the whole block`() {
        // What a blob written by an older version looks like: fields added since simply aren't there.
        val fromThePast = """{"sizeSp":40.0}"""

        val decoded = SettingsCodec.decodeTypography(fromThePast)
        assertEquals(40f, decoded.sizeSp, 0f)
        assertEquals(TypographySettings().weight, decoded.weight)
        assertEquals(TypographySettings().lineHeightMul, decoded.lineHeightMul, 0f)
    }

    @Test
    fun `an out-of-range stored value is coerced on the way out`() {
        // A value that was in range when it was written, under ranges a later build narrowed.
        val decoded = SettingsCodec.decodeTypography("""{"sizeSp":9000.0,"weight":40}""")

        assertEquals(TypographySettings.MAX_SIZE_SP, decoded.sizeSp, 0f)
        assertEquals(TypographySettings.MIN_WEIGHT, decoded.weight)
    }

    @Test
    fun `an enum name this build does not know falls back to the default`() {
        val decoded = SettingsCodec.decodeScroll("""{"endBehaviour":"REWIND_AND_CHIME"}""")

        assertEquals(ScrollSettings().endBehaviour, decoded.endBehaviour)
    }

    @Test
    fun `defaults are written out, so a blob records the settings rather than a diff`() {
        val encoded = SettingsCodec.encode(ScrollSettings())

        assertEquals(SettingsCodec.decodeScroll(encoded), ScrollSettings())
        assertTrue("encodeDefaults should have written every field: $encoded", encoded.contains("speedWpm"))
    }
}
