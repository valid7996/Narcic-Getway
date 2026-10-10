@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package dev.cluvex.zedsecure.ui.servers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.cluvex.zedsecure.domain.config.AetherCommands
import dev.cluvex.zedsecure.domain.config.AetherProfile
import dev.cluvex.zedsecure.shared.resources.Res
import dev.cluvex.zedsecure.shared.resources.*
import dev.cluvex.zedsecure.ui.components.PickerField
import dev.cluvex.zedsecure.ui.components.SectionTitle
import dev.cluvex.zedsecure.ui.platform.LocalPlatform
import dev.cluvex.zedsecure.ui.theme.ZedGreen
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Full manual Aether editor, crafted faithfully after PattNG's ServerAetherActivity.
 * Features:
 * - Complete options suite (WARP over MASQUE, ECH, Fragmentation, Obfuscation, Psiphon CDN sets, Tor bridges).
 * - Live log panel with color-coded streaming output from the Rust core.
 * - Real-time endpoint scanning with cancel capability and output relay.
 * - WARP keys renewal dialog with Cloudflare API.
 * - Dual-theme support (Dark neon glassmorphism & soft light mode).
 */
@Composable
fun AetherSheet(
    initial: AetherProfile? = null,
    initialName: String = "",
    onDismiss: () -> Unit,
    onSave: (String, AetherProfile) -> Unit,
) {
    val platform = LocalPlatform.current
    val aether = LocalAetherActions.current
    val scope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    // Form state
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
    var targetStrategy by remember { mutableStateOf(initial?.targetStrategy.orEmpty()) }
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
    var psiphonBundledList by remember { mutableStateOf(initial?.psiphonBundledList ?: true) }
    var finalMask by remember { mutableStateOf(initial?.finalMask.orEmpty()) }
    var dialMode by remember { mutableStateOf(initial?.dialMode.orEmpty()) }
    var command by remember { mutableStateOf(initial?.command.orEmpty()) }
    var showOther by remember { mutableStateOf(dns.isNotBlank() || exitLoc.isNotBlank() || targetStrategy.isNotBlank()) }
    var showCdnSets by remember { mutableStateOf(psiphonCdnSets.isNotBlank()) }
    var showFinalMaskPresets by remember { mutableStateOf(false) }

    // Scanner state
    var isScanning by remember { mutableStateOf(false) }
    var scanStatusText by remember { mutableStateOf<String?>(null) }
    var scanSuccess by remember { mutableStateOf<Boolean?>(null) }

    // Live log entries
    val logEntries = remember { mutableStateListOf<AetherLogUiEntry>() }
    var nextLogId by remember { mutableStateOf(1L) }

    fun addLog(priority: Int, text: String) {
        logEntries.add(AetherLogUiEntry(nextLogId++, priority, text))
        if (logEntries.size > 500) logEntries.removeAt(0)
    }

    // Keys dialog
    var showKeysDialog by remember { mutableStateOf(false) }

    val twoHops = protocol == AetherProfile.PROTO_GOOL || protocol == AetherProfile.PROTO_MIM
    val overMasque = protocol == AetherProfile.PROTO_MASQUE || protocol == AetherProfile.PROTO_MIM || protocol == AetherProfile.PROTO_WG_OVER_MASQUE
    val warpUsed = psiphon != AetherProfile.CARRIER_ONLY && tor != AetherProfile.CARRIER_ONLY
    val overHttp2 = overMasque && transport == AetherProfile.TRANSPORT_H2

    fun buildProfile(): AetherProfile = AetherProfile(
        protocol = protocol,
        server = server.trim(),
        serverPort = port.trim().toIntOrNull() ?: 0,
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
        targetStrategy = targetStrategy.trim(),
        command = command.trim(),
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = if (isDark) Color(0xF20B1C38) else Color(0xF8F8FAFD),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 38.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF64748B).copy(alpha = 0.4f)),
            )
        },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Header
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(Res.string.aether_add_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A),
                    )
                    Text(
                        stringResource(Res.string.aether_intro),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) Color(0xFFA0B0C4) else Color(0xFF64748B),
                    )
                }
            }

            // Renew Keys button (PattNG top action)
            OutlinedButton(
                onClick = { showKeysDialog = true },
                enabled = !isScanning,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(46.dp),
            ) {
                Icon(painterResource(Res.drawable.ic_bolt), null, Modifier.size(18.dp), tint = if (isDark) ZedGreen else Color(0xFF0D9488))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.aether_action_renew_key), fontWeight = FontWeight.SemiBold)
            }

            // Remarks field
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(Res.string.manual_remark)) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            // Protocol & Tunnel shape
            if (warpUsed) {
                SectionTitle(stringResource(Res.string.aether_lab_protocol))

                PickerField(
                    label = stringResource(Res.string.aether_lab_protocol),
                    options = AetherProfile.protocols,
                    selected = protocol,
                    onSelect = { protocol = it },
                )

                if (overMasque) {
                    PickerField(
                        label = stringResource(Res.string.aether_lab_transport),
                        options = AetherProfile.transports,
                        selected = transport,
                        onSelect = { transport = it },
                    )
                }

                if (overHttp2) {
                    SettingToggleRow(
                        title = stringResource(Res.string.aether_lab_fragment),
                        checked = fragment,
                        onCheckedChange = { fragment = it },
                    )
                    if (fragment) {
                        OutlinedTextField(
                            value = fragmentSize,
                            onValueChange = { fragmentSize = it },
                            label = { Text(stringResource(Res.string.aether_lab_fragment_size)) },
                            placeholder = { Text("8-16") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = fragmentDelay,
                            onValueChange = { fragmentDelay = it },
                            label = { Text(stringResource(Res.string.aether_lab_fragment_delay)) },
                            placeholder = { Text("2-10") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                if (overMasque) {
                    SettingToggleRow(
                        title = stringResource(Res.string.aether_lab_ech),
                        
                        checked = ech,
                        onCheckedChange = { ech = it },
                    )
                    if (ech) {
                        PickerField(
                            label = stringResource(Res.string.aether_lab_ech_dns),
                            options = AetherProfile.defaultEchDnsOptions.map { it to it },
                            selected = echDns.ifBlank { AetherProfile.DEFAULT_ECH_DNS },
                            onSelect = { echDns = it },
                        )
                        PickerField(
                            label = stringResource(Res.string.aether_lab_ech_domain),
                            options = AetherProfile.defaultEchDomainOptions.map { it to it },
                            selected = echDomain.ifBlank { AetherProfile.DEFAULT_ECH_DOMAIN },
                            onSelect = { echDomain = it },
                        )
                    }
                }

                PickerField(
                    label = stringResource(Res.string.aether_lab_scan_mode),
                    options = AetherProfile.scanModes,
                    selected = scanMode,
                    onSelect = { scanMode = it },
                )

                if (!overHttp2) {
                    PickerField(
                        label = stringResource(Res.string.aether_lab_obfuscation),
                        options = AetherProfile.obfuscations,
                        selected = obfuscation,
                        onSelect = { obfuscation = it },
                    )
                }

                if (overMasque) {
                    PickerField(
                        label = stringResource(Res.string.aether_lab_fingerprint),
                        options = AetherProfile.fingerprints,
                        selected = fingerprint,
                        onSelect = { fingerprint = it },
                    )
                }
            }

            // Exit-Node Section
            SectionTitle(stringResource(Res.string.aether_final_mask))

            Box(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = finalMask,
                    onValueChange = { finalMask = it },
                    label = { Text(stringResource(Res.string.aether_final_mask)) },
                    minLines = 1,
                    maxLines = 4,
                    shape = RoundedCornerShape(16.dp),
                    trailingIcon = {
                        IconButton(onClick = { showFinalMaskPresets = true }) {
                            Icon(painterResource(Res.drawable.ic_keyboard_arrow_down), null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                DropdownMenu(
                    expanded = showFinalMaskPresets,
                    onDismissRequest = { showFinalMaskPresets = false },
                    shape = RoundedCornerShape(16.dp),
                ) {
                    AetherProfile.finalMaskPresets.forEach { (label, value) ->
                        DropdownMenuItem(
                            text = { Text(label, style = MaterialTheme.typography.bodyMedium) },
                            onClick = {
                                finalMask = value
                                showFinalMaskPresets = false
                            },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = dialMode,
                onValueChange = { dialMode = it },
                label = { Text(stringResource(Res.string.aether_dial_mode)) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            // Psiphon Carrier Section
            SectionTitle(stringResource(Res.string.aether_psiphon))
            PickerField(
                label = stringResource(Res.string.aether_psiphon),
                options = AetherProfile.carrierModes,
                selected = psiphon,
                onSelect = { psiphon = it },
            )
            if (psiphon != AetherProfile.CARRIER_OFF) {
                PickerField(
                    label = stringResource(Res.string.aether_psiphon_connection),
                    options = AetherProfile.psiphonModes,
                    selected = psiphonMode,
                    onSelect = { psiphonMode = it },
                )
                if (psiphonMode != AetherProfile.PSIPHON_MODE_DIRECT) {
                    OutlinedTextField(
                        value = psiphonCdnIps,
                        onValueChange = { psiphonCdnIps = it },
                        label = { Text(stringResource(Res.string.aether_psiphon_cdn_ips)) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = psiphonCdnSni,
                        onValueChange = { psiphonCdnSni = it },
                        label = { Text(stringResource(Res.string.aether_psiphon_cdn_sni)) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // CDN Sets collapsible section
                    SettingToggleRow(
                        title = stringResource(Res.string.aether_psiphon_cdn_sets),
                        
                        checked = showCdnSets,
                        onCheckedChange = { showCdnSets = it },
                    )
                    if (showCdnSets) {
                        val activeSets = psiphonCdnSets.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                AetherProfile.psiphonCdnSets.forEach { (key, label) ->
                                    val isSet = key in activeSets
                                    Row(
                                        Modifier.fillMaxWidth().clickable {
                                            val next = if (isSet) activeSets - key else activeSets + key
                                            psiphonCdnSets = next.joinToString(",")
                                        }.padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Checkbox(checked = isSet, onCheckedChange = null)
                                        Spacer(Modifier.width(10.dp))
                                        Text(label, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = psiphonRegion,
                    onValueChange = { psiphonRegion = it },
                    label = { Text("Psiphon Region (e.g. US, DE)") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )

                SettingToggleRow(
                    title = "Bundled Server List",
                    summary = "Use default built-in server list",
                    checked = psiphonBundledList,
                    onCheckedChange = { psiphonBundledList = it },
                )

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val ok = aether?.clearPsiphonData() ?: false
                            addLog(if (ok) 4 else 6, if (ok) "Psiphon cache cleared" else "Failed to clear Psiphon cache")
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Clear Psiphon Cache")
                }
            }

            // Tor Carrier Section
            SectionTitle(stringResource(Res.string.aether_tor))
            PickerField(
                label = stringResource(Res.string.aether_tor),
                options = AetherProfile.carrierModes,
                selected = tor,
                onSelect = { tor = it },
            )
            if (tor != AetherProfile.CARRIER_OFF) {
                PickerField(
                    label = stringResource(Res.string.aether_tor_bridges),
                    options = AetherProfile.torBridgeModes,
                    selected = torBridges,
                    onSelect = { torBridges = it },
                )
                if (torBridges == AetherProfile.TOR_BRIDGES_AUTO || torBridges == AetherProfile.TOR_BRIDGES_FIRST) {
                    PickerField(
                        label = stringResource(Res.string.aether_tor_relays),
                        options = AetherProfile.torRelayModes,
                        selected = torRelays,
                        onSelect = { torRelays = it },
                    )
                }
                if (torBridges == AetherProfile.TOR_BRIDGES_OWN) {
                    OutlinedTextField(
                        value = torBridgeLines,
                        onValueChange = { torBridgeLines = it },
                        label = { Text(stringResource(Res.string.aether_tor_bridge_lines)) },
                        placeholder = { Text("Enter bridge lines") },
                        minLines = 2,
                        maxLines = 6,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // Endpoint & Scanning Section
            if (warpUsed) {
                SectionTitle(stringResource(Res.string.aether_lab_endpoint))

                if (twoHops) {
                    OutlinedTextField(
                        value = wiwOuter,
                        onValueChange = { wiwOuter = it },
                        label = { Text(stringResource(Res.string.aether_lab_wiw_outer)) },
                        placeholder = { Text(stringResource(Res.string.aether_hint_endpoint)) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = wiwInner,
                        onValueChange = { wiwInner = it },
                        label = { Text(stringResource(Res.string.aether_lab_wiw_inner)) },
                        placeholder = { Text(stringResource(Res.string.aether_hint_endpoint)) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    OutlinedTextField(
                        value = server,
                        onValueChange = { server = it },
                        label = { Text(stringResource(Res.string.aether_lab_endpoint)) },
                        placeholder = { Text(stringResource(Res.string.aether_hint_endpoint)) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it.filter(Char::isDigit).take(5) },
                        label = { Text(stringResource(Res.string.aether_lab_port)) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                PickerField(
                    label = stringResource(Res.string.aether_lab_ip_version),
                    options = AetherProfile.ipVersions,
                    selected = ipVersion,
                    onSelect = { ipVersion = it },
                )

                // Scan Action Row
                Row(
                    Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = {
                            val current = buildProfile()
                            isScanning = true
                            scanSuccess = null
                            scanStatusText = null
                            addLog(4, "Scan started for ${current.protocol.uppercase()}...")
                            scope.launch {
                                val result = aether?.scan(current) { line ->
                                    val prio = when {
                                        line.startsWith("E/") || line.startsWith("Error:") -> 6
                                        line.startsWith("W/") || line.startsWith("Warn:") -> 5
                                        else -> 4
                                    }
                                    addLog(prio, line)
                                }
                                isScanning = false
                                if (result != null) {
                                    scanSuccess = true
                                    scanStatusText = "Endpoint found: ${result.endpoint}"
                                    addLog(4, "Endpoint found: ${result.endpoint}")
                                    if (twoHops && result.innerHop != null) {
                                        wiwOuter = result.endpoint
                                        wiwInner = result.innerHop
                                    } else {
                                        val text = result.endpoint
                                        val sep = text.lastIndexOf(':')
                                        if (sep > 0) {
                                            server = text.substring(0, sep).removeSurrounding("[", "]")
                                            port = text.substring(sep + 1)
                                        }
                                    }
                                } else {
                                    scanSuccess = false
                                    scanStatusText = "No endpoint found"
                                    addLog(5, "Scan finished: no endpoint found")
                                }
                            }
                        },
                        enabled = !isScanning,
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) ZedGreen else Color(0xFF0D9488),
                            contentColor = Color.White,
                        ),
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(Res.string.aether_action_scanning))
                        } else {
                            Icon(painterResource(Res.drawable.ic_bolt), null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(Res.string.aether_action_scan), fontWeight = FontWeight.Bold)
                        }
                    }

                    if (isScanning) {
                        OutlinedButton(
                            onClick = {
                                aether?.cancelScan()
                                isScanning = false
                                addLog(5, "Scan cancelled by user")
                            },
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.height(48.dp),
                        ) {
                            Text(stringResource(Res.string.action_cancel))
                        }
                    }
                }

                scanStatusText?.let { msg ->
                    Text(
                        msg,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (scanSuccess == true) (if (isDark) Color(0xFF60E0B0) else Color(0xFF059669)) else MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }

            // Other Settings Collapsible
            SettingToggleRow(
                title = "Other Settings",
                checked = showOther,
                onCheckedChange = { showOther = it },
            )
            if (showOther) {
                if (warpUsed) {
                    OutlinedTextField(
                        value = dns,
                        onValueChange = { dns = it },
                        label = { Text(stringResource(Res.string.aether_lab_dns)) },
                        placeholder = { Text("e.g. udp://1.1.1.1") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = exitLoc,
                        onValueChange = { exitLoc = it },
                        label = { Text(stringResource(Res.string.aether_lab_exit_location)) },
                        placeholder = { Text("e.g. DE") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                PickerField(
                    label = "Target Strategy",
                    options = AetherProfile.targetStrategies.map { it to it },
                    selected = targetStrategy.ifBlank { "AsIs" },
                    onSelect = { targetStrategy = it },
                )
            }

            // Custom command (Expert mode)
            val builtCmd = remember(protocol, server, port, scanMode, transport, obfuscation, ipVersion, psiphon, tor) {
                AetherCommands.buildArguments(buildProfile(), 10890).joinToString(" ")
            }
            val isCustom = command.isNotBlank() && command != builtCmd
            OutlinedTextField(
                value = command.ifBlank { builtCmd },
                onValueChange = { command = it },
                label = { Text(stringResource(Res.string.aether_lab_command)) },
                supportingText = if (isCustom) {
                    { Text(stringResource(Res.string.aether_command_custom), color = MaterialTheme.colorScheme.primary) }
                } else null,
                minLines = 2,
                maxLines = 6,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            if (isCustom) {
                TextButton(
                    onClick = { command = "" },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(stringResource(Res.string.aether_action_use_settings))
                }
            }

            // 🌟 PattNG AetherLogPanel (Live color-coded log panel!)
            SectionTitle(stringResource(Res.string.aether_log_title))
            AetherLiveLogPanel(
                entries = logEntries,
                onCopy = {
                    val full = logEntries.joinToString("\n") { it.text }
                    platform.copyToClipboard(full)
                    platform.toast("Log copied")
                },
                isDark = isDark,
            )

            Spacer(Modifier.height(8.dp))

            // Save button
            Button(
                onClick = {
                    onSave(name, buildProfile())
                },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) ZedGreen else Color(0xFF0D9488),
                    contentColor = Color.White,
                ),
                modifier = Modifier.fillMaxWidth().height(54.dp),
            ) {
                Icon(painterResource(Res.drawable.ic_bolt), null, Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(Res.string.action_save), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    // WARP Keys Renewal Dialog
    if (showKeysDialog && aether != null) {
        AetherKeysDialog(
            actions = aether,
            onDismiss = { showKeysDialog = false },
            onLog = { prio, line -> addLog(prio, line) },
            isDark = isDark,
        )
    }
}

/**
 * Live Log Panel matching PattNG: monospace, colored priority text, scrollable surface with copy button.
 */
@Composable
private fun AetherLiveLogPanel(
    entries: List<AetherLogUiEntry>,
    onCopy: () -> Unit,
    isDark: Boolean,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty()) listState.scrollToItem(entries.lastIndex)
    }

    val panelBorder = if (isDark) {
        Brush.horizontalGradient(listOf(Color(0xFF20D8C0).copy(alpha = 0.40f), Color(0xFF2088FF).copy(alpha = 0.25f)))
    } else {
        Brush.horizontalGradient(listOf(Color(0xFF0D9488).copy(alpha = 0.25f), Color(0xFF3B82F6).copy(alpha = 0.15f)))
    }
    val panelBg = if (isDark) Color(0xFF06101E) else Color(0xFFF1F5F9)

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(Res.string.aether_log_title),
                style = MaterialTheme.typography.titleSmall,
                color = if (isDark) Color(0xFFC0C0D0) else Color(0xFF475569),
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onCopy, enabled = entries.isNotEmpty()) {
                Icon(painterResource(Res.drawable.ic_content_paste), null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(Res.string.logs_copy), style = MaterialTheme.typography.labelSmall)
            }
        }

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = panelBg,
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .border(1.dp, panelBorder, RoundedCornerShape(18.dp)),
        ) {
            if (entries.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(14.dp), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(Res.string.aether_log_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                        fontFamily = FontFamily.Monospace,
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(entries, key = { it.id }) { entry ->
                        val textColor = when {
                            entry.priority >= 6 -> MaterialTheme.colorScheme.error
                            entry.priority == 5 -> Color(0xFFF59E0B)
                            entry.priority == 4 -> if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                            else -> if (isDark) Color(0xFFA0B0C4) else Color(0xFF475569)
                        }
                        Text(
                            text = entry.text,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = textColor,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dedicated WARP Keys Renewal Dialog.
 */
@Composable
private fun AetherKeysDialog(
    actions: AetherActions,
    onDismiss: () -> Unit,
    onLog: (Int, String) -> Unit,
    isDark: Boolean,
) {
    val scope = rememberCoroutineScope()
    var selectedKind by remember { mutableStateOf(AetherProfile.PROTO_WG) }
    var isRenewing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var keysList by remember { mutableStateOf<List<AetherKeyUiEntry>>(emptyList()) }

    LaunchedEffect(Unit) {
        keysList = actions.keys()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xF20B1C38) else Color(0xF8F8FAFD),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(Res.string.aether_action_renew_key),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color(0xFF0F172A),
            )

            // Current keys status
            if (keysList.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        keysList.forEach { entry ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(entry.file, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (entry.deviceId != null) "Ready (${entry.deviceId.take(8)})" else "Missing",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (entry.deviceId != null) (if (isDark) Color(0xFF60E0B0) else Color(0xFF059669)) else MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }

            PickerField(
                label = stringResource(Res.string.aether_lab_protocol),
                options = listOf(
                    AetherProfile.PROTO_WG to "WireGuard",
                    AetherProfile.PROTO_MASQUE to "MASQUE",
                ),
                selected = selectedKind,
                onSelect = { selectedKind = it },
            )

            Button(
                onClick = {
                    isRenewing = true
                    statusMessage = null
                    onLog(4, "Renewing WARP key for ${selectedKind.uppercase()}...")
                    scope.launch {
                        val ok = actions.registerKeys(selectedKind) { line ->
                            onLog(4, line)
                        }
                        isRenewing = false
                        keysList = actions.keys()
                        if (ok) {
                            statusMessage = "Key registered successfully"
                            onLog(4, "WARP registration finished successfully")
                        } else {
                            statusMessage = "Registration failed"
                            onLog(6, "WARP registration failed")
                        }
                    }
                },
                enabled = !isRenewing,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) ZedGreen else Color(0xFF0D9488),
                    contentColor = Color.White,
                ),
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) {
                if (isRenewing) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Registering with Cloudflare…")
                } else {
                    Text("Register Key Now", fontWeight = FontWeight.Bold)
                }
            }

            statusMessage?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) Color(0xFF60E0B0) else Color(0xFF059669),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    summary: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (!summary.isNullOrBlank()) {
                Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
