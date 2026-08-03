package com.paulchibamba.teleprompter.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.paulchibamba.teleprompter.domain.model.Script

/**
 * The stored shape of a script (docs/SPEC.md §3.1). Kept separate from
 * [com.paulchibamba.teleprompter.domain.model.Script] so Room's annotations stay inside `data.db`
 * and the domain layer stays plain Kotlin.
 *
 * [wordCount] is denormalised — recomputed on every save — because the library list shows a
 * duration estimate for every row and counting words in a 10,000-word body per frame of a scroll
 * is not something to do lazily.
 */
@Entity(tableName = "scripts")
data class ScriptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val body: String,
    val createdAt: Long,
    val updatedAt: Long,
    val wordCount: Int,
    val lastPositionPx: Float = 0f,
    val presetId: Long? = null,
    val sortIndex: Int = 0,
)

fun ScriptEntity.toDomain(): Script = Script(
    id = id,
    title = title,
    body = body,
    createdAt = createdAt,
    updatedAt = updatedAt,
    wordCount = wordCount,
    lastPositionPx = lastPositionPx,
    presetId = presetId,
    sortIndex = sortIndex,
)

fun Script.toEntity(): ScriptEntity = ScriptEntity(
    id = id,
    title = title,
    body = body,
    createdAt = createdAt,
    updatedAt = updatedAt,
    wordCount = wordCount,
    lastPositionPx = lastPositionPx,
    presetId = presetId,
    sortIndex = sortIndex,
)
