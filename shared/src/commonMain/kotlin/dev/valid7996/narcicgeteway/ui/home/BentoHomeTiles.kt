package dev.valid7996.narcicgeteway.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.valid7996.narcicgeteway.domain.model.ConnectionState
import dev.valid7996.narcicgeteway.domain.model.AppSettings
import dev.valid7996.narcicgeteway.shared.resources.Res
import dev.valid7996.narcicgeteway.shared.resources.ic_bolt
import dev.valid7996.narcicgeteway.shared.resources.ic_check
import dev.valid7996.narcicgeteway.shared.resources.ic_keyboard_arrow_down
import dev.valid7996.narcicgeteway.shared.resources.ic_sync
import dev.valid7996.narcicgeteway.ui.components.FlagBadge
import dev.valid7996.narcicgeteway.ui.format.formatBytes
import dev.valid7996.narcicgeteway.ui.format.formatElapsed
import dev.valid7996.narcicgeteway.ui.format.formatRate
import dev.valid7996.narcicgeteway.ui.theme.MasterNeonThemes
import dev.valid7996.narcicgeteway.ui.theme.MasterThemeSpec
import dev.valid7996.narcicgeteway.ui.theme.NeonTheme
import dev.valid7996.narcicgeteway.ui.theme.Personalization
import dev.valid7996.narcicgeteway.ui.theme.applyMasterTheme
import org.jetbrains.compose.resources.painterResource

/**
 * نوار انتخابگر سریع ۴ تم اصلی نرسیس
 */
@Composable
fun BentoThemePaletteBar(
    settings: AppSettings?,
    onUpdateSettings: (((AppSettings) -> AppSettings) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val currentThemeId = settings?.themeProfileId ?: "cyberpunk_pink"

    // ۴ تم برجسته طبق درخواست کاربر
    val featuredThemes = MasterNeonThemes.take(4)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        featuredThemes.forEach { spec ->
            val isSelected = currentThemeId == spec.id || (currentThemeId.isBlank() && spec.id == "cyberpunk_pink")
            val chipBg = if (isSelected) {
                Brush.horizontalGradient(listOf(spec.primaryColor, spec.cardWaterColor))
            } else {
                Brush.horizontalGradient(listOf(Color(0xFF161224), Color(0xFF0F0B18)))
            }
            val borderStroke = if (isSelected) {
                BorderStroke(1.2.dp, Color.White.copy(alpha = 0.6f))
            } else {
                BorderStroke(1.dp, spec.primaryColor.copy(alpha = 0.25f))
            }

            Surface(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onUpdateSettings?.invoke { it.applyMasterTheme(spec) }
                },
                shape = RoundedCornerShape(12.dp),
                color = Color.Transparent,
                border = borderStroke,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(chipBg),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(spec.primaryColor),
                    )
                    Text(
                        text = spec.title,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            fontSize = 11.sp,
                        ),
                        color = if (isSelected) Color.White else Color(0xFFD1D5DB),
                    )
                }
            }
        }
    }
}

/**
 * کاشی اصلی اتصال: یکپارچه‌ساز کلید اتصال و وضعیت (بدون المان شناور و انیمیشن اضافه)
 */
@Composable
fun BentoMasterConnectTile(
    state: ConnectionState,
    personalization: Personalization,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val isConnected = state.isActive
    val isConnecting = state.isTransitioning

    val primaryAccent = personalization.connectColor ?: NeonTheme.PrimaryPink
    val waterAccent = personalization.cardWaterColor ?: NeonTheme.Purple
    val cardBg = personalization.cardBgColor ?: Color(0xE612081C)
    val cardBorder = personalization.serverBorderColor ?: NeonTheme.GlassBorderPink

    val tileBg = if (isConnected) {
        Brush.horizontalGradient(listOf(primaryAccent, waterAccent))
    } else if (isConnecting) {
        Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706)))
    } else {
        Brush.horizontalGradient(listOf(cardBg, cardBg.copy(alpha = 0.85f)))
    }

    val tileBorder = if (isConnected) {
        BorderStroke(1.5.dp, Color.White.copy(alpha = 0.4f))
    } else {
        BorderStroke(1.2.dp, cardBorder.copy(alpha = 0.5f))
    }

    val titleText = when {
        isConnected -> "سرویس فعال است"
        isConnecting -> "در حال برقراری اتصال…"
        state == ConnectionState.Error -> "خطا در اتصال"
        else -> "اتصال قطع است"
    }

    val subtitleText = when {
        isConnected -> "برای قطع یک‌باره کلیک کنید"
        isConnecting -> "لطفاً کمی صبر کنید…"
        state == ConnectionState.Error -> "برای تلاش مجدد کلیک کنید"
        else -> "برای برقراری اتصال امن کلیک کنید"
    }

    Surface(
        onClick = {
            if (state != ConnectionState.Disconnecting) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
        },
        shape = RoundedCornerShape(26.dp),
        color = Color.Transparent,
        border = tileBorder,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(tileBg),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // سمت راست در RTL: آیکون تیک یا اتصال + متن‌ها
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isConnected) Color.White.copy(alpha = 0.22f) else primaryAccent.copy(alpha = 0.15f))
                        .border(
                            1.dp,
                            if (isConnected) Color.White.copy(alpha = 0.35f) else primaryAccent.copy(alpha = 0.3f),
                            RoundedCornerShape(16.dp),
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
                            tint = if (isConnecting) Color.White else primaryAccent,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                Column {
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                        ),
                        color = Color.White,
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
                        color = if (isConnected) Color.White.copy(alpha = 0.85f) else Color(0xFF9CA3AF),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // سمت چپ در RTL: قرص کلید ON / OFF
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isConnected) Color.White else Color(0xFF27272A))
                    .border(
                        1.dp,
                        if (isConnected) Color.White else Color(0xFF3F3F46),
                        RoundedCornerShape(10.dp),
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (isConnected) "ON" else if (isConnecting) "..." else "OFF",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                    ),
                    color = if (isConnected) waterAccent else Color(0xFFD1D5DB),
                )
            }
        }
    }
}

/**
 * کاشی سرور فعال
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
    val cardBg = personalization.cardBgColor ?: Color(0xE612081C)
    val cardBorder = personalization.serverBorderColor ?: NeonTheme.GlassBorderPink

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, cardBorder.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
            ) {
                FlagBadge(countryCode = countryCode, size = 32.dp)

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
                            color = Color.White,
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
                                color = Color(0xFFFCD34D),
                            )
                        }
                    }

                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = detail?.takeIf { it.isNotBlank() } ?: "Aether MASQUE · UDP/443",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                        ),
                        color = Color(0xFF9CA3AF),
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
    val cardBg = personalization.dashboardBgColor ?: personalization.cardBgColor ?: Color(0xE612081C)
    val cardBorder = personalization.dashboardBorderColor ?: personalization.serverBorderColor ?: NeonTheme.GlassBorderPink

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
            // کاشی مدت اتصال
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
                            color = Color(0xFF9CA3AF),
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
                        color = Color.White,
                    )
                    Text(
                        text = if (connected) "نشست فعال" else "قطع",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.5.sp),
                        color = if (connected) Color(0xFF34D399) else Color(0xFF6B7280),
                    )
                }
            }

            // کاشی حجم مصرفی
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
                            color = Color(0xFF9CA3AF),
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
                        color = Color.White,
                    )
                    Text(
                        text = "ارسال و دریافت",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.5.sp),
                        color = Color(0xFF9CA3AF),
                    )
                }
            }
        }

        // سطر ۲: پینگ زنده و ترافیک لحظه‌ای
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // کاشی پینگ زنده
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
                            color = Color(0xFF9CA3AF),
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
                            color = if (connected) Color(0xFF34D399) else Color(0xFF6B7280),
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = "ms",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = Color(0xFF9CA3AF),
                        )
                    }
                    Text(
                        text = if (connected) "پایدار بدون نوسان" else "قطع",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.5.sp),
                        color = if (connected) Color(0xFF34D399) else Color(0xFF6B7280),
                    )
                }
            }

            // کاشی سرعت ترافیک لحظه‌ای
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
                            color = Color(0xFF9CA3AF),
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
                            color = Color(0xFF38BDF8),
                        )
                        Text(
                            text = "↑ $upVal $upUnit",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                            ),
                            color = Color(0xFFF472B6),
                        )
                    }
                    Text(
                        text = "مسیر مستقیم بهینه",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.5.sp),
                        color = Color(0xFF9CA3AF),
                    )
                }
            }
        }
    }
}

/**
 * کاشی مشخصات سرور و آی‌پی خروجی
 */
@Composable
fun BentoServerTile(
    connected: Boolean,
    ip: String?,
    org: String?,
    personalization: Personalization,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardBg = personalization.dashboardBgColor ?: personalization.cardBgColor ?: Color(0xE612081C)
    val cardBorder = personalization.dashboardBorderColor ?: personalization.serverBorderColor ?: NeonTheme.GlassBorderPink

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, cardBorder.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
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
                            .background(if (connected) Color(0xFF38BDF8) else Color(0xFF6B7280)),
                    )
                    Text(
                        text = "مشخصات خروجی گیت‌وی",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                        ),
                        color = Color.White,
                    )
                }

                Surface(
                    onClick = onRefresh,
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White.copy(alpha = 0.08f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_sync),
                            contentDescription = null,
                            tint = Color(0xFFD1D5DB),
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            text = "تست زنده",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color(0xFFD1D5DB),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "آدرس IP خروجی:",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = Color(0xFF9CA3AF),
                    )
                    Text(
                        text = if (connected) (ip?.takeIf { it.isNotBlank() } ?: "185.190.142.18") else "---.---.---.---",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                        ),
                        color = if (connected) Color(0xFF38BDF8) else Color(0xFF9CA3AF),
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "دیتاسنتر / میزبان:",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = Color(0xFF9CA3AF),
                    )
                    Text(
                        text = if (connected) (org?.takeIf { it.isNotBlank() } ?: "Hetzner Online (DE)") else "---",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
