package com.paulchibamba.teleprompter.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.paulchibamba.teleprompter.data.json.SettingsCodec
import com.paulchibamba.teleprompter.domain.model.LayoutSettings
import com.paulchibamba.teleprompter.domain.model.ScrollSettings
import com.paulchibamba.teleprompter.domain.model.TypographySettings
import com.paulchibamba.teleprompter.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

/** The single Preferences DataStore holding the global default settings. */
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * The DataStore-backed [SettingsRepository].
 *
 * Each block is stored as **one JSON string** rather than one preference key per field. Two
 * reasons. A block is always read and written whole — nothing in the app wants `letterSpacingEm`
 * without the rest of the type settings — so per-field keys would only add thirty-odd names to keep
 * in sync with the model. And it makes the stored form identical to a preset's JSON columns, so a
 * preset applied to the defaults and the defaults saved back as a preset round-trip exactly.
 *
 * The cost is that a single unparseable character reverts a whole block instead of one field. That
 * is the right trade for a settings cache: [SettingsCodec] falls back to defaults rather than
 * throwing, and the user re-tunes at worst.
 */
class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val typography: Flow<TypographySettings> =
        readingPreferences().map { SettingsCodec.decodeTypography(it[TYPOGRAPHY_KEY]) }.distinctUntilChanged()

    override val layout: Flow<LayoutSettings> =
        readingPreferences().map { SettingsCodec.decodeLayout(it[LAYOUT_KEY]) }.distinctUntilChanged()

    override val scroll: Flow<ScrollSettings> =
        readingPreferences().map { SettingsCodec.decodeScroll(it[SCROLL_KEY]) }.distinctUntilChanged()

    override suspend fun setTypography(settings: TypographySettings) {
        dataStore.edit { it[TYPOGRAPHY_KEY] = SettingsCodec.encode(settings.coerced()) }
    }

    override suspend fun setLayout(settings: LayoutSettings) {
        dataStore.edit { it[LAYOUT_KEY] = SettingsCodec.encode(settings.coerced()) }
    }

    override suspend fun setScroll(settings: ScrollSettings) {
        dataStore.edit { it[SCROLL_KEY] = SettingsCodec.encode(settings.coerced()) }
    }

    override suspend fun setAll(
        typography: TypographySettings,
        layout: LayoutSettings,
        scroll: ScrollSettings,
    ) {
        dataStore.edit { preferences ->
            preferences[TYPOGRAPHY_KEY] = SettingsCodec.encode(typography.coerced())
            preferences[LAYOUT_KEY] = SettingsCodec.encode(layout.coerced())
            preferences[SCROLL_KEY] = SettingsCodec.encode(scroll.coerced())
        }
    }

    override suspend fun resetToDefaults() {
        dataStore.edit { it.clear() }
    }

    /**
     * A read of the stored preferences that survives a damaged file. DataStore surfaces a corrupt
     * or unreadable file as an [IOException] *in the flow*; without this the prompter's settings
     * flow would terminate and the screen would sit on whatever it last saw. Falling back to empty
     * preferences means the app opens on its defaults instead, which is recoverable.
     */
    private fun readingPreferences(): Flow<Preferences> = dataStore.data
        .catch { cause -> if (cause is IOException) emit(emptyPreferences()) else throw cause }

    private companion object {
        val TYPOGRAPHY_KEY = stringPreferencesKey("typography")
        val LAYOUT_KEY = stringPreferencesKey("layout")
        val SCROLL_KEY = stringPreferencesKey("scroll")
    }
}
