package dev.valid7996.narcicgeteway.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object NeonTheme {
    val Base = Color(0xFF050507)
    val BackgroundDark = Color(0xFF07060B)
    val BackgroundDeep = Color(0xFF0C0717)

    // Neon Accents
    val PrimaryPink = Color(0xFFF72585)
    val SecondaryPink = Color(0xFFFF4FA3)
    val Magenta = Color(0xFFC026D3)
    val Purple = Color(0xFF7C3AED)
    val DarkPurple = Color(0xFF4C1D95)
    val ElectricViolet = Color(0xFF8B5CF6)

    // Functional Accents
    val SuccessGreen = Color(0xFF22C55E)
    val WarningAmber = Color(0xFFF59E0B)
    val ErrorRed = Color(0xFFEF4444)
    val InfoBlue = Color(0xFF3B82F6)
    val Cyan = Color(0xFF06B6D4)

    // Glass Surfaces
    val GlassSurface = Color(0xCC0E0C17)
    val GlassSurfaceDark = Color(0xEB0A0812)
    val GlassSurfaceLight = Color(0xB3181426)
    val GlassBorderPink = Color(0x40F72585)
    val GlassBorderPurple = Color(0x357C3AED)
    val GlassBorderMuted = Color(0x18FFFFFF)
    val GlassBorderWhite = Color(0x2AFFFFFF)

    // Gradients
    val PinkGradient = Brush.horizontalGradient(
        listOf(PrimaryPink, Magenta)
    )
    val PinkPurpleGradient = Brush.horizontalGradient(
        listOf(PrimaryPink, Purple)
    )
    val PurpleDarkGradient = Brush.verticalGradient(
        listOf(Purple.copy(alpha = 0.25f), DarkPurple.copy(alpha = 0.10f))
    )
    val CardBackgroundGradient = Brush.verticalGradient(
        listOf(Color(0xFF130E1F), Color(0xFF0A0812))
    )
    val ActiveCardGradient = Brush.verticalGradient(
        listOf(Color(0xFF1F0D24), Color(0xFF0E0817))
    )
}

/**
 * Ambient background with subtle deep purple gradient and a gentle glowing magenta nebula.
 */
@Composable
fun NeonAmbientBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "ambient-breath")
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breath-scale",
    )
    val breathAlpha by infiniteTransition.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breath-alpha",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF080510),
                        Color(0xFF0C0618),
                        Color(0xFF050507),
                    )
                )
            )
    ) {
        // Top glowing magenta/pink ambient halo
        Box(
            modifier = Modifier
                .size(360.dp)
                .offset(x = 10.dp, y = (-80).dp)
                .graphicsLayer {
                    scaleX = breathScale
                    scaleY = breathScale
                    alpha = breathAlpha
                }
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            NeonTheme.PrimaryPink.copy(alpha = 0.45f),
                            NeonTheme.Purple.copy(alpha = 0.25f),
                            Color.Transparent,
                        ),
                    ),
                    shape = CircleShape,
                )
        )

        // Bottom right subtle violet ambient glow
        Box(
            modifier = Modifier
                .size(320.dp)
                .offset(x = 140.dp, y = 480.dp)
                .graphicsLayer { alpha = 0.18f }
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            NeonTheme.Purple.copy(alpha = 0.4f),
                            Color.Transparent,
                        ),
                    ),
                    shape = CircleShape,
                )
        )
    }
}

/**
 * Bottom wavy accent shape rendered inside tiles and cards.
 */
@Composable
fun NeonWaveAccent(
    color: Color,
    modifier: Modifier = Modifier,
    alpha: Float = 0.25f,
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val path = Path().apply {
            moveTo(0f, height * 0.4f)
            cubicTo(
                width * 0.35f, height * 0.1f,
                width * 0.65f, height * 0.85f,
                width, height * 0.35f,
            )
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }

        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                listOf(color.copy(alpha = alpha), color.copy(alpha = alpha * 0.4f))
            ),
            style = Fill,
        )
    }
}

/**
 * Reusable Glassmorphic Card Container.
 */
@Composable
fun NeonGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
    borderColor: Color = NeonTheme.GlassBorderPink,
    backgroundColor: Color = NeonTheme.GlassSurface,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(1.2.dp, borderColor, shape),
    ) {
        content()
    }
}
