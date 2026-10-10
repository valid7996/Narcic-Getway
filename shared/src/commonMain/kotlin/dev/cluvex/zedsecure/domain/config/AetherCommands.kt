package dev.cluvex.zedsecure.domain.config

/**
 * Builds command line arguments for the Rust Aether core daemon.
 * Strictly adheres to PattNG's AetherCoreManager.buildArguments logic.
 */
object AetherCommands {

    const val BIND = "--bind"
    const val UPSTREAM = "--upstream"
    const val TOR_BIND = "--tor-bind"
    const val PSIPHON_BIND = "--psiphon-bind"
    const val REGISTER = "--register"
    const val LOG_LEVEL = "--log-level"
    const val DEFAULT_LOG_LEVEL = "info"

    private fun settingValue(value: String?): String? =
        value?.trim()?.takeUnless { it.isEmpty() || it.startsWith('-') }

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

    fun buildArguments(
        profile: AetherProfile,
        port: Int,
        scan: Boolean = false,
        logLevel: String = DEFAULT_LOG_LEVEL,
    ): List<String> {
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
                settingValue(profile.targetStrategy)?.let { addAll(listOf("--target-strategy", it)) }

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

            settingValue(profile.finalMask)?.let { addAll(listOf("--final-mask", it)) }
            settingValue(profile.dialMode)?.let { addAll(listOf("--dial-mode", it)) }

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
                if (profile.psiphonBundledList) add("--psiphon-bundled-list")
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
