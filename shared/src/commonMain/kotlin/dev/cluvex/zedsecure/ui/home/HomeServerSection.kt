@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.cluvex.zedsecure.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.cluvex.zedsecure.data.net.PingCoordinator
import dev.cluvex.zedsecure.data.config.ConfigRepository
import dev.cluvex.zedsecure.data.net.GeoLookup
import dev.cluvex.zedsecure.domain.config.VpnProfile
import dev.cluvex.zedsecure.shared.resources.Res
import dev.cluvex.zedsecure.shared.resources.*
import dev.cluvex.zedsecure.data.net.PingService
import dev.cluvex.zedsecure.ui.servers.AddServerSheet
import dev.cluvex.zedsecure.ui.servers.ServerCard
import dev.cluvex.zedsecure.ui.servers.SubscriptionsSheet
import dev.cluvex.zedsecure.ui.theme.Personalization
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * The servers under the connect control of the home screen: the same cards the servers screen
 * keeps, with the plus that adds from the clipboard or a subscription, and the ping button that
 * tests every server shown.
 */
@Composable
internal fun HomeServerSection(
    repository: ConfigRepository,
    personalization: Personalization,
    realPingConcurrency: Int,
    delayTestUrl: String,
    autoSortAfterTest: Boolean,
    modifier: Modifier = Modifier,
) {
    val platform = dev.cluvex.zedsecure.ui.platform.LocalPlatform.current
    val scope = rememberCoroutineScope()
    val profiles by repository.profiles.collectAsStateWithLifecycle()
    val activeId by repository.activeId.collectAsStateWithLifecycle()
    val subscriptions by repository.subscriptions.collectAsStateWithLifecycle()
    val pingProgress by PingCoordinator.progress.collectAsStateWithLifecycle()
    val testing = pingProgress != null

    val servers = profiles.filterNot { it.isLocked }
    val subscriptionNames = subscriptions.associate { it.id to it.name }

    var showAdd by remember { mutableStateOf(false) }
    var showSubs by remember { mutableStateOf(false) }
    var pingMenu by remember { mutableStateOf(false) }
    val importedTemplate = stringResource(Res.string.servers_imported)
    val importFailed = stringResource(Res.string.config_invalid)

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

    Box(modifier.fillMaxWidth()) {
        androidx.compose.foundation.layout.Column {
            Row(
                Modifier.fillMaxWidth().padding(start = 4.dp, end = 0.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(Res.string.nav_servers),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                if (testing) {
                    IconButton(onClick = { PingCoordinator.cancel() }) {
                        Icon(
                            painterResource(Res.drawable.ic_close),
                            contentDescription = stringResource(Res.string.ping_stop),
                        )
                    }
                } else {
                    Box {
                        IconButton(onClick = { pingMenu = true }) {
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
                IconButton(onClick = { showAdd = true }) {
                    Icon(
                        painterResource(Res.drawable.ic_add),
                        contentDescription = stringResource(Res.string.servers_add),
                    )
                }
            }

            if (servers.isNotEmpty()) {
                // A bounded, scrollable column: the cards are plain children, so the section keeps
                // the home layout's exact sizing while long lists scroll inside their own box.
                androidx.compose.foundation.layout.Column(
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
