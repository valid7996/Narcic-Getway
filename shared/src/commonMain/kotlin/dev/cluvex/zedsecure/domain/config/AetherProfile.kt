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
    val tor: String = CARRIER_OFF,
    val torBridges: String = TOR_BRIDGES_AUTO,
    val torBridgeLines: String = "",
    val torRelays: String = TOR_RELAYS_AUTO,
    val psiphon: String = CARRIER_OFF,
    val psiphonMode: String = PSIPHON_MODE_AUTO,
    val psiphonRegion: String = "",
    val psiphonCdnIps: String = "",
    val psiphonCdnSni: String = "",
    val psiphonCdnSets: String = "",
    val finalMask: String = "",
    val dialMode: String = "",
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

        const val CARRIER_OFF = "off"
        const val CARRIER_CHAIN = "chain"
        const val CARRIER_REVERSE = "reverse"
        const val CARRIER_ONLY = "only"

        const val TOR_BRIDGES_AUTO = "auto"
        const val TOR_BRIDGES_FIRST = "first"
        const val TOR_BRIDGES_NEVER = "never"
        const val TOR_BRIDGES_OWN = "own"

        const val TOR_RELAYS_AUTO = "auto"
        const val TOR_RELAYS_ONLY = "only"
        const val TOR_RELAYS_OFF = "off"

        const val PSIPHON_MODE_AUTO = "auto"
        const val PSIPHON_MODE_CDN = "cdn"
        const val PSIPHON_MODE_DIRECT = "direct"

        const val DEFAULT_ECH_DNS = "udp://1.1.1.1"
        const val DEFAULT_ECH_DOMAIN = "cloudflare-ech.com"
        const val DEFAULT_ENROLL_ADDRESS = "api.cloudflareclient.com"

        val protocols = listOf(PROTO_WG, PROTO_MASQUE, PROTO_GOOL, PROTO_MIM)
        val transports = listOf(TRANSPORT_H3, TRANSPORT_H2)
        val scanModes = listOf(SCAN_TURBO, SCAN_BALANCED, SCAN_THOROUGH, SCAN_VERIFIED, SCAN_IRONCLAD)
        val obfuscations = listOf(OBF_AUTO, OBF_OFF, OBF_LIGHT, OBF_FIREWALL, OBF_BALANCED, OBF_GFW, OBF_AGGRESSIVE)
        val ipVersions = listOf(IP_V4, IP_V6, IP_DUAL)
        val fingerprints = listOf(FINGERPRINT_CHROME, FINGERPRINT_FIREFOX, FINGERPRINT_SEMI_PYTHON, FINGERPRINT_GO)
        val carriers = listOf(CARRIER_OFF, CARRIER_CHAIN, CARRIER_REVERSE, CARRIER_ONLY)
        val torBridgeModes = listOf(TOR_BRIDGES_AUTO, TOR_BRIDGES_FIRST, TOR_BRIDGES_NEVER, TOR_BRIDGES_OWN)
        val torRelayModes = listOf(TOR_RELAYS_AUTO, TOR_RELAYS_ONLY, TOR_RELAYS_OFF)
        val psiphonModes = listOf(PSIPHON_MODE_AUTO, PSIPHON_MODE_CDN, PSIPHON_MODE_DIRECT)
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
    const val TOR_BIND = "--tor-bind"
    const val PSIPHON_BIND = "--psiphon-bind"
    const val REGISTER = "--register"

    private const val LOG_LEVEL = "--log-level"
    private const val DEFAULT_LOG_LEVEL = "info"

    /** [text] as an option value: trimmed, and null when it is blank or would read as an option of its own. */
    private fun settingValue(value: String?): String? =
        value?.trim()?.takeUnless { it.isEmpty() || it.startsWith('-') }

    /** The cipher suites of [fingerprint], as the core's --tls-ciphers takes them, with GREASE where the client sends it. */
    private fun fingerprintArguments(fingerprint: String): List<String> {
        val (ciphers, grease) = when (fingerprint) {
            AetherProfile.FINGERPRINT_FIREFOX -> (
                "ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-CHACHA20-POLY1305:ECDHE-RSA-CHACHA20-POLY1305:" +
                    "ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-RSA-AES128-SHA:ECDHE-RSA-AES256-SHA:" +
                    "AES128-GCM-SHA256:AES256-GCM-SHA384:AES128-SHA:AES256-SHA"
                ) to false

            AetherProfile.FINGERPRINT_SEMI_PYTHON -> (
                "ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:" +
                    "ECDHE-ECDSA-CHACHA20-POLY1305:ECDHE-RSA-CHACHA20-POLY1305:ECDHE-ECDSA-AES256-SHA:ECDHE-RSA-AES256-SHA:" +
                    "ECDHE-ECDSA-AES128-SHA256:ECDHE-RSA-AES128-SHA256"
                ) to false

            AetherProfile.FINGERPRINT_GO -> (
                "ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384:" +
                    "ECDHE-ECDSA-CHACHA20-POLY1305:ECDHE-RSA-CHACHA20-POLY1305:ECDHE-ECDSA-AES128-SHA:ECDHE-RSA-AES128-SHA:" +
                    "ECDHE-ECDSA-AES256-SHA:ECDHE-RSA-AES256-SHA"
                ) to false

            else -> "ALL:!aPSK:!ECDSA+SHA1:!3DES" to true
        }
        return listOf("--tls-ciphers", ciphers) + if (grease) emptyList() else listOf("--disable-grease")
    }

    /**
     * The arguments [profile] runs the core with. The listener the app dials takes [port]: the
     * core's own, or Tor's or Psiphon's when one of them runs inside the tunnel and is what the app
     * reaches. With [scan], hops and the endpoint are left out and the quick reconnect is off, so
     * the run looks for an endpoint and ends; a carrier around the tunnel stays for the scan.
     */
    fun buildArguments(profile: AetherProfile, port: Int, scan: Boolean = false, logLevel: String = DEFAULT_LOG_LEVEL): List<String> {
        val psiphon = profile.psiphon
        val tor = profile.tor
        val dialsPsiphon = psiphon == AetherProfile.CARRIER_CHAIN
        val dialsTor = tor == AetherProfile.CARRIER_CHAIN && !dialsPsiphon
        var next = port + 1
        val own = if (dialsPsiphon || dialsTor) next++ else port
        val torBind = when (tor) {
            AetherProfile.CARRIER_CHAIN -> if (dialsTor) port else next++
            AetherProfile.CARRIER_REVERSE -> next++
            else -> null
        }
        val psiphonBind = when (psiphon) {
            AetherProfile.CARRIER_CHAIN -> if (dialsPsiphon) port else next++
            AetherProfile.CARRIER_REVERSE -> 0
            else -> null
        }
        return buildList {
            addAll(listOf(BIND, "127.0.0.1:$own"))
            if (psiphon != AetherProfile.CARRIER_ONLY && tor != AetherProfile.CARRIER_ONLY) {
                addAll(listOf("--protocol", profile.protocol))
                addAll(listOf("--scan", profile.scanMode))
                if (profile.obfuscation != AetherProfile.OBF_AUTO &&
                    !(profile.overMasque && profile.transport == AetherProfile.TRANSPORT_H2)
                ) {
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
                    val outer = profile.wiwOuter.trim().takeUnless { scan }
                    val inner = profile.wiwInner.trim().takeUnless { scan }
                    outer?.takeIf { it.isNotEmpty() }?.let { addAll(listOf("$hop-outer", it)) }
                    inner?.takeIf { it.isNotEmpty() }?.let { addAll(listOf("$hop-inner", it)) }
                    if (outer.isNullOrEmpty() && inner.isNullOrEmpty()) add("$hop-scan")
                } else {
                    profile.endpointText()?.takeUnless { scan }?.let { addAll(listOf("--peer", it)) }
                }

                add(if (scan) "--no-quick-reconnect" else "--quick-reconnect")
            }

            when (tor) {
                AetherProfile.CARRIER_OFF -> Unit
                AetherProfile.CARRIER_CHAIN -> add("--tor")
                AetherProfile.CARRIER_REVERSE -> add("--tor-reverse")
                AetherProfile.CARRIER_ONLY -> add("--tor-only")
            }
            torBind?.let { addAll(listOf(TOR_BIND, "127.0.0.1:$it")) }
            if (tor != AetherProfile.CARRIER_OFF) {
                when (profile.torBridges) {
                    AetherProfile.TOR_BRIDGES_AUTO -> Unit
                    AetherProfile.TOR_BRIDGES_FIRST -> add("--tor-bridges")
                    AetherProfile.TOR_BRIDGES_NEVER -> add("--no-tor-bridges")
                    AetherProfile.TOR_BRIDGES_OWN -> profile.torBridgeLines.lines()
                        .map(String::trim)
                        .filter { it.isNotEmpty() }
                        .flatMap { line -> listOf("--tor-bridge", line) }
                }
                if (profile.torBridges == AetherProfile.TOR_BRIDGES_AUTO ||
                    profile.torBridges == AetherProfile.TOR_BRIDGES_FIRST
                ) {
                    settingValue(profile.torRelays)?.takeIf { it != AetherProfile.TOR_RELAYS_AUTO }
                        ?.let { addAll(listOf("--tor-relays", it)) }
                }
            }

            when (psiphon) {
                AetherProfile.CARRIER_OFF -> Unit
                AetherProfile.CARRIER_CHAIN -> add("--psiphon")
                AetherProfile.CARRIER_REVERSE -> add("--psiphon-reverse")
                AetherProfile.CARRIER_ONLY -> add("--psiphon-only")
            }
            psiphonBind?.let { addAll(listOf(PSIPHON_BIND, "127.0.0.1:$it")) }
            if (psiphon != AetherProfile.CARRIER_OFF) {
                addAll(listOf("--psiphon-mode", profile.psiphonMode))
                val cdnIps = settingValue(profile.psiphonCdnIps)?.takeIf { profile.psiphonMode != AetherProfile.PSIPHON_MODE_DIRECT }
                cdnIps?.let { addAll(listOf("--psiphon-cdn-ips", it)) }
                if (cdnIps != null) settingValue(profile.psiphonCdnSni)?.let { addAll(listOf("--psiphon-cdn-sni", it)) }
                if (profile.psiphonMode != AetherProfile.PSIPHON_MODE_DIRECT) {
                    settingValue(profile.psiphonCdnSets)?.let { addAll(listOf("--psiphon-cdn-sets", it)) }
                }
                settingValue(profile.psiphonRegion)?.let { addAll(listOf("--psiphon-region", it)) }
            }
            addAll(listOf(LOG_LEVEL, logLevel))
        }
    }

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
    fun runArguments(profile: AetherProfile, port: Int, scan: Boolean = false, logLevel: String = DEFAULT_LOG_LEVEL): List<String> =
        if (isCustom(profile)) argumentsOf(profile.command)
        else buildArguments(profile, port, scan, logLevel)

    /** Whether [profile] carries a command written by hand: one that is not blank and not the built one. */
    fun isCustom(profile: AetherProfile): Boolean =
        profile.command.isNotBlank() && profile.command.trim() != buildArguments(profile, port = 0).joinToString(" ") { word ->
            if (word.any(Char::isWhitespace)) "\"$word\"" else word
        }

    /** The option naming the listener the app dials, read the way the core reads the arguments. */
    fun listenerFlagOf(arguments: List<String>): String = when {
        carrierModeOf(arguments, "--psiphon") == AetherProfile.CARRIER_CHAIN -> PSIPHON_BIND
        carrierModeOf(arguments, "--tor") == AetherProfile.CARRIER_CHAIN -> TOR_BIND
        else -> BIND
    }

    /** The carrier mode [flag] selects, as the core reads it: the last mode flag wins. */
    fun carrierModeOf(arguments: List<String>, flag: String): String = arguments.fold(AetherProfile.CARRIER_OFF) { mode, word ->
        when (word) {
            flag -> AetherProfile.CARRIER_CHAIN
            "$flag-reverse" -> AetherProfile.CARRIER_REVERSE
            "$flag-only" -> AetherProfile.CARRIER_ONLY
            else -> mode
        }
    }

    /** The value after the last [flag] in [arguments]; the last one is the one the core keeps. */
    fun valueAfter(arguments: List<String>, flag: String): String? =
        arguments.lastIndexOf(flag).takeIf { it >= 0 }?.let { arguments.getOrNull(it + 1) }

    /** The loopback port [arguments] listen on for the app, read the way the core reads them; null without one. */
    fun listenerPortOf(arguments: List<String>): Int? =
        valueAfter(arguments, listenerFlagOf(arguments))?.substringAfterLast(':')?.toIntOrNull()?.takeIf { it in 1..65535 }

    /** The loopback port [arguments] name with [BIND], whatever else they name; null without one. */
    fun bindPortOf(arguments: List<String>): Int? =
        valueAfter(arguments, BIND)?.substringAfterLast(':')?.toIntOrNull()?.takeIf { it in 1..65535 }
}
