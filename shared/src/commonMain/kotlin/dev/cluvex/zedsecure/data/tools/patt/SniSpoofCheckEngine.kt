package dev.cluvex.zedsecure.data.tools.patt

import dev.cluvex.zedsecure.platform.httpGetText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

// ─── Configuration ───────────────────────────────────────────────────────────

val kDefaultSniTargets = """
hcaptcha.com
www.sciencedirect.com
auth.vercel.com
chess.com
unpkg.com
static.cloudflareinsights.com
www.speedtest.net
""".trimIndent()

val kDefaultSniPorts = listOf(443)
val kAllSniPorts = listOf(443, 2053, 2083, 2087, 2096, 8443)

private const val PUBLIC_IP_API_URL = "http://chabokan.net/ip/"
private const val FALLBACK_IP_API_URL = "https://api.ipify.org"

data class SniSpoofCheckConfig(
    val ports: List<Int> = kDefaultSniPorts,
    val timeout: Int = 5,
    val retries: Int = 3,
    val concurrency: Int = 20,
    val enableIpCheck: Boolean = true,
    val manualIp: String? = null,
)

// ─── Result types ────────────────────────────────────────────────────────────

enum class SniResultStatus {
    OK,
    FAIL,
    FILTERED,
    ERROR,
}

data class SniPortResult(
    val port: Int,
    val isOpen: Boolean,
)

data class SniIpCheckResult(
    val matched: Boolean,
    val detectedIp: String? = null,
)

data class SniIpResult(
    val target: String,
    val ip: String,
    val status: SniResultStatus,
    val portResults: List<SniPortResult> = emptyList(),
    val ipCheckResult: SniIpCheckResult? = null,
    val errorMessage: String? = null,
) {
    val openPortCount: Int get() = portResults.count { it.isOpen }

    fun toDisplayString(): String {
        val tag = "[${status.name}]"
        if (status == SniResultStatus.ERROR) {
            return "$tag $target (${errorMessage ?: "Could not resolve"})"
        }
        if (status == SniResultStatus.FILTERED) {
            return "$tag $target -> $ip (Blocked/Internal IP)"
        }

        val portStr = portResults.joinToString(" ") { p ->
            "${p.port}${if (p.isOpen) "✔" else "✖"}"
        }

        var line = "$tag $target -> $ip -> $portStr"
        if (ipCheckResult != null) {
            line += if (ipCheckResult.matched) {
                " IP✔"
            } else if (ipCheckResult.detectedIp != null) {
                " IP✖(${ipCheckResult.detectedIp})"
            } else {
                " IP✖"
            }
        }
        return line
    }
}

data class SniSpoofScanProgress(
    val latestResult: SniIpResult? = null,
    val completedTargets: Int = 0,
    val totalTargets: Int = 0,
    val allResults: List<SniIpResult> = emptyList(),
) {
    val progress: Float get() = if (totalTargets > 0) completedTargets.toFloat() / totalTargets else 0f
    val okCount: Int get() = allResults.count { it.status == SniResultStatus.OK }
    val failCount: Int get() = allResults.count { it.status == SniResultStatus.FAIL }
    val filteredCount: Int get() = allResults.count { it.status == SniResultStatus.FILTERED }
    val errorCount: Int get() = allResults.count { it.status == SniResultStatus.ERROR }
}

// ─── Service ─────────────────────────────────────────────────────────────────

class SniSpoofCheckService(
    var config: SniSpoofCheckConfig = SniSpoofCheckConfig(),
) {
    @Volatile
    private var isCancelled = false

    fun cancel() {
        isCancelled = true
    }

    companion object {
        private val IPV4_REGEX = Regex("""^(\d{1,3}\.){3}\d{1,3}$""")

        fun parseTargets(input: String): List<String> {
            return input.lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
        }

        fun isIpAddress(target: String): Boolean = IPV4_REGEX.matches(target)

        suspend fun resolveTarget(target: String): List<String> = withContext(Dispatchers.IO) {
            if (isIpAddress(target)) return@withContext listOf(target)
            try {
                val addresses = InetAddress.getAllByName(target)
                addresses.mapNotNull { addr ->
                    val hostAddr = addr.hostAddress ?: return@mapNotNull null
                    if (IPV4_REGEX.matches(hostAddr)) hostAddr else null
                }.distinct()
            } catch (_: Exception) {
                emptyList()
            }
        }

        suspend fun getUserPublicIp(): String? = withContext(Dispatchers.IO) {
            for (attempt in 0 until 3) {
                try {
                    val body = httpGetText(
                        url = PUBLIC_IP_API_URL,
                        userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
                        connectTimeoutMs = 7_000,
                        readTimeoutMs = 10_000,
                    )
                    val match = Regex(""""ip"\s*:\s*"([^"]+)"""").find(body)
                    if (match != null) return@withContext match.groupValues[1]

                    val plainIp = Regex("""\b(\d{1,3}\.){3}\d{1,3}\b""").find(body)
                    if (plainIp != null) return@withContext plainIp.value
                } catch (_: Exception) {
                    try {
                        val fallback = httpGetText(
                            url = FALLBACK_IP_API_URL,
                            userAgent = "curl/7.68.0",
                            connectTimeoutMs = 7_000,
                            readTimeoutMs = 10_000,
                        ).trim()
                        if (IPV4_REGEX.matches(fallback)) return@withContext fallback
                    } catch (_: Exception) {}
                }
                delay(800)
            }
            null
        }

        fun generateReport(
            results: List<SniIpResult>,
            config: SniSpoofCheckConfig,
        ): String {
            val sb = StringBuilder()
            sb.appendLine("=== SNI Spoof Check Report ===")
            sb.appendLine("Ports: ${config.ports.joinToString(",")}")
            sb.appendLine("Timeout: ${config.timeout}s | Retries: ${config.retries}")
            sb.appendLine("Total Checked: ${results.size}")
            sb.appendLine("---------------------------------------------------")

            val ok = results.filter { it.status == SniResultStatus.OK }
            val fail = results.filter { it.status == SniResultStatus.FAIL }
            val error = results.filter { it.status == SniResultStatus.ERROR }
            val filtered = results.filter { it.status == SniResultStatus.FILTERED }

            if (ok.isNotEmpty()) {
                sb.appendLine()
                sb.appendLine("=== OK (at least one open port) [${ok.size}] ===")
                ok.forEach { sb.appendLine(it.toDisplayString()) }
            }

            if (fail.isNotEmpty()) {
                sb.appendLine()
                sb.appendLine("=== FAIL (all ports closed) [${fail.size}] ===")
                fail.forEach { sb.appendLine(it.toDisplayString()) }
            }

            if (error.isNotEmpty()) {
                sb.appendLine()
                sb.appendLine("=== RESOLVE FAILED [${error.size}] ===")
                error.forEach { sb.appendLine(it.toDisplayString()) }
            }

            if (filtered.isNotEmpty()) {
                sb.appendLine()
                sb.appendLine("=== FILTERED (Blocked/IP 10.x) [${filtered.size}] ===")
                filtered.forEach { sb.appendLine(it.toDisplayString()) }
            }

            sb.appendLine("---------------------------------------------------")
            sb.appendLine("Scan finished.")
            return sb.toString()
        }
    }

    suspend fun checkPort(ip: String, port: Int): Boolean = withContext(Dispatchers.IO) {
        for (attempt in 0 until config.retries) {
            if (isCancelled) return@withContext false
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(ip, port), config.timeout * 1000)
                try { socket.close() } catch (_: Exception) {}
                return@withContext true
            } catch (_: Exception) {
                // retry
            }
        }
        false
    }

    suspend fun checkIp(domain: String, ip: String, userPublicIp: String): SniIpCheckResult =
        withContext(Dispatchers.IO) {
            var rawSocket: Socket? = null
            var sslSocket: SSLSocket? = null
            try {
                rawSocket = Socket()
                rawSocket.connect(InetSocketAddress(ip, 443), 10_000)

                val sslContext = SSLContext.getInstance("TLS")
                val trustAll = arrayOf<TrustManager>(object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                })
                sslContext.init(null, trustAll, SecureRandom())

                sslSocket = sslContext.socketFactory.createSocket(rawSocket, domain, 443, true) as SSLSocket
                val params = sslSocket.sslParameters
                params.serverNames = listOf(SNIHostName(domain))
                sslSocket.sslParameters = params
                sslSocket.soTimeout = 10_000

                sslSocket.startHandshake()

                val out = sslSocket.outputStream
                val req = "GET /cdn-cgi/trace HTTP/1.1\r\nHost: $domain\r\nConnection: close\r\n\r\n"
                out.write(req.toByteArray(Charsets.UTF_8))
                out.flush()

                val reader = BufferedReader(InputStreamReader(sslSocket.inputStream))
                val sb = StringBuilder()
                var line = reader.readLine()
                var linesRead = 0
                while (line != null && linesRead < 50) {
                    sb.append(line).append("\n")
                    line = reader.readLine()
                    linesRead++
                }

                val responseStr = sb.toString()
                val ipMatch = Regex("""(?m)^ip=(.+)$""").find(responseStr)
                if (ipMatch == null) {
                    return@withContext SniIpCheckResult(matched = false)
                }

                val detectedIp = ipMatch.groupValues[1].trim()
                SniIpCheckResult(
                    matched = detectedIp.equals(userPublicIp.trim(), ignoreCase = true),
                    detectedIp = detectedIp,
                )
            } catch (_: Exception) {
                SniIpCheckResult(matched = false)
            } finally {
                try { sslSocket?.close() } catch (_: Exception) {}
                try { rawSocket?.close() } catch (_: Exception) {}
            }
        }

    suspend fun scanTargets(
        targets: List<String>,
        onProgress: (SniSpoofScanProgress) -> Unit,
    ) = coroutineScope {
        isCancelled = false
        if (targets.isEmpty()) return@coroutineScope

        var userPublicIp: String? = null
        if (config.enableIpCheck) {
            userPublicIp = if (!config.manualIp.isNullOrBlank()) {
                config.manualIp
            } else {
                getUserPublicIp()
            }
        }

        var completedTargets = 0
        val allResults = mutableListOf<SniIpResult>()

        val chunks = targets.chunked(config.concurrency)
        for (chunk in chunks) {
            if (isCancelled) break

            val chunkDeferred = chunk.map { target ->
                async(Dispatchers.IO) {
                    processTarget(target, userPublicIp)
                }
            }

            val chunkResults = chunkDeferred.awaitAll()
            for (targetResults in chunkResults) {
                if (isCancelled) break
                completedTargets++
                allResults.addAll(targetResults)

                onProgress(
                    SniSpoofScanProgress(
                        latestResult = targetResults.lastOrNull(),
                        completedTargets = completedTargets,
                        totalTargets = targets.size,
                        allResults = allResults.toList(),
                    ),
                )
            }
        }
    }

    private suspend fun processTarget(
        target: String,
        userPublicIp: String?,
    ): List<SniIpResult> {
        val ips = resolveTarget(target)
        if (ips.isEmpty()) {
            return listOf(
                SniIpResult(
                    target = target,
                    ip = "",
                    status = SniResultStatus.ERROR,
                    errorMessage = "Could not resolve",
                ),
            )
        }

        val results = mutableListOf<SniIpResult>()
        for (ip in ips) {
            if (isCancelled) break

            if (ip.startsWith("10.")) {
                results.add(
                    SniIpResult(
                        target = target,
                        ip = ip,
                        status = SniResultStatus.FILTERED,
                    ),
                )
                continue
            }

            val portResults = mutableListOf<SniPortResult>()
            var openCount = 0

            for (port in config.ports) {
                if (isCancelled) break
                val isOpen = checkPort(ip, port)
                portResults.add(SniPortResult(port = port, isOpen = isOpen))
                if (isOpen) openCount++
            }

            if (openCount > 0) {
                var ipCheckResult: SniIpCheckResult? = null
                if (config.enableIpCheck && userPublicIp != null && !isIpAddress(target)) {
                    ipCheckResult = checkIp(target, ip, userPublicIp)
                }

                results.add(
                    SniIpResult(
                        target = target,
                        ip = ip,
                        status = SniResultStatus.OK,
                        portResults = portResults,
                        ipCheckResult = ipCheckResult,
                    ),
                )
            } else {
                results.add(
                    SniIpResult(
                        target = target,
                        ip = ip,
                        status = SniResultStatus.FAIL,
                        portResults = portResults,
                    ),
                )
            }
        }
        return results
    }
}

object SniSpoofCheckEngine {
    fun parseTargets(input: String): List<String> = SniSpoofCheckService.parseTargets(input)

    suspend fun getUserPublicIp(): String? = SniSpoofCheckService.getUserPublicIp()

    fun generateReport(
        results: List<SniIpResult>,
        userPublicIp: String? = null,
        config: SniSpoofCheckConfig = SniSpoofCheckConfig(),
    ): String = SniSpoofCheckService.generateReport(results, config)

    suspend fun runScan(
        targetsText: String,
        config: SniSpoofCheckConfig = SniSpoofCheckConfig(),
        onPublicIpDetected: (String) -> Unit = {},
        onProgress: (current: Int, total: Int, status: String) -> Unit = { _, _, _ -> },
        onResult: (SniIpResult) -> Unit = {},
    ) {
        val targets = parseTargets(targetsText)
        if (targets.isEmpty()) return
        val service = SniSpoofCheckService(config)
        if (config.enableIpCheck) {
            val ip = config.manualIp ?: getUserPublicIp()
            if (ip != null) onPublicIpDetected(ip)
        }

        service.scanTargets(targets) { progress ->
            val latest = progress.latestResult
            if (latest != null) {
                onResult(latest)
            }
            onProgress(progress.completedTargets, progress.totalTargets, "بررسی ${progress.completedTargets} از ${progress.totalTargets}...")
        }
    }
}

