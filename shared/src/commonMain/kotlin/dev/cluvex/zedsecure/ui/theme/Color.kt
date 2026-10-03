@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package dev.cluvex.zedsecure.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.ui.graphics.Color

private val Narcis = Color(0xFF0A7B58)

val LightColors: ColorScheme = expressiveLightColorScheme().copy(
    primary = Narcis,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC4F2DD),
    onPrimaryContainer = Color(0xFF00251A),
    inversePrimary = Color(0xFF6FDBA8),

    secondary = Color(0xFF9C7A0A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFEDC2),
    onSecondaryContainer = Color(0xFF2E2000),

    tertiary = Color(0xFF2D5DA9),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD6E2FF),
    onTertiaryContainer = Color(0xFF001945),
)

val ZedLime = Color(0xFFFFD952)
val ZedOnLime = Color(0xFF241A00)
val ZedHotPink = Color(0xFFFF9A4D)
val ZedViolet = Color(0xFF1FC984)
val ZedDeepViolet = Color(0xFF06382A)
val ZedCyan = Color(0xFF45D8E6)

val DarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF6FDBA8),
    onPrimary = Color(0xFF003822),
    primaryContainer = Color(0xFF05523A),
    onPrimaryContainer = Color(0xFFC4F2DD),
    inversePrimary = Narcis,

    secondary = Color(0xFFFFCE54),
    onSecondary = Color(0xFF3A2E00),
    secondaryContainer = Color(0xFF574300),
    onSecondaryContainer = Color(0xFFFFEDC2),

    tertiary = Color(0xFFA9C6F5),
    onTertiary = Color(0xFF0F3161),
    tertiaryContainer = Color(0xFF294779),
    onTertiaryContainer = Color(0xFFD6E2FF),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    background = Color(0xFF0F1412),
    onBackground = Color(0xFFDFE4E0),
    surface = Color(0xFF0F1412),
    onSurface = Color(0xFFDFE4E0),
    surfaceVariant = Color(0xFF3F4944),
    onSurfaceVariant = Color(0xFFBFC9C2),
    surfaceTint = Color(0xFF6FDBA8),

    inverseSurface = Color(0xFFDFE4E0),
    inverseOnSurface = Color(0xFF2C3531),

    outline = Color(0xFF89938C),
    outlineVariant = Color(0xFF3F4944),
    scrim = Color(0xFF000000),

    surfaceBright = Color(0xFF353F3A),
    surfaceDim = Color(0xFF0F1412),
    surfaceContainerLowest = Color(0xFF090D0B),
    surfaceContainerLow = Color(0xFF161C19),
    surfaceContainer = Color(0xFF1A201D),
    surfaceContainerHigh = Color(0xFF242B27),
    surfaceContainerHighest = Color(0xFF2F3733),
)
