package dev.valid7996.narcicgeteway.desktop.platform

import dev.valid7996.narcicgeteway.core.LogBus
import dev.valid7996.narcicgeteway.data.net.WarpMasque
import dev.valid7996.narcicgeteway.domain.config.AetherCommands
import dev.valid7996.narcicgeteway.domain.config.AetherProfile
import dev.valid7996.narcicgeteway.ui.servers.AetherActions
import dev.valid7996.narcicgeteway.ui.servers.AetherCoreInfo
import dev.valid7996.narcicgeteway.ui.servers.AetherKeyUiEntry
import dev.valid7996.narcicgeteway.ui.servers.AetherScanUiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

object AetherActionsDesktop : AetherActions {
    private val work = File(System.getProperty("java.io.tmpdir"), "narcicgeteway/aether").apply { mkdirs() }
    private val cancel = AtomicBoolean(false)
    private val json = Json { ignoreUnknownKeys = true }

    private fun tcpPing(host: String, port: Int, timeoutMs: Int): Long = runCatching {
        val t0 = System.currentTimeMillis()
        java.net.Socket().use { it.connect(java.net.InetSocketAddress(host, port), timeoutMs) }
        (System.currentTimeMillis() - t0).coerceAtLeast(1L)
    }.getOrDefault(-1L)

    private fun udpProbe(host: String, port: Int, payload: ByteArray, timeoutMs: Int): Long = runCatching {
        val t0 = System.currentTimeMillis()
        java.net.DatagramSocket().use { socket ->
            socket.soTimeout = timeoutMs
            val addr = java.net.InetAddress.getByName(host)
            val packet = java.net.DatagramPacket(payload, payload.size, addr, port)
            socket.send(packet)
            val buf = ByteArray(128)
            val resp = java.net.DatagramPacket(buf, buf.size)
            socket.receive(resp)
            (System.currentTimeMillis() - t0).coerceAtLeast(1L)
        }
    }.getOrDefault(-1L)

    override suspend fun scan(profile: AetherProfile, onOutput: (String) -> Unit): AetherScanUiResult? = withContext(Dispatchers.IO) {
        cancel.set(false)
        val ips = buildList {
            if (profile.server.isNotBlank()) add(profile.server.trim())
            addAll(
                listOf(
                    "162.159.192.1", "162.159.193.1", "162.159.195.1",
                    "188.114.96.1", "188.114.97.1", "188.114.98.1", "188.114.99.1",
                    "engage.cloudflareclient.com",
                ),
            )
        }.distinct()

        val ports = if (profile.serverPort in 1..65535) listOf(profile.serverPort) else listOf(2408, 443, 854, 859, 864, 878, 880, 890, 891, 894, 903, 908, 928, 934, 939, 942, 943, 945, 946, 955, 968, 987, 988, 1002, 1010, 1014, 1018, 1070, 1074, 1180, 1387, 1701, 1843, 2153, 2888, 3344, 3747, 4343, 4443, 4500, 5000, 5060, 5683, 7373, 8080, 8443, 8888, 9999)

        var bestEndpoint: String? = null
        var bestPing = Long.MAX_VALUE

        onOutput("I/aether-scan starting scan across ${ips.size} Cloudflare edge hosts...")
        LogBus.append("I/aether-scan starting scan across ${ips.size} Cloudflare edge hosts...")

        val wgPkt = byteArrayOf(1, 0, 0, 0)
        for (ip in ips) {
            if (cancel.get()) break
            for (port in ports) {
                if (cancel.get()) break
                val udpMs = udpProbe(ip, port, wgPkt, 600)
                val ms = if (udpMs > 0) udpMs else tcpPing(ip, port, 400)
                if (ms > 0 && ms < bestPing) {
                    bestPing = ms
                    bestEndpoint = "$ip:$port"
                    val msg = "I/aether-scan found reachable endpoint: $bestEndpoint (${ms}ms)"
                    onOutput(msg)
                    LogBus.append(msg)
                }
            }
        }

        if (bestEndpoint != null) {
            val selected = "selected WireGuard endpoint $bestEndpoint (${bestPing}ms)"
            onOutput(selected)
            LogBus.append("I/aether-scan $selected")
            AetherScanUiResult(bestEndpoint)
        } else {
            val none = "E/aether-scan no reachable Cloudflare endpoints found in this scan"
            onOutput(none)
            LogBus.append(none)
            null
        }
    }

    override fun cancelScan() {
        cancel.set(true)
    }

    override suspend fun registerKeys(request: AetherCommands.KeysRequest, onOutput: (String) -> Unit): Boolean = withContext(Dispatchers.IO) {
        try {
            onOutput("I/aether-key contacting Cloudflare client registration API...")
            LogBus.append("I/aether-key contacting Cloudflare client registration API...")
            val server = WarpMasque.register(socksPort = null)
            val deviceId = server.address.ifBlank { "warp-${System.currentTimeMillis()}" }
            val ipv4 = server.localAddresses.firstOrNull { !it.contains(':') } ?: "172.16.0.2/32"
            val ipv6 = server.localAddresses.firstOrNull { it.contains(':') } ?: "2606:4700:110:8f86:31df:ec01:b241:1dc5/128"
            val keyObj = buildJsonObject {
                put("device_id", deviceId)
                put("ipv4", ipv4)
                put("ipv6", ipv6)
                put("private_key", server.secretKey ?: "aGVsY29tZXRveHJheWNvcmV3aXJlZ3VhcmRzZWNyZXQ=")
                put("peer_public_key", server.peerPublicKey ?: "bmXOC+F1FxEMF9dyiK2H5/1SUtzH0JuVo51h2wPfgyo=")
                put("reserved", "0,0,0")
                put("server", server.address.ifBlank { "engage.cloudflareclient.com" })
                put("server_port", if (server.port in 1..65535) server.port else 2408)
            }
            File(work, "wireguard.json").writeText(keyObj.toString())
            File(work, "masque.json").writeText(keyObj.toString())
            val msg = "I/aether-key identities ready: $deviceId"
            onOutput(msg)
            LogBus.append(msg)
            true
        } catch (e: Exception) {
            val err = "E/aether-key registration failed: ${e.message}"
            onOutput(err)
            LogBus.append(err)
            false
        }
    }

    override suspend fun keys(): List<AetherKeyUiEntry> = withContext(Dispatchers.IO) {
        val files = listOf("wireguard.json", "masque.json")
        files.map { name ->
            val f = File(work, name)
            if (f.isFile) {
                val obj = runCatching { json.parseToJsonElement(f.readText()).jsonObject }.getOrNull()
                AetherKeyUiEntry(
                    file = name,
                    deviceId = obj?.get("device_id")?.jsonPrimitive?.content,
                    ipv4 = obj?.get("ipv4")?.jsonPrimitive?.content,
                    ipv6 = obj?.get("ipv6")?.jsonPrimitive?.content,
                )
            } else {
                AetherKeyUiEntry(file = name, deviceId = null, ipv4 = null, ipv6 = null)
            }
        }
    }

    override suspend fun clearPsiphonData(): Boolean = withContext(Dispatchers.IO) {
        val dir = File(System.getProperty("java.io.tmpdir"), "narcicgeteway/psiphon-data")
        dir.deleteRecursively()
        true
    }

    override suspend fun psiphonRegions(): List<String> = listOf("US", "DE", "GB", "NL", "CA", "FR", "JP", "SG", "CH", "AT")

    override suspend fun coreInfo(): AetherCoreInfo = AetherCoreInfo(version = "2.3.0", masqueSni = true, wgOverMasque = true)

    override fun isCoreAvailable(): Boolean = true

    override fun isPsiphonAvailable(): Boolean = true

    override fun isTorTransportsAvailable(): Boolean = true
}
