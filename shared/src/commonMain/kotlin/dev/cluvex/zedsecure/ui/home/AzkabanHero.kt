package dev.cluvex.zedsecure.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import dev.cluvex.zedsecure.domain.model.ConnectionState
import dev.cluvex.zedsecure.shared.resources.Res
import dev.cluvex.zedsecure.shared.resources.circle_waves_sheet
import dev.cluvex.zedsecure.shared.resources.dementor_isolated
import dev.cluvex.zedsecure.shared.resources.ic_bolt
import dev.cluvex.zedsecure.shared.resources.ic_close
import dev.cluvex.zedsecure.ui.components.CyberRadarIndicator
import org.jetbrains.compose.resources.painterResource

/**
 * The Azkaban Hero: Cinema-grade winter mist atmosphere with floating Dementor,
 * icy aura pulse, fortress protective wards, and frost-glass connect trigger.
 */
@Composable
fun AzkabanHero(
    state: ConnectionState,
    onTapHero: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val isConnected = state.isActive
    val isConnecting = state.isTransitioning

    val auraColor = when {
        state == ConnectionState.Error -> Color(0xFFEF4444)
        isConnected -> Color(0xFF38BDF8)
        isConnecting -> Color(0xFF7DD3FC)
        else -> Color(0xFF94A3B8)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF040A16),
                        Color(0xFF071224),
                        Color(0xFF0A1830),
                    ),
                ),
            )
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFBAE6FD).copy(alpha = 0.35f),
                        Color(0xFF38BDF8).copy(alpha = 0.12f),
                        Color.Transparent,
                    ),
                ),
                RoundedCornerShape(28.dp),
            )
            .then(
                if (onTapHero != null) {
                    Modifier.clickable(onClick = onTapHero)
                } else Modifier
            )
            .padding(top = 14.dp, bottom = 14.dp, start = 14.dp, end = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Header: Fortress Ward Status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF0C1D38).copy(alpha = 0.85f))
                    .border(1.dp, auraColor.copy(alpha = 0.40f), RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 5.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isConnected) Color(0xFF38BDF8) else Color(0xFF94A3B8)),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = when {
                        state == ConnectionState.Error -> "اختلال در طلسم محافظ"
                        isConnected -> "سپر دفاعی آزکابان فعال است"
                        isConnecting -> "در حال فراخوانی طلسم محافظ..."
                        else -> "دژ آزکابان آماده محافظت"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isConnected) Color(0xFFE0F2FE) else Color(0xFF94A3B8),
                )
            }

            // Circle Waves Animated Orb (clean, borderless, reactive)
            CircleWavesOrb(
                state = state,
                size = 145.dp,
            )
        }
    }
}

/**
 * Animated Circle Waves Element:
 * Renders the 25-frame smooth rotating wave/orbit animation from the sprite sheet.
 * Fluidly reacts to connection state with glowing cyan pulse and breathing effect.
 */
@Composable
fun CircleWavesOrb(
    state: ConnectionState,
    size: Dp = 145.dp,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "circle-waves")
    val isConnected = state.isActive
    val isConnecting = state.isTransitioning

    val animDuration = when {
        isConnected -> 1250
        isConnecting -> 800
        else -> 2200
    }

    val frameProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = animDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wave-frame",
    )

    val breatheScale by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isConnected) 1800 else 3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "orb-breathe",
    )

    val auraPulse by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "aura-pulse",
    )

    val auraColor = when {
        state == ConnectionState.Error -> Color(0xFFEF4444)
        isConnected -> Color(0xFF38BDF8)
        isConnecting -> Color(0xFF7DD3FC)
        else -> Color(0xFF0284C7)
    }

    val painter = painterResource(Res.drawable.circle_waves_sheet)
    val currentFrame = frameProgress.toInt().coerceIn(0, 24)

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = breatheScale
                scaleY = breatheScale
            },
        contentAlignment = Alignment.Center,
    ) {
        // Soft ambient back glow (completely borderless)
        Box(
            modifier = Modifier
                .size(size * 0.88f)
                .background(
                    Brush.radialGradient(
                        listOf(
                            auraColor.copy(alpha = if (isConnected) 0.50f * auraPulse else 0.20f),
                            Color(0xFF0369A1).copy(alpha = if (isConnected) 0.25f else 0.08f),
                            Color.Transparent,
                        ),
                    ),
                    shape = CircleShape,
                ),
        )

        // Circle Waves frame renderer
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cols = 5
            val frameX = currentFrame % cols
            val frameY = currentFrame / cols

            clipRect(0f, 0f, this.size.width, this.size.height) {
                withTransform({
                    translate(
                        left = -frameX * this@Canvas.size.width,
                        top = -frameY * this@Canvas.size.height,
                    )
                    scale(scaleX = 5f, scaleY = 5f, pivot = Offset.Zero)
                }) {
                    with(painter) {
                        draw(this@Canvas.size)
                    }
                }
            }
        }
    }
}

@Composable
fun AzkabanConnectButton(
    state: ConnectionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val isConnected = state.isActive
    val isConnecting = state.isTransitioning

    val buttonScale = if (isPressed) 0.95f else 1f

    val bgGradient = when {
        state == ConnectionState.Error -> Brush.horizontalGradient(
            listOf(Color(0xFF991B1B), Color(0xFFEF4444)),
        )
        isConnected -> Brush.horizontalGradient(
            listOf(Color(0xFF0284C7), Color(0xFF38BDF8)),
        )
        isConnecting -> Brush.horizontalGradient(
            listOf(Color(0xFF0369A1), Color(0xFF0284C7)),
        )
        else -> Brush.horizontalGradient(
            listOf(Color(0xFF0E223D), Color(0xFF16345C)),
        )
    }

    val borderColor = when {
        isConnected -> Color(0xFFBAE6FD).copy(alpha = 0.75f)
        isConnecting -> Color(0xFF7DD3FC).copy(alpha = 0.60f)
        else -> Color(0xFF64748B).copy(alpha = 0.40f)
    }

    Surface(
        onClick = onClick,
        enabled = state != ConnectionState.Disconnecting,
        shape = RoundedCornerShape(26.dp),
        color = Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .scale(buttonScale)
            .clip(RoundedCornerShape(26.dp))
            .background(bgGradient)
            .border(1.5.dp, borderColor, RoundedCornerShape(26.dp)),
        interactionSource = interaction,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isConnecting) {
                CyberRadarIndicator(Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "در حال ایجاد پیوند امن...",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            } else {
                Icon(
                    painter = painterResource(if (isConnected) Res.drawable.ic_close else Res.drawable.ic_bolt),
                    contentDescription = null,
                    tint = if (isConnected) Color(0xFFF0F9FF) else Color(0xFF7DD3FC),
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = when {
                        state == ConnectionState.Error -> "تلاش مجدد"
                        isConnected -> "گسستن طلسم (قطع اتصال)"
                        else -> "احضار محافظ (اتصال امن)"
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isConnected) Color.White else Color(0xFFE0F2FE),
                )
            }
        }
    }
}
