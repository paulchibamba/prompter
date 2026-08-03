package com.paulchibamba.teleprompter.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.paulchibamba.teleprompter.AppContainer
import com.paulchibamba.teleprompter.PrompterApplication

/**
 * Reaches the hand-built object graph from inside a `ViewModelProvider.Factory`.
 *
 * There is no DI framework here (docs/SPEC.md §1), so each ViewModel declares its own factory and
 * pulls exactly the use-cases it needs:
 *
 * ```
 * companion object {
 *     val Factory = viewModelFactory {
 *         initializer { LibraryViewModel(prompterContainer().observeScripts) }
 *     }
 * }
 * ```
 *
 * Depending on named use-cases rather than the whole container keeps each ViewModel's requirements
 * visible in its constructor, which is most of what a DI framework would have bought us.
 */
fun CreationExtras.prompterContainer(): AppContainer {
    val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
    require(application is PrompterApplication) {
        "Expected the application to be PrompterApplication, but was ${application?.javaClass?.name}"
    }
    return application.container
}
