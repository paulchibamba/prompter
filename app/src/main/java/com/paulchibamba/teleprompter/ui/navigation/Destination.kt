package com.paulchibamba.teleprompter.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Every place the app can navigate to. These are type-safe Navigation Compose routes: arguments are
 * real Kotlin properties rather than strings spliced into a URL, so a wrong argument is a compile
 * error instead of a crash at runtime.
 */
sealed interface Destination {

    /** The start destination. Reading is the common case, so this lists scripts to read. */
    @Serializable
    data object Library : Destination

    /** Editing an existing script, or writing a new one when [scriptId] is [NEW_SCRIPT]. */
    @Serializable
    data class Editor(val scriptId: Long = NEW_SCRIPT) : Destination

    /** The prompter itself: full-screen, immersive, the reason the app exists. */
    @Serializable
    data class Prompter(val scriptId: Long) : Destination

    /** Global defaults, preset management, and the way in to the remote screens. */
    @Serializable
    data object Settings : Destination

    /** Binding remote buttons to actions. */
    @Serializable
    data object RemoteMapping : Destination

    /** Diagnostic: shows exactly what key events arrive, and from which device. */
    @Serializable
    data object KeySniffer : Destination

    companion object {
        /** Sentinel for "the editor has no script yet"; a real script id is always positive. */
        const val NEW_SCRIPT: Long = 0L
    }
}
