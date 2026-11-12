package dev.valid7996.narcicgeteway.data.net

import dev.valid7996.narcicgeteway.core.CoreProbe
import dev.valid7996.narcicgeteway.core.LogBus
import dev.valid7996.narcicgeteway.core.VpnManager
import dev.valid7996.narcicgeteway.domain.config.AetherProfile
import dev.valid7996.narcicgeteway.domain.config.CustomConfig
import dev.valid7996.narcicgeteway.domain.config.SniSpoofConfig
import dev.valid7996.narcicgeteway.domain.config.VpnProfile
import dev.valid7996.narcicgeteway.platform.httpTimedTransfer
import dev.valid7996.narcicgeteway.platform.tcpConnectMillis
import dev.valid7996.narcicgeteway.platform.udpProbeMillis
import dev.valid7996.narcicgeteway.platform.tlsConnectMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

object PingService {
    @kotlin.concurrent.Volatile
    var probeOptions: () -> dev.valid7996.narcicgeteway.domain.config.XrayJsonBuilder.BuildOptions =
        { dev.valid7996.narcicgeteway.domain.config.XrayJsonBuilder.BuildOptions() }

    private const val TCP_TIMEOUT_MS = 2_500

    /** An Aether profile has no server to knock on: its core is started for the test (see AetherDelayProbe) and timed through. */
    private const val AETHER_PROBE_TIMEOUT_MS = 150_000L

    /**
     * Runs a core for an Aether profile and hands the port of its SOCKS listener to the block, whose result it returns.
     * The platform sets it; without it an Aether profile cannot be pinged.
     */
    /**
     * Keeps the no-root SNI spoof engine running (with the settings of the app) until the returned function is called;
     * null when the engine could not be started. A profile that points at 127.0.0.1:40443 has no server of its own: it
     * can only be measured while the engine is up, which is what the real delay needs. The platform sets it.
     */
    @Volatile
    var rstaSpoofLease: (suspend () -> (() -> Unit)?)? = null

    @Volatile
    var aetherTunnel: (suspend (AetherProfile, suspend (Int) -> Long) -> Long)? = null

    @Volatile
    var sniProbe: (suspend (String, String) -> Long)? = null

    /**
     * Starts the real engine of a DNS-tunnel profile (WhiteDNS, MasterDNS, DNSTT) for the test, hands the port of its
     * SOCKS listener to the block and returns the block's result: the delay of a request that really went through the
     * tunnel. It returns null when this platform cannot run that profile's engine; the platform sets it.
     */
    @Volatile
    var dnsEngineTunnel: (suspend (VpnProfile, suspend (Int) -> Long) -> Long?)? = null

    // A DNS tunnel needs minutes to scan resolvers and set up a session, and the engines share ports and (MasterDNS)
    // one global instance, so they are tested one at a time.
    private val dnsEngineLock = Mutex()

    private const val DNS_ENGINE_PROBE_TIMEOUT_MS = 200_000L

    private val DNS_TUNNEL_KINDS = setOf(VpnManager.KIND_DNS_TUNNEL, VpnManager.KIND_MASTERDNS, VpnManager.KIND_WHITEDNS)

    private const val REAL_PING_CONCURRENCY = 16

    private const val GEO_CONCURRENCY = 8

    const val FAILED_PING = -1

    private const val PRECHECK_TIMEOUT_MS = 2_200

    private const val PROBE_TIMEOUT_MS = 5_500L

    private const val FIRST_PROBE_TIMEOUT_MS = 3_800L

    private const val FALLBACK_PROBE_TIMEOUT_MS = 2_200L

    private const val REQUEST_PROBE_TIMEOUT_MS = 10_000

    private const val CUSTOM_PROBE_CONCURRENCY = 16

    private val geoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun tcpPing(host: String, port: Int, timeoutMs: Int = TCP_TIMEOUT_MS): Long =
        withContext(Dispatchers.IO) { tcpConnectMillis(host, port, timeoutMs) }

    suspend fun realDelay(
        profile: VpnProfile,
        url: String = NetworkInfoRepository.DELAY_TEST_URL,
        chainConfig: (VpnProfile) -> String? = { null },
    ): Long = withContext(Dispatchers.IO) { measureProfile(profile, url, chainConfig) }

    suspend fun activeConnectionHealth(
        url: String = NetworkInfoRepository.DELAY_TEST_URL,
    ): CoreProbe.DelayOutcome = withContext(Dispatchers.IO) {
        val first = probeActiveConnection(url)
        if (first.ok) return@withContext first
        if (url != NetworkInfoRepository.DELAY_TEST_URL2) {
            val second = probeActiveConnection(NetworkInfoRepository.DELAY_TEST_URL2)
            if (second.ok) return@withContext second
            return@withContext CoreProbe.DelayOutcome(-1L, second.error ?: first.error)
        }
        first
    }

    private suspend fun probeActiveConnection(url: String): CoreProbe.DelayOutcome {
        if (runsXrayCore(VpnManager.activeKind.value)) {
            return runCatching { CoreProbe.measureDelayDetailed(url) }
                .getOrElse { CoreProbe.DelayOutcome(-1L, it.message) }
        }
        return runCatching {
            val timing = httpTimedTransfer(
                url,
                socksPort = VpnManager.activeSocksPort,
                upload = false,
                uploadBytes = 0,
                connectTimeoutMs = REQUEST_PROBE_TIMEOUT_MS,
                readTimeoutMs = REQUEST_PROBE_TIMEOUT_MS,
            )
            CoreProbe.DelayOutcome((timing.ttfbNanos / 1_000_000).coerceAtLeast(1), null)
        }.getOrElse { CoreProbe.DelayOutcome(-1L, it.message ?: it.toString()) }
    }

    fun runsXrayCore(kind: String?): Boolean =
        kind == null || kind == VpnManager.KIND_XRAY || kind == VpnManager.KIND_SNISPOOF ||
            kind == VpnManager.KIND_CROSS_CHAIN

    suspend fun realDelayOfActiveConnection(
        url: String = NetworkInfoRepository.DELAY_TEST_URL,
    ): Long = withContext(Dispatchers.IO) {
        val first = probeActiveConnection(url)
        if (first.ok) return@withContext first.ms

        if (url != NetworkInfoRepository.DELAY_TEST_URL2) {
            val second = probeActiveConnection(NetworkInfoRepository.DELAY_TEST_URL2)
            if (second.ok) return@withContext second.ms
        }

        -1L
    }

    suspend fun measureAll(
        profiles: List<VpnProfile>,
        useRealDelay: Boolean,

        concurrency: Int = REAL_PING_CONCURRENCY,
        delayUrl: String = NetworkInfoRepository.DELAY_TEST_URL,

        chainConfig: (VpnProfile) -> String? = { null },
        detectCountry: Boolean = false,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
        onResult: (id: String, ms: Int?, countryCode: String?) -> Unit,

    ) = withContext(Dispatchers.IO) {
        val realPermits = concurrency.takeIf { it in 1..128 } ?: REAL_PING_CONCURRENCY
        val url = delayUrl.ifBlank { NetworkInfoRepository.DELAY_TEST_URL }
        val permits = Semaphore(if (useRealDelay) realPermits else realPermits * 2)

        val customPermits = Semaphore(minOf(realPermits, CUSTOM_PROBE_CONCURRENCY))
        val geoPermits = Semaphore(GEO_CONCURRENCY)

        val targets = profiles.filterNot {
            ((it.isManagedTunnel && !it.isSingBoxConfig && !it.isSniSpoof && !it.isNoRootSniSpoof &&
                !it.isMhrvRelay && !it.isSstp && !it.isDnsBasedTunnel && !it.isIkev2 &&
                !it.isOpenConnect && !it.isOpenVpn)) || it.isAether
        }
        val aetherTargets = profiles.filter { it.isAether }
        val aetherPermits = Semaphore(1)
        val total = targets.size + aetherTargets.size
        var done = 0
        val doneLock = Semaphore(1)
        onProgress(0, total)
        val aetherJobs = aetherTargets.map { profile ->
            launch(Dispatchers.IO) {
                // One core at a time: the cores share the exit proxy and the key files.
                val ms = aetherPermits.withPermit { aetherDelay(profile, url) }
                onResult(profile.id, if (ms > 0) ms.toInt() else FAILED_PING, profile.countryCode)
                doneLock.withPermit { onProgress(++done, total) }
            }
        }
        // The engine stays up for the whole batch when some profile points at it, instead of starting and stopping around each probe.
        val batchLease = if (targets.any { it.isSniSpoof || it.isNoRootSniSpoof || SniSpoofConfig.isSpoofTarget(it.address, it.port) }) rstaSpoofLease?.invoke() else null
        try {
        (aetherJobs + targets.map { profile ->
            launch(Dispatchers.IO) {
                val isSpoof = profile.isSniSpoof || profile.isNoRootSniSpoof || SniSpoofConfig.isSpoofTarget(profile.address, profile.port)
                val isSealed = profile.isLocked || profile.source is dev.valid7996.narcicgeteway.domain.config.ProfileSource.Sealed
                val isMhrv = profile.isMhrvRelay || (isSealed && profile.resolveUnsealed()?.isMhrvRelay == true)
                val isSpecial = profile.isIkev2 || profile.isOpenConnect || profile.isOpenVpn || profile.isSstp
                val heavy = useRealDelay || profile.isProxyChain || isSpoof || isSealed || isMhrv || profile.isDnsBasedTunnel || isSpecial
                val pool = if (heavy && (profile.isCustom || isSealed || isMhrv)) customPermits else permits
                val ms = pool.withPermit {
                    if (heavy) measureProfile(profile, url, chainConfig)
                    else tcpPing(profile.address, profile.port)
                }

                val latency = if (ms > 0) ms.toInt() else FAILED_PING

                onResult(profile.id, latency, profile.countryCode)
                doneLock.withPermit { onProgress(++done, total) }

                val addressForGeo = when {
                    profile.isIkev2 -> profile.ikev2Settings()?.server?.substringBefore(':') ?: profile.address
                    profile.isOpenConnect -> profile.openConnectSettings()?.server?.removePrefix("https://")?.removePrefix("http://")?.substringBefore('/')?.substringBefore(':') ?: profile.address
                    profile.isDnsTunnel -> profile.dnsTunnelSettings()?.domain?.split(',', '\n')?.firstOrNull()?.trim() ?: profile.address
                    profile.isMasterDns -> profile.masterDnsSettings()?.domains?.split(',', '\n')?.firstOrNull()?.trim() ?: profile.address
                    else -> profile.address
                }

                if (detectCountry && latency > 0 && profile.countryCode == null) {
                    geoScope.launch {
                        val country = geoPermits.withPermit { GeoLookup.countryOf(addressForGeo) }
                        if (country != null) onResult(profile.id, latency, country)
                    }
                }

                if (latency > 0 && profile.countryCode == null && !isSpoof && !isSealed) {
                    geoScope.launch {
                        val country = geoPermits.withPermit { GeoLookup.countryOf(addressForGeo) }
                        if (country != null) onResult(profile.id, latency, country)
                    }
                }
            }
        }).joinAll()
        } finally {
            batchLease?.invoke()
        }
    }

    private suspend fun aetherDelay(profile: VpnProfile, url: String): Long {
        val runner = aetherTunnel
        val effective = profile.resolveUnsealed() ?: profile
        val settings = effective.aetherSettings() ?: return -1L
        if (runner != null) {
            return withTimeoutOrNull(AETHER_PROBE_TIMEOUT_MS) {
                runner(settings) { port ->
                    var best = -1L
                    repeat(2) {
                        val ms = runCatching {
                            httpTimedTransfer(url, port, false, 0, REQUEST_PROBE_TIMEOUT_MS, REQUEST_PROBE_TIMEOUT_MS).ttfbNanos / 1_000_000
                        }.getOrDefault(-1L)
                        if (ms > 0 && (best < 0 || ms < best)) best = ms.coerceAtLeast(1)
                    }
                    best
                }
            } ?: -1L
        }
        val endpoint = settings.endpointText() ?: settings.server.trim().ifBlank { profile.address.trim() }
        val host = if (endpoint.contains(':') && !endpoint.startsWith("[")) endpoint.substringBefore(':') else endpoint
        val port = if (endpoint.contains(':') && !endpoint.startsWith("[")) endpoint.substringAfter(':').toIntOrNull() ?: 2408 else profile.port.takeIf { it in 1..65535 } ?: 2408
        if (host.isNotBlank() && host != "-") {
            val wgInit = byteArrayOf(1, 0, 0, 0)
            val udp = udpProbeMillis(host, port, wgInit, 2_200)
            if (udp > 0) return udp
        }
        return -1L
    }

    private suspend fun ikev2Delay(profile: VpnProfile): Long {
        val ike = profile.ikev2Settings()
        var host = ike?.server?.trim()?.ifBlank { profile.address.trim() } ?: profile.address.trim()
        if (host.isBlank() || host == "-") return -1L
        var targetPort = 500
        if (host.contains(':') && !host.startsWith("[")) {
            val parts = host.split(':')
            host = parts[0]
            targetPort = parts.getOrNull(1)?.toIntOrNull() ?: 500
        }

        // If IKEv2 is actively connected on device, measure true end-to-end HTTP delay through tunnel
        if (VpnManager.activeKind.value == VpnManager.KIND_IKEV2 && VpnManager.deviceVpnActive()) {
            val activeOutcome = probeActiveConnection(NetworkInfoRepository.DELAY_TEST_URL)
            if (activeOutcome.ok && activeOutcome.ms > 0) return activeOutcome.ms
        }

        val spi = ByteArray(8)
        kotlin.random.Random.nextBytes(spi)
        // Standard IKE_SA_INIT probe header
        val ikePkt = spi + ByteArray(8) + byteArrayOf(33, 0x20, 34, 0x08, 0, 0, 0, 0, 0, 0, 0, 32, 0, 0, 0, 4)
        val udp500 = udpProbeMillis(host, targetPort, ikePkt, 1_500)
        if (udp500 > 0) return udp500

        if (targetPort == 500) {
            val nonEspPkt = byteArrayOf(0, 0, 0, 0) + ikePkt
            val udp4500 = udpProbeMillis(host, 4500, nonEspPkt, 1_500)
            if (udp4500 > 0) return udp4500
        }

        return -1L
    }

    private suspend fun openVpnDelay(profile: VpnProfile): Long {
        var host = profile.address.trim()
        if (host.isBlank() || host == "-") return -1L
        var port = if (profile.port in 1..65535) profile.port else 1194
        if (host.contains(':') && !host.startsWith("[")) {
            val parts = host.split(':')
            host = parts[0]
            port = parts.getOrNull(1)?.toIntOrNull() ?: port
        }

        val sessionId = ByteArray(8)
        kotlin.random.Random.nextBytes(sessionId)
        val ovpnPkt = byteArrayOf((7 shl 3).toByte()) + sessionId + byteArrayOf(0, 0, 0, 0, 1)
        val udpDelay = udpProbeMillis(host, port, ovpnPkt, 2_200)
        if (udpDelay > 0) return udpDelay

        val tls = tlsConnectMillis(host, port, 2_000)
        if (tls > 0) return tls

        return -1L
    }

    private suspend fun openConnectDelay(profile: VpnProfile): Long {
        val oc = profile.openConnectSettings()
        var host = oc?.server?.removePrefix("https://")?.removePrefix("http://")?.substringBefore('/')?.substringBefore(':')?.trim()
            ?: profile.address.trim()
        if (host.isBlank() || host == "-") return -1L
        val port = oc?.serverPort() ?: if (profile.port in 1..65535) profile.port else 443

        val tls = tlsConnectMillis(host, port, 2_500)
        if (tls > 0) return tls

        return -1L
    }

    // Used when a connection has no resolvers of its own (the engine then uses its built-in list). Public
    // resolvers come first so the probe does not depend on the first few local ones being reachable.
    private val DEFAULT_FALLBACK_RESOLVERS = listOf(
        "8.8.8.8:53",
        "1.1.1.1:53",
        "9.9.9.9:53",
        "208.67.222.222:53",
        "185.53.142.174:53",
        "217.219.162.200:53",
        "2.188.21.138:53",
        "2.188.21.70:53",
        "2.189.44.68:53",
        "2.189.44.86:53",
        "2.144.2.72:53",
        "2.144.2.154:53",
    )

    /**
     * The delay of a DNS-tunnel profile through its real engine. A resolver that answers a DNS query says nothing
     * about the tunnel behind it, so this starts the engine and times a request through it; only a platform that
     * cannot run the engine falls back to the resolver query.
     */
    private suspend fun dnsEngineDelay(profile: VpnProfile, url: String): Long {
        val live = VpnManager.activeKind.value in DNS_TUNNEL_KINDS && VpnManager.deviceVpnActive() &&
            VpnManager.status.value.serverName == profile.name
        if (live) return realDelayOfActiveConnection(url)

        val runner = dnsEngineTunnel ?: return dnsTunnelDelay(profile)
        return dnsEngineLock.withLock {
            var supported = true
            val ms = withTimeoutOrNull(DNS_ENGINE_PROBE_TIMEOUT_MS) {
                runner(profile) { port ->
                    var best = -1L
                    repeat(2) {
                        val t = runCatching {
                            httpTimedTransfer(url, port, false, 0, REQUEST_PROBE_TIMEOUT_MS, REQUEST_PROBE_TIMEOUT_MS).ttfbNanos / 1_000_000
                        }.getOrDefault(-1L)
                        if (t > 0 && (best < 0 || t < best)) best = t.coerceAtLeast(1)
                    }
                    best
                }.also { if (it == null) supported = false }
            }
            when {
                ms != null -> ms
                !supported -> dnsTunnelDelay(profile)
                else -> {
                    LogBus.append("W/PingService the engine of ${profile.name} did not come up in time")
                    -1L
                }
            }
        }
    }

    private suspend fun dnsTunnelDelay(profile: VpnProfile): Long {
        val master = profile.masterDnsSettings()
        val white = profile.whiteDnsSettings()
        val dnsTunnel = profile.dnsTunnelSettings()

        val domain = when {
            master != null -> master.domains.split(',', '\n', ' ').map { it.trim().trimEnd('.') }.firstOrNull { it.isNotBlank() } ?: master.domains
            white != null -> white.primaryDomain()
            dnsTunnel != null -> dnsTunnel.domain.split(',', '\n', ' ').map { it.trim().trimEnd('.') }.firstOrNull { it.isNotBlank() } ?: dnsTunnel.domain
            else -> profile.address
        }.trim().trimEnd('.')

        if (domain.isBlank() || domain == "-") return -1L

        val configuredResolvers = when {
            master != null -> master.resolvers.split(',', '\n', ' ').map { it.trim() }.filter { it.isNotBlank() && !it.startsWith("#") }
            white != null -> white.resolverList().filter { !it.contains('/') }
            dnsTunnel != null -> dnsTunnel.resolvers.split(',', '\n', ' ').map { it.trim() }.filter { it.isNotBlank() && !it.startsWith("#") }
            else -> emptyList()
        }
        val resolverCandidates = configuredResolvers.ifEmpty { DEFAULT_FALLBACK_RESOLVERS }

        val txId = ByteArray(2)
        kotlin.random.Random.nextBytes(txId)
        val flagsAndCounts = byteArrayOf(1, 0, 0, 1, 0, 0, 0, 0, 0, 0)
        val qName = buildList {
            domain.split('.').forEach { part ->
                if (part.isNotBlank()) {
                    add(part.length.toByte())
                    addAll(part.encodeToByteArray().toList())
                }
            }
            add(0.toByte())
        }.toByteArray()
        val qTypeAndClass = byteArrayOf(0, 1, 0, 1)
        val dnsQuery = txId + flagsAndCounts + qName + qTypeAndClass

        for (candidate in resolverCandidates.take(if (configuredResolvers.isEmpty()) 8 else 6)) {
            val raw = candidate.trim()
            val host = when {
                raw.startsWith("[") -> raw.substringAfter('[').substringBefore(']')
                ':' in raw -> raw.substringBefore(':')
                else -> raw
            }
            val port = when {
                raw.startsWith("[") -> raw.substringAfter("]:", "").toIntOrNull() ?: 53
                ':' in raw -> raw.substringAfter(':').toIntOrNull() ?: 53
                else -> 53
            }
            val delay = udpProbeMillis(host, port, dnsQuery, 2_000)
            if (delay > 0) return delay
        }

        return -1L
    }

    private suspend fun measureProfile(
        profile: VpnProfile,
        url: String,
        chainConfig: (VpnProfile) -> String? = { null },
    ): Long {
        val effective = profile.resolveUnsealed() ?: profile
        if (effective.isAether) return aetherDelay(effective, url)
        if (effective.isDnsTunnel || effective.isMasterDns || effective.isWhiteDns) return dnsEngineDelay(effective, url)
        if (effective.isIkev2) return ikev2Delay(effective)
        if (effective.isOpenConnect) return openConnectDelay(effective)
        if (effective.isOpenVpn) {
            val config = runCatching { effective.toXrayConfigJson(forSpeedtest = true) }.getOrNull()
            // A request through the real core is the answer; a port that happens to be open is not.
            if (config != null) return probeWithTimeout(effective, config, url)
            return openVpnDelay(effective)
        }
        if (effective.isSstp) {
            val sni = effective.address
            val probe = sniProbe?.invoke(effective.address, sni)
            return if (probe != null && probe > 0) probe else -1L
        }
        if (effective.isMhrvRelay) {
            val mhrv = effective.mhrvRelaySettings()
            if (mhrv != null) {
                val ip = mhrv.googleIp.trim().ifEmpty { dev.valid7996.narcicgeteway.domain.config.MhrvRelayProfile.DEFAULT_GOOGLE_IP }
                val sni = mhrv.sniList().firstOrNull()?.trim()?.ifEmpty { null }
                    ?: mhrv.frontDomain.trim().ifEmpty { dev.valid7996.narcicgeteway.domain.config.MhrvRelayProfile.DEFAULT_FRONT_DOMAIN }
                val probe = sniProbe?.invoke(ip, sni)
                return if (probe != null && probe > 0) probe else -1L
            }
        }
        if (effective.isProxyChain) {
            val config = chainConfig(effective) ?: return -1L
            return probeWithTimeout(effective, config, url)
        }

        val config = runCatching { effective.toXrayConfigJson(forSpeedtest = true) }
            .onFailure { LogBus.append("W/PingService build config failed for ${effective.name}: ${it.message}") }
            .getOrElse { return -1L }

        return probeWithTimeout(effective, config, url)
    }

    fun shouldPrecheck(profile: VpnProfile): Boolean {
        val effective = profile.resolveUnsealed() ?: profile
        if (effective.isMhrvRelay) return false
        if (effective.isAether) return false
        if (effective.isDnsTunnel || effective.isMasterDns || effective.isWhiteDns) return false
        if (effective.isIkev2) return false
        if (effective.isOpenConnect) return false
        if (effective.isOpenVpn) return false
        if (effective.isSstp) return false
        if (effective.isProxyChain) return false
        if (effective.isSniSpoof || effective.isNoRootSniSpoof || SniSpoofConfig.isSpoofTarget(effective.address, effective.port)) return false
        if (effective.address.isBlank() || effective.address == "-") return false
        if (effective.port !in 1..65535) return false
        if (effective.isCustom) {
            val raw = effective.rawPayload() ?: return false
            return CustomConfig.tcpProbeable(raw)
        }

        (effective.source as? dev.valid7996.narcicgeteway.domain.config.ProfileSource.SingBox)?.let { src ->
            val servers = dev.valid7996.narcicgeteway.domain.config.SingBoxJson.servers(src.json)
            val server = servers.firstOrNull() ?: return false
            return !server.udp && !src.json.contains("\"detour\"")
        }
        if (effective.isSingBoxConfig) return false
        return effective.protocol !in UDP_OUTBOUND_PROTOCOLS
    }

    private val UDP_OUTBOUND_PROTOCOLS = setOf("WIREGUARD", "AMNEZIAWG", "HYSTERIA")

    private suspend fun probeWithTimeout(profile: VpnProfile, config: String, url: String): Long {
        val isSpoof = profile.isSniSpoof || profile.isNoRootSniSpoof || SniSpoofConfig.isSpoofTarget(profile.address, profile.port)
        if (!isSpoof) return probeCore(profile, config, url)
        val release = rstaSpoofLease?.invoke()
        if (release == null) {
            LogBus.append("W/PingService the SNI spoof engine did not start; ${profile.name} cannot be measured")
            return -1L
        }
        try {
            return probeCore(profile, config, url)
        } finally {
            release()
        }
    }

    private suspend fun probeCore(profile: VpnProfile, config: String, url: String): Long =
        withContext(Dispatchers.IO) {
            withTimeoutOrNull(PROBE_TIMEOUT_MS) {
                val start = System.currentTimeMillis()
                val firstMs = withTimeoutOrNull(FIRST_PROBE_TIMEOUT_MS) {
                    runCatching { CoreProbe.measureOutboundDelay(config, url) }
                        .onFailure {
                            LogBus.append("W/PingService real delay failed for ${profile.name}: ${it.message}")
                        }
                        .getOrDefault(-1L)
                } ?: -1L

                if (firstMs > 0) return@withTimeoutOrNull firstMs

                val elapsed = System.currentTimeMillis() - start
                val remainingTime = PROBE_TIMEOUT_MS - elapsed
                // If first attempt timed out or almost exhausted time, don't waste time on fallback
                if (remainingTime < 1_500L) return@withTimeoutOrNull -1L

                val fallbackUrl = if (url != NetworkInfoRepository.DELAY_TEST_URL2) {
                    NetworkInfoRepository.DELAY_TEST_URL2
                } else {
                    "http://cp.cloudflare.com/generate_204"
                }

                val fallbackTimeout = minOf(remainingTime, FALLBACK_PROBE_TIMEOUT_MS)
                withTimeoutOrNull(fallbackTimeout) {
                    runCatching { CoreProbe.measureOutboundDelay(config, fallbackUrl) }
                        .onFailure {
                            LogBus.append("W/PingService fallback delay failed for ${profile.name}: ${it.message}")
                        }
                        .getOrDefault(-1L)
                } ?: -1L
            } ?: run {
                LogBus.append("W/PingService real delay timed out for ${profile.name}")
                -1L
            }
        }
}
