package com.paulchibamba.teleprompter

import android.content.Context
import com.paulchibamba.teleprompter.data.db.PrompterDatabase
import com.paulchibamba.teleprompter.data.db.RoomScriptRepository
import com.paulchibamba.teleprompter.domain.repository.ScriptRepository
import com.paulchibamba.teleprompter.domain.usecase.DeleteScript
import com.paulchibamba.teleprompter.domain.usecase.DuplicateScript
import com.paulchibamba.teleprompter.domain.usecase.GetScript
import com.paulchibamba.teleprompter.domain.usecase.ObserveScripts
import com.paulchibamba.teleprompter.domain.usecase.ReorderScripts
import com.paulchibamba.teleprompter.domain.usecase.RestoreScript
import com.paulchibamba.teleprompter.domain.usecase.SaveScript
import com.paulchibamba.teleprompter.domain.usecase.SaveScrollPosition

/**
 * The app's object graph, by hand (see `docs/ARCHITECTURE.md`, "DI: none"). Holds the singletons —
 * database, repositories, use-cases — and is reached through [PrompterApplication.container].
 * Step 6 builds the `ViewModelProvider.Factory` that hands these to ViewModels.
 *
 * Everything is `by lazy`, so opening the database is deferred until something actually reads from
 * it rather than happening on the main thread during `Application.onCreate`.
 */
class AppContainer(context: Context) {

    private val applicationContext = context.applicationContext

    private val database: PrompterDatabase by lazy { PrompterDatabase.build(applicationContext) }

    val scriptRepository: ScriptRepository by lazy { RoomScriptRepository(database.scriptDao()) }

    val observeScripts: ObserveScripts by lazy { ObserveScripts(scriptRepository) }
    val getScript: GetScript by lazy { GetScript(scriptRepository) }
    val saveScript: SaveScript by lazy { SaveScript(scriptRepository) }
    val deleteScript: DeleteScript by lazy { DeleteScript(scriptRepository) }
    val restoreScript: RestoreScript by lazy { RestoreScript(scriptRepository) }
    val duplicateScript: DuplicateScript by lazy { DuplicateScript(scriptRepository, saveScript) }
    val reorderScripts: ReorderScripts by lazy { ReorderScripts(scriptRepository) }
    val saveScrollPosition: SaveScrollPosition by lazy { SaveScrollPosition(scriptRepository) }
}
