package com.paulchibamba.teleprompter.domain.usecase

import com.paulchibamba.teleprompter.domain.model.Preset
import com.paulchibamba.teleprompter.domain.repository.PresetRepository
import com.paulchibamba.teleprompter.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * The operations the UI performs on presets. Same shape as [ScriptUseCases] — a class per
 * operation with an `operator fun invoke` — for the same reason: a ViewModel calls one like a
 * function, and a test hands it a fake repository.
 */

/** Every preset, built-ins first. */
class ObservePresets(private val repository: PresetRepository) {
    operator fun invoke(): Flow<List<Preset>> = repository.observeAll()
}

/** A single preset by id, or null if it has been deleted underneath us. */
class GetPreset(private val repository: PresetRepository) {
    suspend operator fun invoke(id: Long): Preset? = repository.byId(id)
}

/**
 * Saves a preset, deriving the name when the user did not give one.
 *
 * Saving over a built-in stores a **copy** instead. The three shipped presets are the app's known
 * starting points — a user who tweaks "Studio" and saves wants their version, not to lose the
 * original for everyone including themselves next time.
 */
class SavePreset(
    private val repository: PresetRepository,
    private val untitled: String = UNTITLED,
) {
    suspend operator fun invoke(preset: Preset): Long {
        val named = preset.copy(name = preset.name.trim().ifEmpty { untitled })
        val toSave = if (named.isBuiltIn) named.copy(id = 0L, isBuiltIn = false) else named
        return repository.upsert(toSave)
    }

    companion object {
        const val UNTITLED = "Untitled preset"
    }
}

/** Deletes a user preset. Returns false, changing nothing, when [id] names a built-in. */
class DeletePreset(private val repository: PresetRepository) {
    suspend operator fun invoke(id: Long): Boolean = repository.delete(id)
}

/**
 * Copies a preset's three settings blocks into the global defaults, in one atomic write.
 *
 * Returns false if the preset has gone — applying a preset the user deleted on another screen
 * should be a no-op, not an exception thrown out of a click handler.
 */
class ApplyPreset(
    private val presets: PresetRepository,
    private val settings: SettingsRepository,
) {
    suspend operator fun invoke(id: Long): Boolean {
        val preset = presets.byId(id) ?: return false
        settings.setAll(preset.typography, preset.layout, preset.scroll)
        return true
    }
}

/**
 * Captures the current global defaults as a new named preset — the "save what I have" half of
 * preset management, and the only writer that reads settings and presets together.
 *
 * It reads the *stored* defaults, which makes it right for the settings screen and wrong for the
 * prompter: a script with an assigned preset is read from settings that are deliberately never
 * written, so saving from there builds the [Preset] from the live session instead.
 */
class SaveCurrentSettingsAsPreset(
    private val settings: SettingsRepository,
    private val savePreset: SavePreset,
) {
    suspend operator fun invoke(name: String): Long = savePreset(
        Preset(
            name = name,
            typography = settings.typography.first(),
            layout = settings.layout.first(),
            scroll = settings.scroll.first(),
        ),
    )
}

/**
 * Overwrites an existing preset with the current global defaults, keeping its name — "I have
 * adjusted things and this is what Podcast should mean from now on".
 *
 * Refused on a built-in, returning false. Letting it through would go via [SavePreset]'s
 * copy-on-write and leave the user with two presets called "Studio", which is a worse outcome than
 * the action simply not being offered.
 */
class UpdatePresetFromCurrentSettings(
    private val presets: PresetRepository,
    private val settings: SettingsRepository,
) {
    suspend operator fun invoke(id: Long): Boolean {
        val preset = presets.byId(id) ?: return false
        if (preset.isBuiltIn) return false
        presets.upsert(
            preset.copy(
                typography = settings.typography.first(),
                layout = settings.layout.first(),
                scroll = settings.scroll.first(),
            ),
        )
        return true
    }
}

/**
 * Copies a preset under a new name. This is how a built-in becomes editable: the three shipped
 * presets are read-only, so starting from one means taking a copy of it first.
 */
class DuplicatePreset(
    private val presets: PresetRepository,
    private val savePreset: SavePreset,
) {
    suspend operator fun invoke(id: Long): Long? {
        val original = presets.byId(id) ?: return null
        return savePreset(
            original.copy(
                id = 0L,
                name = "${original.name} $COPY_SUFFIX",
                isBuiltIn = false,
            ),
        )
    }

    companion object {
        const val COPY_SUFFIX = "(copy)"
    }
}
