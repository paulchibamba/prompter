package com.paulchibamba.teleprompter.domain.usecase

import com.paulchibamba.teleprompter.domain.model.LayoutSettings
import com.paulchibamba.teleprompter.domain.model.ScrollSettings
import com.paulchibamba.teleprompter.domain.model.TypographySettings
import com.paulchibamba.teleprompter.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory [SettingsRepository] for use-case tests. [writeCount] is how a test tells that
 * [SettingsRepository.setAll] really was one write rather than three, which is the property that
 * keeps a half-applied preset off the screen.
 */
class FakeSettingsRepository : SettingsRepository {

    override val typography = MutableStateFlow(TypographySettings())
    override val layout = MutableStateFlow(LayoutSettings())
    override val scroll = MutableStateFlow(ScrollSettings())

    var writeCount: Int = 0
        private set

    override suspend fun setTypography(settings: TypographySettings) {
        typography.value = settings.coerced()
        writeCount++
    }

    override suspend fun setLayout(settings: LayoutSettings) {
        layout.value = settings.coerced()
        writeCount++
    }

    override suspend fun setScroll(settings: ScrollSettings) {
        scroll.value = settings.coerced()
        writeCount++
    }

    override suspend fun setAll(
        typography: TypographySettings,
        layout: LayoutSettings,
        scroll: ScrollSettings,
    ) {
        this.typography.value = typography.coerced()
        this.layout.value = layout.coerced()
        this.scroll.value = scroll.coerced()
        writeCount++
    }

    override suspend fun resetToDefaults() {
        typography.value = TypographySettings()
        layout.value = LayoutSettings()
        scroll.value = ScrollSettings()
        writeCount++
    }
}
