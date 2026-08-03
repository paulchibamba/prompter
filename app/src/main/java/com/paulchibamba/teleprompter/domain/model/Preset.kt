package com.paulchibamba.teleprompter.domain.model

/**
 * A named bundle of typography, layout and scroll settings (docs/SPEC.md §3.3). Persisted as three
 * JSON columns in Step 5; this is the in-memory shape the UI binds to.
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
