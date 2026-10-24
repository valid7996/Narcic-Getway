package dev.valid7996.narcicgeteway.core

import android.content.Context
import android.content.Intent
import dev.valid7996.narcicgeteway.core.AppLog as Log
import dev.valid7996.narcicgeteway.domain.config.SniSpoofConfig
import dev.valid7996.narcicgeteway.domain.config.VpnProfile
import dev.valid7996.narcicgeteway.domain.model.AppSettings
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Client for managing the isolated RstaSpoofService from the main process.
 * By keeping this separate from RstaSpoofEngine, the main process never loads
 * `libsnispf.so` and avoids Go runtime clashing with `libgojni.so`.
 */
object RstaSpoofClient {

    private const val TAG = "RstaSpoofClient"
    private val probeLock = Mutex()

    private const val PORT = SniSpoofConfig.RSTA_LOCAL_PORT

    private data class Params(val ip: String, val port: Int, val sni: String, val method: String)

    private var current: Params? = null
    private var holders = 0

    @Volatile
    var activeWorkingIp: String? = null

    @Volatile
    var activeWorkingSni: String? = null

    /** Reorders candidate IPs by testing TCP socket connectivity quickly. Live IPs appear first. */
    fun orderIpsByReachability(ips: List<String>, port: Int, timeoutMs: Int = 1200): List<String> {
        if (ips.size <= 1) return ips
        val alive = mutableListOf<String>()
        val dead = mutableListOf<String>()
        for (ip in ips) {
            val ok = runCatching {
                Socket().use { s ->
                    s.connect(InetSocketAddress(ip, port), timeoutMs)
                    true
                }
            }.getOrDefault(false)
            if (ok) alive.add(ip) else dead.add(ip)
        }
        return if (alive.isNotEmpty()) alive + dead else ips
    }

    /** Prioritizes the cached verified working IP at the front of the candidate list. */
    fun orderIpsWithPriority(ips: List<String>, port: Int): List<String> {
        if (ips.isEmpty()) return ips
        val working = activeWorkingIp?.trim()
        val ordered = orderIpsByReachability(ips, port).toMutableList()
        if (working != null && ips.contains(working)) {
            ordered.remove(working)
            ordered.add(0, working)
        } else if (working != null && !ips.contains(working)) {
            activeWorkingIp = null
        }
        return ordered
    }

    /** Prioritizes the cached verified working SNI at the front of the candidate list. */
    fun orderSnisWithPriority(snis: List<String>): List<String> {
        if (snis.isEmpty()) return snis
        val working = activeWorkingSni?.trim()
        val list = snis.toMutableList()
        if (working != null && snis.contains(working)) {
            list.remove(working)
            list.add(0, working)
        } else if (working != null && !snis.contains(working)) {
            activeWorkingSni = null
        }
        return list
    }

    /**
     * The no-root SNI spoof as RstaNG keeps it: one engine on 127.0.0.1:[PORT], with the connect IP, port, fake SNI and
     * method of the app's settings, for every profile that points at that address. It is started for whoever needs it
     * first (a connection, or a batch of delay tests) and stopped when the last lets go; changed settings restart it.
     * Blocking: returns once the listener answers. Supports multi-IP and multi-SNI with automatic fallback.
     */
    @Synchronized
    fun acquire(
        context: Context,
        settings: AppSettings,
        specificIp: String? = null,
        specificSni: String? = null,
    ): Boolean {
        val ips = if (!specificIp.isNullOrBlank()) listOf(specificIp)
        else SniSpoofConfig.parseIpList(settings.rstaSpoofConnectIp).ifEmpty { listOf(SniSpoofConfig.defaultConnectIp) }
        val snis = if (!specificSni.isNullOrBlank()) listOf(specificSni)
        else SniSpoofConfig.parseSniList(settings.rstaSpoofFakeSni).ifEmpty { listOf(SniSpoofConfig.defaultFakeSni) }
        val port = settings.rstaSpoofConnectPort.takeIf { it in 1..65535 } ?: 443
        val method = SniSpoofConfig.method(settings.rstaSpoofMethod)

        val orderedIps = orderIpsWithPriority(ips, port)
        val orderedSnis = orderSnisWithPriority(snis)

        for (candidateIp in orderedIps) {
            for (candidateSni in orderedSnis) {
                val wanted = Params(
                    ip = candidateIp,
                    port = port,
                    sni = candidateSni,
                    method = method,
                )
                val up = current == wanted && awaitListener(PORT, timeoutMs = 400L)
                if (up) {
                    holders++
                    return true
                }
                start(context, PORT, wanted.ip, wanted.port, wanted.sni, wanted.method)
                current = wanted
                if (awaitListener(PORT, timeoutMs = 3_000L)) {
                    holders++
                    return true
                }
            }
        }

        stop(context)
        current = null
        holders = 0
        return false
    }

    /** One who needed the engine lets go; it stops with the last of them. */
    @Synchronized
    fun release(context: Context) {
        if (holders > 0) holders--
        if (holders == 0 && current != null) {
            val vpnActive = VpnManager.status.value.state.isActive
            if (vpnActive) {
                // Keep spoof engine alive while VPN connection is actively using it
                return
            }
            stop(context)
            current = null
        }
    }

    /** The lease of a batch of delay tests: acquires off the main thread, and gives back the function that releases. */
    suspend fun lease(context: Context, settings: AppSettings): (() -> Unit)? = withContext(Dispatchers.IO) {
        if (acquire(context, settings)) ({ release(context) }) else null
    }

    /** What the settings screen shows as the engine's state: the service's own word, while the listener really answers. */
    fun statusSummary(context: Context): String {
        val text = runCatching { File(context.filesDir, RstaSpoofService.STATUS_FILE).readText().trim() }.getOrDefault("")
        return when {
            text.startsWith("active") -> if (awaitListener(PORT, timeoutMs = 300L)) text else "inactive"
            text.isEmpty() -> "inactive"
            else -> text
        }
    }

    fun recentLog(context: Context): List<String> =
        runCatching { File(context.filesDir, RstaSpoofService.LOG_FILE).readLines().takeLast(200) }.getOrDefault(emptyList())

    fun start(context: Context, listenPort: Int, ip: String, port: Int, fakeSni: String, method: String) {
        val intent = Intent(context, RstaSpoofService::class.java).apply {
            action = RstaSpoofService.ACTION_START
            putExtra(RstaSpoofService.EXTRA_LISTEN_PORT, listenPort)
            putExtra(RstaSpoofService.EXTRA_IP, ip)
            putExtra(RstaSpoofService.EXTRA_PORT, port)
            putExtra(RstaSpoofService.EXTRA_FAKE_SNI, fakeSni)
            putExtra(RstaSpoofService.EXTRA_METHOD, method)
        }
        context.startService(intent)
    }

    fun stop(context: Context, listenPort: Int = 0) {
        val intent = Intent(context, RstaSpoofService::class.java).apply {
            action = RstaSpoofService.ACTION_STOP
            if (listenPort > 0) putExtra(RstaSpoofService.EXTRA_LISTEN_PORT, listenPort)
        }
        context.startService(intent)
    }

    fun awaitListener(port: Int, timeoutMs: Long = 4_000L): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val ok = runCatching {
                Socket().use {
                    it.connect(InetSocketAddress("127.0.0.1", port), 500)
                    true
                }
            }.getOrDefault(false)
            if (ok) return true
            try { Thread.sleep(150) } catch (_: InterruptedException) { return false }
        }
        return false
    }

    /** Rewrites an Xray JSON config to direct its proxy outbound through [listenPort] and use [candidateSni]. */
    fun rewriteProbeConfig(configJson: String, listenPort: Int, candidateSni: String): String {
        return runCatching {
            val root = JSONObject(configJson)
            val outbounds = root.optJSONArray("outbounds")
            if (outbounds != null) {
                for (i in 0 until outbounds.length()) {
                    val ob = outbounds.optJSONObject(i) ?: continue
                    val tag = ob.optString("tag")
                    val protocol = ob.optString("protocol")
                    if (tag == "proxy" || protocol in listOf("vless", "vmess", "trojan", "shadowsocks", "socks")) {
                        val settings = ob.optJSONObject("settings")
                        val vnext = settings?.optJSONArray("vnext")?.optJSONObject(0)
                        val servers = settings?.optJSONArray("servers")?.optJSONObject(0)
                        val targetNode = vnext ?: servers
                        if (targetNode != null) {
                            targetNode.put("address", "127.0.0.1")
                            targetNode.put("port", listenPort)
                        }
                        val stream = ob.optJSONObject("streamSettings")
                        val tlsSettings = stream?.optJSONObject("tlsSettings")
                        if (tlsSettings != null && candidateSni.isNotBlank()) {
                            tlsSettings.put("serverName", candidateSni)
                            tlsSettings.put("verifyPeerCertByName", candidateSni)
                        }
                        val realitySettings = stream?.optJSONObject("realitySettings")
                        if (realitySettings != null && candidateSni.isNotBlank()) {
                            realitySettings.put("serverName", candidateSni)
                        }
                    }
                }
            }
            root.toString()
        }.getOrElse {
            configJson.replace(": 40443", ": $listenPort")
                .replace(":40443", ":$listenPort")
        }
    }

    /**
     * Measures real outbound delay for an SNI spoof profile by dynamically testing candidates.
     * Starts an isolated engine listener on a free port for each candidate (IP, fake SNI) pair,
     * measures real outbound latency with a short timeout, and caches the first working candidate
     * in [activeWorkingIp] and [activeWorkingSni] for instant subsequent connections and tests.
     */
    suspend fun probeSmart(
        context: Context,
        settings: AppSettings,
        profile: VpnProfile,
        config: String,
        url: String,
    ): Long = probeLock.withLock {
        withContext(Dispatchers.IO) {
            val spoofSettings = profile.sniSpoofSettings()
            val ips = (spoofSettings?.let { SniSpoofConfig.parseIpList(it.cleanIp) }?.ifEmpty { null }
                ?: SniSpoofConfig.parseIpList(settings.rstaSpoofConnectIp))
                .ifEmpty { listOf(SniSpoofConfig.defaultConnectIp) }

            val snis = (spoofSettings?.let { SniSpoofConfig.parseSniList(it.fakeSni) }?.ifEmpty { null }
                ?: SniSpoofConfig.parseSniList(settings.rstaSpoofFakeSni))
                .ifEmpty { listOf(SniSpoofConfig.defaultFakeSni) }

            val port = spoofSettings?.connectPort?.takeIf { it in 1..65535 }
                ?: settings.rstaSpoofConnectPort.takeIf { it in 1..65535 }
                ?: 443
            val method = SniSpoofConfig.method(spoofSettings?.method ?: settings.rstaSpoofMethod)

            val orderedIps = orderIpsWithPriority(ips, port)
            val orderedSnis = orderSnisWithPriority(snis)

            val probePort = AetherSupport.findFreePort()

            for (candidateIp in orderedIps) {
                val resolvedIp = runCatching {
                    java.net.InetAddress.getByName(candidateIp).hostAddress
                }.getOrNull() ?: candidateIp

                for (candidateSni in orderedSnis) {
                    try {
                        start(context, probePort, resolvedIp, port, candidateSni, method)
                        if (!awaitListener(probePort, timeoutMs = 2_000L)) {
                            stop(context, probePort)
                            continue
                        }

                        val probeConfig = rewriteProbeConfig(config, probePort, candidateSni)

                        val delay = withTimeoutOrNull(2_200L) {
                            runCatching {
                                CoreProbe.measureOutboundDelay(probeConfig, url)
                            }.getOrDefault(-1L)
                        } ?: -1L

                        if (delay > 0) {
                            activeWorkingIp = candidateIp
                            activeWorkingSni = candidateSni
                            LogBus.append("I/PingService verified working spoof IP: $candidateIp (sni=$candidateSni, delay=${delay}ms)")
                            stop(context, probePort)
                            return@withContext delay
                        }
                    } catch (t: Throwable) {
                        Log.w(TAG, "probe error on $resolvedIp: ${t.message}")
                    } finally {
                        stop(context, probePort)
                    }
                }
            }
            -1L
        }
    }

    suspend fun probe(
        context: Context,
        ip: String,
        port: Int,
        fakeSni: String,
        method: String,
        block: suspend (Int) -> Long
    ): Long = probeLock.withLock {
        withContext(Dispatchers.IO) {
            val listen = AetherSupport.findFreePort()
            start(context, listen, ip, port, fakeSni, method)
            try {
                if (awaitListener(listen)) block(listen) else -1L
            } finally {
                stop(context, listen)
            }
        }
    }
}
