@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.cluvex.zedsecure.ui.servers

import org.jetbrains.compose.resources.StringResource

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.cluvex.zedsecure.shared.resources.Res
import dev.cluvex.zedsecure.shared.resources.*
import dev.cluvex.zedsecure.domain.config.SniSpoofConfig
import dev.cluvex.zedsecure.domain.config.SniSpoofProfile

/**
 * Dedicated RSTA Spoof configuration sheet — matching the RstaNg experience:
 *  • Connect IP (real server/CDN clean IP, default: 104.18.38.202)
 *  • Connect Port (real server port, default: 443)
 *  • Fake SNI (DPI disguise domain, default: cdnjs.cloudflare.com)
 *  • Spoof Method (combined, fragment, random_split, etc.)
 *  • Engine Status
 *  • View Logs dialog
 *  • Optional remark name
 *  • Optional advanced proxy link
 *
 * No mandatory server wrapping or complex config creation is required.
 */
@Composable
fun SniSpoofSheet(
    onDismiss: () -> Unit,
    onSave: (name: String, SniSpoofProfile) -> Unit,
    initial: SniSpoofProfile? = null,
    initialName: String = "",
    @Suppress("UNUSED_PARAMETER") candidates: List<dev.cluvex.zedsecure.domain.config.VpnProfile> = emptyList(),
    @Suppress("UNUSED_PARAMETER") noRoot: Boolean = true,
) {
    val initParsed = remember(initial?.link) {
        initial?.link?.trim()?.let { lnk ->
            runCatching { dev.cluvex.zedsecure.domain.config.ConfigParser.parse(lnk) }.getOrNull()
        }
    }

    var name by remember { mutableStateOf(initialName.ifBlank { "RSTA Spoof" }) }
    var ip by remember {
        mutableStateOf(
            initial?.cleanIp?.ifBlank { null }
                ?: initParsed?.address?.takeIf { it != "sni-spoof" && it != "127.0.0.1" }
                ?: SniSpoofConfig.DEFAULT_CONNECT_IP
        )
    }
    var port by remember {
        mutableStateOf(
            initParsed?.port?.takeIf { it > 0 && it != SniSpoofConfig.RSTA_LOCAL_PORT }?.toString()
                ?: initial?.connectPort?.takeIf { it > 0 }?.toString()
                ?: SniSpoofConfig.DEFAULT_CONNECT_PORT.toString()
        )
    }
    var fakeSni by remember { mutableStateOf(initial?.fakeSni ?: SniSpoofConfig.DEFAULT_FAKE_SNI) }
    var method by remember { mutableStateOf(SniSpoofConfig.method(initial?.method.orEmpty())) }
    var methodOpen by remember { mutableStateOf(false) }

    // Advanced: optional link override
    var advancedOpen by remember { mutableStateOf(false) }
    var rawLink by remember { mutableStateOf(initial?.link?.takeIf { it.isNotBlank() && initParsed == null } ?: "") }
    var showLogsDialog by remember { mutableStateOf(false) }

    val portInt = port.trim().toIntOrNull()?.takeIf { it in 1..65535 }
    val ipOk = ip.trim().isNotBlank()
    val portOk = portInt != null
    val sniOk = fakeSni.trim().isNotBlank()
    val advancedLinkOk = !advancedOpen || rawLink.isBlank() ||
        runCatching { dev.cluvex.zedsecure.domain.config.ConfigParser.parse(rawLink.trim()) }.isSuccess
    val canSave = ipOk && portOk && sniOk && advancedLinkOk

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // ── Title & Icon ───────────────────────────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF10B981).copy(alpha = 0.16f),
                        modifier = Modifier.size(40.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painterResource(Res.drawable.ic_bolt),
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        stringResource(Res.string.snispoof_noroot_add_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // ── Explanation note ───────────────────────────────────────────────────────────────
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(Res.string.snispoof_noroot_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(14.dp),
                )
            }

            // ── Connect IP (Real Server / Clean IP) ────────────────────────────────────────────
            SniField(
                value = ip,
                labelRes = Res.string.snispoof_clean_ip,
                placeholder = SniSpoofConfig.DEFAULT_CONNECT_IP,
                isError = ip.isNotBlank() && ip.trim().isBlank(),
            ) { ip = it }

            // ── Connect Port (Real Server Port) ────────────────────────────────────────────────
            SniField(
                value = port,
                labelRes = Res.string.snispoof_connect_port,
                placeholder = SniSpoofConfig.DEFAULT_CONNECT_PORT.toString(),
                isError = port.isNotEmpty() && portInt == null,
            ) { port = it.filter(Char::isDigit).take(5) }

            // ── Fake SNI (DPI Disguise Domain) ─────────────────────────────────────────────────
            SniField(
                value = fakeSni,
                labelRes = Res.string.snispoof_fake_sni,
                placeholder = SniSpoofConfig.DEFAULT_FAKE_SNI,
                isError = fakeSni.isNotBlank() && fakeSni.trim().isBlank(),
            ) { fakeSni = it }

            // ── Bypass Method Picker ───────────────────────────────────────────────────────────
            ExposedDropdownMenuBox(expanded = methodOpen, onExpandedChange = { methodOpen = !methodOpen }) {
                OutlinedTextField(
                    value = method,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(Res.string.snispoof_method)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(methodOpen) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(
                        androidx.compose.material3.MenuAnchorType.PrimaryNotEditable,
                    ),
                )
                DropdownMenu(expanded = methodOpen, onDismissRequest = { methodOpen = false }) {
                    SniSpoofConfig.METHODS.forEach { m ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    m,
                                    fontWeight = if (m == method) FontWeight.Bold else FontWeight.Normal,
                                    color = if (m == method) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                )
                            },
                            onClick = { method = m; methodOpen = false },
                        )
                    }
                }
            }

            // ── Remark / Profile Name ──────────────────────────────────────────────────────────
            SniField(
                value = name,
                labelRes = Res.string.manual_remark,
                placeholder = "RSTA Spoof",
            ) { name = it }

            // ── Engine Status & Log Actions ────────────────────────────────────────────────────
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            stringResource(Res.string.snispoof_engine_status),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            stringResource(Res.string.snispoof_status_ready),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF10B981),
                        )
                    }
                    OutlinedButton(
                        onClick = { showLogsDialog = true },
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text(stringResource(Res.string.snispoof_view_logs), fontSize = 12.sp)
                    }
                }
            }

            // ── Advanced: optional proxy link ──────────────────────────────────────────────────
            HorizontalDivider()
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    stringResource(Res.string.snispoof_advanced_link_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                androidx.compose.material3.Switch(
                    checked = advancedOpen,
                    onCheckedChange = { advancedOpen = it },
                )
            }
            if (advancedOpen) {
                SniField(
                    rawLink,
                    Res.string.snispoof_link,
                    placeholder = "vless://… / trojan://… / { \"outbounds\": … }",
                    minLines = 3,
                    isError = rawLink.isNotBlank() && !advancedLinkOk,
                ) { rawLink = it }
                Text(
                    stringResource(Res.string.snispoof_advanced_link_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // ── Save Button ────────────────────────────────────────────────────────────────────
            Button(
                enabled = canSave,
                onClick = {
                    val resolvedPort = portInt ?: SniSpoofConfig.DEFAULT_CONNECT_PORT
                    val resolvedIp = ip.trim().ifBlank { SniSpoofConfig.DEFAULT_CONNECT_IP }
                    val resolvedSni = fakeSni.trim().ifBlank { SniSpoofConfig.DEFAULT_FAKE_SNI }
                    val profileName = name.trim().ifBlank { "RSTA Spoof" }

                    val link = if (advancedOpen && rawLink.isNotBlank()) {
                        rawLink.trim()
                    } else {
                        "vless://00000000-0000-0000-0000-000000000000@127.0.0.1:${SniSpoofConfig.RSTA_LOCAL_PORT}" +
                            "?security=tls&sni=$resolvedSni&type=ws&path=%2F#$profileName"
                    }

                    onSave(
                        profileName,
                        SniSpoofProfile(
                            link = link,
                            fakeSni = resolvedSni,
                            cleanIp = resolvedIp,
                            noRoot = true,
                            method = SniSpoofConfig.method(method),
                            connectPort = resolvedPort,
                        ),
                    )
                },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().height(54.dp),
            ) {
                Text(stringResource(Res.string.action_save), fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showLogsDialog) {
        AlertDialog(
            onDismissRequest = { showLogsDialog = false },
            title = { Text(stringResource(Res.string.snispoof_view_logs)) },
            text = {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp),
                ) {
                    Column(
                        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(12.dp)
                    ) {
                        Text(
                            text = "127.0.0.1:${SniSpoofConfig.RSTA_LOCAL_PORT} -> ${ip.trim()}:${port.trim()} [sni=${fakeSni.trim()}, method=$method]\nReady to spoof TLS handshakes.",
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLogsDialog = false }) {
                    Text(stringResource(Res.string.action_close))
                }
            },
        )
    }
}

@Composable
private fun SniField(
    value: String,
    labelRes: StringResource,
    placeholder: String? = null,
    minLines: Int = 1,
    isError: Boolean = false,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(labelRes)) },
        placeholder = placeholder?.let { { Text(it) } },
        singleLine = minLines == 1,
        minLines = minLines,
        isError = isError,
        modifier = Modifier.fillMaxWidth(),
    )
}
