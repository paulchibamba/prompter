package com.paulchibamba.teleprompter.ui.theme

import androidx.compose.ui.text.font.FontFamily

/**
 * Maps a stored `fontId` onto a real typeface for the prompter surface.
 *
 * The seam exists now so nothing downstream has to change when the bundled families arrive: the
 * surface already asks for a family by id. Until then every id resolves to the device font, which
 * is a legitimate choice in its own right (docs/SPEC.md §6.1 lists `system` as one of the options).
 */
object PrompterFonts {

    fun familyFor(fontId: String): FontFamily = when (fontId) {
        else -> FontFamily.Default
    }
}
