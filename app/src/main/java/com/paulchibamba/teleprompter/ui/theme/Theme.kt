package com.paulchibamba.teleprompter.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = AmberLight,
    onPrimary = OnAmberLight,
    primaryContainer = AmberContainerLight,
    onPrimaryContainer = OnAmberContainerLight,
    secondary = WarmNeutralLight,
    onSecondary = OnWarmNeutralLight,
    secondaryContainer = WarmNeutralContainerLight,
    onSecondaryContainer = OnWarmNeutralContainerLight,
    tertiary = SageLight,
    onTertiary = OnSageLight,
    tertiaryContainer = SageContainerLight,
    onTertiaryContainer = OnSageContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = SurfaceLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
)

private val DarkColors = darkColorScheme(
    primary = AmberDark,
    onPrimary = OnAmberDark,
    primaryContainer = AmberContainerDark,
    onPrimaryContainer = OnAmberContainerDark,
    secondary = WarmNeutralDark,
    onSecondary = OnWarmNeutralDark,
    secondaryContainer = WarmNeutralContainerDark,
    onSecondaryContainer = OnWarmNeutralContainerDark,
    tertiary = SageDark,
    onTertiary = OnSageDark,
    tertiaryContainer = SageContainerDark,
    onTertiaryContainer = OnSageContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = SurfaceDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
)

/**
 * The theme for the app's chrome — library, editor, settings, and the prompter's control surfaces.
 *
 * The prompter's *text* deliberately sits outside this: it is drawn with the colours the user chose
 * in [com.paulchibamba.teleprompter.domain.model.TypographySettings]. Wallpaper-derived colour would
 * be actively harmful there, because contrast through beam-splitter glass is a legibility
 * requirement rather than a matter of taste.
 */
@Composable
fun PrompterTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    useDynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = resolveColorScheme(useDarkTheme, useDynamicColor),
        typography = PrompterTypography,
        content = content,
    )
}

@Composable
private fun resolveColorScheme(useDarkTheme: Boolean, useDynamicColor: Boolean): ColorScheme {
    if (useDynamicColor && supportsDynamicColor()) {
        val context = LocalContext.current
        return if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    return if (useDarkTheme) DarkColors else LightColors
}

private fun supportsDynamicColor(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
