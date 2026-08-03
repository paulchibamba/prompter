package com.paulchibamba.teleprompter.domain.model

/**
 * A named bundle of typography, layout and scroll settings (docs/SPEC.md §3.3). Stored as three
 * JSON columns in the `presets` table; this is the in-memory shape the UI binds to.
 *
 * [isBuiltIn] marks the three presets in [BuiltInPresets]. They are read-only: saving over one
 * writes a copy and deleting one is refused, so the app can never end up with no presets at all.
 */
data class Preset(
    val id: Long = 0L,
    val name: String,
    val typography: TypographySettings = TypographySettings(),
    val layout: LayoutSettings = LayoutSettings(),
    val scroll: ScrollSettings = ScrollSettings(),
    val isBuiltIn: Boolean = false,
) {
    fun coerced(): Preset = copy(
        typography = typography.coerced(),
        layout = layout.coerced(),
        scroll = scroll.coerced(),
    )
}
