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
import dev.cluvex.zedsecure.domain.model.ConnectionState
import dev.cluvex.zedsecure.shared.resources.Res
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
    val transition = rememberInfiniteTransition(label = "azkaban-anim")

    // Gentle hover animation of the Spectre / Dementor
    val floatOffset by transition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dementor-float",
    )

    // Subtle scale breathing
    val breatheScale by transition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dementor-scale",
    )

    // Glowing aura pulse
    val auraPulse by transition.animateFloat(
        initialValue = 0.40f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "aura-pulse",
    )

    // Slow drifting winter mist rotation
    val mistRotate by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 32000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "mist-rotate",
    )

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

            // Dementor Figure Container with Ethereal Fog Aura
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .padding(4.dp),
                contentAlignment = Alignment.Center,
            ) {
                // Background Frost Aura Glow
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .graphicsLayer {
                            scaleX = breatheScale * (if (isConnected) 1.15f else 1f)
                            scaleY = breatheScale * (if (isConnected) 1.15f else 1f)
                            alpha = auraPulse * (if (isConnected) 0.65f else 0.35f)
                            rotationZ = mistRotate
                        }
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    auraColor.copy(alpha = 0.55f),
                                    Color(0xFF0284C7).copy(alpha = 0.25f),
                                    Color.Transparent,
                                ),
                            ),
                            shape = CircleShape,
                        ),
                )

                // Runic Frost Halo Ring
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .border(
                            width = if (isConnected) 2.dp else 1.dp,
                            brush = Brush.sweepGradient(
                                listOf(
                                    Color.Transparent,
                                    auraColor.copy(alpha = 0.65f),
                                    Color.Transparent,
                                    auraColor.copy(alpha = 0.45f),
                                    Color.Transparent,
                                ),
                            ),
                            shape = CircleShape,
                        )
                        .graphicsLayer {
                            rotationZ = -mistRotate * 0.75f
                        },
                )

                // Floating Dementor Image
                Image(
                    painter = painterResource(Res.drawable.dementor_isolated),
                    contentDescription = "Azkaban Dementor",
                    modifier = Modifier
                        .size(115.dp)
                        .offset(y = floatOffset.dp)
                        .scale(breatheScale),
                )
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
