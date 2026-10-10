@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.cluvex.zedsecure.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.cluvex.zedsecure.core.AutoSelect
import dev.cluvex.zedsecure.data.config.ConfigRepository
import dev.cluvex.zedsecure.data.net.GeoLookup
import dev.cluvex.zedsecure.data.net.PingCoordinator
import dev.cluvex.zedsecure.data.net.PingService
import dev.cluvex.zedsecure.domain.config.AutoSelectIds
import dev.cluvex.zedsecure.domain.config.VpnProfile
import dev.cluvex.zedsecure.shared.resources.Res
import dev.cluvex.zedsecure.shared.resources.*
import dev.cluvex.zedsecure.ui.servers.AddServerSheet
import dev.cluvex.zedsecure.ui.servers.AutoSelectCard
import dev.cluvex.zedsecure.ui.servers.AutoSelectSheet
import dev.cluvex.zedsecure.ui.servers.PsiphonSheet
import dev.cluvex.zedsecure.ui.servers.ServersUiConfig
import dev.cluvex.zedsecure.ui.servers.ServerCard
import dev.cluvex.zedsecure.ui.servers.AetherSheet
import dev.cluvex.zedsecure.ui.servers.ProxyChainSheet
import dev.cluvex.zedsecure.ui.servers.CrossChainSheet
import dev.cluvex.zedsecure.ui.components.QrDialog
import dev.cluvex.zedsecure.ui.servers.SubscriptionsSheet
import dev.cluvex.zedsecure.ui.theme.Personalization
import dev.cluvex.zedsecure.ui.theme.ZedGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * The servers under the connect control of the home screen: the group tabs of the servers screen —
 * every one, the manual servers, and each subscription — then the header with add, ping-all and the
 * overflow actions, then the same cards the servers screen keeps.
 */
@Composable
internal fun HomeServerSection(
    repository: ConfigRepository,
    personalization: Personalization,
    realPingConcurrency: Int,
    delayTestUrl: String,
    autoSortAfterTest: Boolean,
    autoTestAfterUpdate: Boolean,
    autoRemoveInvalidAfterTest: Boolean,
    modifier: Modifier = Modifier,
) {
    val platform = dev.cluvex.zedsecure.ui.platform.LocalPlatform.current
    val scope = rememberCoroutineScope()
    val profiles by repository.profiles.collectAsStateWithLifecycle()
    val activeId by repository.activeId.collectAsStateWithLifecycle()
    val subscriptions by repository.subscriptions.collectAsStateWithLifecycle()
    val pingProgress by PingCoordinator.progress.collectAsStateWithLifecycle()
    val testing = pingProgress != null

    val importedTemplate = stringResource(Res.string.servers_imported)
    val importFailed = stringResource(Res.string.config_invalid)
    val updatedTemplate = stringResource(Res.string.subs_updated)
    val removedTemplate = stringResource(Res.string.removed_count)
    val copiedToast = stringResource(Res.string.copied)

    // The group tabs: every server, the manual ones, then one per subscription.
    var group by remember { mutableStateOf("all") }
    var showAdd by remember { mutableStateOf(false) }
    var showSubs by remember { mutableStateOf(false) }
    var pingMenu by remember { mutableStateOf(false) }
    var overflowOpen by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<VpnProfile?>(null) }
    var renameTarget by remember { mutableStateOf<VpnProfile?>(null) }
    var moveTarget by remember { mutableStateOf<VpnProfile?>(null) }
    var updating by remember { mutableStateOf(false) }
    var confirmDeleteAll by remember { mutableStateOf(false) }
    var showAutoSheet by remember { mutableStateOf(false) }
    var showPsiphon by remember { mutableStateOf(false) }
    var showAether by remember { mutableStateOf(false) }
    var showProxyChain by remember { mutableStateOf(false) }
    var showCrossChain by remember { mutableStateOf(false) }
    var qrTarget by remember { mutableStateOf<Pair<String, String>?>(null) }
    val lockedCannotShare = stringResource(Res.string.locked_cannot_share)

    val allServers = profiles.filterNot { it.isLocked }
    val manualCount = allServers.count { it.subscriptionId.isEmpty() }
    val servers = when (group) {
        "all" -> allServers
        "manual" -> allServers.filter { it.subscriptionId.isEmpty() }
        else -> allServers.filter { it.subscriptionId == group }
    }
    val subscriptionNames = subscriptions.associate { it.id to it.name }

    val chainCandidates = allServers.filter {
        it.rawPayload() != null && (!it.isCustom || it.isServerless) && !it.isManagedTunnel && !it.isProxyChain &&
            !it.isSingBoxConfig
    }
    val crossCarriers = allServers.filter { it.canCarryChain && !it.isCrossChain }
    val crossExits = allServers.filter { it.canDialThroughProxy && !it.isCrossChain }
    val udpMismatchTemplate = stringResource(Res.string.crosschain_udp_unsupported)
    val crossPairError: (VpnProfile, VpnProfile) -> String? = { inner, outer ->
        repository.udpMismatch(inner, outer)?.let {
            udpMismatchTemplate.replace("%1\$s", it.protocol).replace("%2\$s", it.carrier)
        }
    }

    // The auto card: the group's best config comes up on its own.
    val autoScope: String? = when (group) {
        "all" -> null
        "manual" -> ""
        else -> group
    }
    val autoId = AutoSelectIds.of(autoScope)
    val autoMembers = remember(profiles, autoScope) { repository.autoSelectMembers(autoScope) }
    val autoSession by AutoSelect.session.collectAsStateWithLifecycle()
    val autoLive = autoSession?.takeIf { it.profileId == autoId }
    val autoLabel = when (autoScope) {
        null -> stringResource(Res.string.auto_group_all)
        "" -> stringResource(Res.string.group_manual)
        else -> subscriptions.firstOrNull { it.id == autoScope }?.name.orEmpty()
    }

    fun pingAll(useRealDelay: Boolean) {
        PingCoordinator.start(
            profiles = servers,
            useRealDelay = useRealDelay,
            concurrency = realPingConcurrency,
            delayUrl = delayTestUrl,
            chainConfig = repository::chainProbeConfig,
            clearPings = { repository.clearPings(it, persist = false) },
            onResult = { id, ms, cc -> repository.setPing(id, ms, cc, persist = false) },
            flush = { repository.flushProfiles() },
            onFinished = { if (autoSortAfterTest) repository.sortByTestResults() },
        )
    }

    Column(modifier.fillMaxWidth()) {
        // Group tabs: همه · دستی · each subscription
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GroupTab(label = stringResource(Res.string.group_all), count = allServers.size, selected = group == "all") { group = "all" }
            GroupTab(label = stringResource(Res.string.group_manual), count = manualCount, selected = group == "manual") { group = "manual" }
            subscriptions.forEach { sub ->
                GroupTab(
                    label = sub.name,
                    count = allServers.count { it.subscriptionId == sub.id },
                    selected = group == sub.id,
                ) { group = sub.id }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(Res.string.nav_servers),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            if (testing) {
                GlassIconButton(onClick = { PingCoordinator.cancel() }) {
                    Icon(
                        painterResource(Res.drawable.ic_close),
                        contentDescription = stringResource(Res.string.ping_stop),
                    )
                }
            } else {
                Box {
                    GlassIconButton(onClick = { pingMenu = true }) {
                        Icon(
                            painterResource(Res.drawable.ic_speed),
                            contentDescription = stringResource(Res.string.ping_test),
                        )
                    }
                    DropdownMenu(
                        expanded = pingMenu,
                        onDismissRequest = { pingMenu = false },
                        shape = MaterialTheme.shapes.largeIncreased,
                    ) {
                        if (ServersUiConfig.SHOW_TCP_PING) DropdownMenuItem(
                            text = { Text(stringResource(Res.string.ping_tcp_all)) },
                            onClick = { pingMenu = false; pingAll(useRealDelay = false) },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.ping_real_all)) },
                            onClick = { pingMenu = false; pingAll(useRealDelay = true) },
                        )
                    }
                }
            }
            Box {
                GlassIconButton(onClick = { overflowOpen = true }) {
                    Icon(
                        painterResource(Res.drawable.ic_more_vert),
                        contentDescription = stringResource(Res.string.servers_actions),
                    )
                }
                DropdownMenu(
                    expanded = overflowOpen,
                    onDismissRequest = { overflowOpen = false },
                    shape = MaterialTheme.shapes.largeIncreased,
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.subs_update_all)) },
                        leadingIcon = { Icon(painterResource(Res.drawable.ic_schedule), null) },
                        enabled = subscriptions.isNotEmpty() && !updating,
                        onClick = {
                            overflowOpen = false
                            updating = true
                            scope.launch(Dispatchers.Default) {
                                val n = repository.updateAllSubscriptions()
                                if (autoTestAfterUpdate) {
                                    val all = repository.profiles.value
                                    repository.clearPings(all.map { it.id }, persist = false)
                                    PingService.measureAll(
                                        all,
                                        useRealDelay = true,
                                        concurrency = realPingConcurrency,
                                        delayUrl = delayTestUrl,
                                        chainConfig = repository::chainProbeConfig,
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
                        text = { Text(stringResource(Res.string.sort_by_results)) },
                        leadingIcon = { Icon(painterResource(Res.drawable.ic_tune), null) },
                        onClick = {
                            overflowOpen = false
                            repository.sortByTestResults()
                        },
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.del_duplicates)) },
                        leadingIcon = { Icon(painterResource(Res.drawable.ic_delete), null) },
                        onClick = {
                            overflowOpen = false
                            val n = repository.removeDuplicates()
                            platform.toast(removedTemplate.format(n))
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.del_invalid)) },
                        leadingIcon = { Icon(painterResource(Res.drawable.ic_delete), null) },
                        onClick = {
                            overflowOpen = false
                            val n = repository.invalidCount()
                            repository.removeInvalid()
                            platform.toast(removedTemplate.format(n))
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.del_all)) },
                        leadingIcon = { Icon(painterResource(Res.drawable.ic_delete), null) },
                        onClick = {
                            overflowOpen = false
                            confirmDeleteAll = true
                        },
                    )
                }
            }
            GlassIconButton(onClick = { showAdd = true }) {
                Icon(
                    painterResource(Res.drawable.ic_add),
                    contentDescription = stringResource(Res.string.servers_add),
                )
            }
        }

        if (autoMembers.size >= 2) {
            AutoSelectCard(
                groupLabel = autoLabel,
                memberCount = autoMembers.size,
                active = activeId == autoId,
                live = autoLive,
                memberName = { id -> profiles.firstOrNull { it.id == id }?.name },
                onSelect = { repository.setActive(autoId) },
                onDetails = { showAutoSheet = true },
                personalization = personalization,
            )
            Spacer(Modifier.height(10.dp))
        }

        if (servers.isNotEmpty()) {
            // A bounded, scrollable column: the cards are plain children, so the section keeps
            // the home layout's exact sizing while long lists scroll inside their own box.
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                servers.forEach { profile ->
                    ServerCard(
                        profile = profile,
                        active = profile.id == activeId,
                        subscriptionName = subscriptionNames[profile.subscriptionId],
                        personalization = personalization,
                        onClick = { repository.setActive(profile.id) },
                        shareLink = runCatching { repository.shareLinkOf(profile) }.getOrNull(),
                        onShare = {
                            val pl = runCatching { repository.shareLinkOf(profile) }.getOrNull()
                            if (pl == null) platform.toast(lockedCannotShare) else platform.shareText(pl)
                        },
                        onShareQr = {
                            qrTarget = runCatching { repository.shareLinkOf(profile) }.getOrNull()
                                ?.let { profile.name to it }
                        },
                        onPingTcp = {
                            scope.launch(Dispatchers.Default) {
                                pingOne(repository, profile, delayTestUrl, real = false)
                            }
                        },
                        onPingReal = {
                            scope.launch(Dispatchers.Default) {
                                pingOne(repository, profile, delayTestUrl, real = true)
                            }
                        },
                        onRename = { renameTarget = profile },
                        onEdit = { editTarget = profile },
                        onMoveGroup = { moveTarget = profile },
                        onDelete = { repository.remove(profile.id) },
                    )
                }
            }
        }
    }

    if (showAutoSheet) {
        AutoSelectSheet(
            groupLabel = autoLabel,
            members = autoMembers,
            live = autoLive,
            onDismiss = { showAutoSheet = false },
        )
    }

    if (showPsiphon) {
        PsiphonSheet(
            onDismiss = { showPsiphon = false },
            onSave = { name, settings ->
                showPsiphon = false
                repository.addPsiphon(settings, name)
            },
        )
    }

    if (showSubs) {
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
                        repository.importText(text)
                            .onSuccess { platform.toast(importedTemplate.format(it)) }
                            .onFailure { platform.toast(importFailed) }
                    }
                }
            },
            onImportFile = {},
            onScanQr = {
                showAdd = false
                platform.scanQrCode { text ->
                    if (text.isNullOrBlank()) return@scanQrCode
                    scope.launch {
                        repository.importText(text)
                            .onSuccess { platform.toast(importedTemplate.format(it)) }
                            .onFailure { platform.toast(importFailed) }
                    }
                }
            },
            onScanQrImage = {},
            onManual = {},
            onCustom = {},
            onPsiphon = {
                showAdd = false
                showPsiphon = true
            },
            onDnsTunnel = {},
            onMasterDns = {},
            onOpenConnect = {},
            onAether = {
                showAdd = false
                showAether = true
            },
            onIkev2 = {},
            onTor = {},
            onSsh = {},
            onSniSpoof = {},
            onProxyChain = {
                showAdd = false
                showProxyChain = true
            },
            onCrossChain = {
                showAdd = false
                showCrossChain = true
            },
            onSubscription = { showAdd = false; showSubs = true },
        )
    }

    if (showAether) {
        AetherSheet(
            onDismiss = { showAether = false },
            onSave = { name, settings ->
                showAether = false
                repository.addAether(settings, name)
            },
        )
    }

    if (showProxyChain) {
        ProxyChainSheet(
            candidates = chainCandidates,
            onDismiss = { showProxyChain = false },
            onSave = { name, memberIds ->
                showProxyChain = false
                repository.addProxyChain(memberIds, name)
            },
        )
    }

    if (showCrossChain) {
        CrossChainSheet(
            exits = crossExits,
            carriers = crossCarriers,
            pairError = crossPairError,
            onDismiss = { showCrossChain = false },
            onSave = { name, innerId, outerId ->
                showCrossChain = false
                repository.addCrossChain(innerId = innerId, outerId = outerId, name = name)
            },
        )
    }

    qrTarget?.let { (qrTitle, qrText) ->
        QrDialog(
            title = qrTitle,
            text = qrText,
            copyLabel = stringResource(Res.string.action_copy),
            shareLabel = stringResource(Res.string.action_share),
            closeLabel = stringResource(Res.string.action_close),
            unsupportedLabel = stringResource(Res.string.qr_too_large),
            onCopy = { platform.copyToClipboard(qrText); platform.toast(copiedToast) },
            onShare = { platform.shareText(qrText) },
            onDismiss = { qrTarget = null },
        )
    }

    dev.cluvex.zedsecure.ui.servers.CardActionHosts(
        repository = repository,
        editTarget = editTarget,
        renameTarget = renameTarget,
        moveTarget = moveTarget,
        onDismissEdit = { editTarget = null },
        onDismissRename = { renameTarget = null },
        onDismissMove = { moveTarget = null },
    )

    if (confirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAll = false },
            icon = { Icon(painterResource(Res.drawable.ic_delete), null) },
            title = { Text(stringResource(Res.string.del_all)) },
            text = { Text(stringResource(Res.string.del_all_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeleteAll = false
                    val removed = allServers.size
                    repository.removeAll { !it.isLocked }
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
}

/** A header action button sitting in a glass circle. */
@Composable
private fun GlassIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f),
        modifier = Modifier.size(42.dp),
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(42.dp)) { content() }
    }
}

/** The group chip of the servers screen, restyled as glass for the home header. */
@Composable
private fun GroupTab(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    val background = if (selected) {
        ZedGreen
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
    }
    val content = if (selected) {
        Color(0xFF052918)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        Modifier
            .clip(MaterialTheme.shapes.small)
            .background(background)
            .border(
                1.dp,
                if (selected) Color.Transparent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f),
                MaterialTheme.shapes.small,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = content,
            maxLines = 1,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = content.copy(alpha = 0.7f),
        )
    }
}

private suspend fun pingOne(
    repository: ConfigRepository,
    profile: VpnProfile,
    delayTestUrl: String,
    real: Boolean,
) {
    repository.clearPings(listOf(profile.id), persist = false)
    val ms = if (real) {
        PingService.realDelay(
            profile,
            url = delayTestUrl.ifBlank { dev.cluvex.zedsecure.data.net.NetworkInfoRepository.DELAY_TEST_URL },
        )
    } else {
        PingService.tcpPing(profile.address, profile.port)
    }
    val cc = if (ms > 0) GeoLookup.countryOf(profile.address) else null
    repository.setPing(profile.id, if (ms > 0) ms.toInt() else PingService.FAILED_PING, cc)
}
