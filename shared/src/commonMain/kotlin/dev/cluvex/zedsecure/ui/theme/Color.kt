@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package dev.cluvex.zedsecure.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.ui.graphics.Color

// Aurora-Venom: a near-black base with an electric mint accent. The connection state recolors the
// whole app — mint at rest, sky while connecting, amber while connected — through ZedGradients and
// the accents the screens read.
private val AuroraMint = Color(0xFF3FE0A4)

val LightColors: ColorScheme = expressiveLightColorScheme().copy(
    primary = Color(0xFF0E7A54),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB8F0DC),
    onPrimaryContainer = Color(0xFF03271B),
    inversePrimary = Color(0xFF3FE0A4),

    secondary = Color(0xFF0F6E8C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC5ECFB),
    onSecondaryContainer = Color(0xFF06212E),

    tertiary = Color(0xFFB4551B),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDBCB),
    onTertiaryContainer = Color(0xFF361000),
)

// The accent family the screens read directly — remapped onto the aurora identity without
// renaming, so every existing usage picks the new values.
val ZedViolet = Color(0xFF3FE0A4)
val ZedCyan = Color(0xFF38BDF8)
val ZedLime = Color(0xFFA7F7D8)
val ZedHotPink = Color(0xFFFFB454)
val ZedDeepViolet = Color(0xFF04140E)

val DarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF3FE0A4),
    onPrimary = Color(0xFF04120C),
    primaryContainer = Color(0xFF0B3D2C),
    onPrimaryContainer = Color(0xFFB8F5DF),
    inversePrimary = Color(0xFF0E7A54),

    secondary = Color(0xFF38BDF8),
    onSecondary = Color(0xFF04141D),
    secondaryContainer = Color(0xFF0B3A4D),
    onSecondaryContainer = Color(0xFFBDE9FF),

    tertiary = Color(0xFFFFB454),
    onTertiary = Color(0xFF2A1200),
    tertiaryContainer = Color(0xFF5C2E00),
    onTertiaryContainer = Color(0xFFFFDCC2),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    background = Color(0xFF04070C),
    onBackground = Color(0xFFE2E8E4),
    surface = Color(0xFF04070C),
    onSurface = Color(0xFFE2E8E4),
    surfaceVariant = Color(0xFF394139),
    onSurfaceVariant = Color(0xFF93A39B),
    surfaceTint = Color(0xFF3FE0A4),

    inverseSurface = Color(0xFFE2E8E4),
    inverseOnSurface = Color(0xFF1A211D),

    outline = Color(0xFF7D8D85),
    outlineVariant = Color(0xFF394139),
    scrim = Color(0xFF000000),

    surfaceBright = Color(0xFF2C352F),
    surfaceDim = Color(0xFF04070C),
    surfaceContainerLowest = Color(0xFF02050A),
    surfaceContainerLow = Color(0xFF0A0F14),
    surfaceContainer = Color(0xFF0E141A),
    surfaceContainerHigh = Color(0xFF151B21),
    surfaceContainerHighest = Color(0xFF1D242B),
)
