package com.paulchibamba.teleprompter.ui.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.paulchibamba.teleprompter.R
import com.paulchibamba.teleprompter.domain.model.TypographySettings
import java.io.File

/**
 * The reading faces (docs/SPEC.md §6.1).
 *
 * All four bundled families are **variable** fonts, so weight is continuous rather than a jump
 * between two or three cut sizes. That matters on a beam splitter, where 400 tends to thin out and
 * 700 blooms, and the usable answer is often somewhere between them.
 */
@OptIn(ExperimentalTextApi::class)
object PrompterFonts {

    /** A face the user can choose, with the name to show and the face to show it in. */
    data class Choice(val id: String, val displayName: String)

    val bundledChoices: List<Choice> = listOf(
        Choice(TypographySettings.DEFAULT_FONT_ID, "Lexend"),
        Choice(TypographySettings.ATKINSON_FONT_ID, "Atkinson Hyperlegible"),
        Choice(INTER_FONT_ID, "Inter"),
        Choice(NEWSREADER_FONT_ID, "Newsreader"),
        Choice(SYSTEM_FONT_ID, "System default"),
    )

    fun displayNameFor(fontId: String): String =
        bundledChoices.firstOrNull { it.id == fontId }?.displayName
            ?: if (fontId == TypographySettings.CUSTOM_FONT_ID) "Imported font" else "System default"

    /**
     * @param customFontFile the imported face, when [fontId] is `custom`. A missing or unreadable
     *   file falls back to the device font rather than failing — losing a typeface should never
     *   mean losing the script.
     */
    fun familyFor(fontId: String, customFontFile: File? = null): FontFamily = when (fontId) {
        TypographySettings.DEFAULT_FONT_ID -> bundledFamily(R.font.lexend_variable)
        TypographySettings.ATKINSON_FONT_ID -> bundledFamily(R.font.atkinson_hyperlegible_next)
        INTER_FONT_ID -> bundledFamily(R.font.inter_variable)
        NEWSREADER_FONT_ID -> bundledFamily(R.font.newsreader_variable)
        TypographySettings.CUSTOM_FONT_ID -> customFamily(customFontFile)
        else -> FontFamily.Default
    }

    /**
     * One [Font] per supported weight, each asking the variable file for that exact weight along
     * its `wght` axis. Without the variation settings the system would pick the nearest static
     * instance and the weight control would appear to do nothing.
     */
    private fun bundledFamily(fontResId: Int): FontFamily = bundledFamilies.getOrPut(fontResId) {
        FontFamily(
            SUPPORTED_WEIGHTS.map { weight ->
                Font(
                    resId = fontResId,
                    weight = FontWeight(weight),
                    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
                )
            },
        )
    }

    private fun customFamily(file: File?): FontFamily {
        if (file == null || !file.canRead()) return FontFamily.Default
        return customFamilies.getOrPut(file.path) {
            FontFamily(
                SUPPORTED_WEIGHTS.map { weight ->
                    Font(
                        file = file,
                        weight = FontWeight(weight),
                        variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
                    )
                },
            )
        }
    }

    /** Built once per face: constructing a `FontFamily` on every recomposition would be wasteful. */
    private val bundledFamilies = mutableMapOf<Int, FontFamily>()
    private val customFamilies = mutableMapOf<String, FontFamily>()

    private val SUPPORTED_WEIGHTS = (TypographySettings.MIN_WEIGHT..TypographySettings.MAX_WEIGHT step 100).toList()

    const val INTER_FONT_ID = "inter"
    const val NEWSREADER_FONT_ID = "newsreader"
    const val SYSTEM_FONT_ID = "system"
}
