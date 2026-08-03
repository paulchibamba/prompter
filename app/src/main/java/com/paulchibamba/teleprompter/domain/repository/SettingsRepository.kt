package com.paulchibamba.teleprompter.domain.repository

import com.paulchibamba.teleprompter.domain.model.LayoutSettings
import com.paulchibamba.teleprompter.domain.model.ScrollSettings
import com.paulchibamba.teleprompter.domain.model.TypographySettings
import kotlinx.coroutines.flow.Flow

/**
 * The global default settings — the three blocks of docs/SPEC.md §4, as the domain sees them.
 *
 * Each block is observed separately because that is how the prompter consumes them: a tracking
 * change should not invalidate anything reading scroll speed. Every stored value is coerced into
 * range on the way in *and* on the way out, so nothing downstream has to defend itself against a
 * value written by an older version whose ranges were different.
 */
interface SettingsRepository {

    val typography: Flow<TypographySettings>
    val layout: Flow<LayoutSettings>
    val scroll: Flow<ScrollSettings>

    suspend fun setTypography(settings: TypographySettings)
    suspend fun setLayout(settings: LayoutSettings)
    suspend fun setScroll(settings: ScrollSettings)

    /**
     * Writes all three blocks in one atomic edit. Applying a preset goes through here rather than
     * three separate writes so the prompter can never render a half-applied preset — new type size
     * against the old margins — for the frame or two between them.
     */
    suspend fun setAll(
        typography: TypographySettings,
        layout: LayoutSettings,
        scroll: ScrollSettings,
    )

    /** Forgets every stored value, returning all three blocks to their model defaults. */
    suspend fun resetToDefaults()
}
