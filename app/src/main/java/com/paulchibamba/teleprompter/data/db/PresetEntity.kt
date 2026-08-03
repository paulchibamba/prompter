package com.paulchibamba.teleprompter.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.paulchibamba.teleprompter.data.json.SettingsCodec
import com.paulchibamba.teleprompter.domain.model.Preset

/**
 * The stored shape of a preset (docs/SPEC.md §3.3).
 *
 * The three settings blocks are stored as JSON rather than as forty flat columns. A preset is only
 * ever read and written whole, and the settings model is still moving — Steps 11 through 17 each
 * add fields to it. As columns that would be a schema migration per field; as JSON it is a decode
 * that fills the new field with its default. The columns are opaque to SQLite either way, since
 * nothing queries or sorts on a setting.
 */
@Entity(tableName = "presets")
data class PresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val typographyJson: String,
    val layoutJson: String,
    val scrollJson: String,
    val isBuiltIn: Boolean = false,
)

fun PresetEntity.toDomain(): Preset = Preset(
    id = id,
    name = name,
    typography = SettingsCodec.decodeTypography(typographyJson),
    layout = SettingsCodec.decodeLayout(layoutJson),
    scroll = SettingsCodec.decodeScroll(scrollJson),
    isBuiltIn = isBuiltIn,
)

fun Preset.toEntity(): PresetEntity = PresetEntity(
    id = id,
    name = name,
    typographyJson = SettingsCodec.encode(typography),
    layoutJson = SettingsCodec.encode(layout),
    scrollJson = SettingsCodec.encode(scroll),
    isBuiltIn = isBuiltIn,
)
