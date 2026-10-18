package dev.cluvex.zedsecure.data.tools

import dev.cluvex.zedsecure.platform.tcpConnectMillis
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import kotlin.random.Random

data class ScanResult(
    val ip: String,
    val latency: Int,
    val status: Status,
)

enum class Status {
    GREEN,
    YELLOW,
    RED,
}

object CidrGenerator {
    fun randomIps(cidr: String, count: Int): List<String> {
        val parts = cidr.split("/")
        if (parts.size != 2) return emptyList()

        val ip = parts[0].trim()
        val prefix = parts[1].trim().toIntOrNull() ?: return emptyList()

        val ipParts = ip.split(".").mapNotNull { it.trim().toIntOrNull() }
        if (ipParts.size != 4) return emptyList()

        val base =
            (ipParts[0] shl 24) or
            (ipParts[1] shl 16) or
            (ipParts[2] shl 8) or
            ipParts[3]

        val hostBits = (32 - prefix).coerceIn(1, 30)
        val maxHosts = 1 shl hostBits

        val list = mutableListOf<String>()
        val actualCount = minOf(count, maxHosts)

        repeat(actualCount) {
            val host = Random.nextInt(maxHosts)
            val value = base + host

            val a = (value ushr 24) and 0xff
            val b = (value ushr 16) and 0xff
            val c = (value ushr 8) and 0xff
            val d = value and 0xff

            list.add("$a.$b.$c.$d")
        }

        return list
    }
}

object PingEngine {
    fun ping(ip: String, port: Int = 443): Int {
        return try {
            val process = Runtime.getRuntime().exec(
                arrayOf(
                    "/system/bin/ping",
                    "-c",
                    "1",
                    "-W",
                    "1",
                    ip,
                ),
            )
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readText()
            process.waitFor()

            val regex = Regex("""time=([0-9.]+)""")
            val match = regex.find(output)

            if (match != null) {
                match.groupValues[1].toFloat().toInt()
            } else {
                // Fallback to high-precision TCP connect millis if ping binary returned non-zero (or non-Android)
                val tcp = tcpConnectMillis(ip, port, 1000)
                if (tcp > 0) tcp.toInt() else 999
            }
        } catch (e: Exception) {
            // Fallback for desktop / environments without /system/bin/ping
            val tcp = tcpConnectMillis(ip, port, 1000)
            if (tcp > 0) tcp.toInt() else 999
        }
    }
}

object TlsEngine {
    fun test(
        ip: String,
        host: String,
        port: Int = 443,
        timeout: Int = 3000,
    ): Boolean {
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress(ip, port), timeout)

            val context = SSLContext.getInstance("TLS")
            context.init(null, null, null)

            val sslSocket = context.socketFactory.createSocket(
                socket,
                host,
                port,
                true,
            ) as SSLSocket

            val params = sslSocket.sslParameters
            params.serverNames = listOf(SNIHostName(host))
            sslSocket.sslParameters = params
            sslSocket.soTimeout = timeout

            sslSocket.startHandshake()
            sslSocket.close()

            true
        } catch (e: Exception) {
            false
        }
    }
}

object ScannerEngine {

    @Volatile
    private var stopRequested = false

    val cdnTypes = listOf(
        "Cloudflare",
        "Akamai",
        "Fastly",
        "Bunny",
        "EdgeCenter",
        "Gcore",
        "CDN77",
        "Edgio",
        "Imperva",
        "Azure Front Door",
        "Netlify",
        "Google Cloud CDN",
        "Vercel",
    )

    fun getDefaultRangesForCdn(cdnType: String): List<String> {
        return when (cdnType) {
            "Cloudflare" -> listOf(
                "104.16.0.0/13",
                "104.24.0.0/14",
                "172.64.0.0/13",
                "162.158.0.0/15",
                "198.41.128.0/17",
                "188.114.96.0/20",
                "141.101.64.0/18",
                "108.162.192.0/18",
            )
            "Akamai" -> listOf(
                "23.32.0.0/11",
                "23.192.0.0/11",
                "104.64.0.0/10",
                "184.84.0.0/14",
                "2.16.0.0/13",
                "96.6.0.0/15",
                "72.246.0.0/15",
            )
            "Fastly" -> listOf(
                "151.101.0.0/16",
                "199.232.0.0/16",
                "146.75.0.0/16",
                "167.82.0.0/17",
            )
            "Bunny" -> listOf(
                "89.187.160.0/20",
                "185.180.12.0/22",
                "169.150.200.0/22",
                "185.228.168.0/22",
                "169.150.220.0/22",
            )
            "EdgeCenter" -> listOf(
                "91.240.84.0/22",
                "91.240.88.0/22",
                "185.194.216.0/22",
                "185.194.220.0/22",
            )
            "Gcore" -> listOf(
                "92.223.84.0/22",
                "92.38.168.0/22",
                "146.185.240.0/21",
                "5.188.224.0/21",
            )
            "CDN77" -> listOf(
                "84.17.32.0/19",
                "89.187.160.0/20",
                "185.229.224.0/22",
                "195.181.160.0/19",
            )
            "Edgio" -> listOf(
                "192.229.128.0/17",
                "192.16.128.0/17",
                "93.184.216.0/22",
                "152.195.0.0/16",
            )
            "Imperva" -> listOf(
                "199.83.128.0/21",
                "198.143.32.0/19",
                "149.126.72.0/21",
                "103.28.248.0/22",
            )
            "Azure Front Door" -> listOf(
                "13.107.246.0/24",
                "13.107.213.0/24",
                "204.79.197.0/24",
                "13.107.6.0/24",
            )
            "Netlify" -> listOf(
                "75.2.60.0/24",
                "99.83.231.0/24",
                "100.24.0.0/16",
            )
            "Google Cloud CDN" -> listOf(
                "34.96.0.0/16",
                "34.120.0.0/16",
                "35.190.0.0/16",
                "130.211.0.0/16",
            )
            "Vercel" -> listOf(
                "76.76.21.0/24",
                "76.223.126.0/24",
            )
            else -> emptyList()
        }
    }

    fun stop() {
        stopRequested = true
    }

    suspend fun scan(
        ranges: List<String>,
        domain: String = "",
        port: Int = 443,
        ipPerRange: Int = 150,
        threadCount: Int = 6,
        checkCert: Boolean = false,
        onResult: (ScanResult) -> Unit,
    ) = coroutineScope {
        stopRequested = false
        val jobs = mutableListOf<Deferred<Unit>>()

        for (range in ranges) {
            if (stopRequested) break

            val ips = if (range.contains("/")) {
                CidrGenerator.randomIps(range, ipPerRange)
            } else {
                listOf(range.trim())
            }

            ips.chunked(threadCount).forEach { chunk ->
                if (stopRequested) return@forEach

                jobs += async(Dispatchers.IO) {
                    chunk.forEach { ip ->
                        if (stopRequested) return@async

                        // Ping latency measurement
                        val latency = PingEngine.ping(ip, port)
                        if (stopRequested) return@async

                        // TLS Certificate check (if enabled and domain provided)
                        var tlsOk = true
                        if (checkCert && domain.isNotBlank()) {
                            tlsOk = TlsEngine.test(
                                ip = ip,
                                host = domain,
                                port = port,
                            )
                        }

                        val status = when {
                            !tlsOk -> Status.RED
                            latency <= 150 -> Status.GREEN
                            latency <= 250 -> Status.YELLOW
                            else -> Status.RED
                        }

                        onResult(
                            ScanResult(
                                ip = ip,
                                latency = latency,
                                status = status,
                            ),
                        )

                        delay(40)
                    }
                }
            }
        }

        jobs.awaitAll()
    }
}
