package dev.cluvex.zedsecure.domain.config

import kotlinx.serialization.Serializable

/**
 * The Aether tunnel, as the settings the manual editor holds. Text is kept as written and read
 * when the core starts. [command], when not blank, runs as written in the editor's expert mode;
 * the built arguments give way to it, see [AetherCommands.isCustom].
 */
@Serializable
data class AetherProfile(
    val protocol: String = PROTO_WG,
    val server: String = "",
    val serverPort: Int = 0,
    val scanMode: String = SCAN_BALANCED,
    val transport: String = TRANSPORT_H3,
    val obfuscation: String = OBF_AUTO,
    val ipVersion: String = IP_V4,
    val dns: String = "",
    val exitLoc: String = "",
    val fingerprint: String = FINGERPRINT_CHROME,
    val fragment: Boolean = false,
    val fragmentSize: String = "",
    val fragmentDelay: String = "",
    val ech: Boolean = false,
    val echDns: String = "",
    val echDomain: String = "",
    val wiwOuter: String = "",
    val wiwInner: String = "",
    val command: String = "",
) {
    companion object {
        const val PROTO_MASQUE = "masque"
        const val PROTO_WG = "wg"
        const val PROTO_GOOL = "gool"
        const val PROTO_MIM = "mim"

        const val TRANSPORT_H3 = "h3"
        const val TRANSPORT_H2 = "h2"

        const val SCAN_TURBO = "turbo"
        const val SCAN_BALANCED = "balanced"
        const val SCAN_THOROUGH = "thorough"
        const val SCAN_VERIFIED = "verified"
        const val SCAN_IRONCLAD = "ironclad"

        const val OBF_AUTO = "auto"
        const val OBF_OFF = "off"
        const val OBF_LIGHT = "light"
        const val OBF_FIREWALL = "firewall"
        const val OBF_BALANCED = "balanced"
        const val OBF_GFW = "gfw"
        const val OBF_AGGRESSIVE = "aggressive"

        const val IP_V4 = "v4"
        const val IP_V6 = "v6"
        const val IP_DUAL = "both"

        const val FINGERPRINT_CHROME = "chrome"
        const val FINGERPRINT_FIREFOX = "firefox"
        const val FINGERPRINT_SEMI_PYTHON = "semi-python"
        const val FINGERPRINT_GO = "go"

        const val DEFAULT_ECH_DNS = "udp://1.1.1.1"
        const val DEFAULT_ECH_DOMAIN = "cloudflare-ech.com"

        val protocols = listOf(PROTO_WG, PROTO_MASQUE, PROTO_GOOL, PROTO_MIM)
        val transports = listOf(TRANSPORT_H3, TRANSPORT_H2)
        val scanModes = listOf(SCAN_TURBO, SCAN_BALANCED, SCAN_THOROUGH, SCAN_VERIFIED, SCAN_IRONCLAD)
        val obfuscations = listOf(OBF_AUTO, OBF_OFF, OBF_LIGHT, OBF_FIREWALL, OBF_BALANCED, OBF_GFW, OBF_AGGRESSIVE)
        val ipVersions = listOf(IP_V4, IP_V6, IP_DUAL)
        val fingerprints = listOf(FINGERPRINT_CHROME, FINGERPRINT_FIREFOX, FINGERPRINT_SEMI_PYTHON, FINGERPRINT_GO)
    }

    val isTwoHops: Boolean get() = protocol == PROTO_GOOL || protocol == PROTO_MIM
    val overMasque: Boolean get() = protocol == PROTO_MASQUE || protocol == PROTO_MIM

    fun endpointText(): String? {
        val host = server.trim()
        if (host.isEmpty() || serverPort !in 1..65535) return null
        return if (':' in host) "[$host]:$serverPort" else "$host:$serverPort"
    }
}

/** Builds the core command line and reads commands back, as the core itself reads them. */
object AetherCommands {

    const val BIND = "--bind"
    const val UPSTREAM = "--upstream"

    private const val LOG_LEVEL = "--log-level"
    private const val DEFAULT_LOG_LEVEL = "info"

    /** [text] as an option value: trimmed, and null when it is blank or would read as an option of its own. */
    private fun settingValue(value: String?): String? =
        value?.trim()?.takeUnless { it.isEmpty() || it.startsWith('-') }

    /** The cipher suites of [fingerprint], as the core's --tls-ciphers takes them, with GREASE where the client sends it. */
    private fun fingerprintArguments(fingerprint: String): List<String> = when (fingerprint) {
        AetherProfile.FINGERPRINT_FIREFOX -> listOf(
            "ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-CHACHA20-POLY1305:ECDHE-RSA-CHACHA20-POLY1305:" +
                "ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-RSA-AES128-SHA:ECDHE-RSA-AES256-SHA:" +
                "AES128-GCM-SHA256:AES256-GCM-SHA384:AES128-SHA:AES256-SHA",
        ) + listOf("--disable-grease")

        AetherProfile.FINGERPRINT_SEMI_PYTHON -> listOf(
            "ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:" +
                "ECDHE-ECDSA-CHACHA20-POLY1305:ECDHE-RSA-CHACHA20-POLY1305:ECDHE-ECDSA-AES256-SHA:ECDHE-RSA-AES256-SHA:" +
                "ECDHE-ECDSA-AES128-SHA256:ECDHE-RSA-AES128-SHA256",
        ) + listOf("--disable-grease")

        AetherProfile.FINGERPRINT_GO -> listOf(
            "ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384:" +
                "ECDHE-ECDSA-CHACHA20-POLY1305:ECDHE-RSA-CHACHA20-POLY1305:ECDHE-ECDSA-AES128-SHA:ECDHE-RSA-AES128-SHA:" +
                "ECDHE-ECDSA-AES256-SHA:ECDHE-RSA-AES256-SHA",
        ) + listOf("--disable-grease")

        else -> listOf("ALL:!aPSK:!ECDSA+SHA1:!3DES")
    }.let { ciphers -> listOf("--tls-ciphers", ciphers.first()) + ciphers.drop(1) }

    /** The arguments [profile] runs the core with on loopback [port], at [logLevel]. */
    fun buildArguments(profile: AetherProfile, port: Int, logLevel: String = DEFAULT_LOG_LEVEL): List<String> = buildList {
        addAll(listOf(BIND, "127.0.0.1:$port"))
        addAll(listOf("--protocol", profile.protocol))
        addAll(listOf("--scan", profile.scanMode))
        if (profile.obfuscation != AetherProfile.OBF_AUTO && !(profile.overMasque && profile.transport == AetherProfile.TRANSPORT_H2)) {
            addAll(listOf("--noize", profile.obfuscation))
        }
        addAll(listOf("--ip", profile.ipVersion))
        settingValue(profile.dns)?.let { addAll(listOf("--dns", it)) }
        settingValue(profile.exitLoc)?.let { addAll(listOf("--exit-loc", it)) }

        if (profile.overMasque && profile.transport == AetherProfile.TRANSPORT_H2) {
            add("--h2")
            if (profile.fragment) {
                add("--fragment")
                profile.fragmentSize.trim().toIntOrNull()?.takeIf { it in 1..4096 }
                    ?.let { addAll(listOf("--fragment-size", it.toString())) }
                profile.fragmentDelay.trim().toIntOrNull()?.takeIf { it in 0..1000 }
                    ?.let { addAll(listOf("--fragment-delay", it.toString())) }
            }
        }
        if (profile.overMasque && profile.ech) {
            addAll(listOf("--ech", "auto"))
            addAll(listOf("--ech-dns", settingValue(profile.echDns) ?: AetherProfile.DEFAULT_ECH_DNS))
            addAll(listOf("--ech-domain", settingValue(profile.echDomain) ?: AetherProfile.DEFAULT_ECH_DOMAIN))
        }
        if (profile.overMasque) addAll(fingerprintArguments(profile.fingerprint))

        if (profile.isTwoHops) {
            val hop = if (profile.protocol == AetherProfile.PROTO_MIM) "--mim" else "--wiw"
            val outer = profile.wiwOuter.trim()
            val inner = profile.wiwInner.trim()
            if (outer.isNotEmpty()) addAll(listOf("$hop-outer", outer))
            if (inner.isNotEmpty()) addAll(listOf("$hop-inner", inner))
            if (outer.isEmpty() && inner.isEmpty()) add("$hop-scan")
        } else {
            profile.endpointText()?.let { addAll(listOf("--peer", it)) }
        }
        add("--quick-reconnect")
        addAll(listOf(LOG_LEVEL, logLevel))
    }

    /** The word the core registers keys with, which registers and ends without a tunnel. */
    const val REGISTER = "--register"

    /** The register arguments for [profile]'s protocol: the keys that protocol runs on. */
    fun registerArguments(profile: AetherProfile): List<String> = listOf(REGISTER, profile.protocol)

    /** The words of [command]: split on whitespace, with quotes keeping a word together. */
    fun words(command: String): List<String> {
        val words = mutableListOf<String>()
        val word = StringBuilder()
        var quote: Char? = null
        var open = false
        for (c in command) {
            when {
                quote != null -> if (c == quote) quote = null else word.append(c)
                c == '"' || c == '\'' -> {
                    quote = c
                    open = true
                }

                c.isWhitespace() -> if (open) {
                    words.add(word.toString())
                    word.setLength(0)
                    open = false
                }

                else -> {
                    word.append(c)
                    open = true
                }
            }
        }
        if (open) words.add(word.toString())
        return words
    }

    /** The arguments of [command]: its words, without a program name in front. */
    fun argumentsOf(command: String): List<String> {
        val words = words(command)
        return if (words.firstOrNull()?.startsWith("-") == false) words.drop(1) else words
    }

    /** The arguments [profile] starts the core with: its expert command, or the built ones. */
    fun runArguments(profile: AetherProfile, port: Int, logLevel: String = DEFAULT_LOG_LEVEL): List<String> =
        if (isCustom(profile)) argumentsOf(profile.command)
        else buildArguments(profile, port, logLevel)

    /** Whether [profile] carries a command written by hand: one that is not blank and not the built one. */
    fun isCustom(profile: AetherProfile): Boolean =
        profile.command.isNotBlank() && profile.command.trim() != buildArguments(profile, port = 0).joinToString(" ") { word ->
            if (word.any(Char::isWhitespace)) "\"$word\"" else word
        }

    /** The loopback port [arguments] listen on, read the way the core reads its --bind; null without one. */
    fun bindPortOf(arguments: List<String>): Int? {
        val at = arguments.lastIndexOf(BIND)
        if (at < 0) return null
        return arguments.getOrNull(at + 1)?.substringAfterLast(':')?.toIntOrNull()?.takeIf { it in 1..65535 }
    }
}
