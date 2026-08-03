package com.paulchibamba.teleprompter.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Chrome colours only. The prompter surface never reads these — its text and background come from
 * [com.paulchibamba.teleprompter.domain.model.TypographySettings], because the user picks those for
 * legibility through beam-splitter glass rather than for the app's taste.
 *
 * The palette is warm amber on a warm neutral: it reads as "lit" rather than clinical, and it stays
 * out of the way of the pure white-on-black the prompter itself defaults to.
 */

internal val AmberLight = Color(0xFF8A5100)
internal val OnAmberLight = Color(0xFFFFFFFF)
internal val AmberContainerLight = Color(0xFFFFDCC0)
internal val OnAmberContainerLight = Color(0xFF2C1600)

internal val WarmNeutralLight = Color(0xFF725A42)
internal val OnWarmNeutralLight = Color(0xFFFFFFFF)
internal val WarmNeutralContainerLight = Color(0xFFFDDDBF)
internal val OnWarmNeutralContainerLight = Color(0xFF291806)

internal val SageLight = Color(0xFF576237)
internal val OnSageLight = Color(0xFFFFFFFF)
internal val SageContainerLight = Color(0xFFDBE7AE)
internal val OnSageContainerLight = Color(0xFF151E00)

internal val ErrorLight = Color(0xFFBA1A1A)
internal val OnErrorLight = Color(0xFFFFFFFF)
internal val ErrorContainerLight = Color(0xFFFFDAD6)
internal val OnErrorContainerLight = Color(0xFF410002)

internal val SurfaceLight = Color(0xFFFFF8F4)
internal val OnSurfaceLight = Color(0xFF201B16)
internal val SurfaceVariantLight = Color(0xFFF2DFD1)
internal val OnSurfaceVariantLight = Color(0xFF51443A)
internal val OutlineLight = Color(0xFF837468)
internal val OutlineVariantLight = Color(0xFFD5C3B5)

internal val AmberDark = Color(0xFFFFB870)
internal val OnAmberDark = Color(0xFF4A2800)
internal val AmberContainerDark = Color(0xFF693C00)
internal val OnAmberContainerDark = Color(0xFFFFDCC0)

internal val WarmNeutralDark = Color(0xFFE0C1A4)
internal val OnWarmNeutralDark = Color(0xFF412D18)
internal val WarmNeutralContainerDark = Color(0xFF5A432C)
internal val OnWarmNeutralContainerDark = Color(0xFFFDDDBF)

internal val SageDark = Color(0xFFBFCB94)
internal val OnSageDark = Color(0xFF2A330B)
internal val SageContainerDark = Color(0xFF404A20)
internal val OnSageContainerDark = Color(0xFFDBE7AE)

internal val ErrorDark = Color(0xFFFFB4AB)
internal val OnErrorDark = Color(0xFF690005)
internal val ErrorContainerDark = Color(0xFF93000A)
internal val OnErrorContainerDark = Color(0xFFFFDAD6)

internal val SurfaceDark = Color(0xFF17130E)
internal val OnSurfaceDark = Color(0xFFECE0D4)
internal val SurfaceVariantDark = Color(0xFF51443A)
internal val OnSurfaceVariantDark = Color(0xFFD5C3B5)
internal val OutlineDark = Color(0xFF9D8E81)
internal val OutlineVariantDark = Color(0xFF51443A)
