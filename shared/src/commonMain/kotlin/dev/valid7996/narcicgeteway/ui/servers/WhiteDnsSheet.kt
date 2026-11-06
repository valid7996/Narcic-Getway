@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.valid7996.narcicgeteway.ui.servers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import dev.valid7996.narcicgeteway.domain.config.WhiteDnsProfile
import dev.valid7996.narcicgeteway.shared.resources.Res
import dev.valid7996.narcicgeteway.shared.resources.*
import dev.valid7996.narcicgeteway.ui.components.PickerField
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun WhiteDnsSheet(
    initial: WhiteDnsProfile? = null,
    initialName: String = "",
    onDismiss: () -> Unit,
    onSave: (name: String, WhiteDnsProfile) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var engine by remember { mutableStateOf(initial?.engine ?: WhiteDnsProfile.ENGINE_COTTENDNS) }
    var domain by remember { mutableStateOf(initial?.domain ?: "") }
    var key by remember { mutableStateOf(initial?.encryptionKey ?: "") }
    var method by remember { mutableStateOf(initial?.encryptionMethod ?: WhiteDnsProfile.ENC_XOR) }
    var tunnelMode by remember { mutableStateOf(initial?.tunnelMode ?: "normal") }
    var transportMode by remember { mutableStateOf(initial?.transportMode ?: "auto") }
    var useBundledResolvers by remember { mutableStateOf(initial?.useBundledResolvers ?: true) }
    var resolvers by remember { mutableStateOf(initial?.resolvers ?: "") }
    var advancedOpen by remember { mutableStateOf(false) }

    val valid = domain.isNotBlank() && key.isNotBlank() &&
        (useBundledResolvers || resolvers.split(',', '\n').any { it.trim().isNotEmpty() && !it.trim().startsWith("#") })

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
                stringResource(Res.string.whitedns_add_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            // Name
            Field(name, Res.string.manual_remark) { name = it }

            // Engine picker (CottenDNS, StormDNS, Soar)
            PickerField(
                label = stringResource(Res.string.whitedns_engine_label),
                selected = engine,
                options = listOf(
                    WhiteDnsProfile.ENGINE_COTTENDNS to "CottenDNS (CottenMax)",
                    WhiteDnsProfile.ENGINE_STORMDNS to "StormDNS",
                    WhiteDnsProfile.ENGINE_SOAR to "Soar (Lantern)",
                ),
                onSelect = { engine = it },
            )

            // Domain
            Field(
                value = domain,
                labelRes = Res.string.whitedns_domain_label,
                placeholder = stringResource(Res.string.whitedns_domain_hint),
            ) { domain = it }

            // Key
            Field(
                value = key,
                labelRes = Res.string.whitedns_key_label,
            ) { key = it }

            // Encryption Method (for CottenDNS and StormDNS)
            if (engine != WhiteDnsProfile.ENGINE_SOAR) {
                PickerField(
                    label = stringResource(Res.string.whitedns_method_label),
                    options = listOf(
                        WhiteDnsProfile.ENC_XOR to "1: XOR (Fastest)",
                        WhiteDnsProfile.ENC_CHACHA20 to "2: ChaCha20",
                        WhiteDnsProfile.ENC_AES_GCM to "3: AES-GCM",
                        WhiteDnsProfile.ENC_RC4 to "4: RC4",
                        WhiteDnsProfile.ENC_NONE to "0: None",
                    ),
                    selected = method,
                    onSelect = { method = it },
                )
            }

            // Bundled Resolvers switch (4,785 servers)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        stringResource(Res.string.whitedns_bundled_resolvers_label),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        stringResource(Res.string.whitedns_bundled_resolvers_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                    )
                }
                Switch(
                    checked = useBundledResolvers,
                    onCheckedChange = { useBundledResolvers = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF10B981)),
                )
            }

            // Custom resolvers (if bundled switch disabled)
            if (!useBundledResolvers) {
                OutlinedTextField(
                    value = resolvers,
                    onValueChange = { resolvers = it },
                    label = { Text(stringResource(Res.string.whitedns_resolvers_label)) },
                    placeholder = { Text(stringResource(Res.string.whitedns_resolvers_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6,
                )
            }

            // Advanced toggle button
            TextButton(
                onClick = { advancedOpen = !advancedOpen },
                modifier = Modifier.align(Alignment.Start),
            ) {
                Text(
                    if (advancedOpen) "- " + stringResource(Res.string.tile_advanced_title)
                    else "+ " + stringResource(Res.string.tile_advanced_title),
                )
            }

            AnimatedVisibility(advancedOpen) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (engine == WhiteDnsProfile.ENGINE_COTTENDNS) {
                        PickerField(
                            label = stringResource(Res.string.whitedns_tunnel_mode_label),
                            selected = if (tunnelMode == "cottenmax" || tunnelMode == "max") "cottenmax" else "legacy",
                            options = listOf(
                                "legacy" to stringResource(Res.string.whitedns_mode_normal),
                                "cottenmax" to stringResource(Res.string.whitedns_mode_max),
                            ),
                            onSelect = { tunnelMode = it },
                        )

                        PickerField(
                            label = stringResource(Res.string.whitedns_transport_mode_label),
                            selected = transportMode,
                            options = listOf(
                                "auto" to stringResource(Res.string.whitedns_transport_auto),
                                "udp" to stringResource(Res.string.whitedns_transport_udp),
                                "tcp" to stringResource(Res.string.whitedns_transport_tcp),
                            ),
                            onSelect = { transportMode = it },
                        )
                    }
                }
            }

            Button(
                onClick = {
                    val p = WhiteDnsProfile(
                        domain = domain.trim(),
                        encryptionKey = key.trim(),
                        encryptionMethod = method,
                        engine = engine,
                        tunnelMode = tunnelMode,
                        transportMode = transportMode,
                        useBundledResolvers = useBundledResolvers,
                        resolvers = resolvers.trim(),
                    )
                    onSave(name.trim(), p)
                },
                enabled = valid,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
            ) {
                Text(
                    stringResource(Res.string.action_save),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun Field(
    value: String,
    labelRes: StringResource,
    placeholder: String = "",
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(labelRes)) },
        placeholder = if (placeholder.isNotEmpty()) {
            { Text(placeholder) }
        } else null,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
}
