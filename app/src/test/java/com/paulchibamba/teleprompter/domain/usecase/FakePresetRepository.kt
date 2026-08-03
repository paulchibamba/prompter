package com.paulchibamba.teleprompter.domain.usecase

import com.paulchibamba.teleprompter.domain.model.BuiltInPresets
import com.paulchibamba.teleprompter.domain.model.Preset
import com.paulchibamba.teleprompter.domain.repository.PresetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * In-memory [PresetRepository] for use-case tests, mirroring the Room implementation's contract:
 * built-ins seeded on demand and first in the list, user ids from
 * [BuiltInPresets.FIRST_USER_ID] up, and a refusal to delete a built-in. `PresetDaoTest` covers
 * the SQL that backs those rules in the app.
 */
class FakePresetRepository(initial: List<Preset> = emptyList()) : PresetRepository {

    private val presets = MutableStateFlow(initial.associateBy(Preset::id))
    private var nextId = maxOf(
        BuiltInPresets.FIRST_USER_ID,
        (initial.maxOfOrNull { it.id } ?: 0L) + 1,
    )

    val current: List<Preset> get() = presets.value.values.sortedWith(ORDER)

    override fun observeAll(): Flow<List<Preset>> = flow {
        ensureBuiltIns()
        emitAll(presets.map { it.values.sortedWith(ORDER) })
    }

    override suspend fun byId(id: Long): Preset? {
        ensureBuiltIns()
        return presets.value[id]
    }

    override suspend fun upsert(preset: Preset): Long {
        ensureBuiltIns()
        val id = if (preset.id == 0L) nextId++ else preset.id
        presets.value = presets.value + (id to preset.coerced().copy(id = id))
        return id
    }

    override suspend fun delete(id: Long): Boolean {
        if (presets.value[id]?.isBuiltIn != false) return false
        presets.value = presets.value - id
        return true
    }

    override suspend fun ensureBuiltIns() {
        val missing = BuiltInPresets.all.filterNot { presets.value.containsKey(it.id) }
        presets.value = presets.value + missing.associateBy(Preset::id)
    }

    private companion object {
        /** Matches `PresetDao.observeAll`: built-ins in shipped order, then user presets by name. */
        val ORDER = compareByDescending<Preset> { it.isBuiltIn }
            .thenBy { if (it.isBuiltIn) it.id else 0L }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
    }
}
