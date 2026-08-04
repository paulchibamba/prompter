package com.paulchibamba.teleprompter.ui.prompter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.paulchibamba.teleprompter.domain.model.CaseMode
import com.paulchibamba.teleprompter.domain.model.LayoutSettings
import com.paulchibamba.teleprompter.domain.model.MarkerStyle
import com.paulchibamba.teleprompter.domain.model.PromptAlign
import com.paulchibamba.teleprompter.domain.model.TypographySettings
import com.paulchibamba.teleprompter.domain.text.ScriptParser
import com.paulchibamba.teleprompter.ui.theme.PrompterFonts
import java.io.File

/**
 * The scrolling text itself (docs/SPEC.md §6, §8.3).
 *
 * Two things here are not negotiable and are easy to get wrong:
 *
 * The surface opts **out of the system font scale**. A user who sets 72sp means 72sp — they have
 * measured it against glass at a distance. Every other screen in the app respects font scale; this
 * one must not, or the calibration they did is silently undone by an accessibility setting.
 *
 * Paragraphs are **separate list items**, not one enormous `Text`. Compose degrades badly on very
 * large single text nodes, and a ten-thousand-word script is exactly that.
 */
@Composable
fun PrompterSurface(
    paragraphs: List<String>,
    typography: TypographySettings,
    layout: LayoutSettings,
    listState: LazyListState = rememberLazyListState(),
    onTextColumnMeasured: (widthPx: Int) -> Unit = {},
    isSafeAreaVisible: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current

    CompositionLocalProvider(
        LocalDensity provides Density(density.density, fontScale = 1f),
    ) {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .background(Color(typography.backgroundColor)),
        ) {
            val paragraphSpacing = typography.paragraphSpacing()
            val textStyle = rememberPrompterTextStyleFor(typography)
            val metrics = rememberSurfaceMetrics(
                screenWidth = maxWidth,
                screenHeight = maxHeight,
                layout = layout,
                lineHeight = typography.lineHeight(),
                paragraphSpacing = paragraphSpacing,
            )

            val widthWithinMargins = maxWidth - metrics.marginLeft - metrics.marginRight
            val readingWidth = rememberReadingWidth(
                widthWithinMargins = widthWithinMargins,
                maxMeasureCh = layout.maxMeasureCh,
                textStyle = textStyle,
            )

            // The *effective* width, after any line-length cap — measuring the pace against the
            // uncapped width would make the script lay out shorter than it reads.
            ReportTextColumnWidth(columnWidth = readingWidth, onMeasured = onTextColumnMeasured)

            // Text and reading line share one mirrored layer so they can never disagree about
            // where the current line is.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .mirrored(
                        horizontally = layout.mirrorHorizontal,
                        vertically = layout.mirrorVertical,
                    ),
            ) {
            ParagraphList(
                paragraphs = paragraphs,
                textStyle = textStyle,
                paragraphSpacing = paragraphSpacing,
                caseTransform = typography.caseTransform,
                markerStyle = typography.markerStyle,
                markerAppearance = typography.markerAppearance(),
                metrics = metrics,
                readingWidth = readingWidth,
                listState = listState,
                edgeFadePercent = layout.edgeFadePct,
            )

            ReadingLineIndicator(
                style = layout.readingLineStyle,
                colour = Color(layout.readingLineColor),
                readingLineFromTop = metrics.readingLineFromTop,
                lineHeight = typography.lineHeight(),
                marginLeft = metrics.marginLeft,
                marginRight = metrics.marginRight,
            )
            }

            if (isSafeAreaVisible) {
                SafeAreaOverlay(
                    marginLeft = metrics.marginLeft,
                    marginRight = metrics.marginRight,
                    marginTop = metrics.marginTop,
                    marginBottom = metrics.marginBottom,
                )
            }
        }
    }
}

@Composable
private fun ParagraphList(
    paragraphs: List<String>,
    textStyle: TextStyle,
    paragraphSpacing: Dp,
    caseTransform: CaseMode,
    markerStyle: MarkerStyle,
    markerAppearance: MarkerAppearance,
    metrics: SurfaceMetrics,
    readingWidth: Dp,
    listState: LazyListState,
    edgeFadePercent: Float,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = metrics.marginLeft,
                end = metrics.marginRight,
                top = metrics.marginTop,
                bottom = metrics.marginBottom,
            )
            // After the margins, not before: the text is already clipped to this area, so a fade
            // measured against the whole screen would land entirely in the margins and do nothing.
            .edgeFade(edgeFadePercent),
    ) {
    LazyColumn(
        state = listState,
        userScrollEnabled = true,
        // Centred within the margins, so capping the line length narrows the column evenly
        // rather than dragging the text to one side (docs/SPEC.md §6.7).
        modifier = Modifier
            .fillMaxHeight()
            .width(readingWidth)
            .align(Alignment.TopCenter),
    ) {
        item(key = LEAD_IN_KEY) { Spacer(Modifier.height(metrics.leadIn)) }

        itemsIndexed(paragraphs, key = { index, _ -> index }) { _, paragraph ->
            val isMarker = ScriptParser.isMarkerLine(paragraph)
            if (!(isMarker && markerStyle == MarkerStyle.HIDDEN)) {
                ParagraphText(
                    paragraph = paragraph,
                    textStyle = textStyle,
                    bottomSpacing = paragraphSpacing,
                    caseTransform = caseTransform,
                    markerAppearance = if (isMarker) markerAppearance else null,
                )
            }
        }

        item(key = LEAD_OUT_KEY) { Spacer(Modifier.height(metrics.leadOut)) }
    }
    }
}

/**
 * How wide the text column actually is, after any line-length cap.
 *
 * Long lines are the classic teleprompter failure: past a certain measure the eye loses the return
 * sweep and starts re-reading the line it just finished. The cap is expressed in characters, so it
 * is derived from how wide a character actually is in the current face and size.
 */
@Composable
private fun rememberReadingWidth(
    widthWithinMargins: Dp,
    maxMeasureCh: Int,
    textStyle: TextStyle,
): Dp {
    if (maxMeasureCh <= LayoutSettings.MEASURE_OFF) return widthWithinMargins

    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    return remember(widthWithinMargins, maxMeasureCh, textStyle, density) {
        val sample = textMeasurer.measure(AnnotatedString(AVERAGE_WIDTH_SAMPLE), textStyle)
        val averageCharWidthPx = sample.size.width.toFloat() / AVERAGE_WIDTH_SAMPLE.length
        val cappedWidth = with(density) { (averageCharWidthPx * maxMeasureCh).toDp() }
        minOf(cappedWidth, widthWithinMargins)
    }
}

/** Lowercase letters, because that is what most of a script is made of. */
private const val AVERAGE_WIDTH_SAMPLE = "abcdefghijklmnopqrstuvwxyz"

/**
 * One line of the script.
 *
 * Paragraph spacing is padding rather than blank lines in the text, so changing it never edits the
 * user's script (§6.6).
 */
@Composable
private fun ParagraphText(
    paragraph: String,
    textStyle: TextStyle,
    bottomSpacing: Dp,
    caseTransform: CaseMode,
    markerAppearance: MarkerAppearance?,
) {
    Text(
        text = if (caseTransform == CaseMode.UPPER) paragraph.uppercase() else paragraph,
        style = textStyle,
        color = markerAppearance?.color ?: Color.Unspecified,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = bottomSpacing),
    )
}

/**
 * The style the surface draws with. Public because the scroll pace is derived from how tall this
 * exact style lays the script out — measuring with anything else would put the pace subtly wrong.
 */
@Composable
fun rememberPrompterTextStyleFor(typography: TypographySettings): TextStyle =
    remember(typography) {
        TextStyle(
            color = Color(typography.textColor),
            fontSize = typography.sizeSp.sp,
            fontFamily = PrompterFonts.familyFor(
                fontId = typography.fontId,
                customFontFile = typography.customFontUri?.let(::File),
            ),
            fontWeight = FontWeight(typography.weight),
            lineHeight = (typography.sizeSp * typography.lineHeightMul).sp,
            letterSpacing = typography.letterSpacingEm.em,
            textAlign = when (typography.textAlign) {
                PromptAlign.LEFT -> TextAlign.Start
                PromptAlign.CENTER -> TextAlign.Center
            },
            // Trim.None keeps the first and last line's leading intact, so spacing stays even as
            // text scrolls past the reading line rather than tightening at the edges (§6.3).
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.None,
            ),
        )
    }

/** Hands the laid-out text width back up, in pixels, for the content measurement. */
@Composable
private fun ReportTextColumnWidth(columnWidth: Dp, onMeasured: (Int) -> Unit) {
    val widthPx = with(LocalDensity.current) { columnWidth.roundToPx() }
    LaunchedEffect(widthPx) { onMeasured(widthPx) }
}

/** How a marker line differs from ordinary text; null means it does not (docs/SPEC.md §6.10). */
private data class MarkerAppearance(val color: Color)

private fun TypographySettings.markerAppearance(): MarkerAppearance = when (markerStyle) {
    MarkerStyle.NORMAL, MarkerStyle.HIDDEN -> MarkerAppearance(Color(textColor))
    MarkerStyle.DIMMED -> MarkerAppearance(
        Color(textColor).copy(alpha = TypographySettings.DIMMED_MARKER_ALPHA),
    )
    MarkerStyle.ACCENT -> MarkerAppearance(Color(markerColor))
}

private fun TypographySettings.paragraphSpacing(): Dp = (sizeSp * paragraphSpacingEm).dp

/**
 * One line's worth of vertical space. Because the surface pins `fontScale` to 1, an sp value and a
 * dp value describe the same distance here — which is exactly why this conversion is safe.
 */
private fun TypographySettings.lineHeight(): Dp = (sizeSp * lineHeightMul).dp

/** Where the text column sits, and how much empty space brackets it. */
private data class SurfaceMetrics(
    val marginLeft: Dp,
    val marginRight: Dp,
    val marginTop: Dp,
    val marginBottom: Dp,
    val leadIn: Dp,
    val leadOut: Dp,
    /** Where the reading-line indicator is drawn, measured from the top of the screen. */
    val readingLineFromTop: Dp,
)

/**
 * Margins are percentages of the screen, not dp, because the constraint is the beam-splitter's
 * crop and that is proportional (§7.1).
 *
 * The lead-in and lead-out spacers are the part everyone forgets: without them the first line
 * starts at the top edge instead of at the reading line, and the last line can never reach the
 * reading line at all — so the end of every script is unreadable.
 */
@Composable
private fun rememberSurfaceMetrics(
    screenWidth: Dp,
    screenHeight: Dp,
    layout: LayoutSettings,
    lineHeight: Dp,
    paragraphSpacing: Dp,
): SurfaceMetrics = remember(screenWidth, screenHeight, layout, lineHeight, paragraphSpacing) {
    val marginTop = screenHeight * (layout.marginTopPct / 100f)
    val marginBottom = screenHeight * (layout.marginBottomPct / 100f)
    val columnHeight = (screenHeight - marginTop - marginBottom).coerceAtLeast(0.dp)

    // The reading line is measured from the top of the screen, which is how a user calibrating
    // against glass thinks about it.
    val readingLineFromTop = screenHeight * (layout.readingLinePct / 100f)

    // Half a line comes off the lead-in so the *middle* of the current line sits on the mark,
    // rather than the top of its line box. Without this the text settles visibly below the
    // indicator — the line box carries half its leading above the glyphs, and at a typical size
    // and leading that gap is large enough to look like a misalignment rather than a choice.
    val leadIn = (readingLineFromTop - marginTop - lineHeight / 2).coerceIn(0.dp, columnHeight)

    SurfaceMetrics(
        marginLeft = screenWidth * (layout.marginLeftPct / 100f),
        marginRight = screenWidth * (layout.marginRightPct / 100f),
        marginTop = marginTop,
        marginBottom = marginBottom,
        leadIn = leadIn,
        // Enough for the last line to travel up to the reading line and stop *on* it. The final
        // line's own height and trailing paragraph spacing already occupy part of that distance,
        // so they come off — otherwise the script ends with the last line drifting past the mark
        // into blank space, which reads as the text having run away from you.
        leadOut = (columnHeight - leadIn - lineHeight - paragraphSpacing).coerceAtLeast(0.dp),
        readingLineFromTop = readingLineFromTop,
    )
}

private const val LEAD_IN_KEY = "lead-in"
private const val LEAD_OUT_KEY = "lead-out"
