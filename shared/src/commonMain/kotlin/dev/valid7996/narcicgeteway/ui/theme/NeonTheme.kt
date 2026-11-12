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
fun NeonAmbientBackground(
    modifier: Modifier = Modifier,
    personalization: Personalization = Personalization.Default,
) {
    val glowColor = personalization.ambientGlowColor ?: personalization.connectColor ?: NeonTheme.PrimaryPink
    val secondaryGlow = personalization.serverBorderColor ?: personalization.cardWaterColor ?: glowColor.copy(alpha = 0.6f)
    val bgTop = personalization.bgGradientTop ?: Color(0xFF080510)
    val bgBottom = personalization.bgGradientBottom ?: Color(0xFF050507)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        bgTop,
                        bgTop.copy(alpha = 0.85f),
                        bgBottom,
                    )
                )
            )
    ) {
        // Top glowing ambient halo fully synchronized with theme (static 0% CPU cost)
        Box(
            modifier = Modifier
                .size(360.dp)
                .offset(x = 10.dp, y = (-80).dp)
                .graphicsLayer {
                    alpha = 0.28f
                }
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = 0.45f),
                            secondaryGlow.copy(alpha = 0.22f),
                            Color.Transparent,
                        ),
                    ),
                    shape = CircleShape,
                )
        )

        // Bottom right subtle ambient glow
        Box(
            modifier = Modifier
                .size(320.dp)
                .offset(x = 140.dp, y = 480.dp)
                .graphicsLayer { alpha = 0.18f }
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            secondaryGlow.copy(alpha = 0.35f),
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


/**
 * Animated subtle fluid/water wave gently undulating inside cards and tiles.
 */
@Composable
fun NeonFluidWaterWave(
    color: Color,
    modifier: Modifier = Modifier,
    alpha: Float = 0.28f,
    speedMillis: Int = 3600,
) {
    val transition = rememberInfiniteTransition(label = "water-wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * kotlin.math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = speedMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wave-phase",
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val baseLine = h * 0.45f
        val amp = h * 0.22f
        val y0 = baseLine + kotlin.math.sin(phase) * amp
        val y1 = baseLine + kotlin.math.sin(phase + 1.8f) * (amp * 1.05f)
        val y2 = baseLine + kotlin.math.sin(phase + 3.4f) * (amp * 0.95f)
        val y3 = baseLine + kotlin.math.sin(phase + 5.0f) * amp

        val path = Path().apply {
            moveTo(0f, h)
            lineTo(0f, y0)
            cubicTo(w * 0.25f, y0, w * 0.25f, y1, w * 0.5f, y1)
            cubicTo(w * 0.75f, y1, w * 0.75f, y2, w, y3)
            lineTo(w, h)
            close()
        }

        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                listOf(color.copy(alpha = alpha), color.copy(alpha = alpha * 0.20f)),
                startY = baseLine - amp,
                endY = h,
            ),
            style = Fill,
        )
    }
}

data class ColorSwatch(
    val name: String,
    val color: Color,
    val argb: Long,
    val category: String,
)

val ColorPalette32: List<ColorSwatch> = listOf(
    // صورتی و سرخابی
    ColorSwatch("صورتی نئون", Color(0xFFF72585), 0xFFF72585L, "pink"),
    ColorSwatch("سرخابی پررنگ", Color(0xFFFF007F), 0xFFFF007FL, "pink"),
    ColorSwatch("ماژنتا الکتریک", Color(0xFFD946EF), 0xFFD946EFL, "pink"),
    ColorSwatch("ارکیده درخشان", Color(0xFFC026D3), 0xFFC026D3L, "pink"),
    ColorSwatch("رز سایبر", Color(0xFFFB7185), 0xFFFB7185L, "pink"),
    ColorSwatch("یاقوت سرخ", Color(0xFFE11D48), 0xFFE11D48L, "pink"),

    // بنفش و نیلی
    ColorSwatch("بنفش نئونی", Color(0xFF7C3AED), 0xFF7C3AEDL, "purple"),
    ColorSwatch("ویولت الکتریک", Color(0xFF8B5CF6), 0xFF8B5CF6L, "purple"),
    ColorSwatch("اسطوخودوس", Color(0xFFA78BFA), 0xFFA78BFAL, "purple"),
    ColorSwatch("نیلی کیهانی", Color(0xFF6366F1), 0xFF6366F1L, "purple"),
    ColorSwatch("آبی درباری", Color(0xFF4F46E5), 0xFF4F46E5L, "purple"),
    ColorSwatch("آبی عمیق", Color(0xFF1E3A8A), 0xFF1E3A8AL, "purple"),

    // فیروزه‌ای و آبی
    ColorSwatch("فیروزه‌ای نئون", Color(0xFF06B6D4), 0xFF06B6D4L, "cyan"),
    ColorSwatch("آبی آسمانی", Color(0xFF0EA5E9), 0xFF0EA5E9L, "cyan"),
    ColorSwatch("آکوا الکتریک", Color(0xFF22D3EE), 0xFF22D3EEL, "cyan"),
    ColorSwatch("آبی دریا", Color(0xFF0284C7), 0xFF0284C7L, "cyan"),
    ColorSwatch("آبی نئون", Color(0xFF2563EB), 0xFF2563EBL, "cyan"),
    ColorSwatch("آکوامارین", Color(0xFF14B8A6), 0xFF14B8A6L, "cyan"),

    // سبز و فسفری
    ColorSwatch("زمرد نئون", Color(0xFF10B981), 0xFF10B981L, "green"),
    ColorSwatch("سبز فسفری", Color(0xFF22C55E), 0xFF22C55EL, "green"),
    ColorSwatch("نعنایی درخشان", Color(0xFF34D399), 0xFF34D399L, "green"),
    ColorSwatch("لیمویی اسیدی", Color(0xFF84CC16), 0xFF84CC16L, "green"),
    ColorSwatch("سبز زیتونی", Color(0xFFA3E635), 0xFFA3E635L, "green"),
    ColorSwatch("جنگلی تیره", Color(0xFF059669), 0xFF059669L, "green"),

    // طلایی و گرم
    ColorSwatch("زرد نئونی", Color(0xFFFACC15), 0xFFFACC15L, "warm"),
    ColorSwatch("طلایی کهربایی", Color(0xFFF59E0B), 0xFFF59E0BL, "warm"),
    ColorSwatch("نارنجی خورشیدی", Color(0xFFFB923C), 0xFFFB923CL, "warm"),
    ColorSwatch("آتشین سوزان", Color(0xFFF97316), 0xFFF97316L, "warm"),
    ColorSwatch("قرمز پرحرارت", Color(0xFFEF4444), 0xFFEF4444L, "warm"),
    ColorSwatch("مرجانی نئون", Color(0xFFFF5252), 0xFFFF5252L, "warm"),

    // مونوکروم و خنثی
    ColorSwatch("سفید یخی", Color(0xFFFFFFFF), 0xFFFFFFFFL, "mono"),
    ColorSwatch("نقره‌ای پلاتین", Color(0xFFCBD5E1), 0xFFCBD5E1L, "mono"),
)

data class MasterThemeSpec(
    val id: String,
    val title: String,
    val subtitle: String,
    val primaryColor: Color,
    val buttonColor: Color,
    val serverBorderColor: Color,
    val cardWaterColor: Color,
    val cardBgColor: Color,
    val dashboardBorderColor: Color,
    val dashboardBgColor: Color,
    val textColor: Color,
    val ambientGlowColor: Color = primaryColor,
    val bgGradientTop: Color = Color(0xFF080510),
    val bgGradientBottom: Color = Color(0xFF050507),
    val navActiveColor: Color = primaryColor,
    val navBgColor: Color = cardBgColor,
    val heroCharacter: String = "noir",
    val previewGradients: List<Color>,
)

val MasterNeonThemes: List<MasterThemeSpec> = listOf(
    MasterThemeSpec(
        id = "cyberpunk_pink",
        title = "نئون نرسیس",
        subtitle = "صورتی پرحرارت و سرخابی درخشان",
        primaryColor = Color(0xFFF72585),
        buttonColor = Color(0xFFF72585),
        serverBorderColor = Color(0xFFF72585),
        cardWaterColor = Color(0xFF7C3AED),
        cardBgColor = Color(0xFF12081C),
        dashboardBorderColor = Color(0xFFF72585),
        dashboardBgColor = Color(0xE6140A20),
        textColor = Color.White,
        ambientGlowColor = Color(0xFFF72585),
        bgGradientTop = Color(0xFF0E071A),
        bgGradientBottom = Color(0xFF050308),
        navActiveColor = Color(0xFFF72585),
        navBgColor = Color(0xF212081C),
        heroCharacter = "blossom",
        previewGradients = listOf(Color(0xFFF72585), Color(0xFF7C3AED)),
    ),
    MasterThemeSpec(
        id = "obsidian_silver",
        title = "ابسیدین نقره‌ای",
        subtitle = "مونوکروم لوکس، پلاتین و مشکی خالص بدون رنگ صورتی",
        primaryColor = Color(0xFFE2E8F0),
        buttonColor = Color(0xFFE2E8F0),
        serverBorderColor = Color(0xFF94A3B8),
        cardWaterColor = Color(0xFF64748B),
        cardBgColor = Color(0xFF111115),
        dashboardBorderColor = Color(0xFFCBD5E1),
        dashboardBgColor = Color(0xE616161A),
        textColor = Color.White,
        ambientGlowColor = Color(0xFF94A3B8),
        bgGradientTop = Color(0xFF141418),
        bgGradientBottom = Color(0xFF050506),
        navActiveColor = Color(0xFFE2E8F0),
        navBgColor = Color(0xF2111115),
        heroCharacter = "noir",
        previewGradients = listOf(Color(0xFFFFFFFF), Color(0xFF64748B)),
    ),
    MasterThemeSpec(
        id = "matrix_emerald",
        title = "زمرد مات",
        subtitle = "سبز فسفری و زمرد نئونی مات",
        primaryColor = Color(0xFF10B981),
        buttonColor = Color(0xFF10B981),
        serverBorderColor = Color(0xFF10B981),
        cardWaterColor = Color(0xFF059669),
        cardBgColor = Color(0xFF071911),
        dashboardBorderColor = Color(0xFF10B981),
        dashboardBgColor = Color(0xE60A2218),
        textColor = Color(0xFFE6FFFA),
        ambientGlowColor = Color(0xFF10B981),
        bgGradientTop = Color(0xFF061A12),
        bgGradientBottom = Color(0xFF020A06),
        navActiveColor = Color(0xFF10B981),
        navBgColor = Color(0xF2071911),
        heroCharacter = "noir",
        previewGradients = listOf(Color(0xFF10B981), Color(0xFF059669)),
    ),
    MasterThemeSpec(
        id = "ice_ocean",
        title = "سایان اکتیو",
        subtitle = "فیروزه‌ای درخشان و آبی آسمان",
        primaryColor = Color(0xFF06B6D4),
        buttonColor = Color(0xFF06B6D4),
        serverBorderColor = Color(0xFF06B6D4),
        cardWaterColor = Color(0xFF0284C7),
        cardBgColor = Color(0xFF061522),
        dashboardBorderColor = Color(0xFF06B6D4),
        dashboardBgColor = Color(0xE60A1E30),
        textColor = Color(0xFFE0F2FE),
        ambientGlowColor = Color(0xFF06B6D4),
        bgGradientTop = Color(0xFF051726),
        bgGradientBottom = Color(0xFF020910),
        navActiveColor = Color(0xFF06B6D4),
        navBgColor = Color(0xF2061522),
        heroCharacter = "noir",
        previewGradients = listOf(Color(0xFF06B6D4), Color(0xFF0284C7)),
    ),
    MasterThemeSpec(
        id = "sunset_amber",
        title = "طلایی کهربا",
        subtitle = "طلایی آتشی و نارنجی خورشیدی",
        primaryColor = Color(0xFFF59E0B),
        buttonColor = Color(0xFFF59E0B),
        serverBorderColor = Color(0xFFF59E0B),
        cardWaterColor = Color(0xFFEA580C),
        cardBgColor = Color(0xFF1E0E05),
        dashboardBorderColor = Color(0xFFF59E0B),
        dashboardBgColor = Color(0xE6261308),
        textColor = Color(0xFFFEF3C7),
        ambientGlowColor = Color(0xFFF59E0B),
        bgGradientTop = Color(0xFF221006),
        bgGradientBottom = Color(0xFF0B0502),
        navActiveColor = Color(0xFFF59E0B),
        navBgColor = Color(0xF21E0E05),
        heroCharacter = "blossom",
        previewGradients = listOf(Color(0xFFF59E0B), Color(0xFFEA580C)),
    ),
    MasterThemeSpec(
        id = "violet_cosmos",
        title = "کیهان ویولت",
        subtitle = "بنفش الکتریک و اسطوخودوس عمیق",
        primaryColor = Color(0xFF8B5CF6),
        buttonColor = Color(0xFF8B5CF6),
        serverBorderColor = Color(0xFF8B5CF6),
        cardWaterColor = Color(0xFF4F46E5),
        cardBgColor = Color(0xFF0E0B20),
        dashboardBorderColor = Color(0xFF8B5CF6),
        dashboardBgColor = Color(0xE6130E2E),
        textColor = Color(0xFFF5F3FF),
        ambientGlowColor = Color(0xFF8B5CF6),
        bgGradientTop = Color(0xFF110C29),
        bgGradientBottom = Color(0xFF05030C),
        navActiveColor = Color(0xFF8B5CF6),
        navBgColor = Color(0xF20E0B20),
        heroCharacter = "noir",
        previewGradients = listOf(Color(0xFF8B5CF6), Color(0xFF4F46E5)),
    ),
)
