package dev.cluvex.zedsecure.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.cluvex.zedsecure.domain.model.ConnectionState
import dev.cluvex.zedsecure.shared.resources.Res
import dev.cluvex.zedsecure.shared.resources.circle_waves_sheet
import dev.cluvex.zedsecure.shared.resources.ic_bolt
import dev.cluvex.zedsecure.shared.resources.ic_close
import dev.cluvex.zedsecure.ui.components.CyberRadarIndicator
import org.jetbrains.compose.resources.painterResource

/**
 * The Azkaban Hero Element:
 * Completely borderless, free-floating dynamic elemental orb rendering the
 * Circle Waves animation in pristine high definition with state-reactive aura.
 */
@Composable
fun AzkabanHero(
    state: ConnectionState,
    size: Dp = 270.dp,
    onTapHero: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    CircleWavesOrb(
        state = state,
        size = size,
        onTap = onTapHero,
        modifier = modifier,
    )
}

/**
 * Animated Circle Waves Element:
 * Renders the 30-frame ultra-high-definition rotating wave/orbit animation from the vector sheet.
 * Fluidly reacts to connection state: stays static when disconnected, and comes alive only when connecting or connected.
 */
@Composable
fun CircleWavesOrb(
    state: ConnectionState,
    size: Dp = 270.dp,
    onTap: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val isConnected = state.isActive
    val isConnecting = state.isTransitioning
    val isMoving = isConnected || isConnecting

    val transition = rememberInfiniteTransition(label = "circle-waves")

    val frameProgress by if (isMoving) {
        val animDuration = if (isConnecting) 1500 else 3000
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 29.99f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = animDuration, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "wave-frame",
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    val breatheScale by if (isMoving) {
        transition.animateFloat(
            initialValue = 0.96f,
            targetValue = 1.04f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = if (isConnected) 2000 else 1400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "orb-breathe",
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    val auraPulse by if (isMoving) {
        transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 0.85f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "aura-pulse",
        )
    } else {
        remember { mutableFloatStateOf(0.20f) }
    }

    val auraColor = when {
        state == ConnectionState.Error -> Color(0xFFEF4444)
        isConnected -> Color(0xFF38BDF8)
        isConnecting -> Color(0xFF7DD3FC)
        else -> Color(0xFF94A3B8)
    }

    val painter = painterResource(Res.drawable.circle_waves_sheet)
    val currentFrame = if (isMoving) frameProgress.toInt().coerceIn(0, 29) else 0

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = breatheScale
                scaleY = breatheScale
            }
            .then(
                if (onTap != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onTap,
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Soft ambient back glow (completely borderless, free floating)
        Box(
            modifier = Modifier
                .size(size * 0.82f)
                .background(
                    Brush.radialGradient(
                        listOf(
                            auraColor.copy(alpha = if (isMoving) 0.26f * auraPulse else 0.08f),
                            Color(0xFF0369A1).copy(alpha = if (isMoving) 0.08f else 0.02f),
                            Color.Transparent,
                        ),
                    ),
                    shape = CircleShape,
                ),
        )

        // Circle Waves frame renderer (30 frames: 6 cols x 5 rows at 512x512 per frame)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cols = 6
            val rows = 5
            val frameX = currentFrame % cols
            val frameY = currentFrame / cols

            clipRect(0f, 0f, this.size.width, this.size.height) {
                withTransform({
                    translate(
                        left = -frameX * this@Canvas.size.width,
                        top = -frameY * this@Canvas.size.height,
                    )
                }) {
                    with(painter) {
                        draw(
                            size = Size(this@Canvas.size.width * cols, this@Canvas.size.height * rows),
                            colorFilter = ColorFilter.tint(auraColor, BlendMode.SrcIn),
                        )
                    }
                }
            }
        }

        // Center state icon: No spinning radar indicator! Clean and pure.
        AnimatedContent(
            targetState = state,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
            label = "orb-center-icon",
        ) { s ->
            when {
                s == ConnectionState.Error -> {
                    Icon(
                        painter = painterResource(Res.drawable.ic_close),
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(size * 0.22f),
                    )
                }
                s.isActive -> {
                    Icon(
                        painter = painterResource(Res.drawable.ic_bolt),
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(size * 0.22f),
                    )
                }
                else -> {
                    // Static / connecting: pure clean element without any spinning overlay icons
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
