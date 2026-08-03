package com.paulchibamba.teleprompter.domain.usecase

import com.paulchibamba.teleprompter.domain.model.Script
import com.paulchibamba.teleprompter.domain.repository.ScriptRepository
import com.paulchibamba.teleprompter.domain.text.ScriptParser
import kotlinx.coroutines.flow.Flow

/**
 * The operations the UI performs on scripts. They live here, rather than in the ViewModels, because
 * more than one screen performs several of them — the library and the editor both save, both
 * duplicate — and because the rules they encode (how a title is derived, when `updatedAt` moves)
 * are worth testing without a ViewModel or an emulator in the way.
 *
 * Each is a class with an `operator fun invoke`, so a ViewModel calls it like a function while
 * still being able to take a fake in a test.
 */

/** Scripts for the library list, filtered by [query] when the search box is not empty. */
class ObserveScripts(private val repository: ScriptRepository) {
    operator fun invoke(query: String = ""): Flow<List<Script>> = repository.search(query)
}

/** A single script by id, or null if it has been deleted underneath us. */
class GetScript(private val repository: ScriptRepository) {
    suspend operator fun invoke(id: Long): Script? = repository.byId(id)
}

/**
 * Saves a script, deriving everything that should never be the caller's job to get right:
 * the denormalised word count, the timestamps, and a title for scripts the user never named.
 *
 * @param now injected so tests can pin the clock.
 * @param untitled fallback title when neither the title nor the body offers one. Step 8 can pass a
 *   localised string; the default keeps `domain` free of Android resources.
 */
class SaveScript(
    private val repository: ScriptRepository,
    private val now: () -> Long = System::currentTimeMillis,
    private val untitled: String = UNTITLED,
) {
    suspend operator fun invoke(script: Script): Long {
        val timestamp = now()
        val prepared = script.copy(
            title = script.title.trim().ifEmpty { deriveTitle(script.body) },
            wordCount = ScriptParser.wordCount(script.body),
            // A script being inserted for the first time gets both timestamps; an existing one
            // keeps the created date it was born with.
            createdAt = if (script.id == 0L || script.createdAt == 0L) timestamp else script.createdAt,
            updatedAt = timestamp,
        )
        return repository.upsert(prepared)
    }

    /**
     * First non-blank line of the body, with any marker hashes stripped and long lines truncated.
     * Deriving beats prompting: the user typed a script, not a filename, and an untitled row in
     * the library is worse than a slightly odd one.
     */
    private fun deriveTitle(body: String): String {
        val line = body.lineSequence()
            .map { it.trim().removePrefix("###").removePrefix("##").removePrefix("#").trim() }
            .firstOrNull { it.isNotEmpty() && it != SECTION_BREAK }
            ?: return untitled
        return if (line.length <= MAX_DERIVED_TITLE) line else line.take(MAX_DERIVED_TITLE).trimEnd() + "…"
    }

    companion object {
        const val UNTITLED = "Untitled"
        const val MAX_DERIVED_TITLE = 60
        private const val SECTION_BREAK = "---"
    }
}

/**
 * Deletes a script. Pairs with [RestoreScript] for the library's undo snackbar (docs/SPEC.md §5.1)
 * — the caller holds the deleted [Script] for the five seconds the snackbar is up.
 */
class DeleteScript(private val repository: ScriptRepository) {
    suspend operator fun invoke(script: Script) = repository.delete(script.id)
}

/** Puts a deleted script back, id and all, so anything referring to it still resolves. */
class RestoreScript(private val repository: ScriptRepository) {
    suspend operator fun invoke(script: Script): Long = repository.upsert(script)
}

/**
 * Copies a script under a new id. The copy starts unread — carrying the original's resume position
 * into a duplicate would drop the reader into the middle of a script they just made.
 */
class DuplicateScript(
    private val repository: ScriptRepository,
    private val saveScript: SaveScript,
) {
    suspend operator fun invoke(id: Long): Long? {
        val original = repository.byId(id) ?: return null
        return saveScript(
            original.copy(
                id = 0L,
                title = "${original.title} $COPY_SUFFIX",
                createdAt = 0L,
                lastPositionPx = 0f,
            ),
        )
    }

    companion object {
        const val COPY_SUFFIX = "(copy)"
    }
}

/** Applies a drag-reorder: [idsInOrder] is the list exactly as it now reads on screen. */
class ReorderScripts(private val repository: ScriptRepository) {
    suspend operator fun invoke(idsInOrder: List<Long>) = repository.setOrder(idsInOrder)
}

/**
 * Stores the resume point. Deliberately does not touch `updatedAt`: merely reading a script is not
 * editing it, and letting a read reshuffle the library's "recently updated" order would be wrong.
 */
class SaveScrollPosition(private val repository: ScriptRepository) {
    suspend operator fun invoke(id: Long, px: Float) = repository.savePosition(id, px.coerceAtLeast(0f))
}
