package dev.cluvex.zedsecure.domain.config

/**
 * Builds command line arguments for the Rust Aether core daemon.
 * A faithful port of PattNG's AetherCoreManager.buildArguments.
 */
object AetherCommands {

    const val BIND = "--bind"
    const val UPSTREAM = "--upstream"
    const val TOR_BIND = "--tor-bind"
    const val PSIPHON_BIND = "--psiphon-bind"
    const val REGISTER = "--register"
    const val LOG_LEVEL = "--log-level"
    const val DEFAULT_LOG_LEVEL = "info"

    /** The marker that asks the core to load the shipped Psiphon server entries file. */
    const val PSIPHON_SERVER_ENTRIES = "--psiphon-server-entries"
    const val SHIPPED_LIST = "shipped"

    private fun settingValue(value: String?): String? =
        value?.trim()?.takeUnless { it.isEmpty() || it.startsWith('-') }

    /** MASQUE over HTTP/2, or a REVERSE carrier around the tunnel, takes no obfuscation noise. */
    fun masqueOverHttp2(
        protocol: String,
        transport: String,
        tor: String,
        psiphon: String,
    ): Boolean = (protocol == AetherProfile.PROTO_MASQUE || protocol == AetherProfile.PROTO_MIM || protocol == AetherProfile.PROTO_WG_OVER_MASQUE) &&
        (transport == AetherProfile.TRANSPORT_H2 || tor == AetherProfile.CARRIER_REVERSE || psiphon == AetherProfile.CARRIER_REVERSE)

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
     * [text] as a range of [bounds]: "16" or "8-16", trimmed; null when out of bounds or malformed.
     * [AetherRange] equivalent.
     */
    fun parseRange(text: String?, bounds: IntRange): Pair<Int, Int>? {
        val parts = text?.trim()?.split('-') ?: return null
        if (parts.size !in 1..2) return null
        val values = parts.map { part -> part.trim().toIntOrNull()?.takeIf { it in bounds } ?: return null }
        return Pair(values.min(), values.max())
    }

    fun rangeText(range: Pair<Int, Int>): String =
        if (range.first == range.second) "${range.first}" else "${range.first}-${range.second}"

    /**
     * [profile] as an endpoint "host:port"; [AetherEndpoint]-equivalent validation, IPv6 brackets included.
     */
    fun endpointOf(host: String?, port: String?): String? {
        val h = host?.trim()?.removeSurrounding("[", "]")?.takeIf { it.isNotEmpty() } ?: return null
        val p = port?.trim()?.toIntOrNull()?.takeIf { it in 1..65535 } ?: return null
        return if (':' in h) "[$h]:$p" else "$h:$p"
    }

    fun parseEndpoint(value: String?): Pair<String, Int>? {
        val text = value?.trim().orEmpty()
        val separator = text.lastIndexOf(':')
        if (separator <= 0) return null
        val host = text.substring(0, separator)
        if (':' in host && !(host.startsWith('[') && host.endsWith(']'))) return null
        val port = text.substring(separator + 1).trim().toIntOrNull()?.takeIf { it in 1..65535 } ?: return null
        val cleanHost = host.removeSurrounding("[", "]")
        return Pair(cleanHost, port)
    }

    fun buildArguments(
        profile: AetherProfile,
        port: Int,
        scan: Boolean = false,
        logLevel: String = DEFAULT_LOG_LEVEL,
    ): List<String> {
        // A scan looks for WARP endpoints from where the session will look: a carrier around the
        // tunnel stays (REVERSE), while one inside the tunnel (CHAIN / ONLY) has no part in it.
        val tor = profile.tor.takeUnless { scan && it != AetherProfile.CARRIER_REVERSE } ?: AetherProfile.CARRIER_OFF
        val psiphon = profile.psiphon.takeUnless { scan && it != AetherProfile.CARRIER_REVERSE } ?: AetherProfile.CARRIER_OFF

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
                masqueOverHttp2(profile.protocol, profile.transport, tor, psiphon).let { overHttp2 ->
                    if (profile.obfuscation != AetherProfile.OBF_AUTO && !overHttp2) {
                        addAll(listOf("--noize", profile.obfuscation))
                    }
                }
                addAll(listOf("--ip", profile.ipVersion))
                settingValue(profile.dns)?.let { addAll(listOf("--dns", it)) }
                // A scan keeps the exit rule as well, so that it ends on an endpoint the session will accept.
                settingValue(profile.exitLoc)?.let { addAll(listOf("--exit-loc", it)) }

                if (profile.overMasque && profile.transport == AetherProfile.TRANSPORT_H2) {
                    add("--h2")
                    if (profile.fragment) {
                        add("--fragment")
                        parseRange(profile.fragmentSize, 1..4096)
                            ?.let { addAll(listOf("--fragment-size", rangeText(it))) }
                        parseRange(profile.fragmentDelay, 0..1000)
                            ?.let { addAll(listOf("--fragment-delay", rangeText(it))) }
                    }
                }
                if (profile.overMasque && profile.ech) {
                    addAll(listOf("--ech", "auto"))
                    addAll(listOf("--ech-dns", settingValue(profile.echDns) ?: AetherProfile.DEFAULT_ECH_DNS))
                    addAll(listOf("--ech-domain", settingValue(profile.echDomain) ?: AetherProfile.DEFAULT_ECH_DOMAIN))
                }
                if (profile.overMasque) {
                    settingValue(profile.masqueSni)?.let { addAll(listOf("--masque-sni", it)) }
                    addAll(fingerprintArguments(profile.fingerprint))
                }

                if (profile.isTwoHops) {
                    val hop = if (profile.protocol == AetherProfile.PROTO_MIM) "--mim" else "--wiw"
                    val outer = profile.wiwOuter.trim().takeUnless { scan }
                    val inner = profile.wiwInner.trim().takeUnless { scan }
                    outer?.takeIf { it.isNotEmpty() }?.let { addAll(listOf("$hop-outer", it)) }
                    inner?.takeIf { it.isNotEmpty() }?.let { addAll(listOf("$hop-inner", it)) }
                    if (outer.isNullOrEmpty() && inner.isNullOrEmpty()) add("$hop-scan")
                } else if (!scan) {
                    profile.endpointText()?.let { addAll(listOf("--peer", it)) }
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
                        .forEach { addAll(listOf("--tor-bridge", it)) }
                }
                if (profile.torBridges == AetherProfile.TOR_BRIDGES_AUTO ||
                    profile.torBridges == AetherProfile.TOR_BRIDGES_FIRST
                ) {
                    profile.torRelays.takeUnless { it == AetherProfile.TOR_RELAYS_AUTO }
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
                // The bundled list, unless the profile wants Psiphon to fetch a fresh one before it dials anything.
                if (profile.psiphonBundledList) addAll(listOf(PSIPHON_SERVER_ENTRIES, SHIPPED_LIST))
            }
            addAll(listOf(LOG_LEVEL, logLevel))
        }
    }

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

    fun isCustom(profile: AetherProfile): Boolean = profile.command.trim().isNotEmpty()

    fun argumentsOf(command: String): List<String> {
        val words = words(command)
        return if (words.firstOrNull()?.startsWith("-") == false) words.drop(1) else words
    }

    fun runArguments(
        profile: AetherProfile,
        port: Int,
        scan: Boolean = false,
        logLevel: String = DEFAULT_LOG_LEVEL,
    ): List<String> =
        if (isCustom(profile)) argumentsOf(profile.command)
        else buildArguments(profile, port, scan, logLevel)
}
