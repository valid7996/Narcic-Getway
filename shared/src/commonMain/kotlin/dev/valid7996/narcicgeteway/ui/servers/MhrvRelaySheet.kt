@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.valid7996.narcicgeteway.ui.servers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.valid7996.narcicgeteway.domain.config.MhrvRelayProfile
import dev.valid7996.narcicgeteway.shared.resources.*
import dev.valid7996.narcicgeteway.ui.components.PickerField
import dev.valid7996.narcicgeteway.ui.platform.LocalPlatform
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Add / edit sheet of the MasterHttpRelayVPN (Apps Script relay) core. */
@Composable
fun MhrvRelaySheet(
    initial: MhrvRelayProfile? = null,
    initialName: String = "",
    onDismiss: () -> Unit,
    onSave: (name: String, MhrvRelayProfile) -> Unit,
) {
    val platform = LocalPlatform.current
    val pastedOk = stringResource(Res.string.mhrv_pasted_ok)
    val pasteBad = stringResource(Res.string.mhrv_paste_bad)

    var name by remember { mutableStateOf(initialName) }
    var mode by remember { mutableStateOf(initial?.mode ?: MhrvRelayProfile.MODE_APPS_SCRIPT) }
    var scriptIds by remember { mutableStateOf(initial?.scriptIds ?: "") }
    var authKey by remember { mutableStateOf(initial?.authKey ?: "") }
    var googleIp by remember { mutableStateOf(initial?.googleIp ?: MhrvRelayProfile.DEFAULT_GOOGLE_IP) }
    var frontDomain by remember { mutableStateOf(initial?.frontDomain ?: MhrvRelayProfile.DEFAULT_FRONT_DOMAIN) }
    var sniHosts by remember { mutableStateOf(initial?.sniHosts ?: "") }
    var verifySsl by remember { mutableStateOf(initial?.verifySsl ?: true) }
    var parallel by remember { mutableStateOf(initial?.parallelRelay ?: 1) }
    var blockQuic by remember { mutableStateOf(initial?.blockQuic ?: true) }
    var blockDoh by remember { mutableStateOf(initial?.blockDoh ?: false) }
    var youtubeRelay by remember { mutableStateOf(initial?.youtubeViaRelay ?: true) }
    var upstream by remember { mutableStateOf(initial?.upstreamSocks5 ?: "") }
    var advancedOpen by remember { mutableStateOf(false) }

    fun current() = MhrvRelayProfile(
        mode = mode,
        scriptIds = scriptIds.trim(),
        authKey = authKey.trim(),
        googleIp = googleIp.trim(),
        frontDomain = frontDomain.trim(),
        sniHosts = sniHosts.trim(),
        verifySsl = verifySsl,
        parallelRelay = parallel,
        blockQuic = blockQuic,
        blockDoh = blockDoh,
        youtubeViaRelay = youtubeRelay,
        upstreamSocks5 = upstream.trim(),
        logLevel = initial?.logLevel ?: "info",
    )

    val draft = current()
    val needsCreds = draft.needsCredentials
    val valid = draft.isValid

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 36.dp, height = 5.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color.White.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.2f)),
            )
        },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(Res.string.mhrv_add_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(Res.string.mhrv_add_title_sub),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedButton(
                onClick = {
                    val parsed = platform.readClipboard()?.let(MhrvRelayProfile::fromText)
                    if (parsed == null) {
                        platform.toast(pasteBad)
                    } else {
                        mode = parsed.mode
                        scriptIds = parsed.scriptIds
                        authKey = parsed.authKey.ifBlank { authKey }
                        googleIp = parsed.googleIp
                        frontDomain = parsed.frontDomain
                        sniHosts = parsed.sniHosts
                        verifySsl = parsed.verifySsl
                        parallel = parsed.parallelRelay.coerceIn(1, 8)
                        blockQuic = parsed.blockQuic
                        blockDoh = parsed.blockDoh
                        youtubeRelay = parsed.youtubeViaRelay
                        upstream = parsed.upstreamSocks5
                        platform.toast(pastedOk)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(Res.string.mhrv_paste_config)) }

            Field(name, Res.string.manual_remark) { name = it }
            PickerField(
                label = stringResource(Res.string.mhrv_mode),
                options = listOf(
                    MhrvRelayProfile.MODE_APPS_SCRIPT to stringResource(Res.string.mhrv_mode_apps_script),
                    MhrvRelayProfile.MODE_FULL to stringResource(Res.string.mhrv_mode_full),
                    MhrvRelayProfile.MODE_DIRECT to stringResource(Res.string.mhrv_mode_direct),
                ),
                selected = mode,
                onSelect = { mode = it },
            )
            if (needsCreds) {
                Field(
                    scriptIds,
                    Res.string.mhrv_script_ids,
                    placeholder = "AKfycb…\nhttps://script.google.com/macros/s/…/exec",
                    minLines = 2,
                    error = if (scriptIds.isNotBlank() && draft.scriptIdList().isEmpty()) {
                        stringResource(Res.string.mhrv_script_ids_invalid)
                    } else null,
                ) { scriptIds = it }
                Field(authKey, Res.string.mhrv_auth_key) { authKey = it }
            }
            Field(googleIp, Res.string.mhrv_google_ip, placeholder = MhrvRelayProfile.DEFAULT_GOOGLE_IP) { googleIp = it }
            Field(frontDomain, Res.string.mhrv_front_domain, placeholder = MhrvRelayProfile.DEFAULT_FRONT_DOMAIN) { frontDomain = it }

            if (draft.needsCa && platform.supportsMhrvCa) {
                Text(
                    stringResource(Res.string.mhrv_ca_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = { platform.installMhrvCa().takeIf { it.isNotBlank() }?.let(platform::toast) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(Res.string.mhrv_install_ca)) }
            }

            TextButton(onClick = { advancedOpen = !advancedOpen }) {
                Text(stringResource(Res.string.mhrv_advanced))
            }
            AnimatedVisibility(advancedOpen) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Field(sniHosts, Res.string.mhrv_sni_hosts, placeholder = "www.google.com\nmail.google.com", minLines = 2) { sniHosts = it }
                    Field(upstream, Res.string.mhrv_upstream_socks, placeholder = "127.0.0.1:10808") { upstream = it }
                    Stepper(Res.string.mhrv_parallel, parallel, min = 1, max = 8) { parallel = it }
                    SwitchRow(Res.string.mhrv_verify_ssl, verifySsl, isDark) { verifySsl = it }
                    SwitchRow(Res.string.mhrv_block_quic, blockQuic, isDark) { blockQuic = it }
                    SwitchRow(Res.string.mhrv_block_doh, blockDoh, isDark) { blockDoh = it }
                    SwitchRow(Res.string.mhrv_youtube_relay, youtubeRelay, isDark) { youtubeRelay = it }
                }
            }

            Button(
                onClick = { onSave(name, current()) },
                enabled = valid,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF007AFF),
                    contentColor = Color.White,
                ),
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) {
                Text(stringResource(Res.string.action_save), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun Field(
    value: String,
    labelRes: StringResource,
    placeholder: String? = null,
    minLines: Int = 1,
    error: String? = null,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(labelRes)) },
        placeholder = placeholder?.let { { Text(it) } },
        singleLine = minLines == 1,
        shape = RoundedCornerShape(12.dp),
        minLines = minLines,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SwitchRow(labelRes: StringResource, checked: Boolean, isDark: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(labelRes), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF34C759),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = if (isDark) Color(0xFF39393D) else Color(0xFFE9E9EA),
                uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}

@Composable
private fun Stepper(labelRes: StringResource, value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(labelRes), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        androidx.compose.material3.IconButton(
            onClick = { if (value > min) onChange(value - 1) },
            enabled = value > min,
        ) { Text("−", style = MaterialTheme.typography.titleLarge) }
        Text("$value", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        androidx.compose.material3.IconButton(
            onClick = { if (value < max) onChange(value + 1) },
            enabled = value < max,
        ) { Text("+", style = MaterialTheme.typography.titleLarge) }
    }
}
