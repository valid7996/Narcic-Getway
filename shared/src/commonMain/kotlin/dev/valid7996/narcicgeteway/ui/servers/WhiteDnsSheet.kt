@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.valid7996.narcicgeteway.ui.servers

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.valid7996.narcicgeteway.domain.config.MasterDnsProfile
import dev.valid7996.narcicgeteway.domain.config.WhiteDnsConfig
import dev.valid7996.narcicgeteway.domain.config.WhiteDnsConfigException
import dev.valid7996.narcicgeteway.domain.config.WhiteDnsMaxOptions
import dev.valid7996.narcicgeteway.domain.config.WhiteDnsProfile
import dev.valid7996.narcicgeteway.shared.resources.Res
import dev.valid7996.narcicgeteway.shared.resources.*
import dev.valid7996.narcicgeteway.ui.components.PickerField
import org.jetbrains.compose.resources.stringResource

/** The WhiteDNS connection editor: a form, or the same connection as an editable config text. */
@Composable
fun WhiteDnsSheet(
    initial: WhiteDnsProfile? = null,
    initialName: String = "",
    onDismiss: () -> Unit,
    onSave: (name: String, WhiteDnsProfile) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var draft by remember { mutableStateOf(initial ?: WhiteDnsProfile()) }
    var configMode by remember { mutableStateOf(false) }
    var configText by remember { mutableStateOf("") }
    var configError by remember { mutableStateOf<String?>(null) }

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val cotten = draft.isCottenDns
    val maxShown = draft.usesCottenMax
    val badResolvers = MasterDnsProfile.invalidResolvers(draft.resolvers)

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
                stringResource(if (initial == null) Res.string.white_dns_add_title else Res.string.white_dns_edit_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            OutlinedField(name, stringResource(Res.string.manual_remark)) { name = it }

            if (configMode) {
                Text(
                    stringResource(Res.string.white_dns_config_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = configText,
                    onValueChange = { configText = it; configError = null },
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 12,
                    isError = configError != null,
                    supportingText = configError?.let { e -> { Text(stringResource(Res.string.white_dns_config_error, e)) } },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { configMode = false; configError = null },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) { Text(stringResource(Res.string.white_dns_edit_form)) }
                    Button(
                        onClick = {
                            try {
                                draft = WhiteDnsConfig.applyProfileToml(draft, configText).value
                                configMode = false
                                configError = null
                            } catch (e: WhiteDnsConfigException) {
                                configError = e.message ?: ""
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF), contentColor = Color.White),
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) { Text(stringResource(Res.string.white_dns_config_apply)) }
                }
            } else {
                PickerField(
                    label = stringResource(Res.string.white_dns_engine),
                    options = listOf(WhiteDnsProfile.ENGINE_STORM to "StormDNS", WhiteDnsProfile.ENGINE_COTTEN to "CottenDNS"),
                    selected = WhiteDnsProfile.normalizeEngine(draft.engine),
                    onSelect = { draft = draft.copy(engine = it) },
                )
                OutlinedField(draft.domains, stringResource(Res.string.white_dns_domains), placeholder = "v.example.com", minLines = 1) {
                    draft = draft.copy(domains = it)
                }
                OutlinedField(draft.encryptionKey, stringResource(Res.string.white_dns_key)) { draft = draft.copy(encryptionKey = it) }
                PickerField(
                    label = stringResource(Res.string.white_dns_method),
                    options = listOf(
                        0 to "None", 1 to "XOR", 2 to "ChaCha20", 3 to "AES-128-GCM", 4 to "AES-192-GCM", 5 to "AES-256-GCM",
                    ),
                    selected = draft.encryptionMethod,
                    onSelect = { draft = draft.copy(encryptionMethod = it) },
                )
                OutlinedField(
                    draft.resolvers,
                    stringResource(Res.string.white_dns_resolvers),
                    placeholder = "8.8.8.8\n1.1.1.1:53",
                    minLines = 2,
                    supporting = if (badResolvers.isNotEmpty()) {
                        stringResource(Res.string.master_dns_resolvers_skipped, badResolvers.joinToString(", "))
                    } else stringResource(Res.string.white_dns_resolvers_hint),
                ) { draft = draft.copy(resolvers = it) }

                if (cotten) {
                    GroupTitle(stringResource(Res.string.white_dns_group_cotten))
                    PickerField(
                        label = "Preset",
                        options = listOf(
                            "default" to "Default", "speed" to "Speed", "survival" to "Survival",
                            "tcp-survival" to "TCP survival", WhiteDnsProfile.PRESET_MASTER_STORM to "Master / Storm (legacy server)",
                        ),
                        selected = WhiteDnsProfile.normalizePreset(draft.configPreset),
                        onSelect = { draft = draft.copy(configPreset = it) },
                    )
                    if (!draft.isCompatibility) {
                        PickerField(
                            label = "Tunnel mode",
                            options = listOf("legacy" to "Legacy", "cottenmax" to "CottenMax"),
                            selected = WhiteDnsProfile.normalizeTunnelMode(draft.tunnelMode),
                            onSelect = { draft = draft.copy(tunnelMode = it) },
                        )
                        GroupTitle(stringResource(Res.string.white_dns_group_transport))
                        PickerField(
                            label = "Resolver transport",
                            options = WhiteDnsProfile.TRANSPORT_MODES.map { it to if (it == "preset") "From preset" else it.uppercase() },
                            selected = draft.transportMode,
                            onSelect = { draft = draft.copy(transportMode = it) },
                        )
                        PickerField(
                            label = "Record types",
                            options = listOf(
                                "preset" to "From preset", "txt" to "TXT", "txt-cname" to "TXT + CNAME",
                                "txt-https" to "TXT + HTTPS", "txt-aaaa" to "TXT + AAAA", "all" to "All",
                            ),
                            selected = draft.deliveryMode,
                            onSelect = { draft = draft.copy(deliveryMode = it) },
                        )
                        PickerField(
                            label = "Query name shaping",
                            options = listOf("preset" to "From preset", "off" to "Off", "moderate" to "Moderate", "aggressive" to "Aggressive"),
                            selected = draft.qnameMode,
                            onSelect = { draft = draft.copy(qnameMode = it) },
                        )
                        PickerField(
                            label = "IP family",
                            options = listOf(
                                "preset" to "From preset", "auto" to "Auto (IPv4, IPv6 fallback)", "dual" to "IPv4 + IPv6",
                                "ipv4" to "IPv4 only", "ipv6" to "IPv6 only",
                            ),
                            selected = draft.ipMode,
                            onSelect = { draft = draft.copy(ipMode = it) },
                        )
                    }
                    GroupTitle(stringResource(Res.string.white_dns_group_rate))
                    PickerField(
                        label = "Query rate limit (per second)",
                        options = WhiteDnsProfile.RATE_LIMITS.map { it to if (it == "off") "Off" else it },
                        selected = draft.queryRateLimit,
                        onSelect = { draft = draft.copy(queryRateLimit = it) },
                    )
                    PickerField(
                        label = "Rate limit counts",
                        options = listOf("total" to "All queries", "resolver" to "Per resolver", "domain" to "Per domain"),
                        selected = draft.queryRateScope,
                        onSelect = { draft = draft.copy(queryRateScope = it) },
                    )
                    PickerField(
                        label = "Timing mask",
                        options = listOf("off" to "Off", "light" to "Light", "strong" to "Strong"),
                        selected = draft.timingMask,
                        onSelect = { draft = draft.copy(timingMask = it) },
                    )
                    NumField("Background scan parallelism", draft.backgroundScanParallelism) {
                        draft = draft.copy(backgroundScanParallelism = it)
                    }

                    val enc = draft.transportMode in setOf("dot", "doh", "doh3", "doq")
                    if (enc && !draft.isCompatibility) {
                        GroupTitle(stringResource(Res.string.white_dns_group_tls))
                        OutlinedField(draft.resolverTlsServerName, "TLS server name") { draft = draft.copy(resolverTlsServerName = it) }
                        OutlinedField(draft.resolverTlsPin, "TLS pin") { draft = draft.copy(resolverTlsPin = it) }
                        OutlinedField(draft.resolverDotPort, "DoT / DoQ port", numeric = true) { draft = draft.copy(resolverDotPort = it) }
                        OutlinedField(draft.resolverDohPort, "DoH port", numeric = true) { draft = draft.copy(resolverDohPort = it) }
                        OutlinedField(draft.resolverDohPath, "DoH path") { draft = draft.copy(resolverDohPath = it) }
                    }

                    if (maxShown) {
                        GroupTitle(stringResource(Res.string.white_dns_group_max))
                        MaxOptionsForm(draft.max) { draft = draft.copy(max = it) }
                    }
                }

                GroupTitle(stringResource(Res.string.white_dns_group_extra))
                OutlinedField(
                    draft.extraToml,
                    "KEY = value",
                    placeholder = "RESOLVER_DOH_PATH = \"/dns-query\"",
                    minLines = 3,
                    supporting = stringResource(Res.string.white_dns_extra_hint),
                    mono = true,
                ) { draft = draft.copy(extraToml = it) }

                OutlinedButton(
                    onClick = {
                        configText = WhiteDnsConfig.renderProfileToml(draft)
                        configError = null
                        configMode = true
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) { Text(stringResource(Res.string.white_dns_edit_config)) }

                Button(
                    onClick = { onSave(name, draft.normalized()) },
                    enabled = draft.isValid,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF), contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                ) { Text(stringResource(Res.string.action_save), fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@Composable
private fun MaxOptionsForm(o: WhiteDnsMaxOptions, onChange: (WhiteDnsMaxOptions) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PickerField(
            label = "Traffic profile",
            options = listOf("full" to "Full", "low" to "Low"),
            selected = o.trafficProfile,
            onSelect = { onChange(o.copy(trafficProfile = it)) },
        )
        NumField("Low-traffic reduction %", o.lowReductionPercent) { onChange(o.copy(lowReductionPercent = it)) }
        OutlinedField(o.standbyDomains, "Standby domains", minLines = 1) { onChange(o.copy(standbyDomains = it)) }
        NumField("Failover after (ms)", o.failoverAfterMs) { onChange(o.copy(failoverAfterMs = it)) }
        NumField("Safe QNAME length", o.safeQName) { onChange(o.copy(safeQName = it)) }
        NumField("Max QNAME length", o.maxQName) { onChange(o.copy(maxQName = it)) }
        NumField("Max answer size", o.maxAnswer) { onChange(o.copy(maxAnswer = it)) }
        NumField("Max path window", o.maxPathWindow) { onChange(o.copy(maxPathWindow = it)) }
        NumField("Max in-flight", o.maxInflight) { onChange(o.copy(maxInflight = it)) }
        ToggleRow("FEC on download", o.fecDownloadEnabled) { onChange(o.copy(fecDownloadEnabled = it)) }
        if (o.fecDownloadEnabled) {
            NumField("FEC data shards", o.fecDataShards) { onChange(o.copy(fecDataShards = it)) }
            NumField("FEC parity shards", o.fecParityShards) { onChange(o.copy(fecParityShards = it)) }
        }
        NumField("Session idle (ms)", o.sessionIdleMs) { onChange(o.copy(sessionIdleMs = it)) }
        NumField("Hold (ms)", o.holdMs) { onChange(o.copy(holdMs = it)) }
        ToggleRow("Label-count jitter", o.labelCountJitter) { onChange(o.copy(labelCountJitter = it)) }
        ToggleRow("QNAME loss guard", o.qnameLossGuard) { onChange(o.copy(qnameLossGuard = it)) }
        ToggleRow("DoH3 falls back to DoH", o.doh3FallbackToDoh) { onChange(o.copy(doh3FallbackToDoh = it)) }
        NumField("DNS padding block size (0 = off)", o.dnsPaddingBlockSize) { onChange(o.copy(dnsPaddingBlockSize = it)) }
        ToggleRow("Resolver discovery", o.discoveryEnabled) { onChange(o.copy(discoveryEnabled = it)) }
        if (o.discoveryEnabled) {
            OutlinedField(o.discoveryCountry, "Country code (2 letters)") { onChange(o.copy(discoveryCountry = it)) }
            NumField("Sample ranges", o.discoverySampleRanges) { onChange(o.copy(discoverySampleRanges = it)) }
            NumField("Resolvers wanted", o.discoveryWant) { onChange(o.copy(discoveryWant = it)) }
            NumField("Concurrency", o.discoveryConcurrency) { onChange(o.copy(discoveryConcurrency = it)) }
            NumField("Pace (ms)", o.discoveryPaceMs) { onChange(o.copy(discoveryPaceMs = it)) }
            ToggleRow("Cache discovered resolvers", o.discoveryCacheEnabled) { onChange(o.copy(discoveryCacheEnabled = it)) }
            if (o.discoveryCacheEnabled) {
                OutlinedField(o.discoveryNetworkId, "Network id") { onChange(o.copy(discoveryNetworkId = it)) }
            }
            OutlinedField(o.discoveryCandidates, "Candidate resolvers", minLines = 2) { onChange(o.copy(discoveryCandidates = it)) }
        }
    }
}

@Composable
private fun GroupTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 6.dp),
    )
}

@Composable
private fun OutlinedField(
    value: String,
    label: String,
    placeholder: String? = null,
    minLines: Int = 1,
    numeric: Boolean = false,
    supporting: String? = null,
    mono: Boolean = false,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(if (numeric) it.filter { c -> c.isDigit() } else it) },
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        singleLine = minLines == 1,
        minLines = minLines,
        shape = RoundedCornerShape(12.dp),
        textStyle = if (mono) MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
        else MaterialTheme.typography.bodyLarge,
        supportingText = supporting?.let { { Text(it) } },
        modifier = Modifier.fillMaxWidth(),
    )
}

/** A whole-number field. The text is kept while it is being typed; a value is passed on only once it parses. */
@Composable
private fun NumField(label: String, value: Int, onChange: (Int) -> Unit) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = { raw ->
            val digits = raw.filter { it.isDigit() }.take(9)
            text = digits
            digits.toIntOrNull()?.let(onChange)
        },
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
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
