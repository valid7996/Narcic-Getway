package dev.valid7996.narcicgeteway.core

import android.content.Context
import dev.valid7996.narcicgeteway.domain.config.LocalPorts
import dev.valid7996.narcicgeteway.domain.config.MasterDnsProfile
import dev.valid7996.narcicgeteway.domain.config.VpnProfile
import dev.valid7996.narcicgeteway.domain.config.WhiteDnsProfile
import dev.valid7996.narcicgeteway.domain.model.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * The real delay of a DNS-tunnel profile: its engine is started for the test, on a free loopback port and with the
 * same resolvers a connection would use, and once it answers the caller times a request through it. A resolver that
 * answers a DNS query proves nothing about the tunnel behind it, so this is the only honest measure. Everything is
 * torn down again afterwards. The caller runs one probe at a time.
 *
 * Keep the resolver choice in step with NrgVpnService (startWhiteDns / startMasterDns / startDnsTunnel).
 */
object DnsEngineDelayProbe {

    /** An engine that is silent this long while scanning is given up on; a scan that is still working gets longer. */
    private const val READY_IDLE_MS = 90_000L

    private const val READY_MAX_MS = 150_000L

    /** Null when this profile's engine cannot be run for a test (the caller then falls back to a resolver query). */
    suspend fun run(
        context: Context,
        profile: VpnProfile,
        settings: AppSettings,
        measure: suspend (socksPort: Int) -> Long,
    ): Long? = withContext(Dispatchers.IO) {
        try {
            profile.whiteDnsSettings()?.let { return@withContext whiteDns(context, it.normalized(), settings, measure) }
            profile.masterDnsSettings()?.let { return@withContext masterDns(context, it, settings, measure) }
            profile.dnsTunnelSettings()?.let { return@withContext dnsTunnel(it, settings, measure) }
            null
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            LogBus.append("W/dns engine ping failed: ${e.message}")
            -1L
        }
    }

    private suspend fun whiteDns(
        context: Context,
        profile: WhiteDnsProfile,
        s: AppSettings,
        measure: suspend (Int) -> Long,
    ): Long {
        val controller = WhiteDnsController(
            context = context,
            profile = profile,
            settings = s.whiteDns,
            resolvers = whiteDnsResolvers(context, profile, s),
            listenPort = LocalPorts.WHITE_DNS,
            maxListenPort = LocalPorts.WHITE_DNS_MAX,
            systemResolvers = if (profile.isSoar) underlyingDnsServers(context) else emptyList(),
        )
        try {
            val port = controller.start()
            if (port < 0) return -1L
            if (!controller.awaitReady(READY_IDLE_MS, READY_MAX_MS)) return -1L
            return measure(port)
        } finally {
            controller.stop()
        }
    }

    private suspend fun masterDns(
        context: Context,
        profile: MasterDnsProfile,
        s: AppSettings,
        measure: suspend (Int) -> Long,
    ): Long? {
        // MasterDNS is one global instance inside the app; while a session of it is up, a second one would kill it.
        if (VpnManager.activeKind.value == VpnManager.KIND_MASTERDNS) return null

        val bundled = bundledResolvers(context)
        val effective = when {
            s.dnsGlobalResolverEnabled && s.dnsGlobalResolvers.isNotBlank() -> profile.copy(resolvers = s.dnsGlobalResolvers)
            profile.resolvers.isNotBlank() -> profile
            else -> profile.copy(resolvers = bundled.joinToString(","))
        }
        val pool = when {
            s.dnsPoolEnabled && s.dnsPoolText.isNotBlank() ->
                s.dnsPoolText.split('\n', ',').map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }
            s.dnsPoolEnabled -> bundled
            profile.resolvers.isBlank() -> bundled
            else -> emptyList()
        }
        val controller = MasterDnsController(
            effective, context.filesDir,
            listenPort = LocalPorts.MASTER_DNS,
            maxListenPort = LocalPorts.MASTER_DNS_MAX,
            pool = pool,
            poolFullVerification = s.dnsPoolFullVerification,
        )
        try {
            val port = controller.start()
            if (port < 0) return -1L
            if (!controller.awaitReady(READY_IDLE_MS, READY_MAX_MS)) return -1L
            return measure(port)
        } finally {
            controller.stop()
        }
    }

    private suspend fun dnsTunnel(
        profile: dev.valid7996.narcicgeteway.domain.config.DnsTunnelProfile,
        s: AppSettings,
        measure: suspend (Int) -> Long,
    ): Long? {
        // With SSH on top the tunnel is only half of the connection; it is measured as a connection, not here.
        if (profile.sshEnabled) return null
        val pool = if (s.dnsPoolEnabled) {
            s.dnsPoolText.split('\n', ',').map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }
        } else emptyList()
        val controller = DnsTunnelController(
            profile,
            listenPort = LocalPorts.DNS_TUNNEL,
            maxListenPort = LocalPorts.DNS_TUNNEL_MAX,
            pool = pool,
            poolFullVerification = s.dnsPoolFullVerification,
        )
        try {
            val port = controller.start()
            if (port < 0) return -1L
            // The session may still be setting up when the port opens: a few tries.
            var ms = -1L
            repeat(4) {
                if (ms <= 0) {
                    ms = measure(port)
                    if (ms <= 0) delay(1_500)
                }
            }
            return ms
        } finally {
            controller.stop()
        }
    }

    private fun whiteDnsResolvers(context: Context, profile: WhiteDnsProfile, s: AppSettings): List<String> {
        val chosen = when {
            s.dnsGlobalResolverEnabled && s.dnsGlobalResolvers.isNotBlank() -> MasterDnsProfile.splitResolvers(s.dnsGlobalResolvers)
            profile.resolverList().isNotEmpty() -> profile.resolverList()
            else -> MasterDnsProfile.splitResolvers(s.whiteDns.resolvers)
        }.filter { MasterDnsProfile.isNumericResolver(it) }
        if (!profile.isSoar) return chosen.ifEmpty { bundledResolvers(context) }

        // Soar takes single addresses only; with none configured it gets public resolvers up front, then the list.
        val ips = chosen.filter { '/' !in it }
        val use = ips.ifEmpty {
            (listOf("8.8.8.8", "1.1.1.1", "9.9.9.9") + bundledResolvers(context).filter { '/' !in it }).distinct()
        }
        return use.take(if (profile.soar.discovery) 8192 else 128)
    }

    private fun bundledResolvers(context: Context): List<String> = runCatching {
        context.assets.open("whitedns_default_resolvers.txt").bufferedReader().use { r ->
            r.readLines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }
        }
    }.getOrDefault(emptyList())

    /** DNS servers of the phone's own networks (not the tunnel's), for the Soar engine. */
    private fun underlyingDnsServers(context: Context): List<String> = runCatching {
        val cm = context.getSystemService(android.net.ConnectivityManager::class.java)
        cm.allNetworks.flatMap { network ->
            val caps = cm.getNetworkCapabilities(network) ?: return@flatMap emptyList<String>()
            if (caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN) ||
                !caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
            ) {
                return@flatMap emptyList<String>()
            }
            cm.getLinkProperties(network)?.dnsServers.orEmpty()
                .filter { !it.isLoopbackAddress && !it.isLinkLocalAddress && !it.isAnyLocalAddress }
                .mapNotNull { it.hostAddress?.substringBefore('%') }
        }.distinct().take(8)
    }.getOrDefault(emptyList())
}
