package com.paulchibamba.teleprompter.domain.repository

import com.paulchibamba.teleprompter.domain.model.Preset
import kotlinx.coroutines.flow.Flow

/**
 * Storage for presets, as the domain sees it. Like [ScriptRepository], the interface lives here and
 * the Room implementation in `data.db`, so the use-cases above it can be tested against a fake.
 */
interface PresetRepository {

    /** Every preset, built-ins first, then user presets by name. */
    fun observeAll(): Flow<List<Preset>>

    suspend fun byId(id: Long): Preset?

    /** Inserts or updates, returning the row id — which is the new id when [preset] was unsaved. */
    suspend fun upsert(preset: Preset): Long

    /**
     * Deletes a user preset. Returns false, changing nothing, if [id] names a built-in: those are
     * read-only (see [com.paulchibamba.teleprompter.domain.model.BuiltInPresets]).
     */
    suspend fun delete(id: Long): Boolean

    /**
     * Inserts any built-in preset that is not already stored. Idempotent, so it is safe to call on
     * every read; it is what puts the three shipped presets in front of a user who has just
     * installed the app, and what restores them after an upgrade that adds one.
     */
    suspend fun ensureBuiltIns()
}
