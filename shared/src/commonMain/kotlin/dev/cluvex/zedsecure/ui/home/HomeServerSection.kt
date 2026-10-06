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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.cluvex.zedsecure.data.config.ConfigRepository
import dev.cluvex.zedsecure.data.net.GeoLookup
import dev.cluvex.zedsecure.data.net.PingCoordinator
import dev.cluvex.zedsecure.data.net.PingService
import dev.cluvex.zedsecure.domain.config.VpnProfile
import dev.cluvex.zedsecure.shared.resources.Res
import dev.cluvex.zedsecure.shared.resources.*
import dev.cluvex.zedsecure.ui.servers.AddServerSheet
import dev.cluvex.zedsecure.ui.servers.ServerCard
import dev.cluvex.zedsecure.ui.servers.SubscriptionsSheet
import dev.cluvex.zedsecure.ui.theme.Personalization
import dev.cluvex.zedsecure.ui.theme.ZedCyan
import dev.cluvex.zedsecure.ui.theme.ZedViolet
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

    val allServers = profiles.filterNot { it.isLocked }
    val manualCount = allServers.count { it.subscriptionId.isEmpty() }
    val servers = when (group) {
        "all" -> allServers
        "manual" -> allServers.filter { it.subscriptionId.isEmpty() }
        else -> allServers.filter { it.subscriptionId == group }
    }
    val subscriptionNames = subscriptions.associate { it.id to it.name }

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
                HeaderActionButton(
                    icon = Res.drawable.ic_close,
                    label = stringResource(Res.string.ping_stop),
                ) { PingCoordinator.cancel() }
            } else {
                Box {
                    HeaderActionButton(
                        icon = Res.drawable.ic_speed,
                        label = stringResource(Res.string.ping_test),
                    ) { pingMenu = true }
                    DropdownMenu(
                        expanded = pingMenu,
                        onDismissRequest = { pingMenu = false },
                        shape = MaterialTheme.shapes.largeIncreased,
                    ) {
                        DropdownMenuItem(
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
                HeaderActionButton(
                    icon = Res.drawable.ic_more_vert,
                    label = stringResource(Res.string.servers_actions),
                ) { overflowOpen = true }
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
            HeaderActionButton(
                icon = Res.drawable.ic_add,
                label = stringResource(Res.string.servers_add),
                gradient = true,
            ) { showAdd = true }
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

/**
 * Glassy header action chip; the add button wears the brand gradient. Replaces the plain
 * IconButtons so the section header matches the approved mockup.
 */
@Composable
private fun HeaderActionButton(
    icon: org.jetbrains.compose.resources.DrawableResource,
    label: String,
    gradient: Boolean = false,
    onClick: () -> Unit,
) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(13.dp)
    Box(
        Modifier
            .size(42.dp)
            .clip(shape)
            .then(
                if (gradient) {
                    Modifier.background(
                        androidx.compose.ui.graphics.Brush.linearGradient(listOf(ZedViolet, ZedCyan)),
                    )
                } else {
                    Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                            shape,
                        )
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(icon),
            contentDescription = label,
            tint = if (gradient) androidx.compose.ui.graphics.Color.White
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(19.dp),
        )
    }
}

/** The group chip of the servers screen, restyled for the home header. */
@Composable
private fun GroupTab(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    val background = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val content = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        Modifier
            .clip(MaterialTheme.shapes.small)
            .background(background)
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
