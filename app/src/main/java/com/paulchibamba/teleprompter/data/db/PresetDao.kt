package com.paulchibamba.teleprompter.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PresetDao {

    /**
     * Built-ins first in the order they ship — Studio, the everyday one, at the top — then user
     * presets alphabetically. The `CASE` is what mixes the two rules: it sorts built-ins by id and
     * collapses to a constant for user rows, which then fall through to the name.
     *
     * `COLLATE NOCASE` so "atkinson" sorts next to "Atkinson" rather than after every capitalised
     * name, which is what a reader expects of an alphabetical list.
     */
    @Query(
        """
        SELECT * FROM presets
        ORDER BY isBuiltIn DESC, CASE WHEN isBuiltIn = 1 THEN id ELSE 0 END ASC, name COLLATE NOCASE ASC
        """,
    )
    fun observeAll(): Flow<List<PresetEntity>>

    @Query("SELECT * FROM presets WHERE id = :id")
    suspend fun byId(id: Long): PresetEntity?

    @Upsert
    suspend fun upsert(preset: PresetEntity): Long

    /**
     * Seeds the built-ins. `IGNORE` is what makes it idempotent: the rows carry fixed ids, so a
     * second call collides on the primary key and changes nothing rather than resetting a built-in
     * a later step let the user tweak.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(presets: List<PresetEntity>)

    /** Deletes only if the row is a user preset; returns the number of rows removed (0 or 1). */
    @Query("DELETE FROM presets WHERE id = :id AND isBuiltIn = 0")
    suspend fun deleteUserPreset(id: Long): Int

    @Query("SELECT COUNT(*) FROM presets")
    suspend fun count(): Int
}
