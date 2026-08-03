package com.paulchibamba.teleprompter.data.db

import com.paulchibamba.teleprompter.domain.model.Script
import com.paulchibamba.teleprompter.domain.repository.ScriptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The Room-backed [ScriptRepository]. The only class in the app that knows [ScriptDao] exists.
 */
class RoomScriptRepository(private val dao: ScriptDao) : ScriptRepository {

    override fun observeAll(): Flow<List<Script>> =
        dao.observeAll().map { entities -> entities.map(ScriptEntity::toDomain) }

    override fun search(query: String): Flow<List<Script>> {
        // A blank query has to short-circuit: `LIKE '%%'` would match every row anyway, but only
        // by accident, and it would also match rows an empty search box should not be filtering.
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return observeAll()
        return dao.search(trimmed).map { entities -> entities.map(ScriptEntity::toDomain) }
    }

    override suspend fun byId(id: Long): Script? = dao.byId(id)?.toDomain()

    override suspend fun upsert(script: Script): Long {
        val rowId = dao.upsert(script.toEntity())
        // @Upsert returns -1 when the row was updated rather than inserted; the caller wants the
        // id it can navigate to either way.
        return if (rowId == -1L) script.id else rowId
    }

    override suspend fun delete(id: Long) = dao.deleteById(id)

    override suspend fun savePosition(id: Long, px: Float) = dao.savePosition(id, px)

    override suspend fun setOrder(idsInOrder: List<Long>) = dao.setSortIndices(idsInOrder)
}
