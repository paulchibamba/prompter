package com.paulchibamba.teleprompter

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Owns the [AppContainer] for the process lifetime, and starts the one background job that has to
 * outlive every screen.
 */
class PrompterApplication : Application() {

    val container: AppContainer by lazy { AppContainer(this) }

    /**
     * Not the main scope: automatic backup reads the whole library and writes a file, and neither
     * belongs on the thread drawing the prompter.
     */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Collecting starts here rather than from a screen, so a snapshot is still written when the
        // edit that triggered it was the last thing the user did before leaving the app.
        container.automaticBackup.start(applicationScope)
    }
}
