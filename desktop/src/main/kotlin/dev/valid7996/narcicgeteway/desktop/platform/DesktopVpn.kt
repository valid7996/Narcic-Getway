package dev.valid7996.narcicgeteway.desktop.platform

import dev.valid7996.narcicgeteway.core.LogBus
import dev.valid7996.narcicgeteway.core.SocksTunBridge
import dev.valid7996.narcicgeteway.core.SshController
import dev.valid7996.narcicgeteway.core.VpnManager
import dev.valid7996.narcicgeteway.data.config.ConfigRepository
import dev.valid7996.narcicgeteway.desktop.core.DesktopDnsTunnel
import dev.valid7996.narcicgeteway.desktop.core.DesktopIkev2
import dev.valid7996.narcicgeteway.desktop.core.DesktopPsiphon
import dev.valid7996.narcicgeteway.desktop.core.DesktopSstp
import dev.valid7996.narcicgeteway.desktop.core.DesktopTor
import dev.valid7996.narcicgeteway.desktop.core.DesktopTun
import dev.valid7996.narcicgeteway.desktop.core.PhysicalInterface
import dev.valid7996.narcicgeteway.desktop.core.LocalPortPicker
import dev.valid7996.narcicgeteway.desktop.core.SystemProxy
import dev.valid7996.narcicgeteway.desktop.core.Os
import dev.valid7996.narcicgeteway.desktop.core.TunMode
import dev.valid7996.narcicgeteway.desktop.core.XrayCore
import dev.valid7996.narcicgeteway.domain.config.DnsTunnelProfile
import dev.valid7996.narcicgeteway.domain.config.Ikev2Profile
import dev.valid7996.narcicgeteway.domain.config.LocalPorts
import dev.valid7996.narcicgeteway.domain.config.LocalProxy
import dev.valid7996.narcicgeteway.domain.config.MasterDnsProfile
import dev.valid7996.narcicgeteway.domain.config.MhrvRelayProfile
import dev.valid7996.narcicgeteway.domain.config.OvpnConfig
import dev.valid7996.narcicgeteway.domain.config.ProfileSource
import dev.valid7996.narcicgeteway.domain.config.PsiphonProfile
import dev.valid7996.narcicgeteway.domain.config.SingBoxConfigs
import dev.valid7996.narcicgeteway.domain.config.SingBoxJson
import dev.valid7996.narcicgeteway.domain.config.SshProfile
import dev.valid7996.narcicgeteway.domain.config.autoSelectTuning
import dev.valid7996.narcicgeteway.domain.config.toBuildOptions
import dev.valid7996.narcicgeteway.domain.model.AppSettings
import dev.valid7996.narcicgeteway.domain.model.RunMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.File
import java.net.InetAddress
import kotlin.concurrent.thread

object DesktopVpn {
    private const val METRICS_PORT = LocalPorts.DESKTOP_METRICS
    private const val SHIM_PORT = LocalPorts.SHIM
    private const val SSH_PORT = LocalPorts.SSH
    private const val DNS_PORT = LocalPorts.DNS_TUNNEL

    private val work = File(System.getProperty("java.io.tmpdir"), "narcicgeteway").apply { mkdirs() }
    private var xray: XrayCore? = null
    private var innerXray: XrayCore? = null
    private var tun: DesktopTun? = null
    private var stats: DesktopStats? = null
    private var singBoxStats: DesktopSingBoxStats? = null
    private var shim: SocksTunBridge? = null
    private var ssh: SshController? = null
    private var dnsTunnel: DesktopDnsTunnel? = null
    @Volatile private var tor: DesktopTor? = null
    @Volatile private var psiphon: DesktopPsiphon? = null
    @Volatile private var ikev2: DesktopIkev2? = null
    @Volatile private var sstp: DesktopSstp? = null
    private var meter: DesktopMeter? = null
    private var usedSystemProxy = false

    @Volatile var runMode: RunMode = RunMode.SystemProxy

    @Volatile var settingsProvider: () -> AppSettings = { AppSettings() }

    @Volatile
    var chainConfigProvider: (dev.valid7996.narcicgeteway.domain.config.VpnProfile, dev.valid7996.narcicgeteway.domain.config.XrayJsonBuilder.BuildOptions) -> String? =
        { _, _ -> null }

    var ruleOutboundsProvider: (List<dev.valid7996.narcicgeteway.domain.model.RulesetItem>) -> Map<String, dev.valid7996.narcicgeteway.domain.config.ServerConfig> =
        { emptyMap() }

    private fun geoAssetsAvailable(): Boolean =
        File(work, "geoip.dat").let { it.isFile && it.length() > 0 } &&
            File(work, "geosite.dat").let { it.isFile && it.length() > 0 }

    fun toggle(config: ConfigRepository) {
        val state = VpnManager.status.value.state
        if (state.isActive || state.isTransitioning) { stop(); return }
        val profile = config.activeProfile() ?: run { VpnManager.onError("Select a config first"); return }
        val exp = profile.expiresAt
        if (profile.isLocked && exp != null && System.currentTimeMillis() >= exp) {
            VpnManager.onError("این کانفیگ قفل‌شده منقضی شده است")
            return
        }

        runMode = settingsProvider().runMode

        when {
            profile.ikev2Settings() != null -> startIkev2(profile.name, profile.ikev2Settings()!!)
            profile.sstpSettings() != null -> startSstp(profile.name, profile.sstpSettings()!!)
            profile.isCrossChain -> startCrossChain(profile.name, profile, config)
            profile.psiphonSettings() != null -> startPsiphon(profile.name, profile.psiphonSettings()!!)
            profile.isTor -> startTor(profile.name)
            profile.masterDnsSettings() != null -> startMasterDns(profile.name, profile.masterDnsSettings()!!)
            profile.dnsTunnelSettings() != null -> startDns(profile.name, profile.dnsTunnelSettings()!!)
            profile.mhrvRelaySettings() != null -> startMhrv(profile.name, profile.mhrvRelaySettings()!!, config)
            profile.sshSettings() != null -> startSsh(profile.name, profile.sshSettings()!!)
            profile.isSingBoxConfig || profile.isSingBox || profile.isOpenVpn ||
                profile.openConnectSettings() != null || profile.aetherSettings() != null ->
                startSingBox(profile.name, profile)
            else -> startXray(profile.name, profile, config)
        }
    }

    private fun startMasterDns(name: String, masterProfile: MasterDnsProfile) {
        VpnManager.setActiveKind(VpnManager.KIND_MASTERDNS)
        val dnsProfile = DnsTunnelProfile(
            domain = masterProfile.domains.split(',', '\n', ' ').map { it.trim().trimEnd('.') }.firstOrNull { it.isNotBlank() } ?: masterProfile.domains,
            publicKey = masterProfile.encryptionKey,
            resolvers = masterProfile.resolvers,
            engine = "masterdns",
        )
        startDns(name, dnsProfile)
    }

    private fun startMhrv(name: String, mhrv: MhrvRelayProfile, config: ConfigRepository) {
        VpnManager.setActiveKind(VpnManager.KIND_MHRV)
        val ip = mhrv.googleIp.trim().ifEmpty { MhrvRelayProfile.DEFAULT_GOOGLE_IP }
        val sni = mhrv.sniList().firstOrNull()?.trim()?.ifEmpty { null }
            ?: mhrv.frontDomain.trim().ifEmpty { MhrvRelayProfile.DEFAULT_FRONT_DOMAIN }
        val fakeProfile = dev.valid7996.narcicgeteway.domain.config.VpnProfile(
            name = name,
            address = ip,
            port = 443,
            protocol = "VLESS",
            source = ProfileSource.Link("vless://00000000-0000-0000-0000-000000000000@$ip:443?encryption=none&security=tls&sni=$sni&type=tcp#$name"),
        )
        startXray(name, fakeProfile, config)
    }

    private fun startDns(name: String, dnsProfile: DnsTunnelProfile) {
        if (VpnManager.activeKind.value != VpnManager.KIND_MASTERDNS) {
            VpnManager.setActiveKind(VpnManager.KIND_DNS_TUNNEL)
        }
        VpnManager.onStarting(name)
        thread(name = "desktop-dns") {
            val dns = DesktopDnsTunnel(dnsProfile, work, listenPort = DNS_PORT)
            dnsTunnel = dns
            val port = dns.start()
            if (port < 0) { VpnManager.onError("DNS tunnel failed to start"); return@thread }

            val upstreamPort = if (dnsProfile.sshEnabled) {
                val directSsh = dnsProfile.sshHost.isBlank() ||
                    dnsProfile.sshHost == "127.0.0.1" ||
                    dnsProfile.sshHost == "localhost" ||
                    dnsProfile.sshHost.equals(dnsProfile.domain.trim(), ignoreCase = true)

                val sshProfile = SshProfile(
                    host = if (directSsh) "127.0.0.1" else dnsProfile.sshHost,
                    port = if (directSsh) port else dnsProfile.sshPort,
                    username = dnsProfile.sshUsername,
                    authType = dnsProfile.sshAuthType,
                    password = dnsProfile.sshPassword,
                    privateKey = dnsProfile.sshPrivateKey,
                    keyPassphrase = dnsProfile.sshKeyPassphrase,
                )
                val controller = SshController(
                    profile = sshProfile,
                    cipher = "auto",
                    compression = false,
                    listenPort = SSH_PORT,
                    proxySocksHost = if (directSsh) null else "127.0.0.1",
                    proxySocksPort = if (directSsh) 0 else port,
                    proxySocksUser = if (directSsh) "" else dnsProfile.socksUser,
                    proxySocksPass = if (directSsh) "" else dnsProfile.socksPass,
                )
                ssh = controller
                val sshPort = controller.start()
                if (sshPort < 0) {
                    dns.stop(); ssh = null
                    VpnManager.onError("SSH-over-DNS failed to connect: ${controller.lastError}")
                    return@thread
                }
                sshPort
            } else port

            val shimPort = localPort(SHIM_PORT, setOf(port, upstreamPort))
            val bridge = SocksTunBridge("127.0.0.1", shimPort, "127.0.0.1", upstreamPort, dnsHost = "8.8.8.8")
            shim = bridge
            if (!bridge.start()) { stop(); VpnManager.onError("Shim failed to start"); return@thread }

            val bypass = resolveHosts(dnsResolverHosts(dnsProfile) + listOfNotNull(dnsProfile.sshHost.takeIf { it.isNotBlank() }))
            if (!route(shimPort, bypass, udpOverTcp = true)) {
                stop(); VpnManager.onError("Could not route traffic (TUN + system proxy failed)"); return@thread
            }
            VpnManager.activeSocksPort = shimPort
            startShimStats(bridge)
            VpnManager.onConnected(name)
        }
    }

    private fun startTor(name: String) {
        VpnManager.setActiveKind(VpnManager.KIND_TOR)
        VpnManager.onStarting(name)
        thread(name = "desktop-tor") {
            val engine = DesktopTor(settingsProvider(), work)
            tor = engine
            var reported = -1
            val started = engine.start(onProgress = { pct ->
                if (pct > reported) {
                    reported = pct
                    LogBus.append("I/Tor bootstrapped $pct%")
                }
            })
            if (tor !== engine) return@thread
            started.exceptionOrNull()?.let { e ->
                tor = null
                VpnManager.onError(e.message ?: "Tor failed to start")
                return@thread
            }
            if (!connectThrough(engine.socksPort, "Tor")) {
                engine.stop()
                tor = null
                return@thread
            }
            VpnManager.onConnected(name)
        }
    }

    private fun startPsiphon(name: String, profile: PsiphonProfile) {
        VpnManager.setActiveKind(VpnManager.KIND_PSIPHON)
        VpnManager.onStarting(name)
        thread(name = "desktop-psiphon") {
            val socksPort = localPort(LocalPorts.PSIPHON_SOCKS)
            val httpPort = localPort(LocalPorts.PSIPHON_HTTP, setOf(socksPort))
            val engine = DesktopPsiphon(profile, work, socksPort, httpPort)
            psiphon = engine
            val started = engine.start(onNotice = { notice ->
                if (notice.type == "ConnectedServerRegion") {
                    LogBus.append("I/Psiphon exit region ${notice.string("region").orEmpty()}")
                }
            })
            if (psiphon !== engine) return@thread
            started.exceptionOrNull()?.let { e ->
                psiphon = null
                VpnManager.onError(e.message ?: "Psiphon failed to start")
                return@thread
            }
            if (!connectThrough(engine.socksPort, "Psiphon")) {
                engine.stop()
                psiphon = null
                return@thread
            }
            VpnManager.onConnected(name)
        }
    }

    private fun readSystemInterfaceTotals(): Pair<Long, Long>? = runCatching {
        if (Os.current == Os.WINDOWS) {
            val p = Runtime.getRuntime().exec("netstat -e")
            val text = p.inputStream.bufferedReader().use { it.readText() }
            p.waitFor()
            val match = Regex("""Bytes\s+(\d+)\s+(\d+)""").find(text)
            if (match != null) {
                val rx = match.groupValues[1].toLong()
                val tx = match.groupValues[2].toLong()
                rx to tx
            } else null
        } else null
    }.getOrNull()

    private fun startSstp(name: String, profile: dev.valid7996.narcicgeteway.domain.config.SstpProfile) {
        VpnManager.setActiveKind(VpnManager.KIND_SSTP)
        VpnManager.onStarting(name)
        thread(name = "desktop-sstp") {
            val engine = DesktopSstp(profile, work)
            sstp = engine
            val connected = engine.connect()
            if (sstp !== engine) {
                if (connected.isSuccess) engine.disconnect()
                return@thread
            }
            connected.exceptionOrNull()?.let { e ->
                sstp = null
                VpnManager.onError(e.message ?: "SSTP failed to connect")
                return@thread
            }
            LogBus.append("I/SSTP the system VPN '${DesktopSstp.NAME}' carries all traffic")
            VpnManager.onConnected(name)
            meter = DesktopMeter(totals = ::readSystemInterfaceTotals).also { it.start() }
            while (sstp === engine) {
                Thread.sleep(15_000)
                if (sstp === engine && !engine.isUp()) {
                    sstp = null
                    VpnManager.onError("SSTP disconnected")
                    break
                }
            }
        }
    }

    private fun startCrossChain(
        name: String,
        profile: dev.valid7996.narcicgeteway.domain.config.VpnProfile,
        config: ConfigRepository,
    ) {
        VpnManager.setActiveKind(VpnManager.KIND_CROSS_CHAIN)
        VpnManager.onStarting(name)
        thread(name = "desktop-crosschain") {
            val members = try {
                config.crossChainMembers(profile)
            } catch (e: Exception) {
                VpnManager.onError(e.message ?: "Cross-chain members invalid")
                return@thread
            }
            val (inner, outer) = members
            val carrierPort = localPort(10808)
            LogBus.append("I/CrossChain starting outer carrier (${outer.name}) on SOCKS port $carrierPort")

            var carrierOk = false
            when {
                outer.psiphonSettings() != null -> {
                    val pEngine = DesktopPsiphon(outer.psiphonSettings()!!, work, carrierPort, localPort(carrierPort + 1))
                    psiphon = pEngine
                    carrierOk = pEngine.start().isSuccess
                }
                outer.isTor -> {
                    val tEngine = DesktopTor(settingsProvider(), work)
                    tor = tEngine
                    carrierOk = tEngine.start().isSuccess
                }
                outer.dnsTunnelSettings() != null -> {
                    val dEngine = DesktopDnsTunnel(outer.dnsTunnelSettings()!!, work, listenPort = carrierPort)
                    dnsTunnel = dEngine
                    carrierOk = dEngine.start() > 0
                }
                else -> {
                    val settings = settingsProvider()
                    val options = settings.toBuildOptions(geoAssetsAvailable = geoAssetsAvailable(), carrier = true)
                    val carrierJson = outer.toXrayConfigJson(options)
                    val ported = DesktopXray.withSocksPort(carrierJson, LocalProxy.SOCKS_PORT, carrierPort)
                    val core = XrayCore(File(work, "carrier").apply { mkdirs() })
                    if (core.start(ported)) {
                        xray = core
                        carrierOk = true
                    }
                }
            }

            if (!carrierOk) {
                VpnManager.onError("Cross-chain outer engine failed to start")
                teardown()
                return@thread
            }

            LogBus.append("I/CrossChain carrier active; starting inner (${inner.name}) through $carrierPort")

            val settings = settingsProvider()
            val options = settings.toBuildOptions(geoAssetsAvailable = geoAssetsAvailable())
            val baseInnerJson = inner.toXrayConfigJson(options)
            val chainedJson = DesktopXray.withUpstreamSocks(baseInnerJson, carrierPort)

            val socksPort = localPort(LocalProxy.SOCKS_PORT, setOf(carrierPort))
            val metricsPort = localPort(METRICS_PORT, setOf(socksPort, carrierPort))
            val finalJson = DesktopXray.augmentWithMetrics(
                DesktopXray.withSocksPort(chainedJson, LocalProxy.SOCKS_PORT, socksPort),
                metricsPort,
            )

            val core2 = XrayCore(File(work, "inner").apply { mkdirs() })
            innerXray = core2
            if (!core2.start(finalJson)) {
                VpnManager.onError("Cross-chain inner engine failed to start")
                teardown()
                return@thread
            }

            val bypass = resolveHosts(DesktopXray.extractServerHosts(finalJson))
            if (!route(socksPort, bypass, udpOverTcp = false)) {
                core2.stop()
                teardown()
                VpnManager.onError("Could not route traffic")
                return@thread
            }

            VpnManager.activeSocksPort = socksPort
            DesktopStats(metricsPort).also { stats = it; it.start() }
            VpnManager.onConnected(name)
        }
    }

    private fun startIkev2(name: String, profile: Ikev2Profile) {
        VpnManager.setActiveKind(VpnManager.KIND_IKEV2)
        VpnManager.onStarting(name)
        thread(name = "desktop-ikev2") {
            val engine = DesktopIkev2(profile, work)
            ikev2 = engine
            val connected = engine.connect()
            if (ikev2 !== engine) {
                if (connected.isSuccess) engine.disconnect()
                return@thread
            }
            connected.exceptionOrNull()?.let { e ->
                ikev2 = null
                VpnManager.onError(e.message ?: "IKEv2 failed to connect")
                return@thread
            }
            LogBus.append("I/IKEv2 the system VPN '${DesktopIkev2.NAME}' carries all traffic")
            VpnManager.onConnected(name)
            meter = DesktopMeter(totals = ::readSystemInterfaceTotals).also { it.start() }
            while (ikev2 === engine) {
                Thread.sleep(15_000)
                if (ikev2 === engine && !engine.isUp()) {
                    ikev2 = null
                    VpnManager.onError("IKEv2 disconnected")
                    break
                }
            }
        }
    }

    private fun connectThrough(upstreamPort: Int, engine: String): Boolean {
        val socksPort = localPort(LocalProxy.SOCKS_PORT, setOf(upstreamPort))
        val metricsPort = localPort(METRICS_PORT, setOf(socksPort, upstreamPort))
        val json = DesktopXray.augmentWithMetrics(DesktopXray.frontConfig(socksPort, upstreamPort), metricsPort)
        val core = XrayCore(work)
        if (!core.start(json)) {
            VpnManager.onError("Core failed to start")
            return false
        }
        xray = core
        if (!route(socksPort, emptyList(), udpOverTcp = true, tunCarries = engine)) {
            core.stop()
            xray = null
            VpnManager.onError("Could not route traffic (system proxy failed)")
            return false
        }
        VpnManager.activeSocksPort = socksPort
        DesktopStats(metricsPort).also { stats = it; it.start() }
        return true
    }

    private fun dnsResolverHosts(p: DnsTunnelProfile): List<String> =
        if (p.dnsTransport == DnsTunnelProfile.TRANSPORT_DOH) {
            runCatching { java.net.URI(p.dohUrl.trim()).host }.getOrNull()?.let { listOf(it) } ?: emptyList()
        } else {
            p.resolvers.split(',', '\n').map { it.trim() }.filter { it.isNotEmpty() }.map { hp ->
                if (hp.startsWith("[")) hp.substringAfter('[').substringBefore(']')
                else hp.substringBefore(':')
            }
        }

    private fun startXray(
        name: String,
        profile: dev.valid7996.narcicgeteway.domain.config.VpnProfile,
        config: ConfigRepository,
    ) {
        val settings = settingsProvider()
        val options = settings.toBuildOptions(
            geoAssetsAvailable = geoAssetsAvailable(),
            ruleOutbounds = ruleOutboundsProvider(
                dev.valid7996.narcicgeteway.domain.config.RoutingMigration.effectiveRulesets(settings),
            ),
        )

        dev.valid7996.narcicgeteway.core.AutoSelect.clearPrepared()
        val baseJson = runCatching {
            if (profile.isAutoSelect) {
                val build = config.buildAutoSelectConfig(
                    profile,
                    options,
                    settings.autoSelectTuning(),
                )
                dev.valid7996.narcicgeteway.core.AutoSelect.prepare(profile.id, build.memberProfiles)
                build.json
            } else if (profile.isProxyChain) {
                chainConfigProvider(profile, options)
                    ?: error("Proxy chain members are unavailable")
            } else {
                profile.toXrayConfigJson(options)
            }
        }.getOrElse {
            VpnManager.onError(it.message ?: "This config type isn't supported on desktop yet"); return
        }
        val socksPort = localPort(LocalProxy.SOCKS_PORT)
        val metricsPort = localPort(METRICS_PORT, setOf(socksPort))
        val ported = DesktopXray.augmentWithMetrics(
            DesktopXray.withSocksPort(baseJson, LocalProxy.SOCKS_PORT, socksPort),
            metricsPort,
        )
        VpnManager.setActiveKind(VpnManager.KIND_XRAY)
        VpnManager.onStarting(name)
        thread(name = "desktop-xray") {
            val json = if (runMode == RunMode.Vpn && TunMode.supported() && TunMode.usesZeptun()) {
                val iface = PhysicalInterface.detect()
                if (iface != null) {
                    LogBus.append("I/Desktop the core's own connections leave through $iface, outside the tunnel")
                    DesktopXray.bindOutbounds(ported, iface, pinServerAddresses(DesktopXray.extractServerHosts(ported)))
                } else {
                    LogBus.append("W/Desktop could not find the network adapter; only the servers are kept outside the tunnel")
                    ported
                }
            } else {
                ported
            }
            val core = XrayCore(work)
            if (!core.start(json)) { VpnManager.onError("Core failed to start"); return@thread }
            xray = core
            dev.valid7996.narcicgeteway.core.AutoSelect.activate()
            val bypass = resolveHosts(DesktopXray.extractServerHosts(json))
            if (!route(socksPort, bypass, udpOverTcp = false)) {
                core.stop(); xray = null
                VpnManager.onError("Could not route traffic (TUN + system proxy failed)"); return@thread
            }
            VpnManager.activeSocksPort = socksPort
            DesktopStats(metricsPort).also { stats = it; it.start() }
            VpnManager.onConnected(name)
        }
    }

    private fun buildStandaloneSingBox(
        profile: dev.valid7996.narcicgeteway.domain.config.VpnProfile,
        socksPort: Int,
        clashApiPort: Int,
    ): Pair<String, List<String>> {
        val effective = profile.resolveUnsealed() ?: profile
        val aether = effective.aetherSettings()
        val oc = effective.openConnectSettings()
        val singBoxSrc = effective.source as? ProfileSource.SingBox
        val singBoxCfg = effective.source as? ProfileSource.SingBoxConfig

        val bypassHosts = mutableListOf<String>()

        val fullConfigJson: String = when {
            singBoxCfg != null -> singBoxCfg.json
            aether != null -> {
                val aetherWork = File(System.getProperty("java.io.tmpdir"), "narcicgeteway/aether")
                val wgFile = File(aetherWork, "wireguard.json")
                val keyObj = if (wgFile.exists()) {
                    runCatching {
                        Json { ignoreUnknownKeys = true }
                            .parseToJsonElement(wgFile.readText()).jsonObject
                    }.getOrNull()
                } else null

                val endpoint = aether.endpointText() ?: "engage.cloudflareclient.com:2408"
                val host = endpoint.substringBeforeLast(':').removeSurrounding("[", "]")
                val port = endpoint.substringAfterLast(':').toIntOrNull() ?: 2408
                bypassHosts += host

                val privKey = keyObj?.get("private_key")?.jsonPrimitive?.contentOrNull
                    ?: "aGVsY29tZXRveHJheWNvcmV3aXJlZ3VhcmRzZWNyZXQ="
                val pubKey = keyObj?.get("peer_public_key")?.jsonPrimitive?.contentOrNull
                    ?: "bmXOC+F1FxEMF9dyiK2H5/1SUtzH0JuVo51h2wPfgyo="
                val ipv4 = keyObj?.get("ipv4")?.jsonPrimitive?.contentOrNull ?: "172.16.0.2/32"
                val ipv6 = keyObj?.get("ipv6")?.jsonPrimitive?.contentOrNull
                    ?: "2606:4700:110:8f86:31df:ec01:b241:1dc5/128"
                val reservedJson = keyObj?.get("reserved")

                val outbound = buildJsonObject {
                    put("type", "wireguard")
                    put("tag", "proxy")
                    put("server", host)
                    put("server_port", port)
                    putJsonArray("local_address") {
                        add(JsonPrimitive(ipv4))
                        add(JsonPrimitive(ipv6))
                    }
                    put("private_key", privKey)
                    put("peer_public_key", pubKey)
                    if (reservedJson != null) put("reserved", reservedJson)
                }
                wrapOutboundToFullSingBox(outbound.toString(), "proxy", socksPort, clashApiPort)
            }
            oc != null -> {
                val host = oc.serverUrl().removePrefix("https://").removePrefix("http://").substringBefore('/').substringBefore(':')
                bypassHosts += host
                val outbound = buildJsonObject {
                    put("type", "openconnect")
                    put("tag", "proxy")
                    put("server", host)
                    put("server_port", oc.serverPort())
                    if (oc.username.isNotBlank()) put("username", oc.username)
                    if (oc.password.isNotBlank()) put("password", oc.password)
                    if (oc.authgroup.isNotBlank()) put("authgroup", oc.authgroup)
                    if (oc.disableDtls) put("disable_dtls", true)
                }
                wrapOutboundToFullSingBox(outbound.toString(), "proxy", socksPort, clashApiPort)
            }
            singBoxSrc != null -> {
                val raw = singBoxSrc.json
                val servers = SingBoxJson.servers(raw)
                servers.forEach { s -> s.address?.let { bypassHosts += it } }
                val tag = servers.firstOrNull()?.tag ?: "proxy"
                if (SingBoxJson.shapeOf(raw) == SingBoxJson.Shape.FullConfig) {
                    raw
                } else {
                    wrapOutboundToFullSingBox(raw, tag, socksPort, clashApiPort)
                }
            }
            effective.isOpenVpn -> {
                val payload = effective.rawPayload() ?: ""
                val frag = if (OvpnConfig.looksLikeOvpn(payload)) {
                    OvpnConfig.toSingBoxFragment(payload)
                } else {
                    payload
                }
                val retyped = SingBoxJson.retypeOpenVpn(frag) ?: frag
                val servers = SingBoxJson.servers(retyped)
                servers.forEach { s -> s.address?.let { bypassHosts += it } }
                val tag = servers.firstOrNull()?.tag ?: "proxy"
                wrapOutboundToFullSingBox(retyped, tag, socksPort, clashApiPort)
            }
            else -> error("Not a supported sing-box profile")
        }

        if (bypassHosts.isEmpty() && effective.address.isNotBlank() && effective.address != "-") {
            bypassHosts += effective.address
        }

        val prepared = SingBoxConfigs.prepareForDesktop(
            fullConfigJson,
            socksPort = socksPort,
            clashApiPort = clashApiPort,
        )
        return prepared.json to bypassHosts
    }

    private fun wrapOutboundToFullSingBox(
        fragmentOrOutbound: String,
        targetTag: String,
        socksPort: Int,
        clashApiPort: Int,
    ): String {
        val root = runCatching {
            Json { ignoreUnknownKeys = true; isLenient = true }
                .parseToJsonElement(fragmentOrOutbound)
        }.getOrNull()

        val parsedObj = root as? JsonObject

        val outbounds = buildJsonArray {
            if (parsedObj != null) {
                if (parsedObj.containsKey("outbounds")) {
                    val list = parsedObj["outbounds"] as? JsonArray
                    list?.forEach { add(it) }
                } else {
                    add(parsedObj)
                }
            }
            addJsonObject {
                put("type", "direct")
                put("tag", "direct")
            }
            addJsonObject {
                put("type", "dns")
                put("tag", "dns-out")
            }
        }

        val endpoints = parsedObj?.get("endpoints") as? JsonArray

        return buildJsonObject {
            putJsonObject("log") { put("level", "warn") }
            putJsonArray("inbounds") {
                addJsonObject {
                    put("type", "socks")
                    put("tag", "socks-in")
                    put("listen", "127.0.0.1")
                    put("listen_port", socksPort)
                }
            }
            put("outbounds", outbounds)
            if (endpoints != null && endpoints.isNotEmpty()) {
                put("endpoints", endpoints)
            }
            putJsonObject("route") {
                putJsonArray("rules") {
                    addJsonObject {
                        put("protocol", "dns")
                        put("action", "hijack-dns")
                    }
                    addJsonObject {
                        put("action", "sniff")
                    }
                }
                put("final", targetTag)
            }
            putJsonObject("dns") {
                putJsonArray("servers") {
                    addJsonObject {
                        put("tag", "dns-remote")
                        put("address", "tcp://8.8.8.8")
                        put("detour", targetTag)
                    }
                    addJsonObject {
                        put("tag", "dns-direct")
                        put("address", "8.8.8.8")
                        put("detour", "direct")
                    }
                }
                put("strategy", "prefer_ipv4")
            }
            putJsonObject("experimental") {
                putJsonObject("clash_api") {
                    put("external_controller", "127.0.0.1:$clashApiPort")
                }
            }
        }.toString()
    }

    private fun startSingBox(name: String, profile: dev.valid7996.narcicgeteway.domain.config.VpnProfile) {
        val socksPort = localPort(LocalProxy.SOCKS_PORT)
        val clashApiPort = localPort(METRICS_PORT, setOf(socksPort))
        val (finalJson, bypassHosts) = runCatching {
            buildStandaloneSingBox(profile, socksPort, clashApiPort)
        }.getOrElse {
            VpnManager.onError(it.message ?: "Failed to build sing-box config")
            return
        }

        VpnManager.setActiveKind(VpnManager.KIND_SINGBOX)
        VpnManager.onStarting(name)
        thread(name = "desktop-singbox") {
            val core = XrayCore(work)
            if (!core.startSingBox(finalJson)) {
                VpnManager.onError("sing-box failed to start")
                return@thread
            }
            xray = core

            val bypass = resolveHosts(bypassHosts)
            if (!route(socksPort, bypass, udpOverTcp = false)) {
                core.stop()
                xray = null
                VpnManager.onError("Could not route traffic (TUN + system proxy failed)")
                return@thread
            }
            VpnManager.activeSocksPort = socksPort
            DesktopSingBoxStats("127.0.0.1:$clashApiPort", null).also { singBoxStats = it; it.start() }
            VpnManager.onConnected(name)
        }
    }

    private fun startSsh(name: String, sshProfile: dev.valid7996.narcicgeteway.domain.config.SshProfile) {
        VpnManager.setActiveKind(VpnManager.KIND_SSH)
        VpnManager.onStarting(name)
        thread(name = "desktop-ssh") {
            val controller = SshController(
                profile = sshProfile,
                cipher = "auto",
                compression = false,
                listenPort = SSH_PORT,
            )
            ssh = controller
            val sshPort = controller.start()
            if (sshPort < 0) { VpnManager.onError("SSH failed to connect"); return@thread }

            val shimPort = localPort(SHIM_PORT, setOf(sshPort))
            val bridge = SocksTunBridge("127.0.0.1", shimPort, "127.0.0.1", sshPort, dnsHost = "8.8.8.8")
            shim = bridge
            if (!bridge.start()) { controller.stop(); ssh = null; VpnManager.onError("Shim failed"); return@thread }
            val bypass = resolveHosts(listOf(sshProfile.host))
            if (!route(shimPort, bypass, udpOverTcp = true)) {
                bridge.stop(); controller.stop(); ssh = null
                VpnManager.onError("Could not route traffic"); return@thread
            }
            VpnManager.activeSocksPort = shimPort
            startShimStats(bridge)
            VpnManager.onConnected(name)
        }
    }

    private fun localPort(preferred: Int, taken: Set<Int> = emptySet()): Int =
        LocalPortPicker.pick(preferred, taken).also { port ->
            if (port != preferred) {
                LogBus.append("W/Desktop port $preferred is in use by another program; using $port instead")
            }
        }

    private fun route(socksPort: Int, bypassIps: List<String>, udpOverTcp: Boolean, tunCarries: String? = null): Boolean {
        if (runMode == RunMode.ProxyOnly) {
            LogBus.append("I/Desktop SOCKS and HTTP proxy on 127.0.0.1:$socksPort; the system proxy is left alone")
            return true
        }
        if (runMode == RunMode.Vpn && tunCarries != null) {
            LogBus.append("W/Desktop $tunCarries reaches too many servers to route around a tunnel; using the system proxy")
        }
        val useTun = runMode == RunMode.Vpn && tunCarries == null
        if (useTun && !TunMode.supported()) {
            LogBus.append("W/Desktop VPN mode is not available on ${Os.current} yet; using the system proxy")
        }
        if (useTun && TunMode.supported()) {
            LogBus.append("I/Desktop TUN bypass: ${bypassIps.joinToString().ifEmpty { "(none)" }}")
            val engine = TunMode.Factory.create(
                work, socksPort, bypassIps, udpOverTcp, settingsProvider().vpnDns, AdminPassword::ask,
            )
            if (engine != null && engine.start()) { tun = engine; return true }
            LogBus.append("W/Desktop TUN unavailable — falling back to system proxy")
        }
        return if (SystemProxy.set("127.0.0.1", socksPort)) { usedSystemProxy = true; true } else false
    }

    private fun startShimStats(bridge: SocksTunBridge) {
        var totalDown = 0L
        var totalUp = 0L
        meter = DesktopMeter(totals = {
            val (down, up) = bridge.readDelta()
            totalDown += down
            totalUp += up
            totalDown to totalUp
        }).also { it.start() }
    }

    private fun pinServerAddresses(hosts: List<String>): Map<String, List<String>> =
        hosts.filter { host -> host.any { it.isLetter() } && ':' !in host }.associateWith { host ->
            val all = runCatching { InetAddress.getAllByName(host).toList() }.getOrDefault(emptyList())
            all.filterIsInstance<java.net.Inet4Address>().ifEmpty { all }.mapNotNull { it.hostAddress }.distinct()
        }.filterValues { it.isNotEmpty() }

    private fun resolveHosts(hosts: List<String>): List<String> = hosts.flatMap { host ->
        runCatching { InetAddress.getAllByName(host).map { it.hostAddress } }.getOrDefault(emptyList())
    }.distinct()

    fun stop() {
        if (!VpnManager.onStopping()) return
        thread(name = "desktop-vpn-stop") {
            teardown()
            VpnManager.onDisconnected()
        }
    }

    fun shutdown() = teardown()

    @Synchronized
    private fun teardown() {
        meter?.stop(); meter = null
        stats?.stop(); stats = null
        singBoxStats?.stop(); singBoxStats = null
        tun?.stop(); tun = null
        tor?.let { tor = null; it.stop() }
        ikev2?.let { ikev2 = null; it.disconnect() }
        sstp?.let { sstp = null; it.disconnect() }
        psiphon?.let { psiphon = null; it.stop() }
        shim?.stop(); shim = null
        ssh?.stop(); ssh = null
        dnsTunnel?.stop(); dnsTunnel = null
        if (usedSystemProxy) { runCatching { SystemProxy.clear() }; usedSystemProxy = false }
        xray?.stop(); xray = null
        innerXray?.stop(); innerXray = null
        VpnManager.setActiveKind(null)
        dev.valid7996.narcicgeteway.core.AutoSelect.end()
    }
}
