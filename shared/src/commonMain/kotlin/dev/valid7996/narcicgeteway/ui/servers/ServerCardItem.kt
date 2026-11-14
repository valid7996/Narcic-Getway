@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.valid7996.narcicgeteway.ui.servers
import dev.valid7996.narcicgeteway.ui.platform.LocalPlatform
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import dev.valid7996.narcicgeteway.ui.theme.NeonTheme
import dev.valid7996.narcicgeteway.ui.theme.NeonWaveAccent
import dev.valid7996.narcicgeteway.domain.config.ProfileSource
import dev.valid7996.narcicgeteway.ui.format.formatBytes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import kotlinx.coroutines.delay
import androidx.compose.ui.unit.TextUnit
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.valid7996.narcicgeteway.platform.currentTimeMillis
import dev.valid7996.narcicgeteway.shared.resources.Res
import dev.valid7996.narcicgeteway.shared.resources.*
import dev.valid7996.narcicgeteway.data.config.ConfigRepository
import dev.valid7996.narcicgeteway.domain.config.VpnProfile
import dev.valid7996.narcicgeteway.ui.components.FlagBadge
import dev.valid7996.narcicgeteway.ui.format.formatBytes
import dev.valid7996.narcicgeteway.ui.theme.Personalization
import dev.valid7996.narcicgeteway.ui.theme.NrgGreen
import dev.valid7996.narcicgeteway.ui.theme.pingColor
import dev.valid7996.narcicgeteway.ui.theme.readableOn
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

internal fun extractCountryCode(text: String): String? {
    var i = 0
    while (i <= text.length - 4) {
        if (text[i] == '\uD83C' && text[i + 1] in '\uDDE6'..'\uDDFF' &&
            text[i + 2] == '\uD83C' && text[i + 3] in '\uDDE6'..'\uDDFF') {
            val c1 = ('A'.code + (text[i + 1].code - 0xDDE6)).toChar()
            val c2 = ('A'.code + (text[i + 3].code - 0xDDE6)).toChar()
            return "$c1$c2"
        }
        i++
    }
    return null
}

internal fun cleanProfileName(name: String): String {
    val sb = StringBuilder(name.length)
    var i = 0
    while (i < name.length) {
        if (i <= name.length - 4 &&
            name[i] == '\uD83C' && name[i + 1] in '\uDDE6'..'\uDDFF' &&
            name[i + 2] == '\uD83C' && name[i + 3] in '\uDDE6'..'\uDDFF') {
            i += 4
        } else {
            sb.append(name[i])
            i++
        }
    }
    return sb.toString().replace(Regex("""\s+"""), " ").trim().ifBlank { name }
}

/**
 * The server card of the servers list and the home list alike: the side rail, the active dot, the
 * ping chip and the actions menu. The share actions stay behind [ServersUiConfig.SHOW_SHARE_ACTIONS]
 * so they can come back untouched.
 */
@Composable
internal fun ServerCard(
    profile: VpnProfile,
    active: Boolean,
    subscriptionName: String?,
    onClick: () -> Unit,
    onShare: () -> Unit = {},
    onShareLocked: (() -> Unit)? = null,
    onShareQr: () -> Unit = {},
    shareLink: String? = null,
    onRename: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onMoveGroup: (() -> Unit)? = null,
    selecting: Boolean = false,
    isSelected: Boolean = false,
    onPingTcp: () -> Unit = {},
    onPingReal: () -> Unit = {},
    onDelete: () -> Unit,
    personalization: Personalization = Personalization.Default,
    columns: Int = 1,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val isSelectedCard = selecting && isSelected
    val platform = dev.valid7996.narcicgeteway.ui.platform.LocalPlatform.current
    val scope = rememberCoroutineScope()
    val copiedToastText = stringResource(Res.string.address_copied)

    val shape = RoundedCornerShape(26.dp)
    val isNoir = personalization.themeProfileId == "noir"
    val cardBg = personalization.cardBgColor ?: if (active) (if (isNoir) Color(0xE616161A) else Color(0xE6140A1E)) else (if (isNoir) Color(0xE6111115) else Color(0xE612081C))
    val activeDotColor = personalization.serverBorderColor ?: personalization.serverActiveColor ?: personalization.connectColor ?: (if (isNoir) Color(0xFFE2E8F0) else Color(0xFFF72585))
    val cardBorder = if (isSelectedCard) {
        Color(0xFF38BDF8)
    } else if (active) {
        activeDotColor
    } else {
        personalization.serverBorderColor?.copy(alpha = 0.4f) ?: (if (isNoir) Color(0x3094A3B8) else Color(0x357C3AED))
    }
    val textColor = personalization.textColor ?: Color.White
    val isDefaultConfig = ConfigRepository.isDefaultSubscription(profile.subscriptionId) || profile.isLocked

    val pingMs = profile.lastPingMs ?: -1
    val (signalLevel, pingColor) = when {
        pingMs in 1..150 -> Pair(3, Color(0xFF22C55E))
        pingMs in 151..350 -> Pair(2, Color(0xFF06B6D4))
        pingMs in 351..600 -> Pair(2, Color(0xFFF59E0B))
        pingMs > 600 -> Pair(1, Color(0xFFEF4444))
        else -> Pair(0, Color(0xFF6B7280))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(cardBg)
            .border(if (active) 1.5.dp else 1.dp, cardBorder, shape)
            .clickable(onClick = onClick),
    ) {
        // Subtle vector wave accent inside card (zero CPU/thermal cost)
        val waveColor = personalization.cardWaterColor ?: if (active) (personalization.serverActiveColor ?: NeonTheme.PrimaryPink) else NeonTheme.Purple
        NeonWaveAccent(
            color = waveColor,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .align(Alignment.BottomCenter),
            alpha = if (active) 0.28f else 0.14f,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
        ) {
            // Top Row: Name + Flag on right; Checkmark / Ping + More menu on left
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Right in RTL: Flag + Server Name
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val flagCountry = extractCountryCode(profile.name) ?: profile.countryCode
                    if (!flagCountry.isNullOrBlank()) {
                        FlagBadge(countryCode = flagCountry, size = 22.dp)
                    }
                    Text(
                        text = cleanProfileName(profile.name),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        ),
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (isDefaultConfig || profile.isLocked) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            painter = painterResource(Res.drawable.ic_lock),
                            contentDescription = "Locked",
                            tint = Color(0xFFFFB800),
                            modifier = Modifier.size(15.dp),
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Left in RTL: Checkmark or Ping + Expiry Badge + More Menu
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    profile.expiresAt?.let { exp ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x22F59E0B))
                                .border(0.8.dp, Color(0x60F59E0B), RoundedCornerShape(10.dp))
                                .padding(horizontal = 6.dp, vertical = 3.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_schedule),
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(11.dp),
                                )
                                ExpiryCountdownBadge(
                                    expiresAt = exp,
                                    isDark = isDark,
                                    fontSize = 10.sp,
                                )
                            }
                        }
                    }
                    if (active) {
                        val checkTint = if (activeDotColor.luminance() > 0.6f) Color.Black else Color.White
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(activeDotColor),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_check),
                                contentDescription = "Active",
                                tint = checkTint,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    } else if (pingMs > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x25F59E0B))
                                .border(0.8.dp, Color(0x60F59E0B), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Text(
                                text = "ms $pingMs",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                ),
                                color = Color(0xFFF59E0B),
                            )
                        }
                    }

                    Box {
                        IconButton(
                            onClick = { menuOpen = true },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_more_vert),
                                contentDescription = "More",
                                tint = Color(0xFF9CA3AF),
                                modifier = Modifier.size(18.dp),
                            )
                        }

                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            if (!isDefaultConfig) {
                                onEdit?.let {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(Res.string.action_edit)) },
                                        leadingIcon = { Icon(painterResource(Res.drawable.ic_edit), null, Modifier.size(18.dp)) },
                                        onClick = { menuOpen = false; it() },
                                    )
                                }
                                onRename?.let {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(Res.string.servers_rename)) },
                                        leadingIcon = { Icon(painterResource(Res.drawable.ic_edit), null, Modifier.size(18.dp)) },
                                        onClick = { menuOpen = false; it() },
                                    )
                                }
                                onMoveGroup?.let {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(Res.string.groups_move)) },
                                        leadingIcon = { Icon(painterResource(Res.drawable.ic_add_link), null, Modifier.size(18.dp)) },
                                        onClick = { menuOpen = false; it() },
                                    )
                                }
                                if (shareLink != null) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(Res.string.action_share)) },
                                        leadingIcon = { Icon(painterResource(Res.drawable.ic_ios_share), null, Modifier.size(18.dp)) },
                                        onClick = { menuOpen = false; onShare() },
                                    )
                                    onShareLocked?.let { shareLocked ->
                                        DropdownMenuItem(
                                            text = { Text(stringResource(Res.string.server_card_lock_and_share)) },
                                            leadingIcon = { Icon(painterResource(Res.drawable.ic_lock), null, tint = Color(0xFFFF9500), modifier = Modifier.size(18.dp)) },
                                            enabled = true,
                                            onClick = { menuOpen = false; shareLocked() },
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text(stringResource(Res.string.action_share_qr)) },
                                        leadingIcon = { Icon(painterResource(Res.drawable.ic_qr_code_2), null, Modifier.size(18.dp)) },
                                        onClick = { menuOpen = false; onShareQr() },
                                    )
                                    HorizontalDivider()
                                }
                            }
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.ping_real)) },
                                leadingIcon = { Icon(painterResource(Res.drawable.ic_bolt), null, Modifier.size(18.dp)) },
                                onClick = { menuOpen = false; onPingReal() },
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.action_delete), color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(painterResource(Res.drawable.ic_delete), null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) },
                                onClick = { menuOpen = false; onDelete() },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Middle Row: Address + Protocol + Copy Icon (Locked configs do NOT reveal IP or allow copying)
            if (isDefaultConfig) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                ) {
                    Text(
                        text = stringResource(Res.string.server_protected_sub),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        color = Color(0xFF9CA3AF),
                        maxLines = 1,
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        painter = painterResource(Res.drawable.ic_lock),
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(13.dp),
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            platform.copyToClipboard("${profile.address}:${profile.port}")
                            scope.launch { platform.toast(copiedToastText) }
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                ) {
                    Text(
                        text = "${profile.protocol} • ${profile.address}:${profile.port}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.5.sp,
                        ),
                        color = Color(0xFF9CA3AF),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        painter = painterResource(Res.drawable.ic_content_paste),
                        contentDescription = "Copy",
                        tint = Color(0xFF9CA3AF),
                        modifier = Modifier.size(14.dp),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(thickness = 0.5.dp, color = Color(0x20FFFFFF))
            Spacer(Modifier.height(8.dp))

            // Bottom Row: Signal & Ping + MTU on right; Download & Upload on left
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Right in RTL: Signal bars + Ping + MTU
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onPingReal),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ServerSignalBars(level = signalLevel, color = pingColor)
                    Text(
                        text = if (pingMs > 0) "$pingMs ms" else "---",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                        ),
                        color = pingColor,
                    )
                    val configMtu: Int? = remember(profile) {
                        when (val src = profile.source) {
                            is ProfileSource.Link -> {
                                val mtuMatch = Regex("""[?&#]mtu=(\d+)""", RegexOption.IGNORE_CASE).find(src.link)
                                mtuMatch?.groupValues?.get(1)?.toIntOrNull()
                            }
                            is ProfileSource.RawJson -> {
                                val mtuMatch = Regex(""""mtu"\s*:\s*(\d+)""").find(src.json)
                                mtuMatch?.groupValues?.get(1)?.toIntOrNull()
                            }
                            else -> null
                        }
                    }
                    if (configMtu != null) {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "MTU: $configMtu",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = Color(0xFF9CA3AF),
                        )
                    }
                }

                // Left in RTL: Download & Upload traffic
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "↓ ${formatBytes(profile.bytesDown)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                        ),
                        color = Color(0xFF06B6D4),
                    )
                    Text(
                        text = "↑ ${formatBytes(profile.bytesUp)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                        ),
                        color = personalization.connectColor ?: (if (isNoir) Color(0xFFE2E8F0) else Color(0xFFF72585)),
                    )
                }
            }
        }
    }
}

@Composable
private fun ServerSignalBars(
    level: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.height(13.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        listOf(3.5.dp, 6.5.dp, 9.5.dp, 12.5.dp).forEachIndexed { index, h ->
            val barColor = if (index <= level) color else color.copy(alpha = 0.25f)
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(h)
                    .clip(RoundedCornerShape(1.dp))
                    .background(barColor),
            )
        }
    }
}

@Composable
fun ExpiryCountdownBadge(
    expiresAt: Long,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 9.sp,
    fontWeight: FontWeight = FontWeight.Bold,
) {
    var now by remember(expiresAt) { mutableLongStateOf(currentTimeMillis()) }

    LaunchedEffect(expiresAt) {
        while (true) {
            now = currentTimeMillis()
            val remainingMs = expiresAt - now
            if (remainingMs <= 0) break
            delay(1000L)
        }
    }

    val remainingMs = expiresAt - now
    val (expText, expColor) = if (remainingMs <= 0) {
        stringResource(Res.string.lock_expired) to Color(0xFFFF3B30)
    } else {
        val totalSeconds = (remainingMs / 1000L).coerceAtLeast(0)
        val days = totalSeconds / (24 * 3600L)
        val hours = (totalSeconds % (24 * 3600L)) / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L

        val formatted = when {
            days > 0 -> {
                if (hours > 0) stringResource(Res.string.card_countdown_days_hours_mins, days, hours, minutes) else stringResource(Res.string.card_countdown_days_mins, days, minutes)
            }
            hours > 0 -> {
                stringResource(Res.string.card_countdown_hours_mins_secs, hours, minutes, seconds)
            }
            minutes > 0 -> {
                stringResource(Res.string.card_countdown_mins_secs, minutes, seconds)
            }
            else -> {
                stringResource(Res.string.card_countdown_secs, seconds)
            }
        }
        val color = when {
            remainingMs < 10 * 60_000L -> Color(0xFFFF3B30)
            remainingMs < 60 * 60_000L -> Color(0xFFFF9500)
            else -> if (isDark) Color(0xFFAAAAAA) else Color(0xFF666666)
        }
        formatted to color
    }

    Text(
        text = expText,
        fontSize = fontSize,
        fontWeight = fontWeight,
        color = expColor,
        maxLines = 1,
        modifier = modifier,
    )
}


