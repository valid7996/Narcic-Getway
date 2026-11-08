@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.valid7996.narcicgeteway.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.valid7996.narcicgeteway.domain.config.WhiteDnsConfig
import dev.valid7996.narcicgeteway.domain.config.WhiteDnsConfigException
import dev.valid7996.narcicgeteway.domain.config.WhiteDnsSettings
import dev.valid7996.narcicgeteway.domain.model.AppSettings
import dev.valid7996.narcicgeteway.shared.resources.Res
import dev.valid7996.narcicgeteway.shared.resources.*
import org.jetbrains.compose.resources.stringResource

private class NumSpec(
    val title: String,
    val default: String,
    val get: (WhiteDnsSettings) -> String,
    val set: (WhiteDnsSettings, String) -> WhiteDnsSettings,
)

private val BALANCE_SPECS = listOf(
    NumSpec("Upload packet duplication (1–30)", "3", { it.uploadDuplication }) { s, v -> s.copy(uploadDuplication = v) },
    NumSpec("Download packet duplication (1–30)", "7", { it.downloadDuplication }) { s, v -> s.copy(downloadDuplication = v) },
)

private val MTU_SPECS = listOf(
    NumSpec("Min upload MTU", "40", { it.minUploadMtu }) { s, v -> s.copy(minUploadMtu = v) },
    NumSpec("Max upload MTU", "140", { it.maxUploadMtu }) { s, v -> s.copy(maxUploadMtu = v) },
    NumSpec("Min download MTU", "300", { it.minDownloadMtu }) { s, v -> s.copy(minDownloadMtu = v) },
    NumSpec("Max download MTU", "3000", { it.maxDownloadMtu }) { s, v -> s.copy(maxDownloadMtu = v) },
    NumSpec("MTU test retries — resolvers", "3", { it.mtuTestRetriesResolvers }) { s, v -> s.copy(mtuTestRetriesResolvers = v) },
    NumSpec("MTU test timeout (s) — resolvers", "2.5", { it.mtuTestTimeoutResolvers }) { s, v -> s.copy(mtuTestTimeoutResolvers = v) },
    NumSpec("MTU test parallelism — resolvers", "100", { it.mtuTestParallelismResolvers }) { s, v -> s.copy(mtuTestParallelismResolvers = v) },
    NumSpec("MTU test retries — logs", "5", { it.mtuTestRetriesLogs }) { s, v -> s.copy(mtuTestRetriesLogs = v) },
    NumSpec("MTU test timeout (s) — logs", "2.5", { it.mtuTestTimeoutLogs }) { s, v -> s.copy(mtuTestTimeoutLogs = v) },
    NumSpec("MTU test parallelism — logs", "32", { it.mtuTestParallelismLogs }) { s, v -> s.copy(mtuTestParallelismLogs = v) },
)

private val WORKER_SPECS = listOf(
    NumSpec("RX/TX workers", "4", { it.rxTxWorkers }) { s, v -> s.copy(rxTxWorkers = v) },
    NumSpec("Tunnel process workers", "4", { it.tunnelProcessWorkers }) { s, v -> s.copy(tunnelProcessWorkers = v) },
    NumSpec("TX channel size", "2048", { it.txChannelSize }) { s, v -> s.copy(txChannelSize = v) },
    NumSpec("RX channel size", "2048", { it.rxChannelSize }) { s, v -> s.copy(rxChannelSize = v) },
    NumSpec("Resolver UDP connection pool", "64", { it.resolverUdpConnectionPoolSize }) { s, v -> s.copy(resolverUdpConnectionPoolSize = v) },
    NumSpec("Stream queue initial capacity", "128", { it.streamQueueInitialCapacity }) { s, v -> s.copy(streamQueueInitialCapacity = v) },
    NumSpec("Orphan queue initial capacity", "32", { it.orphanQueueInitialCapacity }) { s, v -> s.copy(orphanQueueInitialCapacity = v) },
    NumSpec("DNS response fragment store", "256", { it.dnsResponseFragmentStoreCapacity }) { s, v -> s.copy(dnsResponseFragmentStoreCapacity = v) },
    NumSpec("Max active streams", "2048", { it.maxActiveStreams }) { s, v -> s.copy(maxActiveStreams = v) },
)

private val TIMEOUT_SPECS = listOf(
    NumSpec("Tunnel packet timeout (s)", "10.0", { it.tunnelPacketTimeoutSeconds }) { s, v -> s.copy(tunnelPacketTimeoutSeconds = v) },
    NumSpec("Dispatcher idle poll (s)", "0.020", { it.dispatcherIdlePollIntervalSeconds }) { s, v -> s.copy(dispatcherIdlePollIntervalSeconds = v) },
    NumSpec("Local handshake timeout (s)", "5.0", { it.localHandshakeTimeoutSeconds }) { s, v -> s.copy(localHandshakeTimeoutSeconds = v) },
    NumSpec("SOCKS UDP associate read timeout (s)", "30.0", { it.socksUdpAssociateReadTimeoutSeconds }) { s, v ->
        s.copy(socksUdpAssociateReadTimeoutSeconds = v)
    },
    NumSpec("Terminal stream retention (s)", "45.0", { it.clientTerminalStreamRetentionSeconds }) { s, v ->
        s.copy(clientTerminalStreamRetentionSeconds = v)
    },
    NumSpec("Cancelled setup retention (s)", "120.0", { it.clientCancelledSetupRetentionSeconds }) { s, v ->
        s.copy(clientCancelledSetupRetentionSeconds = v)
    },
    NumSpec("Ping watchdog (s, 0 = off)", "300", { it.pingWatchdogSeconds }) { s, v -> s.copy(pingWatchdogSeconds = v) },
)

private val SESSION_SPECS = listOf(
    NumSpec("Retry base (s)", "1.0", { it.sessionInitRetryBaseSeconds }) { s, v -> s.copy(sessionInitRetryBaseSeconds = v) },
    NumSpec("Retry step (s)", "1.0", { it.sessionInitRetryStepSeconds }) { s, v -> s.copy(sessionInitRetryStepSeconds = v) },
    NumSpec("Linear retry after (attempts)", "5", { it.sessionInitRetryLinearAfter }) { s, v -> s.copy(sessionInitRetryLinearAfter = v) },
    NumSpec("Retry max (s)", "60.0", { it.sessionInitRetryMaxSeconds }) { s, v -> s.copy(sessionInitRetryMaxSeconds = v) },
    NumSpec("Busy retry interval (s)", "60.0", { it.sessionInitBusyRetryIntervalSeconds }) { s, v -> s.copy(sessionInitBusyRetryIntervalSeconds = v) },
)

@Composable
fun WhiteDnsSettingsPage(
    s: AppSettings,
    contentPadding: PaddingValues,
    modifier: Modifier,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
) {
    val w = s.whiteDns
    val change: ((WhiteDnsSettings) -> WhiteDnsSettings) -> Unit = { f -> onUpdate { it.copy(whiteDns = f(it.whiteDns)) } }
    var showConfig by remember { mutableStateOf(false) }
    var showLogs by remember { mutableStateOf(false) }

    SettingsPageScaffold(
        title = stringResource(Res.string.white_dns_settings_title),
        subtitle = stringResource(Res.string.white_dns_settings_sub),
        contentPadding = contentPadding,
        modifier = modifier,
    ) {
        SettingsGroup {
            SettingsInfoRow(
                stringResource(Res.string.white_dns_about_title),
                stringResource(Res.string.white_dns_about_body),
            )
        }

        SettingsGroup(stringResource(Res.string.white_dns_g_resolvers)) {
            SettingsEditRow(
                title = stringResource(Res.string.white_dns_default_resolvers),
                value = w.resolvers,
                placeholder = stringResource(Res.string.white_dns_default_resolvers_sum),
                onValueChanged = { v -> change { it.copy(resolvers = v) } },
            )
        }

        SettingsGroup(stringResource(Res.string.white_dns_g_balance)) {
            SettingsListRow(
                title = "Resolver balancing",
                options = WhiteDnsSettings.BALANCING,
                selected = w.balancingStrategy,
                onSelected = { v -> change { it.copy(balancingStrategy = v) } },
            )
            SettingsDivider(hasIcon = false)
            SettingsListRow(
                title = "Upload compression",
                options = WhiteDnsSettings.COMPRESSION,
                selected = w.uploadCompression,
                onSelected = { v -> change { it.copy(uploadCompression = v) } },
            )
            SettingsDivider(hasIcon = false)
            SettingsListRow(
                title = "Download compression",
                options = WhiteDnsSettings.COMPRESSION,
                selected = w.downloadCompression,
                onSelected = { v -> change { it.copy(downloadCompression = v) } },
            )
            SettingsDivider(hasIcon = false)
            SettingsSwitchRow(
                title = "Base-encode data",
                checked = w.baseEncodeData,
                onCheckedChange = { v -> change { it.copy(baseEncodeData = v) } },
            )
            NumRows(BALANCE_SPECS, w, change)
        }

        SettingsGroup(stringResource(Res.string.white_dns_g_mtu)) { NumRows(MTU_SPECS, w, change, first = true) }
        SettingsGroup(stringResource(Res.string.white_dns_g_workers)) { NumRows(WORKER_SPECS, w, change, first = true) }
        SettingsGroup(stringResource(Res.string.white_dns_g_timeouts)) { NumRows(TIMEOUT_SPECS, w, change, first = true) }
        SettingsGroup(stringResource(Res.string.white_dns_g_session)) { NumRows(SESSION_SPECS, w, change, first = true) }

        SettingsGroup(stringResource(Res.string.white_dns_g_startup)) {
            SettingsListRow(
                title = "Startup mode",
                options = listOf("resolvers" to "Full scan", "logs" to "From logs (fast)"),
                selected = w.resolve().startupMode,
                onSelected = { v -> change { it.copy(startupMode = v) } },
            )
            SettingsDivider(hasIcon = false)
            SettingsListRow(
                title = "Engine log level",
                options = WhiteDnsSettings.LOG_LEVELS.map { it to it },
                selected = WhiteDnsSettings.normalizeLogLevel(w.logLevel),
                onSelected = { v -> change { it.copy(logLevel = v) } },
            )
        }

        SettingsGroup(stringResource(Res.string.white_dns_g_config)) {
            SettingsActionRow(
                title = stringResource(Res.string.white_dns_edit_engine_config),
                summary = stringResource(Res.string.white_dns_edit_engine_config_sum),
                onClick = { showConfig = true },
            )
            SettingsDivider(hasIcon = false)
            SettingsActionRow(
                title = stringResource(Res.string.white_dns_open_logs),
                summary = stringResource(Res.string.white_dns_open_logs_sum),
                onClick = { showLogs = true },
            )
            SettingsDivider(hasIcon = false)
            SettingsActionRow(
                title = stringResource(Res.string.white_dns_reset),
                summary = stringResource(Res.string.white_dns_reset_sum),
                onClick = { change { WhiteDnsSettings(resolvers = it.resolvers) } },
            )
        }
    }

    if (showConfig) {
        WhiteDnsConfigSheet(
            current = w,
            onDismiss = { showConfig = false },
            onApply = { next -> change { next }; showConfig = false },
        )
    }
    if (showLogs) LogSheet(onDismiss = { showLogs = false }, startOnWhiteDns = true)
}

@Composable
private fun NumRows(
    specs: List<NumSpec>,
    w: WhiteDnsSettings,
    change: ((WhiteDnsSettings) -> WhiteDnsSettings) -> Unit,
    first: Boolean = false,
) {
    specs.forEachIndexed { i, spec ->
        if (i > 0 || !first) SettingsDivider(hasIcon = false)
        SettingsEditRow(
            title = spec.title,
            value = spec.get(w),
            placeholder = spec.default,
            onValueChanged = { v -> change { spec.set(it, v.trim()) } },
        )
    }
}

/** The shared settings as engine config text. What is applied lands in the same fields the page shows. */
@Composable
private fun WhiteDnsConfigSheet(
    current: WhiteDnsSettings,
    onDismiss: () -> Unit,
    onApply: (WhiteDnsSettings) -> Unit,
) {
    var text by remember { mutableStateOf(WhiteDnsConfig.renderSettingsToml(current)) }
    var error by remember { mutableStateOf<String?>(null) }
    var ignored by remember { mutableStateOf<List<String>>(emptyList()) }
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
                stringResource(Res.string.white_dns_edit_engine_config),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(Res.string.white_dns_settings_config_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = text,
                onValueChange = { text = it; error = null },
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                shape = RoundedCornerShape(12.dp),
                minLines = 14,
                isError = error != null,
                supportingText = {
                    val e = error
                    if (e != null) Text(stringResource(Res.string.white_dns_config_error, e))
                    else if (ignored.isNotEmpty()) Text(stringResource(Res.string.white_dns_ignored_keys, ignored.joinToString(", ")))
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(48.dp),
                ) { Text(stringResource(Res.string.action_cancel)) }
                Button(
                    onClick = {
                        try {
                            val applied = WhiteDnsConfig.applySettingsToml(current, text)
                            ignored = applied.ignoredKeys
                            onApply(applied.value)
                        } catch (e: WhiteDnsConfigException) {
                            error = e.message ?: ""
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF), contentColor = Color.White),
                    modifier = Modifier.weight(1f).height(48.dp),
                ) { Text(stringResource(Res.string.white_dns_config_apply)) }
            }
        }
    }
}
