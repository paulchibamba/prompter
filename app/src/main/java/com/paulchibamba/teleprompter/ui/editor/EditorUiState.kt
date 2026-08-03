package com.paulchibamba.teleprompter.ui.editor

import androidx.compose.ui.text.input.TextFieldValue

data class EditorUiState(
    val title: String = "",
    /**
     * Carries the caret as well as the text, because inserting a marker has to put the caret back
     * where the user left it. This is a UI type in a ViewModel by deliberate choice — the
     * alternative is threading text and selection through as two values that must never disagree.
     */
    val body: TextFieldValue = TextFieldValue(),
    val wordCount: Int = 0,
    /** "412 words · 2:56" at the user's current reading pace. */
    val summary: String = "",
    val isLoading: Boolean = true,
    /** Null until the script has been written to the database at least once. */
    val savedScriptId: Long? = null,
) {
    val isEmpty: Boolean
        get() = title.isBlank() && body.text.isBlank()
}
