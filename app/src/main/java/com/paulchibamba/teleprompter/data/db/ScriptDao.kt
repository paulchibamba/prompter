package com.paulchibamba.teleprompter.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ScriptDao {

    @Query("SELECT * FROM scripts ORDER BY sortIndex ASC, updatedAt DESC")
    fun observeAll(): Flow<List<ScriptEntity>>

    @Query("SELECT * FROM scripts WHERE id = :id")
    suspend fun byId(id: Long): ScriptEntity?

    @Query(
        """
        SELECT * FROM scripts
        WHERE title LIKE '%' || :query || '%' OR body LIKE '%' || :query || '%'
        ORDER BY sortIndex ASC, updatedAt DESC
        """,
    )
    fun search(query: String): Flow<List<ScriptEntity>>

    @Upsert
    suspend fun upsert(script: ScriptEntity): Long

    @Delete
    suspend fun delete(script: ScriptEntity)

    @Query("DELETE FROM scripts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE scripts SET lastPositionPx = :px WHERE id = :id")
    suspend fun savePosition(id: Long, px: Float)

    @Query("UPDATE scripts SET sortIndex = :sortIndex WHERE id = :id")
    suspend fun setSortIndex(id: Long, sortIndex: Int)

    /**
     * Rewrites the whole ordering in one transaction, so a drag that reorders ten rows either
     * lands completely or not at all — a half-applied order would show the list jumbled.
     */
    @Transaction
    suspend fun setSortIndices(idsInOrder: List<Long>) {
        idsInOrder.forEachIndexed { index, id -> setSortIndex(id, index) }
    }

    @Query("SELECT COUNT(*) FROM scripts")
    suspend fun count(): Int
}
