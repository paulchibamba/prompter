package com.paulchibamba.teleprompter

import android.app.Application

/**
 * Owns the [AppContainer] for the process lifetime. This exists only so there is one place the
 * container lives that outlives any activity — it does no work in `onCreate`.
 */
class PrompterApplication : Application() {

    val container: AppContainer by lazy { AppContainer(this) }
}
