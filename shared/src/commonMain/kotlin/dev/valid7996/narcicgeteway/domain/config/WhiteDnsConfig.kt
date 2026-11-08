@file:OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)

package dev.valid7996.narcicgeteway.domain.config

import kotlin.io.encoding.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject

class WhiteDnsConfigException(message: String) : IllegalArgumentException(message)

/** A `KEY = value` line of a TOML text; [raw] is the value as written, comment removed. */
data class TomlAssignment(val key: String, val raw: String, val line: Int)

/** The outcome of reading a config text back into the form: the new value and what could not be used. */
data class TomlApplied<T>(val value: T, val ignoredKeys: List<String>)

/**
 * Everything that turns a [WhiteDnsProfile] and the shared [WhiteDnsSettings] into the config the engine runs
 * on, and the other way round: the editable config text is read back into the same fields the form shows, so
 * the form and the text never disagree.
 */
object WhiteDnsConfig {

    // ---------------------------------------------------------------- engine config (what really runs)

    /**
     * The complete client TOML of one connection. Keys of [WhiteDnsProfile.extraToml] come last and replace
     * the generated value of the same key.
     */
    fun renderRuntimeToml(
        profile: WhiteDnsProfile,
        settings: WhiteDnsSettings,
        listenIp: String,
        listenPort: Int,
    ): String {
        val p = profile.normalized()
        val lines = LinkedHashMap<String, String>()

        lines["DOMAINS"] = tomlArray(p.domainList())
        lines["DATA_ENCRYPTION_METHOD"] = p.encryptionMethod.toString()
        lines["ENCRYPTION_KEY"] = quote(p.encryptionKey)
        lines["PROTOCOL_TYPE"] = quote("SOCKS5")
        if (p.isCottenDns) putCotten(p, lines)
        putClient(settings.resolve(), listenIp, listenPort, lines)

        for (extra in parseAssignments(p.extraToml)) lines[extra.key] = extra.raw
        return lines.entries.joinToString("\n") { "${it.key} = ${it.value}" }
    }

    /** The resolver list file of a connection, one entry per line. */
    fun renderResolvers(entries: List<String>): String = entries.joinToString("\n")

    private fun putClient(
        r: WhiteDnsResolved,
        listenIp: String,
        listenPort: Int,
        out: MutableMap<String, String>,
    ) {
        out["LISTEN_IP"] = quote(listenIp)
        out["LISTEN_PORT"] = listenPort.toString()
        out["SOCKS5_AUTH"] = "false"
        out["SOCKS5_USER"] = quote("")
        out["SOCKS5_PASS"] = quote("")
        out["LOCAL_DNS_ENABLED"] = "false"
        out["LOCAL_DNS_IP"] = quote("127.0.0.1")
        out["LOCAL_DNS_PORT"] = "10888"
        out["RESOLVER_BALANCING_STRATEGY"] = r.balancingStrategy.toString()
        out["UPLOAD_PACKET_DUPLICATION_COUNT"] = r.uploadDuplication.toString()
        out["DOWNLOAD_PACKET_DUPLICATION_COUNT"] = r.downloadDuplication.toString()
        out["UPLOAD_COMPRESSION_TYPE"] = r.uploadCompression.toString()
        out["DOWNLOAD_COMPRESSION_TYPE"] = r.downloadCompression.toString()
        out["BASE_ENCODE_DATA"] = r.baseEncodeData.toString()
        out["MIN_UPLOAD_MTU"] = r.minUploadMtu.toString()
        out["MIN_DOWNLOAD_MTU"] = r.minDownloadMtu.toString()
        out["MAX_UPLOAD_MTU"] = r.maxUploadMtu.toString()
        out["MAX_DOWNLOAD_MTU"] = r.maxDownloadMtu.toString()
        out["MTU_TEST_RETRIES_RESOLVERS"] = r.mtuTestRetriesResolvers.toString()
        out["MTU_TEST_TIMEOUT_RESOLVERS"] = num(r.mtuTestTimeoutResolvers)
        out["MTU_TEST_PARALLELISM_RESOLVERS"] = r.mtuTestParallelismResolvers.toString()
        out["MTU_TEST_RETRIES_LOGS"] = r.mtuTestRetriesLogs.toString()
        out["MTU_TEST_TIMEOUT_LOGS"] = num(r.mtuTestTimeoutLogs)
        out["MTU_TEST_PARALLELISM_LOGS"] = r.mtuTestParallelismLogs.toString()
        out["RX_TX_WORKERS"] = r.rxTxWorkers.toString()
        out["TUNNEL_PROCESS_WORKERS"] = r.tunnelProcessWorkers.toString()
        out["TUNNEL_PACKET_TIMEOUT_SECONDS"] = num(r.tunnelPacketTimeoutSeconds)
        out["DISPATCHER_IDLE_POLL_INTERVAL_SECONDS"] = num(r.dispatcherIdlePollIntervalSeconds)
        out["TX_CHANNEL_SIZE"] = r.txChannelSize.toString()
        out["RX_CHANNEL_SIZE"] = r.rxChannelSize.toString()
        out["RESOLVER_UDP_CONNECTION_POOL_SIZE"] = r.resolverUdpConnectionPoolSize.toString()
        out["STREAM_QUEUE_INITIAL_CAPACITY"] = r.streamQueueInitialCapacity.toString()
        out["ORPHAN_QUEUE_INITIAL_CAPACITY"] = r.orphanQueueInitialCapacity.toString()
        out["DNS_RESPONSE_FRAGMENT_STORE_CAPACITY"] = r.dnsResponseFragmentStoreCapacity.toString()
        out["MAX_ACTIVE_STREAMS"] = r.maxActiveStreams.toString()
        out["LOCAL_HANDSHAKE_TIMEOUT_SECONDS"] = num(r.localHandshakeTimeoutSeconds)
        out["SOCKS_UDP_ASSOCIATE_READ_TIMEOUT_SECONDS"] = num(r.socksUdpAssociateReadTimeoutSeconds)
        out["CLIENT_TERMINAL_STREAM_RETENTION_SECONDS"] = num(r.clientTerminalStreamRetentionSeconds)
        out["CLIENT_CANCELLED_SETUP_RETENTION_SECONDS"] = num(r.clientCancelledSetupRetentionSeconds)
        out["SESSION_INIT_RETRY_BASE_SECONDS"] = num(r.sessionInitRetryBaseSeconds)
        out["SESSION_INIT_RETRY_STEP_SECONDS"] = num(r.sessionInitRetryStepSeconds)
        out["SESSION_INIT_RETRY_LINEAR_AFTER"] = r.sessionInitRetryLinearAfter.toString()
        out["SESSION_INIT_RETRY_MAX_SECONDS"] = num(r.sessionInitRetryMaxSeconds)
        out["SESSION_INIT_BUSY_RETRY_INTERVAL_SECONDS"] = num(r.sessionInitBusyRetryIntervalSeconds)
        out["STARTUP_MODE"] = quote(r.startupMode)
        out["LOG_SCAN_MAX_DAYS"] = "14"
        out["LOG_SCAN_MAX_RESOLVERS"] = "128"
        out["LOG_BASED_MTU_VERIFY"] = "true"
        out["STATS_REPORT_INTERVAL_SECONDS"] = "1.0"
        out["PING_WATCHDOG_TIMEOUT_SECONDS"] = r.pingWatchdogSeconds.toString()
        out["LOG_LEVEL"] = quote(r.logLevel)
        out["LOG_TO_FILE"] = "true"
        out["LOG_DIR"] = quote("logs")
    }

    private fun putCotten(p: WhiteDnsProfile, out: MutableMap<String, String>) {
        val compat = p.isCompatibility
        val preset = p.configPreset

        out["CONFIG_PRESET"] = quote(enginePresetBase(preset, compat))
        out["LEGACY_SESSION_ID"] = compat.toString()
        if (!compat) {
            out["TUNNEL_MODE"] = quote(p.tunnelMode)
            putCottenOptions(p, out)
        }
        out["TERMINAL_UI"] = quote("plain")
        out["RESOLVER_IP_MODE"] = quote(resolverIpMode(p))

        val rate = p.queryRateLimit.toIntOrNull() ?: 0
        out["QUERY_RATE_LIMIT_PER_SECOND"] = (if (p.queryRateScope == "total") rate else 0).toString()
        out["QUERY_RATE_LIMIT_PER_RESOLVER_PER_SECOND"] = (if (p.queryRateScope == "resolver") rate else 0).toString()
        out["QUERY_RATE_LIMIT_PER_DOMAIN_PER_SECOND"] = (if (p.queryRateScope == "domain") rate else 0).toString()
        out["QUERY_TIMING_JITTER"] = when (p.timingMask) {
            "light" -> "0.3"
            "strong" -> "0.6"
            else -> "0.0"
        }

        val transport = effectiveTransport(p)
        val queryTypes = when {
            compat -> listOf("TXT")
            p.deliveryMode != WhiteDnsProfile.MODE_PRESET -> deliveryTypesFor(p.deliveryMode)
            else -> queryTypeSet(preset)
        }
        out["RESOLVER_TRANSPORT"] = quote(transport)
        putEncryptedResolver(transport, p, out)
        out["QUERY_TYPES"] = tomlArray(queryTypes)
        out["QNAME_LABEL_LENGTH"] = qnameLength(p).toString()

        out["FAST_CONNECT"] = "true"
        out["MTU_BACKGROUND_PARALLELISM"] = p.backgroundScanParallelism.toString()

        out["RESOLVER_RATE_LIMIT_ENABLED"] = "true"
        out["DNS_RANDOMIZE_QUERY_ID"] = "true"
        out["DNS_QNAME_CASE_RANDOMIZATION"] = "false"
        out["RESOLVER_IGNORE_INJECTED_NXDOMAIN"] = "true"

        if (!compat) {
            out["ADAPTIVE_DUPLICATION"] = "true"
            out["DUPLICATION_PREFER_DISTINCT_DOMAINS"] = "true"
            out["ADAPTIVE_DUPLICATION_TARGET_DELIVERY"] = if (preset == "survival") "0.97" else "0.95"
            out["DNS_EDNS_COOKIE"] = "true"
            out["EDNS_UDP_SIZE"] = (if (preset == "survival") 1232 else 4096).toString()
            out["MTU_PROBE_SAMPLES"] = (if (preset == "survival") 5 else 1).toString()
            out["MTU_MAX_LOSS"] = if (preset == "survival") "0.5" else "0.0"
            out["MTU_ADAPTIVE_GROUPING"] = "true"
            out["MTU_GROUP_GAP_RATIO"] = "0.25"
        } else {
            out["ADAPTIVE_DUPLICATION"] = "false"
            out["DUPLICATION_PREFER_DISTINCT_DOMAINS"] = "false"
            out["DNS_EDNS_COOKIE"] = "false"
            out["MTU_ADAPTIVE_GROUPING"] = "false"
            out["MTU_PROBE_SAMPLES"] = "1"
            out["MTU_MAX_LOSS"] = "0.0"
        }
    }

    private fun putCottenOptions(p: WhiteDnsProfile, out: MutableMap<String, String>) {
        val options = p.max.normalized()
        val transport = effectiveTransport(p)
        if (transport in ENCRYPTED_TRANSPORTS && options.dnsPaddingBlockSize > 0) {
            out["DNS_PADDING_BLOCK_SIZE"] = options.dnsPaddingBlockSize.toString()
        }
        if (transport == "doh3" && options.doh3FallbackToDoh) out["RESOLVER_HTTP3_FALLBACK"] = "true"
        if (!p.usesCottenMax) return

        out["COTTENMAX_TRAFFIC_PROFILE"] = quote(options.trafficProfile)
        out["COTTENMAX_LOW_REDUCTION_PERCENT"] = options.lowReductionPercent.toString()
        val primary = p.domainList().map { it.lowercase() }.distinct()
        val standbys = WhiteDnsMaxOptions.entries(options.standbyDomains)
            .filterNot { it in primary }
            .take((8 - primary.size).coerceAtLeast(0))
        if (standbys.isNotEmpty()) out["STANDBY_DOMAINS"] = tomlArray(standbys)
        out["COTTENMAX_FAILOVER_AFTER_MILLISECONDS"] = options.failoverAfterMs.toString()

        val labelLength = when (p.qnameMode) {
            "off" -> 63
            "moderate" -> 42
            "aggressive" -> 32
            else -> if (p.configPreset == "survival") 42 else 63
        }
        // The first HELLO needs 55 base32 characters plus its reserved subtree and the label separators; an
        // account domain can be too long for the engine's 101-character starting budget.
        val handshakeLength = (primary + standbys).maxOfOrNull {
            "cottenmax-v1.$it".length + 1 + 55 + 54 / labelLength + (if (options.labelCountJitter) 3 else 0)
        } ?: 101
        if (handshakeLength > 253) {
            throw WhiteDnsConfigException("CottenMax domain leaves insufficient DNS handshake capacity")
        }
        val safeQName = maxOf(options.safeQName, handshakeLength)
        out["COTTENMAX_SAFE_QNAME"] = safeQName.toString()
        out["COTTENMAX_MAX_QNAME"] = maxOf(options.maxQName, safeQName).toString()
        out["COTTENMAX_MAX_ANSWER"] = options.maxAnswer.toString()
        out["COTTENMAX_MAX_PATH_WINDOW"] = options.maxPathWindow.toString()
        out["COTTENMAX_MAX_INFLIGHT"] = options.maxInflight.toString()
        out["COTTENMAX_FEC_DOWNLOAD_ENABLED"] = options.fecDownloadEnabled.toString()
        out["COTTENMAX_FEC_DATA_SHARDS"] = options.fecDataShards.toString()
        out["COTTENMAX_FEC_PARITY_SHARDS"] = options.fecParityShards.toString()
        out["COTTENMAX_SESSION_IDLE_MILLISECONDS"] = options.sessionIdleMs.toString()
        out["COTTENMAX_HOLD_MILLISECONDS"] = options.holdMs.toString()
        out["QNAME_LABEL_COUNT_JITTER"] = options.labelCountJitter.toString()
        if (options.qnameLossGuard) out["COTTENMAX_QNAME_LOSS_GUARD"] = "true"
        out["RESOLVER_DISCOVERY_ENABLED"] = options.discoveryEnabled.toString()
        if (options.discoveryEnabled) {
            out["RESOLVER_DISCOVERY_COUNTRY"] = quote(options.discoveryCountry)
            out["RESOLVER_DISCOVERY_SAMPLE_RANGES"] = options.discoverySampleRanges.toString()
            out["RESOLVER_DISCOVERY_WANT"] = options.discoveryWant.toString()
            out["RESOLVER_DISCOVERY_CONCURRENCY"] = options.discoveryConcurrency.toString()
            out["RESOLVER_DISCOVERY_PACE_MILLISECONDS"] = options.discoveryPaceMs.toString()
            val candidates = WhiteDnsMaxOptions.entries(options.discoveryCandidates)
            if (candidates.isNotEmpty()) out["RESOLVER_DISCOVERY_CANDIDATES"] = tomlArray(candidates)
            if (options.discoveryCacheEnabled) {
                out["RESOLVER_DISCOVERY_CACHE_FILE"] = quote("cottendns-discovery-cache.json")
                out["RESOLVER_DISCOVERY_NETWORK_ID"] = quote(options.discoveryNetworkId)
            }
        }
    }

    private fun putEncryptedResolver(transport: String, p: WhiteDnsProfile, out: MutableMap<String, String>) {
        if (transport !in ENCRYPTED_TRANSPORTS) return
        if (p.resolverTlsServerName.isNotEmpty()) out["RESOLVER_TLS_SERVER_NAME"] = quote(p.resolverTlsServerName)
        if (p.resolverTlsPin.isNotEmpty()) out["RESOLVER_TLS_PIN"] = quote(p.resolverTlsPin)
        when (transport) {
            "doq" -> out["RESOLVER_DOQ_PORT"] = p.resolverDotPort
            "dot" -> out["RESOLVER_DOT_PORT"] = p.resolverDotPort
            else -> {
                out["RESOLVER_DOH_PORT"] = p.resolverDohPort
                out["RESOLVER_DOH_PATH"] = quote(p.resolverDohPath)
            }
        }
    }

    private val ENCRYPTED_TRANSPORTS = setOf("dot", "doh", "doh3", "doq")

    private fun enginePresetBase(preset: String, compat: Boolean): String {
        if (compat) return "default"
        return when (preset) {
            "speed", "survival", "tcp-survival" -> preset
            else -> "default"
        }
    }

    private fun presetTransport(preset: String): String = when (preset) {
        "tcp-survival" -> "tcp"
        "master-storm" -> "udp"
        else -> "auto"
    }

    private fun effectiveTransport(p: WhiteDnsProfile): String = when {
        p.isCompatibility -> "udp"
        p.transportMode != WhiteDnsProfile.MODE_PRESET -> p.transportMode
        else -> presetTransport(p.configPreset)
    }

    private fun resolverIpMode(p: WhiteDnsProfile): String = when {
        p.isCompatibility -> "auto"
        p.ipMode != WhiteDnsProfile.MODE_PRESET -> p.ipMode
        else -> "auto"
    }

    private fun qnameLength(p: WhiteDnsProfile): Int = when {
        p.isCompatibility -> 63
        p.qnameMode == "off" -> 63
        p.qnameMode == "moderate" -> 42
        p.qnameMode == "aggressive" -> 32
        else -> if (p.configPreset == "survival") 42 else 63
    }

    private fun queryTypeSet(preset: String): List<String> = when (preset) {
        "speed" -> listOf("TXT", "HTTPS")
        "survival" -> listOf("TXT", "CNAME", "HTTPS", "A")
        "tcp-survival" -> listOf("TXT", "HTTPS")
        else -> listOf("TXT")
    }

    private fun deliveryTypesFor(mode: String): List<String> = when (mode) {
        "txt" -> listOf("TXT")
        "txt-cname" -> listOf("TXT", "CNAME")
        "txt-https" -> listOf("TXT", "HTTPS")
        "txt-aaaa" -> listOf("TXT", "AAAA")
        "all" -> listOf("TXT", "CNAME", "NULL", "HTTPS", "AAAA")
        else -> listOf("TXT")
    }

    // ---------------------------------------------------------------- the editable text of one connection

    /** The connection as editable text. Keys are the form's fields; [WhiteDnsProfile.extraToml] follows. */
    fun renderProfileToml(profile: WhiteDnsProfile): String {
        val p = profile.normalized()
        val text = buildString {
            for (key in PROFILE_KEYS) appendLine("${key.name} = ${key.read(p)}")
            if (p.resolverList().isNotEmpty()) appendLine("RESOLVERS = ${tomlArray(p.resolverList())}")
            val extras = parseAssignments(p.extraToml)
            if (extras.isNotEmpty()) {
                appendLine()
                appendLine("# Engine keys that replace the generated value")
                for (e in extras) appendLine("${e.key} = ${e.raw}")
            }
        }
        return text.trimEnd()
    }

    /**
     * Reads the text back into the fields. Keys the form knows go into their field; any other `KEY = value`
     * is kept as an engine override. Throws [WhiteDnsConfigException] with the line of the first mistake.
     */
    fun applyProfileToml(profile: WhiteDnsProfile, text: String): TomlApplied<WhiteDnsProfile> {
        var p = profile
        val extra = LinkedHashMap<String, String>()
        var resolvers: String? = null
        val byName = PROFILE_KEYS.associateBy { it.name }
        for (a in parseAssignments(text)) {
            val known = byName[a.key]
            when {
                known != null -> p = try {
                    known.write(p, a.raw)
                } catch (e: WhiteDnsConfigException) {
                    throw WhiteDnsConfigException("line ${a.line}: ${e.message}")
                }
                a.key == "RESOLVERS" -> resolvers = stringArray(a.raw, a.key, a.line).joinToString("\n")
                else -> extra[a.key] = a.raw
            }
        }
        p = p.copy(
            resolvers = resolvers ?: p.resolvers,
            extraToml = extra.entries.joinToString("\n") { "${it.key} = ${it.value}" },
        )
        return TomlApplied(p.normalized(), emptyList())
    }

    // ---------------------------------------------------------------- the editable text of the shared settings

    /** The shared settings as engine keys, one to one with the engine config. */
    fun renderSettingsToml(settings: WhiteDnsSettings): String =
        SETTINGS_KEYS.joinToString("\n") { "${it.name} = ${it.read(settings)}" }

    fun applySettingsToml(settings: WhiteDnsSettings, text: String): TomlApplied<WhiteDnsSettings> {
        var s = settings
        val ignored = ArrayList<String>()
        val byName = SETTINGS_KEYS.associateBy { it.name }
        for (a in parseAssignments(text)) {
            val known = byName[a.key]
            if (known == null) {
                ignored += a.key
                continue
            }
            s = try {
                known.write(s, a.raw)
            } catch (e: WhiteDnsConfigException) {
                throw WhiteDnsConfigException("line ${a.line}: ${e.message}")
            }
        }
        return TomlApplied(s, ignored)
    }

    // ---------------------------------------------------------------- links (stormdns:// and cottendns://)

    fun isLink(text: String): Boolean {
        val t = text.trim()
        return t.startsWith("stormdns://", ignoreCase = true) || t.startsWith("cottendns://", ignoreCase = true)
    }

    /** One link of the WhiteDNS ecosystem as (name, connection), or null when it cannot be read. */
    fun parseLink(link: String): Pair<String, WhiteDnsProfile>? = runCatching {
        val trimmed = link.trim()
        val schemeEnd = trimmed.indexOf("://")
        if (schemeEnd < 0) return@runCatching null
        val engine = when (trimmed.substring(0, schemeEnd).lowercase()) {
            "cottendns" -> WhiteDnsProfile.ENGINE_COTTEN
            "stormdns" -> WhiteDnsProfile.ENGINE_STORM
            else -> return@runCatching null
        }
        val payload = trimmed.substring(schemeEnd + 3).substringBefore('#').substringBefore('?').trim()
        if (payload.isEmpty()) return@runCatching null
        val root = Json.parseToJsonElement(decodeBase64(payload)).jsonObject
        val profileJson = root["profile"]?.jsonObject ?: return@runCatching null
        val server = profileJson["server"]?.jsonObject ?: return@runCatching null
        val domain = server.str("domain")?.trim()?.trimEnd('.').orEmpty()
        val key = server.str("encryption_key")?.trim().orEmpty()
        if (domain.isEmpty() || key.isEmpty()) return@runCatching null
        val method = (server.int("encryption_method") ?: WhiteDnsProfile.ENC_XOR).coerceIn(0, 5)
        val name = profileJson.str("name")?.trim().orEmpty()

        var profile = WhiteDnsProfile(
            engine = engine,
            domains = domain,
            encryptionKey = key,
            encryptionMethod = method,
        )
        if (engine == WhiteDnsProfile.ENGINE_COTTEN) {
            val c = profileJson["cottenSettings"]?.jsonObject ?: profileJson
            val d = WhiteDnsProfile()
            profile = profile.copy(
                tunnelMode = c.str("tunnelMode") ?: d.tunnelMode,
                configPreset = c.str("configPreset") ?: d.configPreset,
                transportMode = c.str("transportMode") ?: d.transportMode,
                deliveryMode = c.str("deliveryMode") ?: d.deliveryMode,
                qnameMode = c.str("qnameMode") ?: d.qnameMode,
                ipMode = c.str("ipMode") ?: d.ipMode,
                queryRateLimit = c.str("queryRateLimit") ?: d.queryRateLimit,
                queryRateScope = c.str("queryRateScope") ?: d.queryRateScope,
                timingMask = c.str("timingMask") ?: d.timingMask,
                backgroundScanParallelism = c.int("backgroundScanParallelism") ?: d.backgroundScanParallelism,
                resolverTlsServerName = c.str("resolverTlsServerName") ?: d.resolverTlsServerName,
                resolverTlsPin = c.str("resolverTlsPin") ?: d.resolverTlsPin,
                resolverDotPort = c.str("resolverDoTPort") ?: d.resolverDotPort,
                resolverDohPort = c.str("resolverDoHPort") ?: d.resolverDohPort,
                resolverDohPath = c.str("resolverDoHPath") ?: d.resolverDohPath,
                max = maxFromJson(c["cottenMaxOptions"] as? JsonObject),
            )
        }
        name.ifEmpty { domain } to profile.normalized()
    }.getOrNull()

    private fun maxFromJson(m: JsonObject?): WhiteDnsMaxOptions {
        val d = WhiteDnsMaxOptions()
        if (m == null) return d
        return WhiteDnsMaxOptions(
            trafficProfile = m.str("trafficProfile") ?: d.trafficProfile,
            lowReductionPercent = m.int("lowReductionPercent") ?: d.lowReductionPercent,
            standbyDomains = m.str("standbyDomains") ?: d.standbyDomains,
            failoverAfterMs = m.int("failoverAfterMs") ?: d.failoverAfterMs,
            safeQName = m.int("safeQName") ?: d.safeQName,
            maxQName = m.int("maxQName") ?: d.maxQName,
            maxAnswer = m.int("maxAnswer") ?: d.maxAnswer,
            maxPathWindow = m.int("maxPathWindow") ?: d.maxPathWindow,
            maxInflight = m.int("maxInflight") ?: d.maxInflight,
            fecDownloadEnabled = m.bool("fecDownloadEnabled") ?: d.fecDownloadEnabled,
            fecDataShards = m.int("fecDataShards") ?: d.fecDataShards,
            fecParityShards = m.int("fecParityShards") ?: d.fecParityShards,
            sessionIdleMs = m.int("sessionIdleMs") ?: d.sessionIdleMs,
            holdMs = m.int("holdMs") ?: d.holdMs,
            discoveryEnabled = m.bool("discoveryEnabled") ?: d.discoveryEnabled,
            discoveryCountry = m.str("discoveryCountry") ?: d.discoveryCountry,
            discoverySampleRanges = m.int("discoverySampleRanges") ?: d.discoverySampleRanges,
            discoveryCacheEnabled = m.bool("discoveryCacheEnabled") ?: d.discoveryCacheEnabled,
            discoveryNetworkId = m.str("discoveryNetworkId") ?: d.discoveryNetworkId,
            discoveryCandidates = m.str("discoveryCandidates") ?: d.discoveryCandidates,
            discoveryWant = m.int("discoveryWant") ?: d.discoveryWant,
            discoveryConcurrency = m.int("discoveryConcurrency") ?: d.discoveryConcurrency,
            discoveryPaceMs = m.int("discoveryPaceMs") ?: d.discoveryPaceMs,
            dnsPaddingBlockSize = m.int("dnsPaddingBlockSize") ?: d.dnsPaddingBlockSize,
            labelCountJitter = m.bool("labelCountJitter") ?: d.labelCountJitter,
            qnameLossGuard = m.bool("qnameLossGuard") ?: d.qnameLossGuard,
            doh3FallbackToDoh = m.bool("doh3FallbackToDoh") ?: d.doh3FallbackToDoh,
        ).normalized()
    }

    private fun JsonObject.str(name: String): String? = (this[name] as? JsonPrimitive)?.contentOrNull

    private fun JsonObject.int(name: String): Int? {
        val p = this[name] as? JsonPrimitive ?: return null
        return p.intOrNull ?: p.contentOrNull?.trim()?.toIntOrNull()
    }

    private fun JsonObject.bool(name: String): Boolean? = (this[name] as? JsonPrimitive)?.booleanOrNull

    private fun decodeBase64(payload: String): String {
        val clean = payload.trimEnd('=')
        val bytes = runCatching {
            Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL).decode(clean)
        }.getOrElse {
            Base64.Default.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL).decode(clean)
        }
        return bytes.decodeToString()
    }

    // ---------------------------------------------------------------- the key tables

    private class ProfileKey(
        val name: String,
        val read: (WhiteDnsProfile) -> String,
        val write: (WhiteDnsProfile, String) -> WhiteDnsProfile,
    )

    private fun strKey(
        name: String,
        get: (WhiteDnsProfile) -> String,
        set: (WhiteDnsProfile, String) -> WhiteDnsProfile,
    ) = ProfileKey(name, { quote(get(it)) }, { p, raw -> set(p, stringValue(raw, name)) })

    private fun intKey(
        name: String,
        min: Int,
        max: Int,
        get: (WhiteDnsProfile) -> Int,
        set: (WhiteDnsProfile, Int) -> WhiteDnsProfile,
    ) = ProfileKey(name, { get(it).toString() }, { p, raw -> set(p, intValue(raw, name, min, max)) })

    private fun boolKey(
        name: String,
        get: (WhiteDnsProfile) -> Boolean,
        set: (WhiteDnsProfile, Boolean) -> WhiteDnsProfile,
    ) = ProfileKey(name, { get(it).toString() }, { p, raw -> set(p, boolValue(raw, name)) })

    private fun listKey(
        name: String,
        get: (WhiteDnsProfile) -> String,
        set: (WhiteDnsProfile, String) -> WhiteDnsProfile,
    ) = ProfileKey(
        name,
        { tomlArray(WhiteDnsMaxOptions.entries(get(it))) },
        { p, raw -> set(p, stringArray(raw, name, 0).joinToString(", ")) },
    )

    private val PROFILE_KEYS: List<ProfileKey> = listOf(
        strKey("ENGINE", { it.engine }) { p, v -> p.copy(engine = WhiteDnsProfile.normalizeEngine(v)) },
        ProfileKey(
            "DOMAINS",
            { tomlArray(it.domainList()) },
            { p, raw -> p.copy(domains = stringArray(raw, "DOMAINS", 0).joinToString(", ")) },
        ),
        intKey("DATA_ENCRYPTION_METHOD", 0, 5, { it.encryptionMethod }) { p, v -> p.copy(encryptionMethod = v) },
        strKey("ENCRYPTION_KEY", { it.encryptionKey }) { p, v -> p.copy(encryptionKey = v) },
        strKey("CONFIG_PRESET", { it.configPreset }) { p, v -> p.copy(configPreset = v) },
        strKey("TUNNEL_MODE", { it.tunnelMode }) { p, v -> p.copy(tunnelMode = v) },
        strKey("RESOLVER_TRANSPORT", { it.transportMode }) { p, v -> p.copy(transportMode = v) },
        strKey("QUERY_DELIVERY", { it.deliveryMode }) { p, v -> p.copy(deliveryMode = v) },
        strKey("QNAME_MODE", { it.qnameMode }) { p, v -> p.copy(qnameMode = v) },
        strKey("RESOLVER_IP_MODE", { it.ipMode }) { p, v -> p.copy(ipMode = v) },
        strKey("QUERY_RATE_LIMIT", { it.queryRateLimit }) { p, v -> p.copy(queryRateLimit = v) },
        strKey("QUERY_RATE_SCOPE", { it.queryRateScope }) { p, v -> p.copy(queryRateScope = v) },
        strKey("QUERY_TIMING_MASK", { it.timingMask }) { p, v -> p.copy(timingMask = v) },
        intKey("MTU_BACKGROUND_PARALLELISM", 1, 100, { it.backgroundScanParallelism }) { p, v ->
            p.copy(backgroundScanParallelism = v)
        },
        strKey("RESOLVER_TLS_SERVER_NAME", { it.resolverTlsServerName }) { p, v -> p.copy(resolverTlsServerName = v) },
        strKey("RESOLVER_TLS_PIN", { it.resolverTlsPin }) { p, v -> p.copy(resolverTlsPin = v) },
        strKey("RESOLVER_DOT_PORT", { it.resolverDotPort }) { p, v -> p.copy(resolverDotPort = v) },
        strKey("RESOLVER_DOH_PORT", { it.resolverDohPort }) { p, v -> p.copy(resolverDohPort = v) },
        strKey("RESOLVER_DOH_PATH", { it.resolverDohPath }) { p, v -> p.copy(resolverDohPath = v) },

        strKey("COTTENMAX_TRAFFIC_PROFILE", { it.max.trafficProfile }) { p, v -> p.copy(max = p.max.copy(trafficProfile = v)) },
        intKey("COTTENMAX_LOW_REDUCTION_PERCENT", 10, 90, { it.max.lowReductionPercent }) { p, v ->
            p.copy(max = p.max.copy(lowReductionPercent = v))
        },
        listKey("STANDBY_DOMAINS", { it.max.standbyDomains }) { p, v -> p.copy(max = p.max.copy(standbyDomains = v)) },
        intKey("COTTENMAX_FAILOVER_AFTER_MILLISECONDS", 100, 60000, { it.max.failoverAfterMs }) { p, v ->
            p.copy(max = p.max.copy(failoverAfterMs = v))
        },
        intKey("COTTENMAX_SAFE_QNAME", 64, 253, { it.max.safeQName }) { p, v -> p.copy(max = p.max.copy(safeQName = v)) },
        intKey("COTTENMAX_MAX_QNAME", 64, 253, { it.max.maxQName }) { p, v -> p.copy(max = p.max.copy(maxQName = v)) },
        intKey("COTTENMAX_MAX_ANSWER", 512, 1232, { it.max.maxAnswer }) { p, v -> p.copy(max = p.max.copy(maxAnswer = v)) },
        intKey("COTTENMAX_MAX_PATH_WINDOW", 1, 1024, { it.max.maxPathWindow }) { p, v ->
            p.copy(max = p.max.copy(maxPathWindow = v))
        },
        intKey("COTTENMAX_MAX_INFLIGHT", 1, 4096, { it.max.maxInflight }) { p, v -> p.copy(max = p.max.copy(maxInflight = v)) },
        boolKey("COTTENMAX_FEC_DOWNLOAD_ENABLED", { it.max.fecDownloadEnabled }) { p, v ->
            p.copy(max = p.max.copy(fecDownloadEnabled = v))
        },
        intKey("COTTENMAX_FEC_DATA_SHARDS", 2, 16, { it.max.fecDataShards }) { p, v -> p.copy(max = p.max.copy(fecDataShards = v)) },
        intKey("COTTENMAX_FEC_PARITY_SHARDS", 1, 4, { it.max.fecParityShards }) { p, v ->
            p.copy(max = p.max.copy(fecParityShards = v))
        },
        intKey("COTTENMAX_SESSION_IDLE_MILLISECONDS", 1000, 90000, { it.max.sessionIdleMs }) { p, v ->
            p.copy(max = p.max.copy(sessionIdleMs = v))
        },
        intKey("COTTENMAX_HOLD_MILLISECONDS", 0, 1500, { it.max.holdMs }) { p, v -> p.copy(max = p.max.copy(holdMs = v)) },
        boolKey("QNAME_LABEL_COUNT_JITTER", { it.max.labelCountJitter }) { p, v -> p.copy(max = p.max.copy(labelCountJitter = v)) },
        boolKey("COTTENMAX_QNAME_LOSS_GUARD", { it.max.qnameLossGuard }) { p, v -> p.copy(max = p.max.copy(qnameLossGuard = v)) },
        intKey("DNS_PADDING_BLOCK_SIZE", 0, 4096, { it.max.dnsPaddingBlockSize }) { p, v ->
            p.copy(max = p.max.copy(dnsPaddingBlockSize = v))
        },
        boolKey("RESOLVER_HTTP3_FALLBACK", { it.max.doh3FallbackToDoh }) { p, v -> p.copy(max = p.max.copy(doh3FallbackToDoh = v)) },
        boolKey("RESOLVER_DISCOVERY_ENABLED", { it.max.discoveryEnabled }) { p, v -> p.copy(max = p.max.copy(discoveryEnabled = v)) },
        strKey("RESOLVER_DISCOVERY_COUNTRY", { it.max.discoveryCountry }) { p, v -> p.copy(max = p.max.copy(discoveryCountry = v)) },
        intKey("RESOLVER_DISCOVERY_SAMPLE_RANGES", 0, 4096, { it.max.discoverySampleRanges }) { p, v ->
            p.copy(max = p.max.copy(discoverySampleRanges = v))
        },
        boolKey("RESOLVER_DISCOVERY_CACHE", { it.max.discoveryCacheEnabled }) { p, v ->
            p.copy(max = p.max.copy(discoveryCacheEnabled = v))
        },
        strKey("RESOLVER_DISCOVERY_NETWORK_ID", { it.max.discoveryNetworkId }) { p, v ->
            p.copy(max = p.max.copy(discoveryNetworkId = v))
        },
        listKey("RESOLVER_DISCOVERY_CANDIDATES", { it.max.discoveryCandidates }) { p, v ->
            p.copy(max = p.max.copy(discoveryCandidates = v))
        },
        intKey("RESOLVER_DISCOVERY_WANT", 1, 4096, { it.max.discoveryWant }) { p, v -> p.copy(max = p.max.copy(discoveryWant = v)) },
        intKey("RESOLVER_DISCOVERY_CONCURRENCY", 1, 64, { it.max.discoveryConcurrency }) { p, v ->
            p.copy(max = p.max.copy(discoveryConcurrency = v))
        },
        intKey("RESOLVER_DISCOVERY_PACE_MILLISECONDS", 1, 60000, { it.max.discoveryPaceMs }) { p, v ->
            p.copy(max = p.max.copy(discoveryPaceMs = v))
        },
    )

    private class SettingsKey(
        val name: String,
        val read: (WhiteDnsSettings) -> String,
        val write: (WhiteDnsSettings, String) -> WhiteDnsSettings,
    )

    private fun sInt(
        name: String,
        min: Int,
        max: Int,
        get: (WhiteDnsSettings) -> String,
        set: (WhiteDnsSettings, String) -> WhiteDnsSettings,
    ) = SettingsKey(name, { get(it).trim() }, { s, raw -> set(s, intValue(raw, name, min, max).toString()) })

    private fun sDouble(
        name: String,
        min: Double,
        max: Double,
        get: (WhiteDnsSettings) -> String,
        set: (WhiteDnsSettings, String) -> WhiteDnsSettings,
    ) = SettingsKey(name, { get(it).trim() }, { s, raw -> set(s, num(doubleValue(raw, name, min, max))) })

    private val SETTINGS_KEYS: List<SettingsKey> = listOf(
        SettingsKey(
            "RESOLVER_BALANCING_STRATEGY",
            { it.balancingStrategy.toString() },
            { s, raw -> s.copy(balancingStrategy = intValue(raw, "RESOLVER_BALANCING_STRATEGY", 1, 4)) },
        ),
        sInt("UPLOAD_PACKET_DUPLICATION_COUNT", 1, 30, { it.uploadDuplication }) { s, v -> s.copy(uploadDuplication = v) },
        sInt("DOWNLOAD_PACKET_DUPLICATION_COUNT", 1, 30, { it.downloadDuplication }) { s, v -> s.copy(downloadDuplication = v) },
        SettingsKey(
            "UPLOAD_COMPRESSION_TYPE",
            { it.uploadCompression.toString() },
            { s, raw -> s.copy(uploadCompression = intValue(raw, "UPLOAD_COMPRESSION_TYPE", 0, 3)) },
        ),
        SettingsKey(
            "DOWNLOAD_COMPRESSION_TYPE",
            { it.downloadCompression.toString() },
            { s, raw -> s.copy(downloadCompression = intValue(raw, "DOWNLOAD_COMPRESSION_TYPE", 0, 3)) },
        ),
        SettingsKey(
            "BASE_ENCODE_DATA",
            { it.baseEncodeData.toString() },
            { s, raw -> s.copy(baseEncodeData = boolValue(raw, "BASE_ENCODE_DATA")) },
        ),
        sInt("MIN_UPLOAD_MTU", 1, 65535, { it.minUploadMtu }) { s, v -> s.copy(minUploadMtu = v) },
        sInt("MIN_DOWNLOAD_MTU", 1, 65535, { it.minDownloadMtu }) { s, v -> s.copy(minDownloadMtu = v) },
        sInt("MAX_UPLOAD_MTU", 1, 65535, { it.maxUploadMtu }) { s, v -> s.copy(maxUploadMtu = v) },
        sInt("MAX_DOWNLOAD_MTU", 1, 65535, { it.maxDownloadMtu }) { s, v -> s.copy(maxDownloadMtu = v) },
        sInt("MTU_TEST_RETRIES_RESOLVERS", 1, 100, { it.mtuTestRetriesResolvers }) { s, v -> s.copy(mtuTestRetriesResolvers = v) },
        sDouble("MTU_TEST_TIMEOUT_RESOLVERS", 0.001, 3600.0, { it.mtuTestTimeoutResolvers }) { s, v ->
            s.copy(mtuTestTimeoutResolvers = v)
        },
        sInt("MTU_TEST_PARALLELISM_RESOLVERS", 1, 1024, { it.mtuTestParallelismResolvers }) { s, v ->
            s.copy(mtuTestParallelismResolvers = v)
        },
        sInt("MTU_TEST_RETRIES_LOGS", 1, 100, { it.mtuTestRetriesLogs }) { s, v -> s.copy(mtuTestRetriesLogs = v) },
        sDouble("MTU_TEST_TIMEOUT_LOGS", 0.001, 3600.0, { it.mtuTestTimeoutLogs }) { s, v -> s.copy(mtuTestTimeoutLogs = v) },
        sInt("MTU_TEST_PARALLELISM_LOGS", 1, 1024, { it.mtuTestParallelismLogs }) { s, v -> s.copy(mtuTestParallelismLogs = v) },
        sInt("RX_TX_WORKERS", 1, 64, { it.rxTxWorkers }) { s, v -> s.copy(rxTxWorkers = v) },
        sInt("TUNNEL_PROCESS_WORKERS", 1, 64, { it.tunnelProcessWorkers }) { s, v -> s.copy(tunnelProcessWorkers = v) },
        sDouble("TUNNEL_PACKET_TIMEOUT_SECONDS", 0.5, 120.0, { it.tunnelPacketTimeoutSeconds }) { s, v ->
            s.copy(tunnelPacketTimeoutSeconds = v)
        },
        sDouble("DISPATCHER_IDLE_POLL_INTERVAL_SECONDS", 0.001, 1.0, { it.dispatcherIdlePollIntervalSeconds }) { s, v ->
            s.copy(dispatcherIdlePollIntervalSeconds = v)
        },
        sInt("TX_CHANNEL_SIZE", 64, 65536, { it.txChannelSize }) { s, v -> s.copy(txChannelSize = v) },
        sInt("RX_CHANNEL_SIZE", 64, 65536, { it.rxChannelSize }) { s, v -> s.copy(rxChannelSize = v) },
        sInt("RESOLVER_UDP_CONNECTION_POOL_SIZE", 1, 1024, { it.resolverUdpConnectionPoolSize }) { s, v ->
            s.copy(resolverUdpConnectionPoolSize = v)
        },
        sInt("STREAM_QUEUE_INITIAL_CAPACITY", 8, 65536, { it.streamQueueInitialCapacity }) { s, v ->
            s.copy(streamQueueInitialCapacity = v)
        },
        sInt("ORPHAN_QUEUE_INITIAL_CAPACITY", 4, 4096, { it.orphanQueueInitialCapacity }) { s, v ->
            s.copy(orphanQueueInitialCapacity = v)
        },
        sInt("DNS_RESPONSE_FRAGMENT_STORE_CAPACITY", 16, 16384, { it.dnsResponseFragmentStoreCapacity }) { s, v ->
            s.copy(dnsResponseFragmentStoreCapacity = v)
        },
        sInt("MAX_ACTIVE_STREAMS", 1, 65535, { it.maxActiveStreams }) { s, v -> s.copy(maxActiveStreams = v) },
        sDouble("LOCAL_HANDSHAKE_TIMEOUT_SECONDS", 0.5, 60.0, { it.localHandshakeTimeoutSeconds }) { s, v ->
            s.copy(localHandshakeTimeoutSeconds = v)
        },
        sDouble("SOCKS_UDP_ASSOCIATE_READ_TIMEOUT_SECONDS", 1.0, 3600.0, { it.socksUdpAssociateReadTimeoutSeconds }) { s, v ->
            s.copy(socksUdpAssociateReadTimeoutSeconds = v)
        },
        sDouble("CLIENT_TERMINAL_STREAM_RETENTION_SECONDS", 1.0, 3600.0, { it.clientTerminalStreamRetentionSeconds }) { s, v ->
            s.copy(clientTerminalStreamRetentionSeconds = v)
        },
        sDouble("CLIENT_CANCELLED_SETUP_RETENTION_SECONDS", 1.0, 3600.0, { it.clientCancelledSetupRetentionSeconds }) { s, v ->
            s.copy(clientCancelledSetupRetentionSeconds = v)
        },
        sDouble("SESSION_INIT_RETRY_BASE_SECONDS", 0.1, 60.0, { it.sessionInitRetryBaseSeconds }) { s, v ->
            s.copy(sessionInitRetryBaseSeconds = v)
        },
        sDouble("SESSION_INIT_RETRY_STEP_SECONDS", 0.0, 60.0, { it.sessionInitRetryStepSeconds }) { s, v ->
            s.copy(sessionInitRetryStepSeconds = v)
        },
        sInt("SESSION_INIT_RETRY_LINEAR_AFTER", 0, 1000, { it.sessionInitRetryLinearAfter }) { s, v ->
            s.copy(sessionInitRetryLinearAfter = v)
        },
        sDouble("SESSION_INIT_RETRY_MAX_SECONDS", 0.1, 3600.0, { it.sessionInitRetryMaxSeconds }) { s, v ->
            s.copy(sessionInitRetryMaxSeconds = v)
        },
        sDouble("SESSION_INIT_BUSY_RETRY_INTERVAL_SECONDS", 1.0, 3600.0, { it.sessionInitBusyRetryIntervalSeconds }) { s, v ->
            s.copy(sessionInitBusyRetryIntervalSeconds = v)
        },
        SettingsKey(
            "STARTUP_MODE",
            { quote(it.startupMode) },
            { s, raw ->
                val v = stringValue(raw, "STARTUP_MODE").lowercase()
                if (v != "resolvers" && v != "logs") throw WhiteDnsConfigException("STARTUP_MODE must be \"resolvers\" or \"logs\"")
                s.copy(startupMode = v)
            },
        ),
        sInt("PING_WATCHDOG_TIMEOUT_SECONDS", 0, 3600, { it.pingWatchdogSeconds }) { s, v -> s.copy(pingWatchdogSeconds = v) },
        SettingsKey(
            "LOG_LEVEL",
            { quote(it.logLevel) },
            { s, raw ->
                val v = stringValue(raw, "LOG_LEVEL").uppercase()
                if (v !in WhiteDnsSettings.LOG_LEVELS) throw WhiteDnsConfigException("LOG_LEVEL must be DEBUG, INFO, WARN or ERROR")
                s.copy(logLevel = v)
            },
        ),
    )

    // ---------------------------------------------------------------- TOML text

    fun parseAssignments(text: String): List<TomlAssignment> {
        val out = ArrayList<TomlAssignment>()
        val lines = text.replace("\r\n", "\n").replace('\r', '\n').split('\n')
        var i = 0
        while (i < lines.size) {
            val lineNo = i + 1
            val line = stripComment(lines[i]).trim()
            i++
            if (line.isEmpty()) continue
            if (line.startsWith("[")) throw WhiteDnsConfigException("line $lineNo: tables are not supported")
            val eq = line.indexOf('=')
            if (eq <= 0) throw WhiteDnsConfigException("line $lineNo: expected KEY = value")
            val key = line.substring(0, eq).trim()
            if (!KEY.matches(key)) throw WhiteDnsConfigException("line $lineNo: invalid key \"$key\"")
            var value = line.substring(eq + 1).trim()
            if (value.startsWith("[")) {
                while (bracketDepth(value) > 0 && i < lines.size) {
                    value += " " + stripComment(lines[i]).trim()
                    i++
                }
                if (bracketDepth(value) > 0) throw WhiteDnsConfigException("line $lineNo: unterminated array")
            }
            if (value.isEmpty()) throw WhiteDnsConfigException("line $lineNo: missing value for $key")
            out += TomlAssignment(key, value, lineNo)
        }
        return out
    }

    private val KEY = Regex("[A-Za-z0-9_.-]+")

    private fun stripComment(line: String): String {
        var quoteChar = 0.toChar()
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                quoteChar != 0.toChar() -> {
                    if (quoteChar == '"' && c == '\\') i++
                    else if (c == quoteChar) quoteChar = 0.toChar()
                }
                c == '"' || c == '\'' -> quoteChar = c
                c == '#' -> return line.substring(0, i)
            }
            i++
        }
        return line
    }

    private fun bracketDepth(value: String): Int {
        var depth = 0
        var quoteChar = 0.toChar()
        var i = 0
        while (i < value.length) {
            val c = value[i]
            when {
                quoteChar != 0.toChar() -> {
                    if (quoteChar == '"' && c == '\\') i++
                    else if (c == quoteChar) quoteChar = 0.toChar()
                }
                c == '"' || c == '\'' -> quoteChar = c
                c == '[' -> depth++
                c == ']' -> depth--
            }
            i++
        }
        return depth
    }

    private fun stringValue(raw: String, key: String): String {
        val t = raw.trim()
        if (t.length >= 2 && t.first() == '"' && t.last() == '"') return unescape(t.substring(1, t.length - 1), key)
        if (t.length >= 2 && t.first() == '\'' && t.last() == '\'') return t.substring(1, t.length - 1)
        throw WhiteDnsConfigException("$key must be a quoted string")
    }

    private fun intValue(raw: String, key: String, min: Int, max: Int): Int {
        val v = raw.trim().replace("_", "").toIntOrNull()
            ?: throw WhiteDnsConfigException("$key must be a whole number")
        if (v < min || v > max) throw WhiteDnsConfigException("$key must be between $min and $max")
        return v
    }

    private fun doubleValue(raw: String, key: String, min: Double, max: Double): Double {
        val v = raw.trim().replace("_", "").toDoubleOrNull()
            ?: throw WhiteDnsConfigException("$key must be a number")
        if (v.isNaN() || v < min || v > max) throw WhiteDnsConfigException("$key must be between ${num(min)} and ${num(max)}")
        return v
    }

    private fun boolValue(raw: String, key: String): Boolean = when (raw.trim().lowercase()) {
        "true" -> true
        "false" -> false
        else -> throw WhiteDnsConfigException("$key must be true or false")
    }

    private fun stringArray(raw: String, key: String, line: Int): List<String> {
        val t = raw.trim()
        if (!t.startsWith("[") || !t.endsWith("]")) {
            throw WhiteDnsConfigException("${if (line > 0) "line $line: " else ""}$key must be an array of strings")
        }
        val inner = t.substring(1, t.length - 1)
        val items = ArrayList<String>()
        val current = StringBuilder()
        var quoteChar = 0.toChar()
        var i = 0
        fun flush() {
            val item = current.toString().trim()
            current.setLength(0)
            if (item.isNotEmpty()) items += stringValue(item, key)
        }
        while (i < inner.length) {
            val c = inner[i]
            if (quoteChar != 0.toChar()) {
                current.append(c)
                if (quoteChar == '"' && c == '\\' && i + 1 < inner.length) {
                    i++
                    current.append(inner[i])
                } else if (c == quoteChar) {
                    quoteChar = 0.toChar()
                }
            } else if (c == '"' || c == '\'') {
                quoteChar = c
                current.append(c)
            } else if (c == ',') {
                flush()
            } else {
                current.append(c)
            }
            i++
        }
        flush()
        return items
    }

    private fun unescape(s: String, key: String): String {
        if ('\\' !in s) return s
        val sb = StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c != '\\') {
                sb.append(c)
                i++
                continue
            }
            if (i + 1 >= s.length) throw WhiteDnsConfigException("$key has a dangling backslash")
            when (val n = s[i + 1]) {
                '\\' -> sb.append('\\')
                '"' -> sb.append('"')
                'n' -> sb.append('\n')
                'r' -> sb.append('\r')
                't' -> sb.append('\t')
                'u' -> {
                    val hex = if (i + 6 <= s.length) s.substring(i + 2, i + 6) else ""
                    val code = hex.toIntOrNull(16) ?: throw WhiteDnsConfigException("$key has a bad \\u escape")
                    sb.append(code.toChar())
                    i += 4
                }
                else -> throw WhiteDnsConfigException("$key has an unknown escape \\$n")
            }
            i += 2
        }
        return sb.toString()
    }

    private fun quote(raw: String): String = buildString {
        append('"')
        for (c in raw) when (c) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (c.code < 0x20) append("\\u" + c.code.toString(16).padStart(4, '0').uppercase()) else append(c)
        }
        append('"')
    }

    private fun tomlArray(items: List<String>): String = items.joinToString(", ", "[", "]") { quote(it) }

    /** A number the way the engine reads it: no exponent, a decimal point for fractions. */
    internal fun num(d: Double): String {
        val whole = d.toLong()
        return if (whole.toDouble() == d) "$whole.0" else d.toString()
    }
}
