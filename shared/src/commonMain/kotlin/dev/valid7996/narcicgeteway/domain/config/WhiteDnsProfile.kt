package dev.valid7996.narcicgeteway.domain.config

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

@Serializable
data class WhiteDnsProfile(
    val domain: String = "",
    val encryptionKey: String = "",
    val encryptionMethod: Int = 1, // 0: None, 1: XOR, 2: ChaCha20, 3: AES-GCM, 4: RC4
    val engine: String = ENGINE_COTTENDNS, // cottendns, stormdns, soar, masterdns
    val tunnelMode: String = "normal", // legacy, normal, max
    val transportMode: String = "auto", // auto, udp, tcp
    val deliveryMode: String = "auto",
    val qnameMode: String = "auto",
    val configPreset: String = "default",
    val resolvers: String = "",
    val useBundledResolvers: Boolean = true,
    val listenPort: Int = 18008,
    val balancingStrategy: Int = 3,
    val packetDuplication: Int = 3,
    val compression: Int = 0,
    val autoDisableDeadResolvers: Boolean = true,
    val logLevel: String = "INFO",
    val link: String? = null,
) {
    fun normalizedEngine(): String {
        val v = engine.trim().lowercase()
        return when {
            v.contains("soar") -> ENGINE_SOAR
            v.contains("storm") -> ENGINE_STORMDNS
            v.contains("master") -> ENGINE_MASTERDNS
            else -> ENGINE_COTTENDNS
        }
    }

    val isSoar: Boolean get() = normalizedEngine() == ENGINE_SOAR
    val isStormDns: Boolean get() = normalizedEngine() == ENGINE_STORMDNS
    val isCottenDns: Boolean get() = normalizedEngine() == ENGINE_COTTENDNS

    val isValid: Boolean
        get() = domain.isNotBlank() && encryptionKey.isNotBlank()

    fun domainsList(): List<String> =
        domain.split(',', '\n', ' ')
            .map { it.trim().trimEnd('.') }
            .filter { it.isNotBlank() }
            .distinct()

    fun resolversList(): List<String> =
        resolvers.split(',', '\n', ' ')
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .distinct()

    fun toToml(listenIp: String = "127.0.0.1", port: Int = listenPort): String = buildString {
        val domains = domainsList().joinToString(", ", "[", "]") { "\"${escape(it)}\"" }
        appendLine("DOMAINS = $domains")
        appendLine("DATA_ENCRYPTION_METHOD = $encryptionMethod")
        appendLine("ENCRYPTION_KEY = \"${escape(encryptionKey)}\"")
        appendLine("PROTOCOL_TYPE = \"SOCKS5\"")

        // CottenDNS specific settings (StormDNS rejects these keys)
        if (isCottenDns) {
            val isCompatibility = (configPreset == "master-storm")
            val preset = when (configPreset) {
                "speed", "survival", "tcp-survival" -> configPreset
                else -> "default"
            }
            appendLine("CONFIG_PRESET = \"${if (isCompatibility) "default" else preset}\"")
            appendLine("LEGACY_SESSION_ID = $isCompatibility")
            if (!isCompatibility) {
                val normTunnelMode = if (tunnelMode.equals("max", ignoreCase = true) || tunnelMode.equals("cottenmax", ignoreCase = true)) "cottenmax" else "legacy"
                appendLine("TUNNEL_MODE = \"$normTunnelMode\"")
            }
            appendLine("TERMINAL_UI = \"plain\"")
            appendLine("RESOLVER_IP_MODE = \"auto\"")
            appendLine("QUERY_RATE_LIMIT_PER_SECOND = 0")
            appendLine("QUERY_RATE_LIMIT_PER_RESOLVER_PER_SECOND = 0")
            appendLine("QUERY_RATE_LIMIT_PER_DOMAIN_PER_SECOND = 0")
            appendLine("QUERY_TIMING_JITTER = 0.0")

            val transport = when {
                isCompatibility -> "udp"
                transportMode in setOf("udp", "tcp", "dot", "doh", "doh3", "doq") -> transportMode
                preset == "tcp-survival" -> "tcp"
                else -> "auto"
            }
            appendLine("RESOLVER_TRANSPORT = \"$transport\"")

            val queryTypes = when {
                isCompatibility -> "[\"TXT\"]"
                deliveryMode == "txt" -> "[\"TXT\"]"
                deliveryMode == "txt-cname" -> "[\"TXT\", \"CNAME\"]"
                deliveryMode == "txt-https" -> "[\"TXT\", \"HTTPS\"]"
                deliveryMode == "txt-aaaa" -> "[\"TXT\", \"AAAA\"]"
                deliveryMode == "all" -> "[\"TXT\", \"CNAME\", \"NULL\", \"HTTPS\", \"AAAA\"]"
                preset == "speed" -> "[\"TXT\", \"HTTPS\"]"
                preset == "survival" -> "[\"TXT\", \"CNAME\", \"HTTPS\", \"A\"]"
                preset == "tcp-survival" -> "[\"TXT\", \"HTTPS\"]"
                else -> "[\"TXT\"]"
            }
            appendLine("QUERY_TYPES = $queryTypes")

            val qnameLen = when {
                isCompatibility -> 63
                qnameMode == "off" -> 63
                qnameMode == "moderate" -> 42
                qnameMode == "aggressive" -> 32
                preset == "survival" -> 42
                else -> 63
            }
            appendLine("QNAME_LABEL_LENGTH = $qnameLen")
            appendLine("FAST_CONNECT = true")
            appendLine("MTU_BACKGROUND_PARALLELISM = 8")
            appendLine("RESOLVER_RATE_LIMIT_ENABLED = true")
            appendLine("DNS_RANDOMIZE_QUERY_ID = true")
            appendLine("DNS_QNAME_CASE_RANDOMIZATION = false")
            appendLine("RESOLVER_IGNORE_INJECTED_NXDOMAIN = true")

            if (!isCompatibility) {
                appendLine("ADAPTIVE_DUPLICATION = true")
                appendLine("DUPLICATION_PREFER_DISTINCT_DOMAINS = true")
                appendLine("ADAPTIVE_DUPLICATION_TARGET_DELIVERY = ${if (preset == "survival") "0.97" else "0.95"}")
                appendLine("DNS_EDNS_COOKIE = true")
                appendLine("EDNS_UDP_SIZE = ${if (preset == "survival") 1232 else 4096}")
                appendLine("MTU_PROBE_SAMPLES = ${if (preset == "survival") 5 else 1}")
                appendLine("MTU_MAX_LOSS = ${if (preset == "survival") "0.5" else "0.0"}")
                appendLine("MTU_ADAPTIVE_GROUPING = true")
                appendLine("MTU_GROUP_GAP_RATIO = 0.25")
            } else {
                appendLine("ADAPTIVE_DUPLICATION = false")
                appendLine("DUPLICATION_PREFER_DISTINCT_DOMAINS = false")
                appendLine("DNS_EDNS_COOKIE = false")
                appendLine("MTU_ADAPTIVE_GROUPING = false")
                appendLine("MTU_PROBE_SAMPLES = 1")
                appendLine("MTU_MAX_LOSS = 0.0")
            }
        }

        // Common client settings (StormDNS & CottenDNS)
        appendLine("LISTEN_IP = \"${escape(listenIp)}\"")
        appendLine("LISTEN_PORT = $port")
        appendLine("SOCKS5_AUTH = false")
        appendLine("SOCKS5_USER = \"\"")
        appendLine("SOCKS5_PASS = \"\"")
        appendLine("LOCAL_DNS_ENABLED = false")
        appendLine("LOCAL_DNS_IP = \"127.0.0.1\"")
        appendLine("LOCAL_DNS_PORT = 0")
        appendLine("RESOLVER_BALANCING_STRATEGY = $balancingStrategy")
        appendLine("UPLOAD_PACKET_DUPLICATION_COUNT = ${packetDuplication.coerceIn(1, 10)}")
        appendLine("DOWNLOAD_PACKET_DUPLICATION_COUNT = ${packetDuplication.coerceIn(1, 10)}")
        appendLine("UPLOAD_COMPRESSION_TYPE = $compression")
        appendLine("DOWNLOAD_COMPRESSION_TYPE = $compression")
        appendLine("BASE_ENCODE_DATA = false")
        appendLine("MIN_UPLOAD_MTU = 40")
        appendLine("MIN_DOWNLOAD_MTU = 300")
        appendLine("MAX_UPLOAD_MTU = 140")
        appendLine("MAX_DOWNLOAD_MTU = 3000")
        appendLine("MTU_TEST_RETRIES_RESOLVERS = 3")
        appendLine("MTU_TEST_TIMEOUT_RESOLVERS = 2.5")
        appendLine("MTU_TEST_PARALLELISM_RESOLVERS = 64")
        appendLine("MTU_TEST_RETRIES_LOGS = 5")
        appendLine("MTU_TEST_TIMEOUT_LOGS = 2.5")
        appendLine("MTU_TEST_PARALLELISM_LOGS = 32")
        appendLine("RX_TX_WORKERS = 4")
        appendLine("TUNNEL_PROCESS_WORKERS = 4")
        appendLine("TUNNEL_PACKET_TIMEOUT_SECONDS = 30")
        appendLine("DISPATCHER_IDLE_POLL_INTERVAL_SECONDS = 1")
        appendLine("TX_CHANNEL_SIZE = 1024")
        appendLine("RX_CHANNEL_SIZE = 1024")
        appendLine("RESOLVER_UDP_CONNECTION_POOL_SIZE = 256")
        appendLine("STREAM_QUEUE_INITIAL_CAPACITY = 256")
        appendLine("ORPHAN_QUEUE_INITIAL_CAPACITY = 256")
        appendLine("DNS_RESPONSE_FRAGMENT_STORE_CAPACITY = 256")
        appendLine("MAX_ACTIVE_STREAMS = 1024")
        appendLine("LOCAL_HANDSHAKE_TIMEOUT_SECONDS = 10")
        appendLine("SOCKS_UDP_ASSOCIATE_READ_TIMEOUT_SECONDS = 30")
        appendLine("CLIENT_TERMINAL_STREAM_RETENTION_SECONDS = 30")
        appendLine("CLIENT_CANCELLED_SETUP_RETENTION_SECONDS = 30")
        appendLine("SESSION_INIT_RETRY_BASE_SECONDS = 1")
        appendLine("SESSION_INIT_RETRY_STEP_SECONDS = 1")
        appendLine("SESSION_INIT_RETRY_LINEAR_AFTER = 5")
        appendLine("SESSION_INIT_RETRY_MAX_SECONDS = 15")
        appendLine("SESSION_INIT_BUSY_RETRY_INTERVAL_SECONDS = 1")
        appendLine("STARTUP_MODE = \"resolvers\"")
        appendLine("LOG_SCAN_MAX_DAYS = 14")
        appendLine("LOG_SCAN_MAX_RESOLVERS = 128")
        appendLine("LOG_BASED_MTU_VERIFY = true")
        appendLine("STATS_REPORT_INTERVAL_SECONDS = 1.0")
        appendLine("PING_WATCHDOG_TIMEOUT_SECONDS = 0")
        appendLine("LOG_LEVEL = \"${if (logLevel.equals("DEBUG", ignoreCase = true)) "DEBUG" else "INFO"}\"")
        appendLine("LOG_TO_FILE = false")
        appendLine("LOG_DIR = \"logs\"")
    }

    fun toSoarJson(
        listenIp: String = "127.0.0.1",
        port: Int = listenPort,
        systemResolvers: List<String> = emptyList(),
    ): String {
        val zone = domain.split(',', '\n', ' ')
            .map { it.trim().trimEnd('.').lowercase() }
            .firstOrNull { it.isNotBlank() } ?: domain.trim().trimEnd('.').lowercase()
        val pubKey = encryptionKey.trim()

        val sysResolvers = if (systemResolvers.isNotEmpty()) {
            systemResolvers.distinct().take(8)
        } else {
            listOf("8.8.8.8", "1.1.1.1", "9.9.9.9")
        }

        val root = buildJsonObject {
            put("zone", zone)
            put("server_public_key", pubKey)
            put("system_resolvers", buildJsonArray {
                sysResolvers.forEach { add(JsonPrimitive(it)) }
            })
            put("listen", "$listenIp:$port")
            put("username", "")
            put("password", "")
            put("soar", buildJsonObject {
                put("transport", if (transportMode in listOf("auto", "on", "only", "off")) transportMode else "auto")
                put("discovery", true)
                put("country", "IR")
                put("discoveryWant", 2)
                put("discoveryConcurrency", 2)
                put("discoveryPaceMs", 1000)
                put("safeQName", 101)
                put("maxQName", 253)
                put("maxAnswer", 1232)
                put("holdMs", 500)
                put("maxInflight", 128)
                put("maxPathWindow", 32)
                put("maxStreams", 128)
                put("streamWindowKiB", 256)
                put("sendBufferKiB", 64)
                put("sessionIdleSeconds", 90)
                put("dialTimeoutSeconds", 90)
            })
        }
        return root.toString()
    }

    fun toConfigFile(
        listenIp: String = "127.0.0.1",
        port: Int = listenPort,
        systemResolvers: List<String> = emptyList(),
    ): String {
        return if (isSoar) {
            toSoarJson(listenIp, port, systemResolvers)
        } else {
            toToml(listenIp, port)
        }
    }

    private fun escape(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"")

    companion object {
        const val ENGINE_COTTENDNS = "cottendns"
        const val ENGINE_STORMDNS = "stormdns"
        const val ENGINE_SOAR = "soar"
        const val ENGINE_MASTERDNS = "masterdns"

        const val ENC_NONE = 0
        const val ENC_XOR = 1
        const val ENC_CHACHA20 = 2
        const val ENC_AES_GCM = 3
        const val ENC_RC4 = 4
    }
}

@OptIn(ExperimentalEncodingApi::class)
object WhiteDnsLink {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun isWhiteDnsLink(link: String): Boolean {
        val trimmed = link.trim()
        val scheme = trimmed.substringBefore("://", "").lowercase()
        return scheme in setOf("stormdns", "cottendns", "soar", "masterdns", "whitedns")
    }

    fun parse(rawLink: String): Pair<String, WhiteDnsProfile>? = runCatching {
        val trimmed = rawLink.trim()
        val schemeEnd = trimmed.indexOf("://")
        if (schemeEnd <= 0) return null
        val scheme = trimmed.take(schemeEnd).lowercase()
        val payload = trimmed.substring(schemeEnd + 3).trim().substringBefore('#').substringBefore('?')
        val remarkFromUrl = trimmed.substringAfter('#', "").takeIf { it.isNotBlank() }

        val defaultEngine = when (scheme) {
            "cottendns" -> WhiteDnsProfile.ENGINE_COTTENDNS
            "soar" -> WhiteDnsProfile.ENGINE_SOAR
            "stormdns" -> WhiteDnsProfile.ENGINE_STORMDNS
            else -> WhiteDnsProfile.ENGINE_COTTENDNS
        }

        // Try decoding as base64 JSON first (official WhiteDNS export)
        val decodedText = decodeBase64(payload)
        if (decodedText != null && decodedText.trimStart().startsWith("{")) {
            val root = json.parseToJsonElement(decodedText).jsonObject
            val profileObj = root["profile"]?.jsonObject ?: root
            val name = profileObj["name"]?.jsonPrimitive?.content ?: remarkFromUrl ?: "WhiteDNS"

            val serverObj = profileObj["server"]?.jsonObject
            val domain = serverObj?.get("domain")?.jsonPrimitive?.content
                ?: profileObj["domain"]?.jsonPrimitive?.content.orEmpty()
            val encKey = serverObj?.get("public_key")?.jsonPrimitive?.content
                ?: serverObj?.get("encryption_key")?.jsonPrimitive?.content
                ?: profileObj["encryption_key"]?.jsonPrimitive?.content.orEmpty()
            val encMethod = serverObj?.get("encryption_method")?.jsonPrimitive?.intOrNull
                ?: profileObj["encryption_method"]?.jsonPrimitive?.intOrNull ?: 1

            val isSoarDetected = scheme == "soar" ||
                profileObj.containsKey("soarSettings") ||
                serverObj?.containsKey("public_key") == true ||
                domain.contains(".soar.", ignoreCase = true)

            val detectedEngine = when {
                isSoarDetected -> WhiteDnsProfile.ENGINE_SOAR
                scheme == "cottendns" || profileObj.containsKey("cottenSettings") -> WhiteDnsProfile.ENGINE_COTTENDNS
                scheme == "stormdns" || scheme == "masterdns" -> WhiteDnsProfile.ENGINE_STORMDNS
                profileObj.containsKey("engine") -> {
                    val engStr = profileObj["engine"]?.jsonPrimitive?.content.orEmpty().lowercase()
                    when {
                        engStr.contains("soar") -> WhiteDnsProfile.ENGINE_SOAR
                        engStr.contains("storm") -> WhiteDnsProfile.ENGINE_STORMDNS
                        else -> WhiteDnsProfile.ENGINE_COTTENDNS
                    }
                }
                else -> defaultEngine
            }

            val cottenObj = profileObj["cottenSettings"]?.jsonObject
            val tunnelMode = cottenObj?.get("tunnelMode")?.jsonPrimitive?.content ?: "legacy"
            val transportMode = cottenObj?.get("transportMode")?.jsonPrimitive?.content ?: "auto"

            val resolversArray = profileObj["resolvers"]?.jsonArray
            val resolversStr = resolversArray?.mapNotNull { it.jsonPrimitive.content }?.joinToString("\n").orEmpty()

            val profile = WhiteDnsProfile(
                domain = domain,
                encryptionKey = encKey,
                encryptionMethod = if (detectedEngine == WhiteDnsProfile.ENGINE_SOAR) 0 else encMethod,
                engine = detectedEngine,
                tunnelMode = if (tunnelMode == "max" || tunnelMode == "cottenmax") "cottenmax" else "legacy",
                transportMode = transportMode,
                resolvers = resolversStr,
                useBundledResolvers = resolversStr.isBlank() && detectedEngine != WhiteDnsProfile.ENGINE_SOAR,
                link = trimmed,
            )
            return name to profile
        }

        // Alternative URL format: scheme://domain:key@method
        // Example: cottendns://example.com:mysecretkey@1#MyServer
        if (payload.contains('@') || payload.contains(':')) {
            val parts = payload.split('@')
            val method = if (parts.size > 1) parts[1].toIntOrNull() ?: 1 else 1
            val domainAndKey = parts[0]
            val domain = domainAndKey.substringBefore(':')
            val key = domainAndKey.substringAfter(':', "")
            if (domain.isNotBlank() && key.isNotBlank()) {
                val profile = WhiteDnsProfile(
                    domain = domain,
                    encryptionKey = key,
                    encryptionMethod = method,
                    engine = defaultEngine,
                    link = trimmed,
                )
                return (remarkFromUrl ?: "WhiteDNS") to profile
            }
        }

        null
    }.getOrNull()

    fun build(name: String, profile: WhiteDnsProfile): String {
        val root = buildJsonObject {
            put("schema", "whitedns.profile")
            put("version", 1)
            put("profile", buildJsonObject {
                put("name", name)
                put("server", buildJsonObject {
                    put("domain", profile.domain.trim().trimEnd('.'))
                    if (profile.engine == WhiteDnsProfile.ENGINE_SOAR) {
                        put("public_key", profile.encryptionKey.trim())
                    } else {
                        put("encryption_key", profile.encryptionKey.trim())
                        put("encryption_method", profile.encryptionMethod)
                    }
                })
                if (profile.engine == WhiteDnsProfile.ENGINE_COTTENDNS) {
                    put("cottenSettings", buildJsonObject {
                        put("tunnelMode", profile.tunnelMode)
                        put("transportMode", profile.transportMode)
                        put("deliveryMode", profile.deliveryMode)
                        put("qnameMode", profile.qnameMode)
                        put("configPreset", profile.configPreset)
                    })
                }
                if (profile.resolversList().isNotEmpty()) {
                    put("resolvers", buildJsonArray {
                        profile.resolversList().forEach { add(JsonPrimitive(it)) }
                    })
                }
            })
        }
        val encoded = Base64.UrlSafe.encode(root.toString().encodeToByteArray()).trimEnd('=')
        val scheme = when (profile.engine) {
            WhiteDnsProfile.ENGINE_SOAR -> "soar"
            WhiteDnsProfile.ENGINE_STORMDNS -> "stormdns"
            WhiteDnsProfile.ENGINE_MASTERDNS -> "masterdns"
            else -> "cottendns"
        }
        return "$scheme://$encoded#${name}"
    }

    private fun decodeBase64(input: String): String? = runCatching {
        val padded = input.padEnd(input.length + ((4 - input.length % 4) % 4), '=')
        runCatching { Base64.UrlSafe.decode(padded) }
            .recoverCatching { Base64.Default.decode(padded) }
            .getOrNull()?.decodeToString()
    }.getOrNull()
}