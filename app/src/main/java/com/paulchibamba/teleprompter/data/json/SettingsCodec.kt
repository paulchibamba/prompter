package com.paulchibamba.teleprompter.data.json

import com.paulchibamba.teleprompter.domain.model.LayoutSettings
import com.paulchibamba.teleprompter.domain.model.ScrollSettings
import com.paulchibamba.teleprompter.domain.model.TypographySettings
import kotlinx.serialization.json.Json

/**
 * Turns the three settings blocks into JSON and back.
 *
 * One codec serves both storage sites — the DataStore keys holding the global defaults and the
 * `typographyJson` / `layoutJson` / `scrollJson` columns of a preset — so that applying a preset to
 * the defaults and saving the defaults back as a preset are exactly lossless. Two encoders would
 * eventually disagree about one field and lose it silently, which is the worst way for settings to
 * break: nothing crashes, the user's tuning just quietly reverts.
 *
 * Decoding never throws. Stored settings are not a contract with another system, they are a cache
 * of the user's preferences, and a corrupt or older blob should cost them their tuning at worst —
 * never a crash on the way into a read.
 */
object SettingsCodec {

    private val json = Json {
        // A blob written by a newer version, then opened by an older one after a downgrade, carries
        // keys this build has never heard of. Skipping them beats refusing the whole block.
        ignoreUnknownKeys = true
        // Written for the same reason in the other direction: an explicit `null` or a value of the
        // wrong type falls back to the property's default rather than failing the decode.
        coerceInputValues = true
        // Defaults are written out too, so a stored blob is a complete record of the settings at
        // the moment it was saved rather than a diff against whatever the defaults were that day.
        encodeDefaults = true
    }

    fun encode(settings: TypographySettings): String = json.encodeToString(settings)

    fun encode(settings: LayoutSettings): String = json.encodeToString(settings)

    fun encode(settings: ScrollSettings): String = json.encodeToString(settings)

    fun decodeTypography(raw: String?): TypographySettings =
        decodeOrDefault(raw, TypographySettings()) { json.decodeFromString<TypographySettings>(it) }.coerced()

    fun decodeLayout(raw: String?): LayoutSettings =
        decodeOrDefault(raw, LayoutSettings()) { json.decodeFromString<LayoutSettings>(it) }.coerced()

    fun decodeScroll(raw: String?): ScrollSettings =
        decodeOrDefault(raw, ScrollSettings()) { json.decodeFromString<ScrollSettings>(it) }.coerced()

    private inline fun <T> decodeOrDefault(raw: String?, default: T, decode: (String) -> T): T {
        if (raw.isNullOrBlank()) return default
        return try {
            decode(raw)
        } catch (_: IllegalArgumentException) {
            // SerializationException extends IllegalArgumentException, and decodeFromString raises
            // the plain form for some malformed input; catching the supertype covers both.
            default
        }
    }
}
