package com.paulchibamba.teleprompter.domain.usecase

import com.paulchibamba.teleprompter.domain.model.Script
import com.paulchibamba.teleprompter.domain.repository.ScriptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory [ScriptRepository] for use-case tests. It mirrors the Room implementation's contract —
 * id assignment, sort order, blank-query search — closely enough that a use-case passing here is
 * exercising the same behaviour it will see in the app; `ScriptDaoTest` covers the SQL itself.
 */
class FakeScriptRepository(initial: List<Script> = emptyList()) : ScriptRepository {

    private val scripts = MutableStateFlow(initial.associateBy(Script::id))
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0L) + 1

    val current: List<Script> get() = scripts.value.values.sortedWith(ORDER)

    override fun observeAll(): Flow<List<Script>> = scripts.map { it.values.sortedWith(ORDER) }

    override fun search(query: String): Flow<List<Script>> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return observeAll()
        return observeAll().map { all ->
            all.filter { it.title.contains(trimmed, true) || it.body.contains(trimmed, true) }
        }
    }

    override suspend fun byId(id: Long): Script? = scripts.value[id]

    override suspend fun upsert(script: Script): Long {
        val id = if (script.id == 0L) nextId++ else script.id
        scripts.value = scripts.value + (id to script.copy(id = id))
        return id
    }

    override suspend fun delete(id: Long) {
        scripts.value = scripts.value - id
    }

    override suspend fun savePosition(id: Long, px: Float) {
        val existing = scripts.value[id] ?: return
        scripts.value = scripts.value + (id to existing.copy(lastPositionPx = px))
    }

    override suspend fun setOrder(idsInOrder: List<Long>) {
        idsInOrder.forEachIndexed { index, id ->
            val existing = scripts.value[id] ?: return@forEachIndexed
            scripts.value = scripts.value + (id to existing.copy(sortIndex = index))
        }
    }

    private companion object {
        val ORDER = compareBy<Script> { it.sortIndex }.thenByDescending { it.updatedAt }
    }
}
