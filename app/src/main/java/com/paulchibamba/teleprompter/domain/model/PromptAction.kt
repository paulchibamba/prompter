package com.paulchibamba.teleprompter.domain.model

/**
 * Everything a remote button, gesture or control-bar tap can ask the prompter to do
 * (docs/SPEC.md §9.2). The router (Step 21) maps key codes onto these; the prompter only ever
 * handles actions, so it never learns what a key code is.
 */
enum class PromptAction {
    PLAY_PAUSE,
    PLAY,
    PAUSE,
    SPEED_UP,
    SPEED_DOWN,
    FONT_UP,
    FONT_DOWN,
    NUDGE_UP,
    NUDGE_DOWN,
    NEXT_MARKER,
    PREV_MARKER,
    RESTART,
    JUMP_END,
    TOGGLE_MIRROR,
    BLACKOUT,
    NEXT_SCRIPT,
    PREV_SCRIPT,
    EXIT,
    NONE,
}
