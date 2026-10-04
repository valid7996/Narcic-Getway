@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.cluvex.zedsecure.ui.servers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.cluvex.zedsecure.shared.resources.Res
import dev.cluvex.zedsecure.shared.resources.*
import dev.cluvex.zedsecure.domain.config.AetherProfile
import dev.cluvex.zedsecure.ui.components.PickerField
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * The manual Aether editor, ported from PattNG's ServerAetherActivity into the app's sheet style:
 * the endpoint the tunnel dials, the shape of the tunnel, and the details the network sees. The
 * endpoint may stay empty, which asks the core to scan for one.
 */
@Composable
fun AetherSheet(
    initial: AetherProfile? = null,
    initialName: String = "",
    onDismiss: () -> Unit,
    onSave: (String, AetherProfile) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var protocol by remember { mutableStateOf(initial?.protocol ?: AetherProfile.PROTO_WG) }
    var server by remember { mutableStateOf(initial?.server.orEmpty()) }
    var port by remember { mutableStateOf(initial?.serverPort?.takeIf { it > 0 }?.toString().orEmpty()) }
    var scanMode by remember { mutableStateOf(initial?.scanMode ?: AetherProfile.SCAN_BALANCED) }
    var transport by remember { mutableStateOf(initial?.transport ?: AetherProfile.TRANSPORT_H3) }
    var obfuscation by remember { mutableStateOf(initial?.obfuscation ?: AetherProfile.OBF_AUTO) }
    var ipVersion by remember { mutableStateOf(initial?.ipVersion ?: AetherProfile.IP_V4) }
    var dns by remember { mutableStateOf(initial?.dns.orEmpty()) }
    var exitLoc by remember { mutableStateOf(initial?.exitLoc.orEmpty()) }
    var fingerprint by remember { mutableStateOf(initial?.fingerprint ?: AetherProfile.FINGERPRINT_CHROME) }
    var fragment by remember { mutableStateOf(initial?.fragment ?: false) }
    var fragmentSize by remember { mutableStateOf(initial?.fragmentSize.orEmpty()) }
    var fragmentDelay by remember { mutableStateOf(initial?.fragmentDelay.orEmpty()) }
    var ech by remember { mutableStateOf(initial?.ech ?: false) }
    var echDns by remember { mutableStateOf(initial?.echDns.orEmpty()) }
    var echDomain by remember { mutableStateOf(initial?.echDomain.orEmpty()) }
    var wiwOuter by remember { mutableStateOf(initial?.wiwOuter.orEmpty()) }
    var wiwInner by remember { mutableStateOf(initial?.wiwInner.orEmpty()) }
    var expert by remember { mutableStateOf(initial?.command?.isNotBlank() == true) }
    var command by remember { mutableStateOf(initial?.command.orEmpty()) }
    var saving by remember { mutableStateOf(false) }

    val twoHops = protocol == AetherProfile.PROTO_GOOL || protocol == AetherProfile.PROTO_MIM
    val overMasque = protocol == AetherProfile.PROTO_MASQUE || protocol == AetherProfile.PROTO_MIM

    fun build(): AetherProfile? {
        val host = server.trim()
        val portNumber = port.trim().toIntOrNull()
        if (host.isNotEmpty() || port.isNotEmpty()) {
            if (host.isEmpty() || portNumber == null || portNumber !in 1..65535) return null
        }
        if (twoHops) {
            val outer = wiwOuter.trim()
            val inner = wiwInner.trim()
            if ((outer.isNotEmpty() || inner.isNotEmpty()) &&
                (AetherEndpointText.of(outer) == null || AetherEndpointText.of(inner) == null || outer == inner)
            ) {
                return null
            }
        }
        return AetherProfile(
            protocol = protocol,
            server = host,
            serverPort = portNumber ?: 0,
            scanMode = scanMode,
            transport = transport,
            obfuscation = obfuscation,
            ipVersion = ipVersion,
            dns = dns.trim(),
            exitLoc = exitLoc.trim(),
            fingerprint = fingerprint,
            fragment = fragment,
            fragmentSize = fragmentSize.trim(),
            fragmentDelay = fragmentDelay.trim(),
            ech = ech,
            echDns = echDns.trim(),
            echDomain = echDomain.trim(),
            wiwOuter = wiwOuter.trim(),
            wiwInner = wiwInner.trim(),
            command = if (expert) command.trim() else "",
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                stringResource(Res.string.aether_add_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(Res.string.aether_intro),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(Res.string.vault_form_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            PickerField(
                label = stringResource(Res.string.aether_lab_protocol),
                options = AetherProfile.protocols.map { it to it.uppercase() },
                selected = protocol,
                onSelect = { protocol = it },
            )

            if (twoHops) {
                OutlinedTextField(
                    value = wiwOuter,
                    onValueChange = { wiwOuter = it },
                    label = { Text(stringResource(Res.string.aether_lab_wiw_outer)) },
                    placeholder = { Text(stringResource(Res.string.aether_hint_endpoint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = wiwInner,
                    onValueChange = { wiwInner = it },
                    label = { Text(stringResource(Res.string.aether_lab_wiw_inner)) },
                    placeholder = { Text(stringResource(Res.string.aether_hint_endpoint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                OutlinedTextField(
                    value = server,
                    onValueChange = { server = it },
                    label = { Text(stringResource(Res.string.aether_lab_endpoint)) },
                    placeholder = { Text(stringResource(Res.string.aether_hint_endpoint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it.filter(Char::isDigit).take(5) },
                    label = { Text(stringResource(Res.string.aether_lab_port)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            PickerField(
                label = stringResource(Res.string.aether_lab_scan_mode),
                options = AetherProfile.scanModes.map { it to it.replaceFirstChar(Char::uppercase) },
                selected = scanMode,
                onSelect = { scanMode = it },
            )

            if (overMasque) {
                PickerField(
                    label = stringResource(Res.string.aether_lab_transport),
                    options = AetherProfile.transports.map { it to it.uppercase() },
                    selected = transport,
                    onSelect = { transport = it },
                )
            }

            PickerField(
                label = stringResource(Res.string.aether_lab_obfuscation),
                options = AetherProfile.obfuscations.map { it to it.replaceFirstChar(Char::uppercase) },
                selected = obfuscation,
                onSelect = { obfuscation = it },
            )

            PickerField(
                label = stringResource(Res.string.aether_lab_ip_version),
                options = AetherProfile.ipVersions.map { it to it.uppercase() },
                selected = ipVersion,
                onSelect = { ipVersion = it },
            )

            OutlinedTextField(
                value = dns,
                onValueChange = { dns = it },
                label = { Text(stringResource(Res.string.aether_lab_dns)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = exitLoc,
                onValueChange = { exitLoc = it },
                label = { Text(stringResource(Res.string.aether_lab_exit_location)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            if (overMasque) {
                PickerField(
                    label = stringResource(Res.string.aether_lab_fingerprint),
                    options = AetherProfile.fingerprints.map { it to it.replaceFirstChar(Char::uppercase) },
                    selected = fingerprint,
                    onSelect = { fingerprint = it },
                )

                SettingRow(
                    label = stringResource(Res.string.aether_lab_fragment),
                    checked = fragment,
                    enabled = transport == AetherProfile.TRANSPORT_H2,
                    onChecked = { fragment = it },
                )
                if (fragment && transport == AetherProfile.TRANSPORT_H2) {
                    OutlinedTextField(
                        value = fragmentSize,
                        onValueChange = { fragmentSize = it.filter(Char::isDigit).take(4) },
                        label = { Text(stringResource(Res.string.aether_lab_fragment_size)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = fragmentDelay,
                        onValueChange = { fragmentDelay = it.filter(Char::isDigit).take(4) },
                        label = { Text(stringResource(Res.string.aether_lab_fragment_delay)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                SettingRow(
                    label = stringResource(Res.string.aether_lab_ech),
                    checked = ech,
                    enabled = true,
                    onChecked = { ech = it },
                )
                if (ech) {
                    OutlinedTextField(
                        value = echDns,
                        onValueChange = { echDns = it },
                        label = { Text(stringResource(Res.string.aether_lab_ech_dns)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = echDomain,
                        onValueChange = { echDomain = it },
                        label = { Text(stringResource(Res.string.aether_lab_ech_domain)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            SettingRow(
                label = stringResource(Res.string.aether_lab_expert),
                checked = expert,
                enabled = true,
                onChecked = { expert = it },
            )
            if (expert) {
                OutlinedTextField(
                    value = command,
                    onValueChange = { command = it },
                    label = { Text(stringResource(Res.string.aether_lab_command)) },
                    supportingText = { Text(stringResource(Res.string.aether_command_hint)) },
                    minLines = 2,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Button(
                onClick = {
                    val settings = build() ?: return@Button
                    saving = true
                    onSave(name, settings)
                },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth().height(54.dp),
            ) {
                if (saving) {
                    CircularProgressIndicator(Modifier.width(22.dp).height(22.dp), strokeWidth = 2.5.dp)
                } else {
                    Icon(painterResource(Res.drawable.ic_bolt), null, Modifier.width(20.dp).height(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(Res.string.action_save), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SettingRow(label: String, checked: Boolean, enabled: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, enabled = enabled, onCheckedChange = onChecked)
    }
}

/** Endpoint text validation, shared with the argument builder's rules. */
object AetherEndpointText {
    /** Whether [value] reads as a host:port the core takes, IPv4, IPv6 in brackets or a domain. */
    fun of(value: String?): Boolean {
        val text = value?.trim().orEmpty()
        val separator = text.lastIndexOf(':')
        if (separator <= 0) return false
        val host = text.substring(0, separator)
        if (':' in host && !(host.startsWith('[') && host.endsWith(']'))) return false
        val port = text.substring(separator + 1)
        return port.length in 1..5 && port.all { it in '0'..'9' } && port.toInt() in 1..65535 && host.isNotBlank()
    }
}
