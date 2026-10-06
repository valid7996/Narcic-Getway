@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package dev.cluvex.zedsecure.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.ui.graphics.Color

// Night-Ice palette, refreshed: deeper violet-tinted navy surfaces with a luminous ice accent.
private val IcePrimary = Color(0xFF2E5FE8)

val LightColors: ColorScheme = expressiveLightColorScheme().copy(
    primary = IcePrimary,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE4FF),
    onPrimaryContainer = Color(0xFF0E2A6B),
    inversePrimary = Color(0xFFAAC7FF),

    secondary = Color(0xFF006E78),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFB4EBEF),
    onSecondaryContainer = Color(0xFF002022),

    tertiary = Color(0xFFB4551B),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDBCB),
    onTertiaryContainer = Color(0xFF361000),

    background = Color(0xFFF3F6FD),
    onBackground = Color(0xFF16203B),
    surface = Color(0xFFF3F6FD),
    onSurface = Color(0xFF16203B),
    surfaceVariant = Color(0xFFDFE5F2),
    onSurfaceVariant = Color(0xFF5C6880),
    surfaceTint = IcePrimary,

    inverseSurface = Color(0xFF16203B),
    inverseOnSurface = Color(0xFFEDF1FA),

    outline = Color(0xFF7683A0),
    outlineVariant = Color(0xFFCBD5EA),
    scrim = Color(0xFF000000),

    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFE9EEF7),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F9FE),
    surfaceContainer = Color(0xFFEDF1FA),
    surfaceContainerHigh = Color(0xFFE2E9F6),
    surfaceContainerHighest = Color(0xFFD5DEF0),
)

val ZedLime = Color(0xFF7DE8FF)
val ZedOnLime = Color(0xFF06283C)
val ZedHotPink = Color(0xFFFF8A5C)
val ZedViolet = Color(0xFF5B7CFA)
val ZedDeepViolet = Color(0xFF0B1533)
val ZedCyan = Color(0xFF38D6E4)
val ZedMint = Color(0xFF5EEAD4)

val DarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF5B7CFA),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF1C2C63),
    onPrimaryContainer = Color(0xFFDCE4FF),
    inversePrimary = IcePrimary,

    secondary = Color(0xFF56D0DC),
    onSecondary = Color(0xFF00343A),
    secondaryContainer = Color(0xFF084B54),
    onSecondaryContainer = Color(0xFFB4EBEF),

    tertiary = Color(0xFFFFB68F),
    onTertiary = Color(0xFF5A2000),
    tertiaryContainer = Color(0xFF7E3A0C),
    onTertiaryContainer = Color(0xFFFFDBCB),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    background = Color(0xFF0A0F1A),
    onBackground = Color(0xFFE9EEF8),
    surface = Color(0xFF0A0F1A),
    onSurface = Color(0xFFE9EEF8),
    surfaceVariant = Color(0xFF263149),
    onSurfaceVariant = Color(0xFF94A0B8),
    surfaceTint = Color(0xFF5B7CFA),

    inverseSurface = Color(0xFFE9EEF8),
    inverseOnSurface = Color(0xFF1A2438),

    outline = Color(0xFF7E8AA3),
    outlineVariant = Color(0xFF263149),
    scrim = Color(0xFF000000),

    surfaceBright = Color(0xFF2A3448),
    surfaceDim = Color(0xFF0A0F1A),
    surfaceContainerLowest = Color(0xFF060B14),
    surfaceContainerLow = Color(0xFF0E1524),
    surfaceContainer = Color(0xFF121A2C),
    surfaceContainerHigh = Color(0xFF1A2438),
    surfaceContainerHighest = Color(0xFF232E47),
)
