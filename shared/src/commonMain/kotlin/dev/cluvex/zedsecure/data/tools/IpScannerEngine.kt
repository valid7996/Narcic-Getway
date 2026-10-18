package dev.cluvex.zedsecure.data.tools

import dev.cluvex.zedsecure.data.net.PingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlin.random.Random

data class CandidateTarget(
    val ip: String,
    val cdn: CdnProvider,
    val operator: String = "ALL",
)

data class ScannedIp(
    val ip: String,
    val port: Int,
    val pingMs: Long,
    val cdn: String,
    val operator: String,
    val status: IpStatus,
)

enum class IpStatus {
    Clean,  // Green: < 300ms
    Valid,  // Yellow: 300 - 800ms
    Failed, // Red: > 800ms or Timeout (-1)
}

enum class CdnProvider(
    val titleFa: String,
    val titleEn: String,
    val shortName: String,
) {
    All("همه ارائه‌دهندگان", "All CDNs", "ALL"),
    Akamai("آکامای (Akamai)", "Akamai", "AKAMAI"),
    Fastly("فستلی (Fastly)", "Fastly", "FASTLY"),
    Bunny("بانی سی‌دی‌ان (Bunny)", "Bunny CDN", "BUNNY"),
    Cloudflare("کلودفلر (Cloudflare)", "Cloudflare", "CF"),
    GCore("جی‌کور (GCore)", "GCore", "GCORE"),
    CloudFront("کلودفرانت (CloudFront)", "CloudFront", "CLOUDFRONT"),
}

enum class OperatorFilter(val titleFa: String, val titleEn: String) {
    All("همه اپراتورها", "All Operators"),
    Mci("همراه اول (MCI)", "MCI"),
    Irancell("ایرانسل (Irancell)", "Irancell"),
    Rightel("رایتل (Rightel)", "Rightel"),
    Tci("مخابرات و ثابت (TCI)", "TCI"),
}

object IpScannerEngine {

    // 1. Akamai Technologies IP Pools & Presets
    private val AKAMAI_PRESETS = listOf(
        "23.218.211.23", "23.218.211.24", "23.218.211.25", "23.50.59.100", "23.50.59.101",
        "23.62.234.35", "23.77.20.15", "104.86.110.15", "104.96.220.10", "104.103.114.45",
        "104.115.15.20", "184.85.112.50", "184.25.56.80", "184.26.12.90", "2.16.142.10",
        "2.18.60.25", "2.20.180.12", "96.7.128.190", "96.17.180.40", "72.246.120.15",
        "173.222.100.8", "23.4.150.25", "23.195.120.40", "104.120.50.15"
    )
    private val AKAMAI_PREFIXES = listOf("23.32", "23.50", "23.62", "23.192", "104.86", "104.96", "184.85", "2.16", "96.7", "72.246")

    // 2. Fastly Edge IP Pools & Presets
    private val FASTLY_PRESETS = listOf(
        "151.101.1.6", "151.101.65.6", "151.101.129.6", "151.101.193.6",
        "151.101.2.132", "151.101.66.132", "151.101.130.132", "151.101.194.132",
        "199.232.1.6", "199.232.65.6", "199.232.129.6", "199.232.193.6",
        "146.75.32.133", "146.75.64.133", "146.75.96.133", "146.75.120.133",
        "167.82.0.12", "167.82.64.15", "151.101.30.100", "199.232.100.5"
    )
    private val FASTLY_PREFIXES = listOf("151.101", "199.232", "146.75", "167.82")

    // 3. Bunny CDN IP Pools & Presets
    private val BUNNY_PRESETS = listOf(
        "89.187.169.1", "89.187.169.10", "89.187.169.25", "89.187.170.15",
        "185.180.13.1", "185.180.13.15", "185.180.14.20",
        "169.150.200.10", "169.150.201.20", "169.150.220.10", "169.150.224.5", "169.150.240.12",
        "185.228.168.10", "185.228.169.15", "89.187.160.20", "169.150.228.8", "169.150.244.15"
    )
    private val BUNNY_PREFIXES = listOf("89.187.169", "89.187.170", "185.180.13", "169.150.200", "169.150.220", "169.150.224", "169.150.240", "185.228.168")

    // 4. Cloudflare IP Pools & Presets
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

    private val CF_PREFIXES = listOf("104.16", "104.17", "104.18", "104.19", "104.20", "104.21", "104.22", "172.64", "172.67", "162.159", "188.114")

    // 5. GCore IP Pools & Presets
    private val GCORE_PRESETS = listOf(
        "92.223.84.1", "92.223.84.10", "92.223.88.5", "92.223.124.12",
        "92.38.168.10", "92.38.168.25", "92.38.184.5", "92.38.184.20",
        "146.185.240.10", "146.185.240.50", "5.188.224.1", "5.188.224.20"
    )
    private val GCORE_PREFIXES = listOf("92.223.84", "92.223.88", "92.38.168", "92.38.184", "146.185.240")

    // 6. Amazon CloudFront IP Pools & Presets
    private val CLOUDFRONT_PRESETS = listOf(
        "13.32.120.10", "13.35.25.50", "13.224.180.20", "13.249.50.15",
        "18.64.120.30", "18.68.45.10", "99.84.150.40", "99.86.120.25",
        "54.192.100.10", "54.230.150.20", "52.84.120.5", "65.8.50.12", "143.204.80.25"
    )
    private val CLOUDFRONT_PREFIXES = listOf("13.32", "13.35", "13.224", "18.64", "99.84", "54.192", "52.84", "65.8")

    fun parseCidrOrIps(input: String, defaultCdn: CdnProvider): List<CandidateTarget> {
        val items = input.split(',', '\n', ' ', ';').map { it.trim() }.filter { it.isNotEmpty() }
        val result = mutableListOf<CandidateTarget>()

        for (item in items) {
            if (item.contains("/")) {
                val parts = item.split('/')
                val baseIp = parts[0].trim()
                val prefixLen = parts.getOrNull(1)?.toIntOrNull() ?: 24
                val octets = baseIp.split('.').mapNotNull { it.toIntOrNull() }
                if (octets.size == 4) {
                    val prefix = "${octets[0]}.${octets[1]}"
                    val o3Base = octets[2]
                    val rangeSize = (1 shl (32 - prefixLen.coerceIn(16, 30))).coerceAtLeast(4)
                    val count = minOf(24, rangeSize)
                    for (i in 0 until count) {
                        val o3 = if (prefixLen >= 24) o3Base else Random.nextInt(o3Base, minOf(255, o3Base + (rangeSize shr 8) + 1))
                        val o4 = Random.nextInt(1, 254)
                        result.add(CandidateTarget("$prefix.$o3.$o4", defaultCdn, "Custom"))
                    }
                } else {
                    result.add(CandidateTarget(item, defaultCdn, "Custom"))
                }
            } else {
                result.add(CandidateTarget(item, defaultCdn, "Custom"))
            }
        }
        return result.distinctBy { it.ip }
    }

    private fun generateSynthetic(prefixes: List<String>, countPerPrefix: Int = 2): List<String> {
        val list = mutableListOf<String>()
        for (p in prefixes) {
            repeat(countPerPrefix) {
                val dots = p.count { it == '.' }
                val ip = when (dots) {
                    1 -> "$p.${Random.nextInt(1, 254)}.${Random.nextInt(1, 254)}"
                    2 -> "$p.${Random.nextInt(1, 254)}"
                    else -> "$p.${Random.nextInt(1, 254)}"
                }
                list.add(ip)
            }
        }
        return list
    }

    fun getCandidateTargets(
        cdn: CdnProvider,
        operator: OperatorFilter,
        customInput: String,
    ): List<CandidateTarget> {
        val customTrimmed = customInput.trim()
        if (customTrimmed.isNotEmpty()) {
            return parseCidrOrIps(customTrimmed, cdn)
        }

        val opName = when (operator) {
            OperatorFilter.All -> "ALL"
            else -> operator.name
        }

        return when (cdn) {
            CdnProvider.Akamai -> {
                val synthetics = generateSynthetic(AKAMAI_PREFIXES, 2)
                (AKAMAI_PRESETS + synthetics).distinct().shuffled().map { CandidateTarget(it, CdnProvider.Akamai, opName) }
            }
            CdnProvider.Fastly -> {
                val synthetics = generateSynthetic(FASTLY_PREFIXES, 3)
                (FASTLY_PRESETS + synthetics).distinct().shuffled().map { CandidateTarget(it, CdnProvider.Fastly, opName) }
            }
            CdnProvider.Bunny -> {
                val synthetics = generateSynthetic(BUNNY_PREFIXES, 2)
                (BUNNY_PRESETS + synthetics).distinct().shuffled().map { CandidateTarget(it, CdnProvider.Bunny, opName) }
            }
            CdnProvider.GCore -> {
                val synthetics = generateSynthetic(GCORE_PREFIXES, 2)
                (GCORE_PRESETS + synthetics).distinct().shuffled().map { CandidateTarget(it, CdnProvider.GCore, opName) }
            }
            CdnProvider.CloudFront -> {
                val synthetics = generateSynthetic(CLOUDFRONT_PREFIXES, 2)
                (CLOUDFRONT_PRESETS + synthetics).distinct().shuffled().map { CandidateTarget(it, CdnProvider.CloudFront, opName) }
            }
            CdnProvider.Cloudflare -> {
                val base = when (operator) {
                    OperatorFilter.Mci -> CF_MCI_PRESETS
                    OperatorFilter.Irancell -> CF_IRANCELL_PRESETS
                    OperatorFilter.Rightel -> CF_RIGHTEL_PRESETS
                    OperatorFilter.Tci -> CF_TCI_PRESETS
                    OperatorFilter.All -> (CF_MCI_PRESETS + CF_IRANCELL_PRESETS + CF_RIGHTEL_PRESETS + CF_TCI_PRESETS).distinct()
                }
                val synthetics = generateSynthetic(CF_PREFIXES, 2)
                (base + synthetics).distinct().shuffled().map { CandidateTarget(it, CdnProvider.Cloudflare, opName) }
            }
            CdnProvider.All -> {
                // Collect a balanced cross-section across all major CDNs
                val akamai = AKAMAI_PRESETS.take(6).map { CandidateTarget(it, CdnProvider.Akamai, opName) }
                val fastly = FASTLY_PRESETS.take(6).map { CandidateTarget(it, CdnProvider.Fastly, opName) }
                val bunny = BUNNY_PRESETS.take(6).map { CandidateTarget(it, CdnProvider.Bunny, opName) }
                val gcore = GCORE_PRESETS.take(4).map { CandidateTarget(it, CdnProvider.GCore, opName) }
                val cloudfront = CLOUDFRONT_PRESETS.take(4).map { CandidateTarget(it, CdnProvider.CloudFront, opName) }
                val cf = (CF_MCI_PRESETS.take(3) + CF_IRANCELL_PRESETS.take(3) + CF_RIGHTEL_PRESETS.take(2) + CF_TCI_PRESETS.take(2))
                    .map { CandidateTarget(it, CdnProvider.Cloudflare, opName) }

                (akamai + fastly + bunny + gcore + cloudfront + cf).shuffled()
            }
        }
    }

    fun startScan(
        scope: CoroutineScope,
        targets: List<CandidateTarget>,
        port: Int = 443,
        timeoutMs: Int = 1_500,
        concurrency: Int = 8,
        onProgress: (scanned: Int, total: Int, currentIp: String, newResult: ScannedIp?) -> Unit,
        onComplete: () -> Unit,
    ): Job {
        return scope.launch(Dispatchers.Default) {
            val semaphore = Semaphore(concurrency)
            val scanMutex = Mutex()
            var scanned = 0
            val total = targets.size

            val jobs = targets.map { target ->
                launch {
                    semaphore.withPermit {
                        if (!isActive) return@withPermit
                        val ping = PingService.tcpPing(target.ip, port, timeoutMs)
                        if (!isActive) return@withPermit
                        val status = when {
                            ping in 1..299 -> IpStatus.Clean
                            ping in 300..799 -> IpStatus.Valid
                            else -> IpStatus.Failed
                        }
                        val result = ScannedIp(
                            ip = target.ip,
                            port = port,
                            pingMs = ping,
                            cdn = target.cdn.shortName,
                            operator = target.operator,
                            status = status,
                        )
                        scanMutex.withLock {
                            scanned++
                        }
                        withContext(Dispatchers.Main) {
                            onProgress(scanned, total, target.ip, result)
                        }
                    }
                }
            }

            jobs.forEach { it.join() }
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }
}
