package dev.valid7996.narcicgeteway.domain.config

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

    /** Raw engine keys (`KEY = value` lines) that the form does not cover. They override the generated config. */
    val extraToml: String = "",
) {
    val isCottenDns: Boolean get() = normalizeEngine(engine) == ENGINE_COTTEN

    val isCompatibility: Boolean get() = isCottenDns && normalizePreset(configPreset) == PRESET_MASTER_STORM

    val usesCottenMax: Boolean
        get() = isCottenDns && !isCompatibility && normalizeTunnelMode(tunnelMode) == "cottenmax"

    val engineLabel: String get() = if (isCottenDns) "CottenDNS" else "StormDNS"

    fun domainList(): List<String> = splitDomains(domains)

    fun primaryDomain(): String = domainList().firstOrNull().orEmpty()

    fun resolverList(): List<String> = MasterDnsProfile.splitResolvers(resolvers)

    val isValid: Boolean
        get() = domainList().isNotEmpty() && encryptionKey.isNotBlank() && encryptionMethod in 0..5

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
    )

    companion object {
        const val ENGINE_STORM = "stormdns"
        const val ENGINE_COTTEN = "cottendns"

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
