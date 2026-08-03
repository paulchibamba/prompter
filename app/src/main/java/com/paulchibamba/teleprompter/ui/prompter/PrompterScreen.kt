package com.paulchibamba.teleprompter.ui.prompter

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.paulchibamba.teleprompter.domain.model.EndBehaviour
import com.paulchibamba.teleprompter.domain.scroll.WpmCalculator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The prompter (docs/SPEC.md §5.3).
 *
 * Everything the reader interacts with lives above the text and gets out of the way: the control
 * bar hides itself a few seconds after the last touch and comes back on a tap.
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
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    // The surface renders with the font scale pinned to 1 (docs/SPEC.md §6.2), so everything
    // derived from how tall the text lays out has to use the same density or the pace is wrong.
    val promptingDensity = remember(density.density) { Density(density.density, fontScale = 1f) }

    var textColumnWidthPx by remember { mutableIntStateOf(0) }
    var areControlsVisible by remember { mutableStateOf(true) }
    var isQuickSettingsOpen by remember { mutableStateOf(false) }
    var interactionCount by remember { mutableIntStateOf(0) }

    ImmersiveScreen(keepScreenOn = uiState.scroll.keepScreenOn, reapplyToken = isQuickSettingsOpen)
    HideControlsAfterIdle(areControlsVisible, interactionCount) { areControlsVisible = false }

    val paragraphSpacingPx = with(density) {
        (uiState.typography.sizeSp * uiState.typography.paragraphSpacingEm).dp.toPx()
    }
    val lineHeightPx = with(density) {
        (uiState.typography.sizeSp * uiState.typography.lineHeightMul).dp.toPx()
    }

    val contentHeightPx = rememberContentHeightPx(
        paragraphs = uiState.paragraphs,
        textStyle = rememberPrompterTextStyleFor(uiState.typography),
        columnWidthPx = textColumnWidthPx,
        paragraphSpacingPx = paragraphSpacingPx,
        density = promptingDensity,
    )

    val pixelsPerSecond = WpmCalculator.pxPerSecond(
        speedWpm = uiState.scroll.speedWpm,
        contentHeightPx = contentHeightPx,
        wordCount = uiState.wordCount,
    )

    AutoScrollEffect(
        listState = listState,
        isPlaying = uiState.isPlaying,
        pixelsPerSecond = pixelsPerSecond,
        rampMillis = uiState.scroll.rampMillis,
        onReachedEnd = {
            coroutineScope.launch {
                when (uiState.scroll.endBehaviour) {
                    EndBehaviour.HOLD -> viewModel.pause()
                    EndBehaviour.LOOP -> listState.scrollToItem(0)
                    EndBehaviour.EXIT -> {
                        viewModel.pause()
                        onNavigateBack()
                    }
                }
            }
        },
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            // On the parent, so the list below still receives drags for manual scrubbing while
            // taps that nothing else claimed reach us.
            .pointerInput(Unit) {
                detectTapGestures(onTap = { areControlsVisible = !areControlsVisible })
            },
    ) {
        PrompterSurface(
            paragraphs = uiState.paragraphs,
            typography = uiState.typography,
            layout = uiState.layout,
            listState = listState,
            onTextColumnMeasured = { widthPx -> textColumnWidthPx = widthPx },
        )

        if (!uiState.isLoading && !uiState.hasContent) {
            EmptyScriptMessage(backgroundColor = Color(uiState.typography.backgroundColor))
        }

        ControlBar(
            isVisible = areControlsVisible,
            isPlaying = uiState.isPlaying,
            speedWpm = uiState.scroll.speedWpm,
            fontSizeSp = uiState.typography.sizeSp,
            onPlayPause = viewModel::togglePlayPause,
            onRestart = {
                viewModel.pause()
                coroutineScope.launch { listState.animateScrollToItem(0) }
            },
            onNudgeUp = { coroutineScope.launch { listState.scrollBy(-lineHeightPx) } },
            onNudgeDown = { coroutineScope.launch { listState.scrollBy(lineHeightPx) } },
            onSpeedUp = viewModel::increaseSpeed,
            onSpeedDown = viewModel::decreaseSpeed,
            onFontUp = viewModel::increaseFontSize,
            onFontDown = viewModel::decreaseFontSize,
            onNavigateBack = onNavigateBack,
            onOpenQuickSettings = {
                areControlsVisible = false
                isQuickSettingsOpen = true
            },
            onInteraction = { interactionCount++ },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    if (isQuickSettingsOpen) {
        QuickSettingsSheet(
            typography = uiState.typography,
            onTypographyChanged = viewModel::updateTypography,
            customFontFile = viewModel.importedFontFile(),
            onCustomFontImported = viewModel::importCustomFont,
            onDismiss = { isQuickSettingsOpen = false },
        )
    }
}

/**
 * The control bar is chrome over the thing the reader is actually looking at, so it leaves on its
 * own. Any tap brings it back (§5.3).
 */
@Composable
private fun HideControlsAfterIdle(
    areControlsVisible: Boolean,
    interactionCount: Int,
    onHide: () -> Unit,
) {
    LaunchedEffect(areControlsVisible, interactionCount) {
        if (!areControlsVisible) return@LaunchedEffect
        delay(CONTROL_BAR_IDLE_MILLIS)
        onHide()
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

private const val CONTROL_BAR_IDLE_MILLIS = 3_000L
