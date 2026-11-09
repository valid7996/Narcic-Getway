package dev.valid7996.narcicgeteway.domain.config

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.Serializable

/**
 * CottenMax options of a CottenDNS profile. Used only when [WhiteDnsProfile.tunnelMode] is "cottenmax";
 * StormDNS profiles never see these keys (the StormDNS engine rejects them).
 */
@Serializable
data class WhiteDnsMaxOptions(
    val trafficProfile: String = "full",
    val lowReductionPercent: Int = 35,
    val standbyDomains: String = "",
    val failoverAfterMs: Int = 3000,
    val safeQName: Int = 101,
    val maxQName: Int = 253,
    val maxAnswer: Int = 1232,
    val maxPathWindow: Int = 256,
    val maxInflight: Int = 256,
    val fecDownloadEnabled: Boolean = false,
    val fecDataShards: Int = 4,
    val fecParityShards: Int = 1,
    val sessionIdleMs: Int = 90000,
    val holdMs: Int = 500,
    val discoveryEnabled: Boolean = false,
    val discoveryCountry: String = "",
    val discoverySampleRanges: Int = 0,
    val discoveryCacheEnabled: Boolean = false,
    val discoveryNetworkId: String = "",
    val discoveryCandidates: String = "",
    val discoveryWant: Int = 16,
    val discoveryConcurrency: Int = 16,
    val discoveryPaceMs: Int = 20,
    val dnsPaddingBlockSize: Int = 0,
    val labelCountJitter: Boolean = false,
    val qnameLossGuard: Boolean = false,
    val doh3FallbackToDoh: Boolean = false,
) {
    fun normalized(): WhiteDnsMaxOptions {
        val safe = safeQName.coerceIn(64, 253)
        return copy(
            trafficProfile = if (trafficProfile.trim().equals("low", ignoreCase = true)) "low" else "full",
            lowReductionPercent = lowReductionPercent.coerceIn(10, 90),
            standbyDomains = entries(standbyDomains).map { it.trimEnd('.').lowercase() }.distinct().take(7).joinToString(", "),
            failoverAfterMs = failoverAfterMs.coerceIn(100, 60000),
            safeQName = safe,
            maxQName = maxQName.coerceIn(safe, 253),
            maxAnswer = maxAnswer.coerceIn(512, 1232),
            maxPathWindow = maxPathWindow.coerceIn(1, 1024),
            maxInflight = maxInflight.coerceIn(1, 4096),
            fecDataShards = fecDataShards.coerceIn(2, 16),
            fecParityShards = fecParityShards.coerceIn(1, 4),
            sessionIdleMs = sessionIdleMs.coerceIn(1000, 90000),
            holdMs = holdMs.coerceIn(0, 1500) / 100 * 100,
            discoveryCountry = discoveryCountry.trim().lowercase().takeIf { c -> c.length == 2 && c.all { it in 'a'..'z' } }.orEmpty(),
            discoverySampleRanges = discoverySampleRanges.coerceIn(0, 4096),
            discoveryNetworkId = discoveryNetworkId.take(256),
            discoveryCandidates = entries(discoveryCandidates).take(256).joinToString(", "),
            discoveryWant = discoveryWant.coerceIn(1, 4096),
            discoveryConcurrency = discoveryConcurrency.coerceIn(1, 64),
            discoveryPaceMs = discoveryPaceMs.coerceIn(1, 60000),
            dnsPaddingBlockSize = if (dnsPaddingBlockSize <= 0) 0 else dnsPaddingBlockSize.coerceIn(16, 4096),
        )
    }

    companion object {
        private val SEPARATORS = Regex("[,;\\s]+")

        fun entries(text: String): List<String> =
            text.split(SEPARATORS).map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    }
}

/**
 * Lantern Soar options. Soar has its own server and public key; none of the Master/Cotten transport settings
 * apply to it.
 */
@Serializable
data class WhiteDnsSoarOptions(
    val transport: String = "auto",
    val discovery: Boolean = true,
    val country: String = "IR",
    val discoveryWant: Int = 2,
    val discoveryConcurrency: Int = 2,
    val discoveryPaceMs: Int = 1000,
    val safeQName: Int = 101,
    val maxQName: Int = 253,
    val maxAnswer: Int = 1232,
    val holdMs: Int = 500,
    val maxInflight: Int = 128,
    val maxPathWindow: Int = 32,
    val maxStreams: Int = 128,
    val streamWindowKiB: Int = 256,
    val sendBufferKiB: Int = 64,
    val sessionIdleSeconds: Int = 90,
    val dialTimeoutSeconds: Int = 90,
) {
    fun normalized(): WhiteDnsSoarOptions {
        val maxQ = maxQName.coerceIn(101, 253)
        return copy(
            transport = if (transport in TRANSPORTS) transport else "auto",
            country = country.trim().uppercase().takeIf { c -> c.length == 2 && c.all { it in 'A'..'Z' } }.orEmpty(),
            discoveryWant = discoveryWant.coerceIn(1, 32),
            discoveryConcurrency = discoveryConcurrency.coerceIn(1, 16),
            discoveryPaceMs = discoveryPaceMs.coerceIn(100, 5000),
            maxQName = maxQ,
            safeQName = safeQName.coerceIn(101, maxQ),
            maxAnswer = maxAnswer.coerceIn(512, 1232),
            holdMs = holdMs.coerceIn(100, 1500),
            maxInflight = maxInflight.coerceIn(8, 1024),
            maxPathWindow = maxPathWindow.coerceIn(2, 128),
            maxStreams = maxStreams.coerceIn(8, 256),
            streamWindowKiB = streamWindowKiB.coerceIn(64, 1024),
            sendBufferKiB = sendBufferKiB.coerceIn(32, 1024),
            // Stay under the reference server's two minute idle expiry.
            sessionIdleSeconds = sessionIdleSeconds.coerceIn(15, 110),
            dialTimeoutSeconds = dialTimeoutSeconds.coerceIn(5, 120),
        )
    }

    /** The preset's transport tuning, with this connection's discovery choices kept. */
    fun withPreset(name: String): WhiteDnsSoarOptions {
        val d = WhiteDnsSoarOptions()
        val tuned = when (name) {
            "conservative" -> d.copy(maxInflight = 32, maxPathWindow = 8, maxAnswer = 512, maxQName = 101, maxStreams = 16)
            "fast" -> d.copy(maxInflight = 256, maxPathWindow = 64)
            else -> d
        }
        return tuned.copy(
            transport = transport, discovery = discovery, country = country,
            discoveryWant = discoveryWant, discoveryConcurrency = discoveryConcurrency, discoveryPaceMs = discoveryPaceMs,
        ).normalized()
    }

    /** "balanced", "conservative", "fast", or "custom" when the tuning matches none of them. */
    fun presetName(): String {
        val n = normalized()
        return listOf("balanced", "conservative", "fast").firstOrNull { n.withPreset(it) == n } ?: "custom"
    }

    companion object {
        val TRANSPORTS = listOf("auto", "on", "only", "off")
    }
}

/**
 * One WhiteDNS server connection (a StormDNS or CottenDNS tunnel server). The values that are shared by
 * every connection (MTU scan, workers, timeouts, retries ...) live in [WhiteDnsSettings].
 */
@Serializable
data class WhiteDnsProfile(

    val engine: String = ENGINE_STORM,

    val domains: String = "",

    val encryptionKey: String = "",

    val encryptionMethod: Int = ENC_XOR,

    val resolvers: String = "",

    val configPreset: String = "default",

    val tunnelMode: String = "legacy",

    val transportMode: String = MODE_PRESET,

    val deliveryMode: String = MODE_PRESET,

    val qnameMode: String = MODE_PRESET,

    val ipMode: String = MODE_PRESET,

    val queryRateLimit: String = RATE_OFF,

    val queryRateScope: String = "total",

    val timingMask: String = "off",

    val backgroundScanParallelism: Int = 1,

    val resolverTlsServerName: String = "",
    val resolverTlsPin: String = "",
    val resolverDotPort: String = "853",
    val resolverDohPort: String = "443",
    val resolverDohPath: String = "/dns-query",

    val max: WhiteDnsMaxOptions = WhiteDnsMaxOptions(),

    /** Used only when [engine] is Soar. */
    val soar: WhiteDnsSoarOptions = WhiteDnsSoarOptions(),

    /** Raw engine keys (`KEY = value` lines) that the form does not cover. They override the generated config. */
    val extraToml: String = "",
) {
    val isCottenDns: Boolean get() = normalizeEngine(engine) == ENGINE_COTTEN

    val isSoar: Boolean get() = normalizeEngine(engine) == ENGINE_SOAR

    val isCompatibility: Boolean get() = isCottenDns && normalizePreset(configPreset) == PRESET_MASTER_STORM

    val usesCottenMax: Boolean
        get() = isCottenDns && !isCompatibility && normalizeTunnelMode(tunnelMode) == "cottenmax"

    val engineLabel: String
        get() = when (normalizeEngine(engine)) {
            ENGINE_COTTEN -> "CottenDNS"
            ENGINE_SOAR -> "Soar"
            else -> "StormDNS"
        }

    fun domainList(): List<String> = splitDomains(domains)

    fun primaryDomain(): String = domainList().firstOrNull().orEmpty()

    fun resolverList(): List<String> = MasterDnsProfile.splitResolvers(resolvers)

    val isValid: Boolean
        get() = if (isSoar) {
            domainList().size == 1 && isValidSoarServer(primaryDomain(), encryptionKey)
        } else {
            domainList().isNotEmpty() && encryptionKey.isNotBlank() && encryptionMethod in 0..5
        }

    /** Clamps every value the same way the engine would, so the form and the config never disagree. */
    fun normalized(): WhiteDnsProfile = copy(
        engine = normalizeEngine(engine),
        domains = domainList().joinToString(", "),
        encryptionKey = encryptionKey.trim(),
        encryptionMethod = encryptionMethod.coerceIn(0, 5),
        configPreset = if (isCottenDns) normalizePreset(configPreset) else "default",
        tunnelMode = if (isCompatibility || !isCottenDns) "legacy" else normalizeTunnelMode(tunnelMode),
        transportMode = normalizeChoice(transportMode, TRANSPORT_MODES),
        deliveryMode = normalizeChoice(deliveryMode, DELIVERY_MODES),
        qnameMode = normalizeChoice(qnameMode, QNAME_MODES),
        ipMode = normalizeChoice(ipMode, IP_MODES),
        queryRateLimit = normalizeOr(queryRateLimit, RATE_LIMITS, RATE_OFF),
        queryRateScope = normalizeOr(queryRateScope, RATE_SCOPES, "total"),
        timingMask = normalizeOr(timingMask, TIMING_MASKS, "off"),
        backgroundScanParallelism = backgroundScanParallelism.coerceIn(1, 100),
        resolverDotPort = normalizePort(resolverDotPort, "853"),
        resolverDohPort = normalizePort(resolverDohPort, "443"),
        resolverDohPath = resolverDohPath.trim().ifBlank { "/dns-query" },
        max = max.normalized(),
        soar = soar.normalized(),
    )

    companion object {
        const val ENGINE_STORM = "stormdns"
        const val ENGINE_COTTEN = "cottendns"
        const val ENGINE_SOAR = "soar"

        const val MODE_PRESET = "preset"
        const val RATE_OFF = "off"
        const val PRESET_MASTER_STORM = "master-storm"

        const val ENC_NONE = 0
        const val ENC_XOR = 1
        const val ENC_CHACHA20 = 2
        const val ENC_AES128 = 3
        const val ENC_AES192 = 4
        const val ENC_AES256 = 5

        val PRESETS = listOf("default", "speed", "survival", "tcp-survival", PRESET_MASTER_STORM)
        val TRANSPORT_MODES = listOf(MODE_PRESET, "auto", "udp", "tcp", "dot", "doh", "doh3", "doq")
        val DELIVERY_MODES = listOf(MODE_PRESET, "txt", "txt-cname", "txt-https", "txt-aaaa", "all")
        val QNAME_MODES = listOf(MODE_PRESET, "off", "moderate", "aggressive")
        val IP_MODES = listOf(MODE_PRESET, "auto", "dual", "ipv4", "ipv6")
        val RATE_LIMITS = listOf(RATE_OFF, "2", "3", "5", "8", "10")
        val RATE_SCOPES = listOf("total", "resolver", "domain")
        val TIMING_MASKS = listOf("off", "light", "strong")

        fun normalizeEngine(value: String): String = when (value.trim().lowercase()) {
            "cotten", "cotton", "cottendns", "cottondns" -> ENGINE_COTTEN
            "soar" -> ENGINE_SOAR
            else -> ENGINE_STORM
        }

        fun normalizePreset(value: String): String = when (value.trim().lowercase()) {
            "speed" -> "speed"
            "survival" -> "survival"
            "tcp", "tcp-survival", "tcp_survival" -> "tcp-survival"
            "master", "storm", "master-storm", "master_storm" -> PRESET_MASTER_STORM
            else -> "default"
        }

        fun normalizeTunnelMode(value: String): String =
            if (value.trim().lowercase() == "cottenmax") "cottenmax" else "legacy"

        /** One lowercase ASCII zone of at most 45 characters and a base64 32-byte Ed25519 server public key. */
        @OptIn(ExperimentalEncodingApi::class)
        fun isValidSoarServer(domain: String, publicKey: String): Boolean {
            val zone = domain.trim().trimEnd('.').lowercase()
            if (zone.length !in 3..45 || '.' !in zone || !zone.all { it in 'a'..'z' || it in '0'..'9' || it == '.' || it == '-' }) {
                return false
            }
            if (zone.split('.').any { it.isEmpty() || it.length > 63 || it.startsWith('-') || it.endsWith('-') }) return false
            return runCatching { Base64.Default.decode(publicKey.trim()).size == 32 }.getOrDefault(false)
        }

        fun splitDomains(raw: String): List<String> =
            raw.split(',', '\n', ';', ' ')
                .map { it.trim().trimEnd('.') }
                .filter { it.isNotEmpty() }
                .distinctBy { it.lowercase() }

        private fun normalizeChoice(value: String, allowed: List<String>): String {
            val candidate = value.trim().lowercase()
            return if (candidate in allowed) candidate else MODE_PRESET
        }

        private fun normalizeOr(value: String, allowed: List<String>, fallback: String): String {
            val candidate = value.trim().lowercase()
            return if (candidate in allowed) candidate else fallback
        }

        private fun normalizePort(value: String, fallback: String): String {
            val port = value.trim().toIntOrNull() ?: return fallback
            return if (port in 1..65535) port.toString() else fallback
        }
    }
}
