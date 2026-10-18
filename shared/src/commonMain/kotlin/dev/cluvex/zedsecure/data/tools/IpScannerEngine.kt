package dev.cluvex.zedsecure.data.tools

import dev.cluvex.zedsecure.data.net.PingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlin.random.Random

data class ScannedIp(
    val ip: String,
    val port: Int,
    val pingMs: Long,
    val operator: String,
    val status: IpStatus,
)

enum class IpStatus {
    Clean,  // Green: < 300ms
    Valid,  // Yellow: 300 - 800ms
    Failed, // Red: > 800ms or Timeout (-1)
}

enum class OperatorFilter(val titleFa: String, val titleEn: String) {
    All("همه اپراتورها", "All Operators"),
    Mci("همراه اول (MCI)", "MCI"),
    Irancell("ایرانسل (Irancell)", "Irancell"),
    Rightel("رایتل (Rightel)", "Rightel"),
    Tci("مخابرات و ثابت (TCI)", "TCI"),
}

object IpScannerEngine {

    // Proven clean Cloudflare CIDRs commonly utilized in Iran
    private val CF_CIDRS_ALL = listOf(
        "104.16.0.0/13",
        "104.24.0.0/14",
        "172.64.0.0/13",
        "162.158.0.0/15",
        "198.41.128.0/17",
        "197.234.240.0/22",
        "188.114.96.0/20",
        "141.101.64.0/18",
        "108.162.192.0/18",
        "190.93.240.0/20",
    )

    private val CF_MCI_PRESETS = listOf(
        "104.16.132.229", "104.16.133.229", "104.16.148.22", "104.16.149.22",
        "162.159.192.1", "162.159.193.1", "162.159.195.1", "162.159.192.88",
        "188.114.96.1", "188.114.97.1", "188.114.98.1", "188.114.99.1",
        "104.21.5.198", "104.21.8.212", "104.21.11.45", "104.21.22.99"
    )

    private val CF_IRANCELL_PRESETS = listOf(
        "172.67.180.12", "172.67.200.55", "172.67.222.10", "172.67.74.88",
        "104.17.151.218", "104.17.152.218", "104.17.153.218", "104.17.154.218",
        "104.18.32.21", "104.18.33.21", "104.18.34.21", "104.18.35.21",
        "172.64.150.1", "172.64.151.1", "172.64.152.1", "172.64.153.1"
    )

    private val CF_RIGHTEL_PRESETS = listOf(
        "104.18.10.120", "104.18.11.120", "104.18.12.120", "104.18.13.120",
        "104.24.50.11", "104.24.51.11", "104.24.52.11", "104.24.53.11",
        "162.159.192.1", "162.159.193.1", "162.159.195.1", "162.159.192.88",
        "104.26.12.33", "104.26.13.33", "104.26.14.33", "104.26.15.33"
    )

    private val CF_TCI_PRESETS = listOf(
        "188.114.96.1", "188.114.97.1", "188.114.98.1", "188.114.99.1",
        "104.26.12.33", "104.26.13.33", "104.26.14.33", "104.26.15.33",
        "104.27.15.44", "104.27.16.44", "104.27.17.44", "104.27.18.44",
        "104.16.148.22", "104.16.149.22", "172.67.180.12", "172.67.200.55"
    )

    fun getCandidateIps(operator: OperatorFilter, customInput: String): List<String> {
        val customTrimmed = customInput.trim()
        if (customTrimmed.isNotEmpty()) {
            return customTrimmed.split(',', '\n', ' ', ';')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
        }

        val baseList = when (operator) {
            OperatorFilter.Mci -> CF_MCI_PRESETS
            OperatorFilter.Irancell -> CF_IRANCELL_PRESETS
            OperatorFilter.Rightel -> CF_RIGHTEL_PRESETS
            OperatorFilter.Tci -> CF_TCI_PRESETS
            OperatorFilter.All -> (CF_MCI_PRESETS + CF_IRANCELL_PRESETS + CF_RIGHTEL_PRESETS + CF_TCI_PRESETS).distinct()
        }

        // Generate synthetic random samples from Cloudflare CIDRs for exhaustive discovery
        val syntheticSamples = mutableListOf<String>()
        val prefixes = listOf("104.16", "104.17", "104.18", "104.19", "104.20", "104.21", "104.22", "172.64", "172.67", "162.159", "188.114")
        for (p in prefixes) {
            val octet3 = Random.nextInt(1, 254)
            val octet4 = Random.nextInt(1, 254)
            syntheticSamples.add("$p.$octet3.$octet4")
        }

        return (baseList + syntheticSamples).distinct().shuffled()
    }

    fun startScan(
        scope: CoroutineScope,
        ips: List<String>,
        port: Int = 443,
        operator: OperatorFilter,
        timeoutMs: Int = 2_200,
        concurrency: Int = 12,
        onProgress: (scanned: Int, total: Int, currentIp: String, newResult: ScannedIp?) -> Unit,
        onComplete: () -> Unit,
    ): Job {
        return scope.launch(Dispatchers.Default) {
            val semaphore = Semaphore(concurrency)
            val scanMutex = Mutex()
            var scanned = 0
            val total = ips.size

            val jobs = ips.map { ip ->
                launch {
                    semaphore.withPermit {
                        if (!isActive) return@withPermit
                        val ping = PingService.tcpPing(ip, port, timeoutMs)
                        val status = when {
                            ping in 1..299 -> IpStatus.Clean
                            ping in 300..799 -> IpStatus.Valid
                            else -> IpStatus.Failed
                        }
                        val opName = when (operator) {
                            OperatorFilter.All -> "Cloudflare"
                            else -> operator.name
                        }
                        val result = ScannedIp(
                            ip = ip,
                            port = port,
                            pingMs = ping,
                            operator = opName,
                            status = status,
                        )
                        scanMutex.withLock {
                            scanned++
                        }
                        onProgress(scanned, total, ip, result)
                    }
                }
            }

            jobs.forEach { it.join() }
            onComplete()
        }
    }
}
