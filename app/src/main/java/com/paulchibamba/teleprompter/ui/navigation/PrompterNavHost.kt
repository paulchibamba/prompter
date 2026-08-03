package com.paulchibamba.teleprompter.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.paulchibamba.teleprompter.ui.editor.EditorScreen
import com.paulchibamba.teleprompter.ui.library.LibraryScreen
import com.paulchibamba.teleprompter.ui.prompter.PrompterScreen
import com.paulchibamba.teleprompter.ui.settings.KeySnifferScreen
import com.paulchibamba.teleprompter.ui.settings.RemoteMappingScreen
import com.paulchibamba.teleprompter.ui.settings.SettingsScreen

/**
 * The whole navigation graph. Single activity, so this is the only place routes are declared.
 *
 * Screens receive plain lambdas rather than the [NavHostController] itself, which keeps them
 * unaware of navigation and therefore previewable and testable in isolation.
 */
@Composable
fun PrompterNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Destination.Library,
        modifier = modifier,
    ) {
        composable<Destination.Library> {
            LibraryScreen(
                onOpenPrompter = { scriptId -> navController.navigate(Destination.Prompter(scriptId)) },
                onOpenEditor = { scriptId -> navController.navigate(Destination.Editor(scriptId)) },
                onOpenSettings = { navController.navigate(Destination.Settings) },
            )
        }

        composable<Destination.Editor> { backStackEntry ->
            val editor = backStackEntry.toRoute<Destination.Editor>()
            EditorScreen(
                scriptId = editor.scriptId,
                onNavigateBack = navController::navigateUp,
                onPreview = { scriptId -> navController.navigate(Destination.Prompter(scriptId)) },
            )
        }

        composable<Destination.Prompter> { backStackEntry ->
            val prompter = backStackEntry.toRoute<Destination.Prompter>()
            PrompterScreen(
                scriptId = prompter.scriptId,
                onNavigateBack = navController::navigateUp,
            )
        }

        composable<Destination.Settings> {
            SettingsScreen(
                onNavigateBack = navController::navigateUp,
                onOpenRemoteMapping = { navController.navigate(Destination.RemoteMapping) },
                onOpenKeySniffer = { navController.navigate(Destination.KeySniffer) },
            )
        }

        composable<Destination.RemoteMapping> {
            RemoteMappingScreen(onNavigateBack = navController::navigateUp)
        }

        composable<Destination.KeySniffer> {
            KeySnifferScreen(onNavigateBack = navController::navigateUp)
        }
    }
}
