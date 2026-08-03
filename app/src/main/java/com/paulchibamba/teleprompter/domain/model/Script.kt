package com.paulchibamba.teleprompter.domain.model

/**
 * A script as the rest of the app sees it. The Room entity (Step 4) maps onto this; nothing outside
 * `data.db` should touch the entity directly.
 *
 * [body] is plain text with newlines preserved exactly as typed or imported — collapsing them was
 * the reference app's defining bug (docs/SPEC.md §3.1).
 */
data class Script(
    val id: Long = 0L,
    val title: String,
    val body: String,
    val createdAt: Long,
    val updatedAt: Long,
    val wordCount: Int,
    val lastPositionPx: Float = 0f,
    val presetId: Long? = null,
    val sortIndex: Int = 0,
)

/**
 * A cue point derived from the body text — never stored. See [com.paulchibamba.teleprompter.domain.text.ScriptParser].
 *
 * @param charOffset index into the body of the first character of the marker's line.
 * @param label the heading text with the leading hashes and whitespace stripped.
 * @param level 1..3, from the number of hashes.
 */
data class Marker(
    val charOffset: Int,
    val label: String,
    val level: Int,
)
