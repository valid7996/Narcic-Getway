@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.cluvex.zedsecure.ui.servers

import org.jetbrains.compose.resources.StringResource

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.cluvex.zedsecure.shared.resources.Res
import dev.cluvex.zedsecure.shared.resources.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import dev.cluvex.zedsecure.domain.config.ConfigParser
import dev.cluvex.zedsecure.domain.config.SniSpoofConfig
import dev.cluvex.zedsecure.domain.config.SniSpoofProfile
import dev.cluvex.zedsecure.domain.config.VpnProfile

@Composable
fun SniSpoofSheet(
    onDismiss: () -> Unit,
    onSave: (name: String, SniSpoofProfile) -> Unit,
    initial: SniSpoofProfile? = null,
    initialName: String = "",

    candidates: List<VpnProfile> = emptyList(),

    /** The no-root spoof (the Rsta engine) instead of the one that needs root. */
    noRoot: Boolean = false,
) {
    val noRootMode = noRoot || initial?.noRoot == true
    var method by remember { mutableStateOf(SniSpoofConfig.method(initial?.method.orEmpty())) }
    var methodOpen by remember { mutableStateOf(false) }
    var connectPort by remember { mutableStateOf(initial?.connectPort?.takeIf { it > 0 }?.toString().orEmpty()) }
    var name by remember { mutableStateOf(initialName) }
    var link by remember { mutableStateOf(initial?.link ?: "") }
    var fakeSni by remember { mutableStateOf(initial?.fakeSni ?: "") }
    var cleanIp by remember { mutableStateOf(initial?.cleanIp ?: "") }

    var pickerOpen by remember { mutableStateOf(false) }
    var pickedName by remember {
        mutableStateOf(candidates.firstOrNull { it.rawPayload() == initial?.link }?.name ?: "")
    }
    var manual by remember { mutableStateOf(candidates.isEmpty() || (initial != null && pickedName.isEmpty())) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(if (noRootMode) Res.string.snispoof_noroot_add_title else Res.string.snispoof_add_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            Surface(
                color = if (noRootMode) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(if (noRootMode) Res.string.snispoof_noroot_note else Res.string.snispoof_root_warn),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (noRootMode) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(14.dp),
                )
            }

            F(name, Res.string.manual_remark) { name = it }

            if (candidates.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = pickerOpen,
                    onExpandedChange = { pickerOpen = !pickerOpen },
                ) {
                    OutlinedTextField(
                        value = pickedName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(Res.string.snispoof_pick_server)) },
                        placeholder = { Text(stringResource(Res.string.snispoof_pick_hint)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(pickerOpen) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(
                            androidx.compose.material3.MenuAnchorType.PrimaryNotEditable,
                        ),
                    )
                    DropdownMenu(expanded = pickerOpen, onDismissRequest = { pickerOpen = false }) {
                        candidates.forEach { server ->
                            DropdownMenuItem(
                                text = { Text("${server.name} · ${server.address}:${server.port}") },
                                onClick = {
                                    pickerOpen = false
                                    manual = false
                                    pickedName = server.name
                                    link = server.rawPayload().orEmpty()
                                    if (name.isBlank()) name = server.name

                                    if (cleanIp.isBlank() && server.address.count { c -> c == '.' } == 3) {
                                        cleanIp = server.address
                                    }
                                },
                            )
                        }
                    }
                }
                Row {
                    AssistChip(
                        onClick = { manual = !manual },
                        label = {
                            Text(
                                stringResource(
                                    if (manual) Res.string.snispoof_use_picker
                                    else Res.string.snispoof_paste_instead,
                                ),
                            )
                        },
                    )
                }
            }

            if (manual) {
                F(link, Res.string.snispoof_link, minLines = 3, placeholder = "vless://… / trojan://… / { \"outbounds\": … }") { link = it }
            }
            F(fakeSni, Res.string.snispoof_fake_sni, placeholder = "www.speedtest.net") { fakeSni = it }
            F(cleanIp, Res.string.snispoof_clean_ip, placeholder = "104.16.0.0 (optional)") { cleanIp = it }

            // A link made for the Rsta app points at this device (127.0.0.1:40443); where the server really is
            // then only the clean IP says, and the engine has nowhere to connect without it.
            val linkHost = remember(link) { runCatching { ConfigParser.parse(link.trim())?.address }.getOrNull().orEmpty() }
            val needsCleanIp = noRootMode && cleanIp.isBlank() && linkHost.isNotEmpty() && SniSpoofConfig.isLoopback(linkHost)

            if (noRootMode) {
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
                            DropdownMenuItem(text = { Text(m) }, onClick = { method = m; methodOpen = false })
                        }
                    }
                }
                F(connectPort, Res.string.snispoof_connect_port, placeholder = "443") { connectPort = it.filter(Char::isDigit).take(5) }
                if (needsCleanIp) {
                    Text(
                        stringResource(Res.string.snispoof_needs_clean_ip),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Button(

                enabled = link.isNotBlank() && fakeSni.isNotBlank() && !needsCleanIp,
                onClick = {
                    onSave(
                        name.trim(),
                        SniSpoofProfile(
                            link = link.trim(),
                            fakeSni = fakeSni.trim(),
                            cleanIp = cleanIp.trim(),
                            noRoot = noRootMode,
                            method = SniSpoofConfig.method(method),
                            connectPort = connectPort.toIntOrNull()?.takeIf { it in 1..65535 } ?: 0,
                        ),
                    )
                },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().height(54.dp),
            ) { Text(stringResource(Res.string.action_save), fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun F(value: String, labelRes: StringResource, placeholder: String? = null, minLines: Int = 1, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(labelRes)) },
        placeholder = placeholder?.let { { Text(it) } },
        singleLine = minLines == 1,
        minLines = minLines,
        modifier = Modifier.fillMaxWidth(),
    )
}
