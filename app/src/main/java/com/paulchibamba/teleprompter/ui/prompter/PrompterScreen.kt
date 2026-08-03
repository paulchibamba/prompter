package com.paulchibamba.teleprompter.ui.prompter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * The prompter (docs/SPEC.md §5.3).
 *
 * At this stage the surface renders and can be scrolled by hand; the frame-clock scroll engine and
 * the control bar arrive next.
 */
@Composable
fun PrompterScreen(
    scriptId: Long,
    onNavigateBack: () -> Unit,
    viewModel: PrompterViewModel = viewModel(
        key = "prompter-$scriptId",
        factory = PrompterViewModel.Factory,
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ImmersiveScreen(keepScreenOn = uiState.scroll.keepScreenOn)

    Box(modifier = Modifier.fillMaxSize()) {
        PrompterSurface(
            paragraphs = uiState.paragraphs,
            typography = uiState.typography,
            layout = uiState.layout,
        )

        if (!uiState.isLoading && !uiState.hasContent) {
            EmptyScriptMessage(backgroundColor = Color(uiState.typography.backgroundColor))
        }
    }
}

/**
 * A script with nothing in it would otherwise be an unbroken field of colour, which is
 * indistinguishable from the app having failed to load.
 */
@Composable
private fun EmptyScriptMessage(backgroundColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "This script is empty.",
            style = MaterialTheme.typography.titleMedium,
            color = Color.Gray,
            modifier = Modifier.padding(24.dp),
        )
    }
}
