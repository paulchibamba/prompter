package com.paulchibamba.teleprompter.domain.repository

import com.paulchibamba.teleprompter.domain.model.Script
import kotlinx.coroutines.flow.Flow

/**
 * Storage for scripts, as the domain sees it.
 *
 * The interface lives in `domain` and its Room implementation in `data.db` so the use-cases below
 * it depend on nothing from `data` — that is what keeps them unit-testable against a fake without
 * an emulator, and what keeps the dependency rule in `docs/ARCHITECTURE.md` intact.
 */
interface ScriptRepository {

    /** Every script, ordered by `sortIndex` then most-recently-updated. */
    fun observeAll(): Flow<List<Script>>

    /** Scripts whose title or body contains [query]; a blank query yields everything. */
    fun search(query: String): Flow<List<Script>>

    suspend fun byId(id: Long): Script?

    /** Inserts or updates, returning the row id — which is the new id when [script] was unsaved. */
    suspend fun upsert(script: Script): Long

    suspend fun delete(id: Long)

    /** Writes only the resume point, without touching `updatedAt`. */
    suspend fun savePosition(id: Long, px: Float)

    /** Assigns `sortIndex` 0..n-1 to [idsInOrder], atomically. */
    suspend fun setOrder(idsInOrder: List<Long>)
}
