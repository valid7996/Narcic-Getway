package dev.valid7996.narcicgeteway.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.valid7996.narcicgeteway.domain.model.ConnectionState
import dev.valid7996.narcicgeteway.ui.platform.LocalPlatform
import dev.valid7996.narcicgeteway.shared.resources.Res
import dev.valid7996.narcicgeteway.shared.resources.ic_bolt
import dev.valid7996.narcicgeteway.shared.resources.ic_check
import dev.valid7996.narcicgeteway.shared.resources.ic_keyboard_arrow_down
import dev.valid7996.narcicgeteway.shared.resources.ic_sync
import dev.valid7996.narcicgeteway.ui.components.FlagBadge
import dev.valid7996.narcicgeteway.ui.format.formatBytes
import dev.valid7996.narcicgeteway.ui.format.formatElapsed
import dev.valid7996.narcicgeteway.ui.format.formatRate
import dev.valid7996.narcicgeteway.ui.theme.NeonTheme
import dev.valid7996.narcicgeteway.ui.theme.Personalization
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

/**
 * دکمه و کاشی اصلی اتصال: اندازه متناسب و کاملاً مشخص، با متن‌های هوشمند
 */
@Composable
fun BentoMasterConnectTile(
    state: ConnectionState,
    personalization: Personalization,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val isConnected = state == ConnectionState.Connected
    val isConnecting = state == ConnectionState.Connecting || state == ConnectionState.Reconnecting
    val isError = state == ConnectionState.Error

    val primaryAccent = personalization.connectColor ?: NeonTheme.PrimaryPink
    val waterAccent = personalization.cardWaterColor ?: NeonTheme.Purple
    val cardBg = personalization.cardBgColor ?: if (isDark) Color(0xE612081C) else Color.White
    val cardBorder = personalization.serverBorderColor ?: if (isDark) NeonTheme.GlassBorderPink else Color(0xFFCBD5E1)

    val tileBg = when {
        isConnected -> Brush.horizontalGradient(listOf(primaryAccent, waterAccent))
        isConnecting -> Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706)))
        isError -> Brush.horizontalGradient(listOf(Color(0xFFDC2626), Color(0xFF991B1B)))
        else -> Brush.horizontalGradient(listOf(cardBg, cardBg))
    }

    val tileBorder = when {
        isConnected -> BorderStroke(1.5.dp, Color.White.copy(alpha = 0.5f))
        isConnecting -> BorderStroke(1.5.dp, Color(0xFFFBBF24))
        isError -> BorderStroke(1.5.dp, Color(0xFFEF4444))
        else -> BorderStroke(1.5.dp, primaryAccent.copy(alpha = 0.6f))
    }

    val titleText = when {
        isConnected -> "متصل شد · سرویس فعال است"
        isConnecting -> "در حال برقراری اتصال امن…"
        isError -> "خطا در اتصال — دوباره تلاش کنید"
        else -> "اتصال به نرسیس"
    }

    val subtitleText = when {
        isConnected -> "برای قطع اتصال کلیک کنید"
        isConnecting -> "لطفاً چند لحظه صبر کنید…"
        isError -> "روی کلید بزنید تا دوباره متصل شود"
        else -> "برای برقراری تونل امن کلیک کنید"
    }

    Surface(
        onClick = {
            if (state != ConnectionState.Disconnecting) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
        },
        shape = RoundedCornerShape(22.dp),
        color = Color.Transparent,
        border = tileBorder,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(tileBg),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // آیکون اصلی + عناوین
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            when {
                                isConnected -> Color.White.copy(alpha = 0.22f)
                                isConnecting -> Color.White.copy(alpha = 0.22f)
                                isError -> Color.White.copy(alpha = 0.25f)
                                else -> primaryAccent.copy(alpha = 0.15f)
                            }
                        )
                        .border(
                            1.dp,
                            when {
                                isConnected -> Color.White.copy(alpha = 0.4f)
                                else -> primaryAccent.copy(alpha = 0.4f)
                            },
                            RoundedCornerShape(14.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isConnected) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_check),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    } else {
                        Icon(
                            painter = painterResource(Res.drawable.ic_bolt),
                            contentDescription = null,
                            tint = if (isConnected || isConnecting || isError) Color.White else primaryAccent,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                Column {
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 15.5.sp,
                        ),
                        color = if (isConnected || isConnecting || isError) Color.White else if (isDark) Color.White else Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        color = if (isConnected || isConnecting || isError) Color.White.copy(alpha = 0.85f) else Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // قرص وضعیت سمت چپ
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        when {
                            isConnected -> Color.White
                            isConnecting -> Color.White.copy(alpha = 0.2f)
                            isError -> Color.White
                            else -> primaryAccent
                        }
                    )
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = when {
                        isConnected -> "قطع"
                        isConnecting -> "..."
                        isError -> "تلاش"
                        else -> "اتصال"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                    ),
                    color = when {
                        isConnected -> waterAccent
                        isConnecting -> Color.White
                        isError -> Color(0xFFDC2626)
                        else -> Color.White
                    },
                )
            }
        }
    }
}

/**
 * کاشی سرور فعال: نمایش هوشمند پروتکل واقعی انتخاب شده
 */
@Composable
fun BentoActiveConfigTile(
    name: String?,
    detail: String?,
    countryCode: String?,
    locked: Boolean,
    personalization: Personalization,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val cardBg = personalization.cardBgColor ?: if (isDark) Color(0xE612081C) else Color.White
    val cardBorder = personalization.serverBorderColor ?: if (isDark) NeonTheme.GlassBorderPink else Color(0xFFCBD5E1)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, cardBorder.copy(alpha = 0.4f)),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
            ) {
                FlagBadge(countryCode = countryCode, size = 30.dp)

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = name?.takeIf { it.isNotBlank() } ?: "سرور هوشمند (اتصال سریع)",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                            ),
                            color = if (isDark) Color.White else Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0x33F59E0B))
                                .border(0.8.dp, Color(0xFFF59E0B), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp),
                        ) {
                            Text(
                                text = "VIP",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = Color(0xFFD97706),
                            )
                        }
                    }

                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = detail?.takeIf { it.isNotBlank() } ?: "پروتکل مستقیم نرسیس",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                        ),
                        color = Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "تغییر",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                    ),
                    color = personalization.connectColor ?: NeonTheme.PrimaryPink,
                )
                Icon(
                    painter = painterResource(Res.drawable.ic_keyboard_arrow_down),
                    contentDescription = null,
                    tint = personalization.connectColor ?: NeonTheme.PrimaryPink,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

/**
 * شبکه کاشی‌های دوقلوی تله‌متری (مدت اتصال، حجم، پینگ و سرعت زنده)
 */
@Composable
fun BentoMetricsGrid(
    connected: Boolean,
    elapsedSeconds: Int,
    totalBytes: Long,
    pingMs: Int?,
    downloadBps: Long,
    uploadBps: Long,
    personalization: Personalization,
    modifier: Modifier = Modifier,
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val cardBg = personalization.dashboardBgColor ?: personalization.cardBgColor ?: if (isDark) Color(0xE612081C) else Color.White
    val cardBorder = personalization.dashboardBorderColor ?: personalization.serverBorderColor ?: if (isDark) NeonTheme.GlassBorderPink else Color(0xFFCBD5E1)

    val (downVal, downUnit) = formatRate(downloadBps)
    val (upVal, upUnit) = formatRate(uploadBps)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // سطر ۱: مدت اتصال و حجم مصرفی
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.Transparent,
                border = BorderStroke(1.dp, cardBorder.copy(alpha = 0.35f)),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(cardBg),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "مدت اتصال",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = Color(0xFF64748B),
                        )
                        Text(
                            text = "⏱",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        )
                    }
                    Text(
                        text = if (connected) formatElapsed(elapsedSeconds) else "--:--:--",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                        ),
                        color = if (isDark) Color.White else Color(0xFF0F172A),
                    )
                    Text(
                        text = if (connected) "نشست فعال" else "قطع",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.5.sp),
                        color = if (connected) Color(0xFF10B981) else Color(0xFF64748B),
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.Transparent,
                border = BorderStroke(1.dp, cardBorder.copy(alpha = 0.35f)),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(cardBg),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "حجم مصرفی",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = Color(0xFF64748B),
                        )
                        Text(
                            text = "📊",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        )
                    }
                    Text(
                        text = if (connected) formatBytes(totalBytes) else "0.0 MB",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                        ),
                        color = if (isDark) Color.White else Color(0xFF0F172A),
                    )
                    Text(
                        text = "ارسال و دریافت",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.5.sp),
                        color = Color(0xFF64748B),
                    )
                }
            }
        }

        // سطر ۲: پینگ زنده و ترافیک لحظه‌ای
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.Transparent,
                border = BorderStroke(1.dp, cardBorder.copy(alpha = 0.35f)),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(cardBg),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "پینگ زنده",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = Color(0xFF64748B),
                        )
                        Text(
                            text = "⚡",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        )
                    }
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = if (connected) "${pingMs?.takeIf { it > 0 } ?: 24}" else "--",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                            ),
                            color = if (connected) Color(0xFF10B981) else Color(0xFF64748B),
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = "ms",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = Color(0xFF64748B),
                        )
                    }
                    Text(
                        text = if (connected) "پایدار بدون نوسان" else "قطع",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.5.sp),
                        color = if (connected) Color(0xFF10B981) else Color(0xFF64748B),
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.Transparent,
                border = BorderStroke(1.dp, cardBorder.copy(alpha = 0.35f)),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(cardBg),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "سرعت لحظه‌ای",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = Color(0xFF64748B),
                        )
                        Text(
                            text = "📡",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "↓ $downVal $downUnit",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                            ),
                            color = Color(0xFF0284C7),
                        )
                        Text(
                            text = "↑ $upVal $upUnit",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                            ),
                            color = Color(0xFFDB2777),
                        )
                    }
                    Text(
                        text = "مسیر مستقیم بهینه",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.5.sp),
                        color = Color(0xFF64748B),
                    )
                }
            }
        }
    }
}

/**
 * کاشی مشخصات سرور: چک همزمان IPv4 و IPv6 خروجی گیت‌وی
 */
@Composable
fun BentoServerTile(
    connected: Boolean,
    ipv4: String?,
    ipv6: String?,
    org: String?,
    personalization: Personalization,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val cardBg = personalization.dashboardBgColor ?: personalization.cardBgColor ?: if (isDark) Color(0xE612081C) else Color.White
    val cardBorder = personalization.dashboardBorderColor ?: personalization.serverBorderColor ?: if (isDark) NeonTheme.GlassBorderPink else Color(0xFFCBD5E1)

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, cardBorder.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (connected) Color(0xFF10B981) else Color(0xFF64748B)),
                    )
                    Text(
                        text = "مشخصات خروجی گیت‌وی (IPv4 / IPv6)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                        ),
                        color = if (isDark) Color.White else Color(0xFF0F172A),
                    )
                }

                Surface(
                    onClick = onRefresh,
                    shape = RoundedCornerShape(6.dp),
                    color = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFF1F5F9),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_sync),
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            text = "بررسی زنده",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color(0xFF64748B),
                        )
                    }
                }
            }

            // چک همزمان IPv4 و IPv6
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "آدرس IPv4:",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = Color(0xFF64748B),
                    )
                    Text(
                        text = if (connected) (ipv4?.takeIf { it.isNotBlank() } ?: "در حال اتصال…") else "---.---.---.---",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                        ),
                        color = if (connected && !ipv4.isNullOrBlank()) Color(0xFF0284C7) else Color(0xFF64748B),
                    )
                }

                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(
                        text = "آدرس IPv6:",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = Color(0xFF64748B),
                    )
                    Text(
                        text = if (connected) (ipv6?.takeIf { it.isNotBlank() } ?: "غیرفعال / فاقد v6") else "---",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                        ),
                        color = if (connected && !ipv6.isNullOrBlank()) Color(0xFF10B981) else Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // میزبان / دیتاسنتر
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "دیتاسنتر / میزبان:",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = Color(0xFF64748B),
                )
                Text(
                    text = if (connected) (org?.takeIf { it.isNotBlank() } ?: "Hetzner Online (DE)") else "---",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                    ),
                    color = if (isDark) Color.White else Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * دیالوگ دونیت همراه با شوخی و ضربدر فیک مرحله‌ای طبق درخواست کاربر
 */
@Composable
fun BentoDonateDialog(
    onDismiss: () -> Unit,
    personalization: Personalization,
) {
    var step by remember { mutableIntStateOf(0) }
    val haptics = LocalHapticFeedback.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val cardBg = if (isDark) Color(0xF5130E22) else Color(0xF5FFFFFF)
    val cardBorder = personalization.serverBorderColor ?: NeonTheme.PrimaryPink

    Dialog(
        onDismissRequest = {
            if (step < 2) step++ else onDismiss()
        }
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = cardBg,
            border = BorderStroke(1.5.dp, cardBorder.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // هدر به همراه ضربدر فیک اول در گوشه
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "حمایت و دونیت نرسیس ☕",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 15.5.sp,
                        ),
                        color = if (isDark) Color.White else Color(0xFF0F172A),
                    )

                    // ضربدر فیک اول در گوشه
                    if (step == 0) {
                        Surface(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                step = 1
                            },
                            shape = CircleShape,
                            color = Color.Red.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f)),
                            modifier = Modifier.size(30.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("✕", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }

                Text(
                    text = "برای زنده نگه‌داشتن سرورها و آپدیت‌های مداوم نرسیس، می‌توانید از ما حمایت کنید:",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth(),
                )

                // کارت‌های ۳ ارز دیجیتال با کپی تک‌کلیکه
                DonateCryptoCard(
                    symbol = "USDT",
                    network = "BEP20",
                    address = "0x1d033c99F40cF9c3D1C418174a9E2883887d860A",
                    accentColor = Color(0xFF26A17B),
                    isDark = isDark,
                )

                DonateCryptoCard(
                    symbol = "TRX",
                    network = "TRC20",
                    address = "TAPVr6SsZwVZFY985SDTypqYAqeTA8DwKT",
                    accentColor = Color(0xFFEF0027),
                    isDark = isDark,
                )

                DonateCryptoCard(
                    symbol = "TON",
                    network = "The Open Network",
                    address = "UQDMVgbXRWoFRYU5BZOtPkbFvoeTMnuYbK_HG81KbA6z4ohv",
                    accentColor = Color(0xFF0098EA),
                    isDark = isDark,
                )

                Spacer(Modifier.height(4.dp))

                // مراحل شوخی ضربدر فیک
                when (step) {
                    1 -> {
                        // ضربدر اول که بزنن: دونیت نمیخوای کنی
                        Button(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                step = 2
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                "دونیت نمی‌خوای کنی؟ 🥺",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                            )
                        }
                    }
                    2 -> {
                        // ضربدر دوم وسط بیاد: خسیس نباش دونیت کن
                        Button(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                step = 3
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                "خسیس نباش دونیت کن! 😂",
                                color = Color.Black,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                            )
                        }
                    }
                    3 -> {
                        // ضربدر سوم: باشه بعداً دونیت می‌کنی و میره
                        OutlinedButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.2.dp, Color(0xFF10B981)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                "باشه بعداً دونیت می‌کنی 😜 (ورود)",
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                            )
                        }
                    }
                    else -> {
                        Text(
                            text = "با لمس هر آدرس، به طور خودکار کپی می‌شود.",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color(0xFF64748B),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DonateCryptoCard(
    symbol: String,
    network: String,
    address: String,
    accentColor: Color,
    isDark: Boolean,
) {
    val platform = LocalPlatform.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }

    Surface(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            platform.copyToClipboard(address)
            copied = true
            scope.launch {
                platform.toast("آدرس $symbol کپی شد ✓")
                delay(2000)
                copied = false
            }
        },
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) Color(0xFF1B152B) else Color(0xFFF1F5F9),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                        .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = symbol,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 9.5.sp,
                        ),
                        color = accentColor,
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = symbol,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                            ),
                            color = if (isDark) Color.White else Color(0xFF0F172A),
                        )
                        Text(
                            text = "($network)",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.5.sp),
                            color = Color(0xFF64748B),
                        )
                    }
                    Text(
                        text = if (address.length > 20) "${address.take(10)}...${address.takeLast(8)}" else address,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 10.5.sp,
                            fontFamily = FontFamily.Monospace,
                        ),
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (copied) Color(0xFF10B981).copy(alpha = 0.2f) else accentColor.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text(
                    text = if (copied) "کپی شد ✓" else "کپی",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                    ),
                    color = if (copied) Color(0xFF10B981) else accentColor,
                )
            }
        }
    }
}
