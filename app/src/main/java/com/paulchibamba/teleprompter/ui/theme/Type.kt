package com.paulchibamba.teleprompter.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The Material 3 type scale for the app's chrome.
 *
 * Chrome uses the device font and honours the system font scale, exactly as a well-behaved Android
 * app should. The prompter surface does neither — a user who asks for 72sp means 72sp, so it opts
 * out of font scaling entirely (docs/SPEC.md §6.2) and uses the reading face they chose.
 */
private fun chromeTextStyle(
    fontSize: Int,
    lineHeight: Int,
    weight: FontWeight = FontWeight.Normal,
    letterSpacing: Double = 0.0,
) = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = weight,
    fontSize = fontSize.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
)

val PrompterTypography = Typography(
    displayLarge = chromeTextStyle(fontSize = 57, lineHeight = 64, letterSpacing = -0.25),
    displayMedium = chromeTextStyle(fontSize = 45, lineHeight = 52),
    displaySmall = chromeTextStyle(fontSize = 36, lineHeight = 44),

    headlineLarge = chromeTextStyle(fontSize = 32, lineHeight = 40),
    headlineMedium = chromeTextStyle(fontSize = 28, lineHeight = 36),
    headlineSmall = chromeTextStyle(fontSize = 24, lineHeight = 32),

    titleLarge = chromeTextStyle(fontSize = 22, lineHeight = 28),
    titleMedium = chromeTextStyle(fontSize = 16, lineHeight = 24, weight = FontWeight.Medium, letterSpacing = 0.15),
    titleSmall = chromeTextStyle(fontSize = 14, lineHeight = 20, weight = FontWeight.Medium, letterSpacing = 0.1),

    bodyLarge = chromeTextStyle(fontSize = 16, lineHeight = 24, letterSpacing = 0.5),
    bodyMedium = chromeTextStyle(fontSize = 14, lineHeight = 20, letterSpacing = 0.25),
    bodySmall = chromeTextStyle(fontSize = 12, lineHeight = 16, letterSpacing = 0.4),

    labelLarge = chromeTextStyle(fontSize = 14, lineHeight = 20, weight = FontWeight.Medium, letterSpacing = 0.1),
    labelMedium = chromeTextStyle(fontSize = 12, lineHeight = 16, weight = FontWeight.Medium, letterSpacing = 0.5),
    labelSmall = chromeTextStyle(fontSize = 11, lineHeight = 16, weight = FontWeight.Medium, letterSpacing = 0.5),
)
