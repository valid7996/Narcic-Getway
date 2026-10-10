@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.cluvex.zedsecure.ui.servers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.cluvex.zedsecure.shared.resources.Res
import dev.cluvex.zedsecure.shared.resources.*
import dev.cluvex.zedsecure.domain.config.AetherProfile
import dev.cluvex.zedsecure.ui.components.PickerField
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * The manual Aether editor, ported from PattNG's ServerAetherActivity into the app's sheet style:
 * the endpoint the tunnel dials — scanned for when left empty — the shape of the tunnel, the
 * carriers around and inside it, and the details the network sees.
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
    var tor by remember { mutableStateOf(initial?.tor ?: AetherProfile.CARRIER_OFF) }
    var torBridges by remember { mutableStateOf(initial?.torBridges ?: AetherProfile.TOR_BRIDGES_AUTO) }
    var torBridgeLines by remember { mutableStateOf(initial?.torBridgeLines.orEmpty()) }
    var torRelays by remember { mutableStateOf(initial?.torRelays ?: AetherProfile.TOR_RELAYS_AUTO) }
    var psiphon by remember { mutableStateOf(initial?.psiphon ?: AetherProfile.CARRIER_OFF) }
    var psiphonMode by remember { mutableStateOf(initial?.psiphonMode ?: AetherProfile.PSIPHON_MODE_AUTO) }
    var psiphonRegion by remember { mutableStateOf(initial?.psiphonRegion.orEmpty()) }
    var psiphonCdnIps by remember { mutableStateOf(initial?.psiphonCdnIps.orEmpty()) }
    var psiphonCdnSni by remember { mutableStateOf(initial?.psiphonCdnSni.orEmpty()) }
    var psiphonCdnSets by remember { mutableStateOf(initial?.psiphonCdnSets.orEmpty()) }
    var finalMask by remember { mutableStateOf(initial?.finalMask.orEmpty()) }
    var dialMode by remember { mutableStateOf(initial?.dialMode.orEmpty()) }
    var expert by remember { mutableStateOf(initial?.command?.isNotBlank() == true) }
    var targetStrategy by remember { mutableStateOf(initial?.targetStrategy ?: "AsIs") }
    var psiphonBundledList by remember { mutableStateOf(initial?.psiphonBundledList ?: true) }
    var command by remember { mutableStateOf(initial?.command.orEmpty()) }
    var saving by remember { mutableStateOf(false) }

    var showIdentity by remember { mutableStateOf(false) }
    val aether = LocalAetherActions.current
    val scope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    // The scanner's state: idle, running, or what it ended with.
    var scanning by remember { mutableStateOf(false) }
    var scanOutcome by remember { mutableStateOf<ScanOutcome?>(null) }

    val twoHops = protocol == AetherProfile.PROTO_GOOL || protocol == AetherProfile.PROTO_MIM
    val overMasque = protocol == AetherProfile.PROTO_MASQUE || protocol == AetherProfile.PROTO_MIM
    val carriersUsed = tor != AetherProfile.CARRIER_OFF || psiphon != AetherProfile.CARRIER_OFF

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
                (!AetherEndpointText.of(outer) || !AetherEndpointText.of(inner) || outer == inner)
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
            tor = tor,
            torBridges = torBridges,
            torBridgeLines = torBridgeLines.trim(),
            torRelays = torRelays,
            psiphon = psiphon,
            psiphonMode = psiphonMode,
            psiphonRegion = psiphonRegion.trim(),
            psiphonCdnIps = psiphonCdnIps.trim(),
            psiphonCdnSni = psiphonCdnSni.trim(),
            psiphonCdnSets = psiphonCdnSets.trim(),
            psiphonBundledList = psiphonBundledList,
            finalMask = finalMask.trim(),
            dialMode = dialMode.trim(),
            targetStrategy = targetStrategy,
            command = if (expert) command.trim() else "",
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) androidx.compose.ui.graphics.Color(0xF20B1C38) else androidx.compose.ui.graphics.Color(0xF8F8FAFD),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            androidx.compose.foundation.layout.Box(
                Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 38.dp, height = 4.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(if (isDark) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f) else androidx.compose.ui.graphics.Color(0xFF64748B).copy(alpha = 0.4f)),
            )
        },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                stringResource(Res.string.aether_add_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (isDark) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color(0xFF0F172A),
                modifier = Modifier.padding(vertical = 4.dp),
            )
            Text(
                stringResource(Res.string.aether_intro),
                style = MaterialTheme.typography.bodySmall,
                color = if (isDark) androidx.compose.ui.graphics.Color(0xFFA0B0C4) else androidx.compose.ui.graphics.Color(0xFF64748B),
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(Res.string.manual_remark)) },
                singleLine = true,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            if (carriersUsed || aether != null) {
                PickerField(
                    label = stringResource(Res.string.aether_lab_protocol),
                    options = AetherProfile.protocols.map { it to it.uppercase() },
                    selected = protocol,
                    onSelect = { protocol = it },
                )
                
                if (overMasque) {
                    PickerField(
                        label = stringResource(Res.string.aether_lab_transport),
                        options = AetherProfile.transports.map { it to it.uppercase() },
                        selected = transport,
                        onSelect = { transport = it },
                    )
                }
                if (overMasque && transport == AetherProfile.TRANSPORT_H2) {
                    SettingRow(
                        label = stringResource(Res.string.aether_lab_fragment),
                        checked = fragment,
                        enabled = true,
                        onChecked = { fragment = it },
                    )
                    if (fragment) {
                        OutlinedTextField(
                            value = fragmentSize,
                            onValueChange = { fragmentSize = it.filter(Char::isDigit).take(4) },
                            label = { Text(stringResource(Res.string.aether_lab_fragment_size)) },
                            placeholder = { Text(stringResource(Res.string.aether_hint_fragment_size)) },
                            singleLine = true,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = fragmentDelay,
                            onValueChange = { fragmentDelay = it.filter(Char::isDigit).take(4) },
                            label = { Text(stringResource(Res.string.aether_lab_fragment_delay)) },
                            placeholder = { Text(stringResource(Res.string.aether_hint_fragment_delay)) },
                            singleLine = true,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                
                if (overMasque) {
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
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = echDomain,
                            onValueChange = { echDomain = it },
                            label = { Text(stringResource(Res.string.aether_lab_ech_domain)) },
                            singleLine = true,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                
                PickerField(
                    label = stringResource(Res.string.aether_lab_scan_mode),
                    options = AetherProfile.scanModes.map { it to it.replaceFirstChar(Char::uppercase) },
                    selected = scanMode,
                    onSelect = { scanMode = it },
                )
                
                if (transport != AetherProfile.TRANSPORT_H2 || !overMasque) {
                    PickerField(
                        label = stringResource(Res.string.aether_lab_obfuscation),
                        options = AetherProfile.obfuscations.map { it to it.replaceFirstChar(Char::uppercase) },
                        selected = obfuscation,
                        onSelect = { obfuscation = it },
                    )
                }
                
                if (overMasque) {
                    PickerField(
                        label = stringResource(Res.string.aether_lab_fingerprint),
                        options = AetherProfile.fingerprints.map { it to it.replaceFirstChar(Char::uppercase) },
                        selected = fingerprint,
                        onSelect = { fingerprint = it },
                    )
                }
            }

            dev.cluvex.zedsecure.ui.components.SectionTitle(stringResource(Res.string.aether_lab_exit_loc))

            OutlinedTextField(
                value = finalMask,
                onValueChange = { finalMask = it },
                label = { Text(stringResource(Res.string.aether_lab_exit_final_mask)) },
                minLines = 1,
                maxLines = 4,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = dialMode,
                onValueChange = { dialMode = it },
                label = { Text(stringResource(Res.string.aether_lab_exit_dial_mode)) },
                singleLine = true,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            dev.cluvex.zedsecure.ui.components.SectionTitle(stringResource(Res.string.aether_group_carriers))

            if (tor != AetherProfile.CARRIER_OFF || aether != null) {
                PickerField(
                    label = stringResource(Res.string.aether_lab_tor),
                    options = AetherProfile.carriers.map { it to it.replaceFirstChar(Char::uppercase) },
                    selected = tor,
                    onSelect = { tor = it },
                )
                if (tor != AetherProfile.CARRIER_OFF) {
                    PickerField(
                        label = stringResource(Res.string.aether_lab_tor_bridges),
                        options = AetherProfile.torBridgeModes.map { it to it.replaceFirstChar(Char::uppercase) },
                        selected = torBridges,
                        onSelect = { torBridges = it },
                    )
                    if (torBridges == AetherProfile.TOR_BRIDGES_OWN) {
                        OutlinedTextField(
                            value = torBridgeLines,
                            onValueChange = { torBridgeLines = it },
                            label = { Text(stringResource(Res.string.aether_lab_tor_bridge_lines)) },
                            placeholder = { Text(stringResource(Res.string.aether_hint_tor_bridge_lines)) },
                            minLines = 2,
                            maxLines = 6,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (torBridges == AetherProfile.TOR_BRIDGES_AUTO || torBridges == AetherProfile.TOR_BRIDGES_FIRST) {
                        PickerField(
                            label = stringResource(Res.string.aether_lab_tor_relays),
                            options = AetherProfile.torRelayModes.map { it to it.replaceFirstChar(Char::uppercase) },
                            selected = torRelays,
                            onSelect = { torRelays = it },
                        )
                    }
                }
            }

            if (psiphon != AetherProfile.CARRIER_OFF || aether != null) {
                PickerField(
                    label = stringResource(Res.string.aether_lab_psiphon),
                    options = AetherProfile.carriers.map { it to it.replaceFirstChar(Char::uppercase) },
                    selected = psiphon,
                    onSelect = { psiphon = it },
                )
                if (psiphon != AetherProfile.CARRIER_OFF) {
                    PickerField(
                        label = stringResource(Res.string.aether_lab_psiphon_mode),
                        options = AetherProfile.psiphonModes.map { it to it.replaceFirstChar(Char::uppercase) },
                        selected = psiphonMode,
                        onSelect = { psiphonMode = it },
                    )
                    OutlinedTextField(
                        value = psiphonRegion,
                        onValueChange = { psiphonRegion = it },
                        label = { Text("Psiphon Region") },
                        singleLine = true,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (psiphonMode != AetherProfile.PSIPHON_MODE_DIRECT) {
                        OutlinedTextField(
                            value = psiphonCdnIps,
                            onValueChange = { psiphonCdnIps = it },
                            label = { Text(stringResource(Res.string.aether_lab_psiphon_cdn_ips)) },
                            singleLine = true,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = psiphonCdnSni,
                            onValueChange = { psiphonCdnSni = it },
                            label = { Text(stringResource(Res.string.aether_lab_psiphon_cdn_sni)) },
                            singleLine = true,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = psiphonCdnSets,
                            onValueChange = { psiphonCdnSets = it },
                            label = { Text(stringResource(Res.string.aether_lab_psiphon_cdn_sets)) },
                            singleLine = true,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    SettingRow(
                        label = stringResource(Res.string.aether_lab_psiphon_bundled_list),
                        checked = psiphonBundledList,
                        enabled = true,
                        onChecked = { psiphonBundledList = it },
                    )
                }
            }

            dev.cluvex.zedsecure.ui.components.SectionTitle("Endpoint")

            if (twoHops) {
                OutlinedTextField(
                    value = wiwOuter,
                    onValueChange = { wiwOuter = it },
                    label = { Text(stringResource(Res.string.aether_lab_wiw_outer)) },
                    placeholder = { Text(stringResource(Res.string.aether_hint_endpoint)) },
                    singleLine = true,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = wiwInner,
                    onValueChange = { wiwInner = it },
                    label = { Text(stringResource(Res.string.aether_lab_wiw_inner)) },
                    placeholder = { Text(stringResource(Res.string.aether_hint_endpoint)) },
                    singleLine = true,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                OutlinedTextField(
                    value = server,
                    onValueChange = { server = it },
                    label = { Text(stringResource(Res.string.aether_lab_endpoint)) },
                    placeholder = { Text(stringResource(Res.string.aether_hint_endpoint)) },
                    singleLine = true,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it.filter(Char::isDigit).take(5) },
                    label = { Text(stringResource(Res.string.aether_lab_port)) },
                    singleLine = true,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            PickerField(
                label = stringResource(Res.string.aether_lab_ip_version),
                options = AetherProfile.ipVersions.map { it to it.uppercase() },
                selected = ipVersion,
                onSelect = { ipVersion = it },
            )

            if (aether != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = {
                            val current = build() ?: return@FilledTonalButton
                            scanning = true
                            scanOutcome = null
                            scope.launch {
                                val result = aether.scan(current)
                                scanning = false
                                scanOutcome = if (result == null) ScanOutcome.Failed else {
                                    if (twoHops && result.innerHop != null) {
                                        wiwOuter = result.endpoint
                                        wiwInner = result.innerHop
                                    } else {
                                        val text = result.endpoint
                                        val separator = text.lastIndexOf(':')
                                        if (separator > 0) {
                                            server = text.substring(0, separator).removeSurrounding("[", "]")
                                            port = text.substring(separator + 1)
                                        }
                                    }
                                    ScanOutcome.Found
                                }
                            }
                        },
                        enabled = !scanning,
                        modifier = Modifier.weight(1f),
                    ) {
                        if (scanning) {
                            CircularProgressIndicator(Modifier.width(18.dp).height(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(stringResource(Res.string.aether_scanning))
                        } else {
                            Text(stringResource(Res.string.aether_action_scan))
                        }
                    }
                    if (scanning) {
                        TextButton(onClick = { /* ViewModel cancellation would go here */ }) {
                            Text(stringResource(Res.string.action_cancel))
                        }
                    }
                }
                when (scanOutcome) {
                    ScanOutcome.Failed -> Text(
                        stringResource(Res.string.aether_scan_failed),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    ScanOutcome.Found -> Text(
                        stringResource(Res.string.aether_scan_success),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    null -> Unit
                }
            }

            dev.cluvex.zedsecure.ui.components.SectionTitle(stringResource(Res.string.aether_lab_other_settings))
            
            OutlinedTextField(
                value = dns,
                onValueChange = { dns = it },
                label = { Text(stringResource(Res.string.aether_lab_dns)) },
                placeholder = { Text(stringResource(Res.string.aether_hint_dns)) },
                singleLine = true,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = exitLoc,
                onValueChange = { exitLoc = it },
                label = { Text(stringResource(Res.string.aether_lab_exit_loc)) },
                placeholder = { Text(stringResource(Res.string.aether_hint_exit_loc)) },
                singleLine = true,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            PickerField(
                label = stringResource(Res.string.aether_lab_target_strategy),
                options = AetherProfile.targetStrategies.map { it to it },
                selected = targetStrategy,
                onSelect = { targetStrategy = it },
            )

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
                    supportingText = { Text(stringResource(Res.string.aether_command_custom)) },
                    minLines = 2,
                    maxLines = 6,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (aether != null) {
                FilledTonalButton(
                    onClick = { showIdentity = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.aether_action_renew_key))
                }
            }

            Button(
                onClick = {
                    val settings = build() ?: return@Button
                    saving = true
                    onSave(name, settings)
                },
                enabled = !saving,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = if (isDark) dev.cluvex.zedsecure.ui.theme.ZedGreen else androidx.compose.ui.graphics.Color(0xFF0D9488),
                    contentColor = androidx.compose.ui.graphics.Color.White,
                ),
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

    if (showIdentity && aether != null) {
        AetherIdentitySheet(actions = aether, onDismiss = { showIdentity = false })
    }
}

/** The WARP keys of the device: the identities they hold, and new keys on request. */
@Composable
private fun AetherIdentitySheet(actions: AetherActions, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var entries by remember { mutableStateOf<List<AetherKeyUiEntry>>(emptyList()) }
    var renewing by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<MessageOutcome?>(null) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        entries = actions.keys()
    }

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) androidx.compose.ui.graphics.Color(0xF20B1C38) else androidx.compose.ui.graphics.Color(0xF8F8FAFD),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            androidx.compose.foundation.layout.Box(
                Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 38.dp, height = 4.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(if (isDark) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f) else androidx.compose.ui.graphics.Color(0xFF64748B).copy(alpha = 0.4f)),
            )
        },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                stringResource(Res.string.aether_identity),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (isDark) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color(0xFF0F172A),
                modifier = Modifier.padding(vertical = 4.dp),
            )

            if (entries.isEmpty()) {
                Text(
                    stringResource(Res.string.aether_identity_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(Modifier.fillMaxWidth().height(220.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(entries, key = { it.file }) { entry ->
                        Column(Modifier.fillMaxWidth()) {
                            Text(entry.file, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            if (entry.deviceId != null) {
                                Text(
                                    "${entry.ipv4.orEmpty()}  ${entry.ipv6.orEmpty()}".trim(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            when (message) {
                MessageOutcome.Renewed -> Text(
                    stringResource(Res.string.aether_identity_renewed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )

                MessageOutcome.Failed -> Text(
                    stringResource(Res.string.aether_identity_failed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                null -> Unit
            }

            FilledTonalButton(
                onClick = {
                    renewing = true
                    message = null
                    scope.launch {
                        val ok = actions.registerKeys(AetherProfile.PROTO_WG)
                        renewing = false
                        message = if (ok) MessageOutcome.Renewed else MessageOutcome.Failed
                        if (ok) entries = actions.keys()
                    }
                },
                enabled = !renewing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (renewing) {
                    CircularProgressIndicator(Modifier.width(18.dp).height(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(Res.string.aether_identity_renewing))
                } else {
                    Text(stringResource(Res.string.aether_identity_renew))
                }
            }
        }
    }
}

private enum class ScanOutcome { Failed, Found }

private enum class MessageOutcome { Renewed, Failed }

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
