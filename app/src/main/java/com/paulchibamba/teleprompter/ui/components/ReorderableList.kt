package com.paulchibamba.teleprompter.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Drag-to-reorder for a `LazyColumn`, driven from a dedicated handle.
 *
 * The order is held here while the finger is down and committed once on release, so a drag across
 * six rows is one database write rather than six. Long-press is deliberately not the trigger: the
 * library already spends it on "open in editor" (docs/SPEC.md §5.1), so reordering gets a handle.
 *
 * Items must be keyed by [Long] id in the `LazyColumn` for the hit-testing below to find them.
 */
@Composable
fun <T> rememberReorderState(
    listState: LazyListState,
    items: List<T>,
    keyOf: (T) -> Long,
    onOrderCommitted: (List<Long>) -> Unit,
): ReorderState<T> {
    val state = remember(listState) { ReorderState(listState, keyOf, onOrderCommitted) }
    state.syncWith(items)
    return state
}

class ReorderState<T>(
    private val listState: LazyListState,
    private val keyOf: (T) -> Long,
    private val onOrderCommitted: (List<Long>) -> Unit,
) {
    /** The list as it currently reads, including any not-yet-committed drag. */
    var orderedItems by mutableStateOf(emptyList<T>())
        private set

    var draggedKey by mutableStateOf<Long?>(null)
        private set

    private var dragOffsetY by mutableFloatStateOf(0f)

    /** Vertical shift to apply to the row being dragged; zero for every other row. */
    fun offsetFor(key: Long): Float = if (key == draggedKey) dragOffsetY else 0f

    fun isDragging(key: Long): Boolean = key == draggedKey

    /**
     * Adopts a new list from the database, unless a drag is in flight — accepting an update
     * mid-drag would yank the row out from under the finger.
     */
    internal fun syncWith(items: List<T>) {
        if (draggedKey == null) orderedItems = items
    }

    fun onDragStarted(key: Long) {
        draggedKey = key
        dragOffsetY = 0f
    }

    fun onDragged(deltaY: Float) {
        val key = draggedKey ?: return
        dragOffsetY += deltaY
        moveIfPastNeighbour(key)
    }

    fun onDragStopped() {
        if (draggedKey != null) onOrderCommitted(orderedItems.map(keyOf))
        draggedKey = null
        dragOffsetY = 0f
    }

    /**
     * Swaps the dragged row with whichever row its centre has moved over, then rebases the offset
     * by the distance travelled — the row keeps sitting under the finger even though its index,
     * and therefore its resting position, just changed.
     */
    private fun moveIfPastNeighbour(key: Long) {
        val dragged = visibleItemFor(key) ?: return
        val draggedCentre = dragged.offset + dragOffsetY + dragged.size / 2f

        val target = listState.layoutInfo.visibleItemsInfo.firstOrNull { candidate ->
            candidate.key != key && draggedCentre.toInt() in candidate.offset..(candidate.offset + candidate.size)
        } ?: return

        val fromIndex = orderedItems.indexOfFirst { keyOf(it) == key }
        val toIndex = orderedItems.indexOfFirst { keyOf(it) == target.key }
        if (fromIndex == -1 || toIndex == -1) return

        orderedItems = orderedItems.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
        dragOffsetY -= (target.offset - dragged.offset)
    }

    private fun visibleItemFor(key: Long) =
        listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }
}
