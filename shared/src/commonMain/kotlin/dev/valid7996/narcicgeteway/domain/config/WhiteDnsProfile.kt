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

        // CottenDNS specific settings
        if (engine.equals(ENGINE_COTTENDNS, ignoreCase = true)) {
            appendLine("CONFIG_PRESET = \"${escape(configPreset)}\"")
            appendLine("TUNNEL_MODE = \"${escape(tunnelMode)}\"")
            if (transportMode != "auto") appendLine("TRANSPORT_MODE = \"${escape(transportMode)}\"")
            if (deliveryMode != "auto") appendLine("DELIVERY_MODE = \"${escape(deliveryMode)}\"")
            if (qnameMode != "auto") appendLine("QNAME_MODE = \"${escape(qnameMode)}\"")
            if (tunnelMode == "max") {
                appendLine("ENABLE_ADAPTIVE_MTU = true")
                appendLine("ENABLE_LOSS_RECOVERY = true")
                appendLine("ENABLE_QUERY_SHAPING = true")
                appendLine("FAST_SESSION_RESUME = true")
            }
        }

        // Common client settings
        appendLine("LISTEN_IP = \"$listenIp\"")
        appendLine("LISTEN_PORT = $port")
        appendLine("SOCKS5_AUTH = false")
        appendLine("RESOLVER_BALANCING_STRATEGY = $balancingStrategy")
        appendLine("UPLOAD_PACKET_DUPLICATION_COUNT = ${packetDuplication.coerceIn(1, 10)}")
        appendLine("DOWNLOAD_PACKET_DUPLICATION_COUNT = ${packetDuplication.coerceIn(1, 10)}")
        appendLine("UPLOAD_COMPRESSION_TYPE = $compression")
        appendLine("DOWNLOAD_COMPRESSION_TYPE = $compression")
        appendLine("AUTO_DISABLE_TIMEOUT_SERVERS = $autoDisableDeadResolvers")
        appendLine("BASE_ENCODE_DATA = 1")
        appendLine("MIN_UPLOAD_MTU = 100")
        appendLine("MAX_UPLOAD_MTU = 220")
        appendLine("MIN_DOWNLOAD_MTU = 100")
        appendLine("MAX_DOWNLOAD_MTU = 1200")
        appendLine("MTU_TEST_RETRIES_RESOLVERS = 2")
        appendLine("MTU_TEST_TIMEOUT_RESOLVERS = 3")
        appendLine("MTU_TEST_PARALLELISM_RESOLVERS = 16")
        appendLine("LOG_LEVEL = \"${if (logLevel.equals("DEBUG", ignoreCase = true)) "DEBUG" else "INFO"}\"")
    }

    fun toSoarJson(listenIp: String = "127.0.0.1", port: Int = listenPort): String {
        val zone = domain.split(',', '\n', ' ')
            .map { it.trim().trimEnd('.').lowercase() }
            .firstOrNull { it.isNotBlank() } ?: domain.trim().trimEnd('.').lowercase()
        val pubKey = encryptionKey.trim()

        val root = buildJsonObject {
            put("zone", zone)
            put("server_public_key", pubKey)
            put("system_resolvers", buildJsonArray { })
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

    fun toConfigFile(listenIp: String = "127.0.0.1", port: Int = listenPort): String {
        return if (engine.equals(ENGINE_SOAR, ignoreCase = true)) {
            toSoarJson(listenIp, port)
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
            val encKey = serverObj?.get("encryption_key")?.jsonPrimitive?.content
                ?: serverObj?.get("public_key")?.jsonPrimitive?.content
                ?: profileObj["encryption_key"]?.jsonPrimitive?.content.orEmpty()
            val encMethod = serverObj?.get("encryption_method")?.jsonPrimitive?.intOrNull
                ?: profileObj["encryption_method"]?.jsonPrimitive?.intOrNull ?: 1

            val cottenObj = profileObj["cottenSettings"]?.jsonObject
            val tunnelMode = cottenObj?.get("tunnelMode")?.jsonPrimitive?.content ?: "normal"
            val transportMode = cottenObj?.get("transportMode")?.jsonPrimitive?.content ?: "auto"

            val resolversArray = profileObj["resolvers"]?.jsonArray
            val resolversStr = resolversArray?.mapNotNull { it.jsonPrimitive.content }?.joinToString("\n").orEmpty()

            val profile = WhiteDnsProfile(
                domain = domain,
                encryptionKey = encKey,
                encryptionMethod = encMethod,
                engine = defaultEngine,
                tunnelMode = tunnelMode,
                transportMode = transportMode,
                resolvers = resolversStr,
                useBundledResolvers = resolversStr.isBlank(),
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
