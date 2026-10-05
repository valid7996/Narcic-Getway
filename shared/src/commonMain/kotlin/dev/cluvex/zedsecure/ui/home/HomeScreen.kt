@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
)

package dev.cluvex.zedsecure.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.cluvex.zedsecure.data.config.ConfigRepository
import dev.cluvex.zedsecure.data.net.GeoLookup
import dev.cluvex.zedsecure.data.net.PingCoordinator
import dev.cluvex.zedsecure.data.net.PingService
import dev.cluvex.zedsecure.domain.model.ConnectButtonStyle
import dev.cluvex.zedsecure.domain.model.ConnectionState
import dev.cluvex.zedsecure.domain.config.VpnProfile
import dev.cluvex.zedsecure.shared.resources.Res
import dev.cluvex.zedsecure.shared.resources.*
import dev.cluvex.zedsecure.ui.connection.ConnectionViewModel
import dev.cluvex.zedsecure.ui.format.formatBytes
import dev.cluvex.zedsecure.ui.format.formatElapsed
import dev.cluvex.zedsecure.ui.format.formatRate
import dev.cluvex.zedsecure.ui.home.HomeViewModel
import dev.cluvex.zedsecure.ui.servers.AddServerSheet
import dev.cluvex.zedsecure.ui.servers.CardActionHosts
import dev.cluvex.zedsecure.ui.servers.SubscriptionsSheet
import dev.cluvex.zedsecure.ui.telemetry.Tel
import dev.cluvex.zedsecure.ui.telemetry.TelAction
import dev.cluvex.zedsecure.ui.telemetry.TelKV
import dev.cluvex.zedsecure.ui.telemetry.TelPanel
import dev.cluvex.zedsecure.ui.telemetry.TelStatusLine
import dev.cluvex.zedsecure.ui.telemetry.TelTabs
import dev.cluvex.zedsecure.ui.theme.Personalization
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * The telemetry console: LINK / THROUGHPUT / SYSTEM panels under OVERVIEW, the node table under
 * NODES. The public signature is identical to the previous home — only the presentation changed.
 */
@Composable
fun HomeScreen(
    connectionVm: ConnectionViewModel,
    activeConfigName: String?,
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
    activeConfigDetail: String? = null,
    exitGeneration: Int = 0,
    onToggleConnection: () -> Unit,
    onBrowseConfigs: () -> Unit,
    onSecretUnlocked: () -> Unit,
    modifier: Modifier = Modifier,
    lockedNote: String? = null,
    activeLocked: Boolean = false,
    activeCountryCode: String? = null,
    reduceMotion: Boolean = false,
    showConnectionInfo: Boolean = true,
    ipApiUrl: String = "",
    delayTestUrl: String = "",
    personalization: Personalization = Personalization.Default,
    connectStyle: ConnectButtonStyle = ConnectButtonStyle.Pill,
    homeVm: HomeViewModel = viewModel { HomeViewModel() },
    repository: ConfigRepository? = null,
    realPingConcurrency: Int = 4,
    autoSortAfterTest: Boolean = false,
    autoTestAfterUpdate: Boolean = false,
    autoRemoveInvalidAfterTest: Boolean = false,
    onOpenSpeedTest: () -> Unit = {},
    onOpenMap: () -> Unit = {},
) {
    val ui by connectionVm.ui.collectAsStateWithLifecycle()
    val info by homeVm.info.collectAsStateWithLifecycle()
    val live = ui.state == ConnectionState.Connected
    val platform = dev.cluvex.zedsecure.ui.platform.LocalPlatform.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(live, ui.sessionId, exitGeneration, showConnectionInfo, ipApiUrl, delayTestUrl) {
        homeVm.preferredIpApiUrl = ipApiUrl.ifBlank { null }
        homeVm.preferredDelayUrl = delayTestUrl.ifBlank { null }
        if (live && showConnectionInfo) homeVm.refresh(settleDelayMs = 300) else homeVm.clear()
    }

    var tab by rememberSaveable { mutableStateOf(0) }
    var group by remember { mutableStateOf("all") }
    var expandedId by remember { mutableStateOf<String?>(null) }
    var showLogs by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
    var showSubs by remember { mutableStateOf(false) }
    var overflowOpen by remember { mutableStateOf(false) }
    var updating by remember { mutableStateOf(false) }
    var confirmDeleteAll by remember { mutableStateOf(false) }
    var pingMenu by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<VpnProfile?>(null) }
    var renameTarget by remember { mutableStateOf<VpnProfile?>(null) }
    var moveTarget by remember { mutableStateOf<VpnProfile?>(null) }
    var brandTaps by remember { mutableIntStateOf(0) }
    var brandLastTap by remember { mutableLongStateOf(0L) }

    val profiles = repository?.profiles?.collectAsStateWithLifecycle()?.value.orEmpty()
    val activeId = repository?.activeId?.collectAsStateWithLifecycle()?.value
    val subscriptions = repository?.subscriptions?.collectAsStateWithLifecycle()?.value.orEmpty()
    val pingProgress by PingCoordinator.progress.collectAsStateWithLifecycle()
    val testing = pingProgress != null
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current

    val importedTemplate = stringResource(Res.string.servers_imported)
    val importFailed = stringResource(Res.string.config_invalid)
    val updatedTemplate = stringResource(Res.string.subs_updated)
    val removedTemplate = stringResource(Res.string.removed_count)

    val allServers = profiles.filterNot { it.isLocked }
    val manualCount = allServers.count { it.subscriptionId.isEmpty() }
    val servers = when (group) {
        "all" -> allServers
        "manual" -> allServers.filter { it.subscriptionId.isEmpty() }
        else -> allServers.filter { it.subscriptionId == group }
    }
    val subscriptionNames = subscriptions.associate { it.id to it.name }
    val active = allServers.firstOrNull { it.id == activeId }

    fun pingAll(useRealDelay: Boolean) {
        if (repository == null) return
        PingCoordinator.start(
            profiles = servers,
            useRealDelay = useRealDelay,
            concurrency = realPingConcurrency,
            delayUrl = delayTestUrl,
            chainConfig = { x -> repository.chainProbeConfig(x) },
            clearPings = { repository.clearPings(it, persist = false) },
            onResult = { id, ms, cc -> repository.setPing(id, ms, cc, persist = false) },
            flush = { repository.flushProfiles() },
            onFinished = { if (autoSortAfterTest) repository.sortByTestResults() },
        )
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            modifier
                .fillMaxSize()
                .background(Tel.bg)
                .padding(contentPadding),
        ) {
            // The command bar.
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "NARCIC",
                    style = Tel.mono.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                    color = Tel.text,
                    modifier = Modifier.clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                    ) {
                        val now = System.currentTimeMillis()
                        brandTaps = if (now - brandLastTap < 1200) brandTaps + 1 else 1
                        brandLastTap = now
                        if (brandTaps >= 5) {
                            brandTaps = 0
                            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onSecretUnlocked()
                        }
                    },
                )
                Text(
                    text = "GETWAY",
                    style = Tel.mono.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                    color = Tel.accent,
                )
                Spacer(Modifier.weight(1f))
                if (testing) {
                    Text(
                        text = "SCAN ${pingProgress?.done ?: 0}/${pingProgress?.total ?: 0}",
                        style = Tel.mono.copy(fontSize = 11.sp),
                        color = Tel.info,
                        modifier = Modifier.clickable { PingCoordinator.cancel() },
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "LOGS",
                    style = Tel.mono.copy(fontSize = 11.sp, letterSpacing = 1.sp),
                    color = Tel.dim,
                    modifier = Modifier.clickable { showLogs = true },
                )
            }
            HorizontalDivider(color = Tel.border)

            TelTabs(items = listOf("OVERVIEW", "NODES"), selected = tab, onSelect = { tab = it })

            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (tab == 0) {
                    TelPanel(
                        title = "LINK",
                        actions = {
                            if (live && showConnectionInfo) {
                                Text(
                                    text = "SPEED",
                                    style = Tel.mono.copy(fontSize = 11.sp, letterSpacing = 1.sp),
                                    color = Tel.accent,
                                    modifier = Modifier.clickable { onOpenSpeedTest() },
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "MAP",
                                    style = Tel.mono.copy(fontSize = 11.sp, letterSpacing = 1.sp),
                                    color = Tel.dim,
                                    modifier = Modifier.clickable { onOpenMap() },
                                )
                            }
                        },
                    ) {
                        TelStatusLine(ui.state)
                        TelKV("NODE", activeConfigName ?: "—")
                        if (activeConfigDetail != null) TelKV("ROUTE", activeConfigDetail)
                        TelKV(
                            label = "COUNTRY",
                            value = (activeCountryCode ?: info.countryCode ?: "—").uppercase(),
                        )
                        TelKV(
                            label = "SESSION",
                            value = if (live) formatElapsed(ui.elapsedSeconds) else "—",
                            valueColor = if (live) Tel.accent else Tel.text,
                        )
                        if (lockedNote != null) {
                            Text(
                                text = lockedNote,
                                style = Tel.mono.copy(fontSize = 11.sp),
                                color = Tel.dim,
                            )
                        }
                        if (ui.state == ConnectionState.Error && !ui.error.isNullOrBlank()) {
                            TelKV(label = "FAULT", value = ui.error.orEmpty().take(140), valueColor = Tel.error)
                        }
                        Spacer(Modifier.height(2.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TelAction(
                                text = when {
                                    ui.state == ConnectionState.Disconnecting -> "CLOSING"
                                    ui.state.isTransitioning -> "CANCEL"
                                    live -> "DISCONNECT"
                                    else -> "CONNECT"
                                },
                                onClick = onToggleConnection,
                                enabled = ui.state != ConnectionState.Disconnecting,
                                filled = !live,
                                modifier = Modifier.weight(1f),
                            )
                            if (live) {
                                TelAction(
                                    text = "STOP",
                                    onClick = onToggleConnection,
                                    danger = true,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }

                    TelPanel(title = "THROUGHPUT") {
                        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                            MetricBlock(
                                label = "DOWN",
                                value = formatRate(ui.downloadBps),
                                valueColor = if (ui.downloadBps > 0) Tel.accent else Tel.dim,
                            )
                            MetricBlock(
                                label = "UP",
                                value = formatRate(ui.uploadBps),
                                valueColor = if (ui.uploadBps > 0) Tel.info else Tel.dim,
                            )
                            MetricBlock(
                                label = "TOTAL",
                                value = formatBytes(ui.totalDownload + ui.totalUpload),
                                valueColor = Tel.text,
                            )
                        }
                        if (live) {
                            Text(
                                text = "SESSION ${formatElapsed(ui.elapsedSeconds)} · NODE ${activeConfigName ?: "—"}",
                                style = Tel.mono.copy(fontSize = 10.sp),
                                color = Tel.dim,
                            )
                        }
                    }

                    TelPanel(title = "SYSTEM") {
                        TelKV("ENGINE", active?.transportLabel ?: "—")
                        TelKV(
                            label = "ENDPOINT",
                            value = if (active?.address.isNullOrBlank()) "—" else "${active?.address}:${active?.port}",
                        )
                        TelKV(
                            label = "GROUP",
                            value = if (active?.subscriptionId.isNullOrBlank()) {
                                "MANUAL"
                            } else {
                                subscriptionNames[active?.subscriptionId] ?: "—"
                            },
                        )
                    }

                    Text(
                        text = "NODES ARE LISTED UNDER THE NODES TAB",
                        style = Tel.mono.copy(fontSize = 10.sp, letterSpacing = 1.sp),
                        color = Tel.dim,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                } else {
                    TelPanel(title = "GROUPS") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TelAction("ALL ${allServers.size}", { group = "all" }, filled = group == "all")
                            TelAction("MANUAL $manualCount", { group = "manual" }, filled = group == "manual")
                            subscriptions.forEach { sub ->
                                TelAction(
                                    text = "${sub.name} ${allServers.count { it.subscriptionId == sub.id }}",
                                    onClick = { group = sub.id },
                                    filled = group == sub.id,
                                )
                            }
                        }
                    }

                    TelPanel(
                        title = "NODES (${servers.size})",
                        actions = {
                            Box {
                                Text(
                                    text = if (testing) "STOP" else "PING",
                                    style = Tel.mono.copy(fontSize = 11.sp, letterSpacing = 1.sp),
                                    color = if (testing) Tel.error else Tel.accent,
                                    modifier = Modifier.clickable {
                                        if (testing) PingCoordinator.cancel() else pingMenu = true
                                    },
                                )
                                DropdownMenu(
                                    expanded = pingMenu,
                                    onDismissRequest = { pingMenu = false },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("PING ALL · TCP", style = Tel.mono.copy(fontSize = 12.sp), color = Tel.text) },
                                        onClick = { pingMenu = false; pingAll(useRealDelay = false) },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("PING ALL · REAL", style = Tel.mono.copy(fontSize = 12.sp), color = Tel.text) },
                                        onClick = { pingMenu = false; pingAll(useRealDelay = true) },
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Box {
                                Text(
                                    text = if (updating) "SYNC…" else "GEAR",
                                    style = Tel.mono.copy(fontSize = 11.sp),
                                    color = if (updating) Tel.info else Tel.dim,
                                    modifier = Modifier.clickable { overflowOpen = true },
                                )
                                DropdownMenu(
                                    expanded = overflowOpen,
                                    onDismissRequest = { overflowOpen = false },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("UPDATE ALL", style = Tel.mono.copy(fontSize = 12.sp), color = Tel.text) },
                                        onClick = {
                                            overflowOpen = false
                                            updating = true
                                            scope.launch(Dispatchers.Default) {
                                                val n = repository?.updateAllSubscriptions() ?: 0
                                                if (autoTestAfterUpdate && repository != null) {
                                                    val all = repository.profiles.value
                                                    repository.clearPings(all.map { it.id }, persist = false)
                                                    PingService.measureAll(
                                                        all,
                                                        useRealDelay = true,
                                                        concurrency = realPingConcurrency,
                                                        delayUrl = delayTestUrl,
                                                        chainConfig = { x -> repository.chainProbeConfig(x) },
                                                    ) { id, ms, cc ->
                                                        repository.setPing(id, ms, cc, persist = false)
                                                    }
                                                    repository.flushProfiles()
                                                    if (autoRemoveInvalidAfterTest) repository.removeInvalid()
                                                    if (autoSortAfterTest) repository.sortByTestResults()
                                                }
                                                updating = false
                                                platform.toast(updatedTemplate.format(n))
                                            }
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("SORT BY RESULT", style = Tel.mono.copy(fontSize = 12.sp), color = Tel.text) },
                                        onClick = { overflowOpen = false; repository?.sortByTestResults() },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("DELETE DUPLICATES", style = Tel.mono.copy(fontSize = 12.sp), color = Tel.text) },
                                        onClick = {
                                            overflowOpen = false
                                            val n = repository?.removeDuplicates() ?: 0
                                            platform.toast(removedTemplate.format(n))
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("DELETE INVALID", style = Tel.mono.copy(fontSize = 12.sp), color = Tel.error) },
                                        onClick = {
                                            overflowOpen = false
                                            val n = repository?.invalidCount() ?: 0
                                            repository?.removeInvalid()
                                            platform.toast(removedTemplate.format(n))
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("DELETE ALL", style = Tel.mono.copy(fontSize = 12.sp), color = Tel.error) },
                                        onClick = { overflowOpen = false; confirmDeleteAll = true },
                                    )
                                }
                            }
                        },
                    ) {
                        if (servers.isEmpty()) {
                            Text(
                                text = "NO NODES IN THIS GROUP",
                                style = Tel.mono.copy(fontSize = 11.sp),
                                color = Tel.dim,
                            )
                        } else {
                            Column {
                                servers.forEachIndexed { index, profile ->
                                    val isActive = profile.id == activeId
                                    val expanded = expandedId == profile.id
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                repository?.setActive(profile.id)
                                            }
                                            .padding(horizontal = 2.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = if (isActive) "►" else " ",
                                            style = Tel.mono.copy(fontSize = 12.sp),
                                            color = Tel.accent,
                                            modifier = Modifier.width(16.dp),
                                        )
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                text = profile.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isActive) Tel.text else Tel.text2,
                                                maxLines = 1,
                                            )
                                            Text(
                                                text = buildString {
                                                    append(if (profile.isCustom) "CUSTOM" else profile.transportLabel)
                                                    if (profile.address.isNotBlank() && profile.address != "-") {
                                                        append(" · ")
                                                        append(profile.address)
                                                        if (profile.port > 0) append(":${profile.port}")
                                                    }
                                                },
                                                style = Tel.mono.copy(fontSize = 10.sp),
                                                color = Tel.dim,
                                                maxLines = 1,
                                            )
                                        }
                                        profile.lastPingMs?.takeIf { it >= 0 }?.let { ping ->
                                            Text(
                                                text = "$ping ms",
                                                style = Tel.mono.copy(fontSize = 12.sp),
                                                color = when {
                                                    ping <= 200 -> Tel.accent
                                                    ping <= 600 -> Tel.warn
                                                    else -> Tel.error
                                                },
                                            )
                                        } ?: Text(
                                            text = "—",
                                            style = Tel.mono.copy(fontSize = 12.sp),
                                            color = Tel.dim,
                                        )
                                        Text(
                                            text = if (expanded) "[-]" else "[+]",
                                            style = Tel.mono.copy(fontSize = 11.sp),
                                            color = Tel.dim,
                                            modifier = Modifier
                                                .width(34.dp)
                                                .clickable { expandedId = if (expanded) null else profile.id }
                                                .padding(start = 8.dp),
                                        )
                                    }
                                    if (index < servers.lastIndex) {
                                        HorizontalDivider(color = Tel.divider)
                                    }
                                    if (expanded) {
                                        Column(
                                            Modifier.fillMaxWidth().background(Tel.panel2).padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            TelKV(
                                                label = "PROTO",
                                                value = if (profile.isCustom) "CUSTOM" else profile.transportLabel,
                                            )
                                            if (profile.address.isNotBlank() && profile.address != "-") {
                                                TelKV(label = "HOST", value = "${profile.address}:${profile.port}")
                                            }
                                            subscriptionNames[profile.subscriptionId]?.let { subName ->
                                                TelKV(label = "GROUP", value = subName)
                                            }
                                            if (profile.bytesDown > 0 || profile.bytesUp > 0) {
                                                TelKV(
                                                    label = "USAGE",
                                                    value = "↓ ${formatBytes(profile.bytesDown)} · ↑ ${formatBytes(profile.bytesUp)}",
                                                )
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                TelAction("USE", { repository?.setActive(profile.id) }, filled = !isActive)
                                                TelAction("EDIT", { editTarget = profile })
                                                TelAction("RENAME", { renameTarget = profile })
                                                TelAction("MOVE", { moveTarget = profile })
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                TelAction(
                                                    text = "PING TCP",
                                                    onClick = {
                                                        scope.launch(Dispatchers.Default) {
                                                            repository?.clearPings(listOf(profile.id), persist = false)
                                                            val ms = PingService.tcpPing(profile.address, profile.port)
                                                            val cc = if (ms > 0) GeoLookup.countryOf(profile.address) else null
                                                            repository?.setPing(
                                                                profile.id,
                                                                if (ms > 0) ms.toInt() else PingService.FAILED_PING,
                                                                cc,
                                                            )
                                                        }
                                                    },
                                                )
                                                TelAction(
                                                    text = "PING REAL",
                                                    onClick = {
                                                        scope.launch(Dispatchers.Default) {
                                                            repository?.clearPings(listOf(profile.id), persist = false)
                                                            val ms = PingService.realDelay(
                                                                profile,
                                                                url = delayTestUrl.ifBlank {
                                                                    dev.cluvex.zedsecure.data.net.NetworkInfoRepository.DELAY_TEST_URL
                                                                },
                                                            )
                                                            val cc = if (ms > 0) GeoLookup.countryOf(profile.address) else null
                                                            repository?.setPing(
                                                                profile.id,
                                                                if (ms > 0) ms.toInt() else PingService.FAILED_PING,
                                                                cc,
                                                            )
                                                        }
                                                    },
                                                )
                                                TelAction(
                                                    text = "DELETE",
                                                    onClick = {
                                                        repository?.remove(profile.id)
                                                        expandedId = null
                                                    },
                                                    danger = true,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "NARCIC GETWAY · TELEMETRY CONSOLE",
                    style = Tel.mono.copy(fontSize = 9.sp, letterSpacing = 1.5.sp),
                    color = Tel.dim,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                )
            }

            if (showLogs) LogSheet(onDismiss = { showLogs = false })
            if (showSubs && repository != null) {
                SubscriptionsSheet(
                    repository = repository,
                    onDismiss = { showSubs = false },
                )
            }
            if (showAdd) {
                AddServerSheet(
                    onDismiss = { showAdd = false },
                    onPasteLink = {
                        showAdd = false
                        val text = platform.readClipboard()
                        if (!text.isNullOrBlank()) {
                            scope.launch {
                                repository?.importText(text)
                                    ?.onSuccess { platform.toast(importedTemplate.format(it)) }
                                    ?.onFailure { platform.toast(importFailed) }
                            }
                        }
                    },
                    onImportFile = {},
                    onScanQr = {},
                    onScanQrImage = {},
                    onManual = {},
                    onCustom = {},
                    onPsiphon = {},
                    onDnsTunnel = {},
                    onMasterDns = {},
                    onOpenConnect = {},
                    onAether = {},
                    onIkev2 = {},
                    onTor = {},
                    onSsh = {},
                    onSniSpoof = {},
                    onProxyChain = {},
                    onCrossChain = {},
                    onSubscription = { showAdd = false; showSubs = true },
                )
            }
            if (confirmDeleteAll) {
                AlertDialog(
                    onDismissRequest = { confirmDeleteAll = false },
                    title = { Text("DELETE ALL", style = Tel.mono.copy(fontSize = 15.sp), color = Tel.error) },
                    text = { Text(stringResource(Res.string.del_all_confirm)) },
                    confirmButton = {
                        TextButton(onClick = {
                            confirmDeleteAll = false
                            val removed = allServers.size
                            repository?.removeAll { !it.isLocked }
                            platform.toast(removedTemplate.format(removed))
                        }) { Text(stringResource(Res.string.action_delete)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmDeleteAll = false }) {
                            Text(stringResource(Res.string.action_cancel))
                        }
                    },
                )
            }
            if (repository != null) {
                CardActionHosts(
                    repository = repository,
                    editTarget = editTarget,
                    renameTarget = renameTarget,
                    moveTarget = moveTarget,
                    onDismissEdit = { editTarget = null },
                    onDismissRename = { renameTarget = null },
                    onDismissMove = { moveTarget = null },
                )
            }
        }
    }
}

@Composable
private fun MetricBlock(label: String, value: String, valueColor: Color) {
    Column {
        Text(
            text = label,
            style = TextStyle(fontSize = 10.sp, letterSpacing = 1.5.sp),
            color = Tel.dim,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = value,
            style = Tel.mono.copy(fontSize = 19.sp, fontWeight = FontWeight.Bold),
            color = valueColor,
        )
    }
}
