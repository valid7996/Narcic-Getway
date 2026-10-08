@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.cluvex.zedsecure.ui.servers

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.cluvex.zedsecure.shared.resources.Res
import dev.cluvex.zedsecure.shared.resources.*
import dev.cluvex.zedsecure.domain.config.VpnProfile
import dev.cluvex.zedsecure.ui.format.formatBytes
import dev.cluvex.zedsecure.ui.theme.Personalization
import dev.cluvex.zedsecure.ui.theme.ZedGreen
import dev.cluvex.zedsecure.ui.theme.pingColor
import dev.cluvex.zedsecure.ui.theme.readableOn
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

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
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val isSelectedCard = selecting && isSelected
    val container = if (isSelectedCard) {
        MaterialTheme.colorScheme.secondaryContainer
    } else if (active) {
        personalization.serverActiveColor ?: ZedGreen
    } else {
        personalization.serverCardColor
            ?: MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
    }
    val frame = when {
        isSelectedCard -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
        active -> ZedGreen.copy(alpha = 0.95f)
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
    }

    val onContainer = when {
        isSelectedCard -> MaterialTheme.colorScheme.onSecondaryContainer
        active && personalization.serverActiveColor != null -> personalization.serverActiveColor.readableOn()
        !active && personalization.serverCardColor != null -> personalization.serverCardColor.readableOn()
        else -> MaterialTheme.colorScheme.onSurface
    }
    val vPad = personalization.density.cardVerticalDp.dp
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(personalization.cornerStyle.serverCardDp.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .clip(RoundedCornerShape(personalization.cornerStyle.serverCardDp.dp))
                .background(container)
                .border(1.dp, frame, RoundedCornerShape(personalization.cornerStyle.serverCardDp.dp))
                .clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(
                        when {
                            active -> MaterialTheme.colorScheme.primary
                            isSelectedCard -> MaterialTheme.colorScheme.secondary
                            else -> Color.Transparent
                        },
                    ),
            )
            Row(
                Modifier
                    .weight(1f)
                    .padding(start = 14.dp, end = 4.dp, top = vPad, bottom = vPad),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (active) {
                            Box(
                                Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                            )
                            Spacer(Modifier.width(7.dp))
                        }
                        Text(
                            text = profile.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = onContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    val subtle = onContainer.copy(alpha = 0.72f)

                    val typeLabel = if (profile.isCustom) {
                        stringResource(Res.string.servers_custom_config)
                    } else {
                        profile.transportLabel
                    }

                    Text(
                        text = buildString {
                            append(typeLabel)
                            if (profile.address.isNotBlank() && profile.address != "-") {
                                append("  •  ")
                                append(profile.address)
                                if (profile.port > 0) append(":${profile.port}")
                            }
                            if (subscriptionName != null) append("  •  $subscriptionName")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = if (personalization.monospaceAddress) FontFamily.Monospace else null,
                        color = subtle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (personalization.showServerUsage && (profile.bytesDown > 0 || profile.bytesUp > 0)) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = stringResource(
                                Res.string.servers_usage,
                                formatBytes(profile.bytesDown),
                                formatBytes(profile.bytesUp),
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = subtle.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                profile.lastPingMs?.takeIf { it >= 0 }?.takeIf { personalization.showServerPing }?.let { ping ->
                    Surface(
                        shape = CircleShape,
                        color = pingColor(ping, container).copy(alpha = 0.16f),
                        contentColor = pingColor(ping, container),
                        modifier = Modifier.padding(end = 4.dp),
                    ) {
                        Text(
                            text = if (ping < 0) "$ping" else "$ping ms",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
                if (active) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        painterResource(Res.drawable.ic_check_circle),
                        contentDescription = stringResource(Res.string.config_active),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                if (ServersUiConfig.SHOW_CARD_MENU) Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            painterResource(Res.drawable.ic_more_vert),
                            contentDescription = stringResource(Res.string.servers_actions),
                        )
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                        shape = MaterialTheme.shapes.largeIncreased,
                    ) {
                        if (ServersUiConfig.SHOW_SHARE_ACTIONS) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.action_share)) },
                                leadingIcon = { Icon(painterResource(Res.drawable.ic_ios_share), null) },

                                enabled = shareLink != null,
                                onClick = { menuOpen = false; onShare() },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.action_share_qr)) },
                                leadingIcon = { Icon(painterResource(Res.drawable.ic_qr_code_2), null) },
                                enabled = shareLink != null,
                                onClick = { menuOpen = false; onShareQr() },
                            )
                        }
                        onEdit?.let { edit ->
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.action_edit)) },
                                leadingIcon = { Icon(painterResource(Res.drawable.ic_edit), null) },
                                enabled = profile.isEditable,
                                onClick = { menuOpen = false; edit() },
                            )
                        }
                        onRename?.let { rename ->
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.servers_rename)) },
                                leadingIcon = { Icon(painterResource(Res.drawable.ic_description), null) },
                                onClick = { menuOpen = false; rename() },
                            )
                        }
                        onMoveGroup?.let { move ->
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.groups_move)) },
                                leadingIcon = { Icon(painterResource(Res.drawable.ic_add_link), null) },
                                onClick = { menuOpen = false; move() },
                            )
                        }

                        if ((!profile.isManagedTunnel || profile.isSingBoxConfig) && !profile.isDnsBasedTunnel) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.ping_tcp)) },
                                leadingIcon = { Icon(painterResource(Res.drawable.ic_speed), null) },
                                onClick = { menuOpen = false; onPingTcp() },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.ping_real)) },
                                leadingIcon = { Icon(painterResource(Res.drawable.ic_bolt), null) },
                                onClick = { menuOpen = false; onPingReal() },
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.action_delete)) },
                            leadingIcon = { Icon(painterResource(Res.drawable.ic_delete), null) },
                            onClick = { menuOpen = false; onDelete() },
                        )
                    }
                }
            }
        }
    }
}

