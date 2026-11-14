package dev.valid7996.narcicgeteway.data.config

import dev.valid7996.narcicgeteway.domain.config.ConfigParser
import dev.valid7996.narcicgeteway.domain.config.Protocol
import dev.valid7996.narcicgeteway.domain.config.ServerConfig
import dev.valid7996.narcicgeteway.domain.config.SingBoxLinks
import dev.valid7996.narcicgeteway.domain.config.TransportConfig
import dev.valid7996.narcicgeteway.platform.httpGetResponse
import dev.valid7996.narcicgeteway.platform.httpJson
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

object KaviroHiveProvider {
    const val DEFAULT_KAVIRO_BASE = "https://kaviro.ir"
    const val CATALOG_ENDPOINT = "/api/v1/catalog"
    const val HIVE_ENDPOINT = "/device/hive"
    const val ROUTE_ENDPOINT = "/device/route"
    const val REFRESH_ENDPOINT = "/device/refresh"

    private const val MIRROR_DISCOVERY_URL_1 = "https://raw.githubusercontent.com/YamYamVPN/refactored-barnacle/refs/heads/main/LOREM"
    private const val MIRROR_DISCOVERY_URL_2 = "https://docs.google.com/document/d/1jqH4YHTkBy-6m6Mdmw8LqQHESgLjOdlJv7XoXxgZuvI/export?format=txt"

    private val DOMAIN_EXTRACTOR_REGEX = Regex(
        """volutpat\.\s+([A-Za-z0-9.-]{1,200})\s+Ut wisi enim\b.*\bcommodo consequat\.\s+\.?([A-Za-z]{2,24})""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun isKaviroEndpoint(url: String): Boolean {
        val u = url.trim().lowercase()
        return u.contains("kaviro.ir") ||
            u.contains("/api/v1/catalog") ||
            u.contains("/device/hive") ||
            u.contains("/device/route") ||
            u.contains("/device/refresh") ||
            u.contains("yamyam")
    }

    /**
     * Resolves the current live base URL for Kaviro/SmartCore.
     * Tries default kaviro.ir first; if unreachable, attempts dynamic domain discovery.
     */
    suspend fun resolveActiveBaseUrl(tunnelPort: Int? = null): String {
        val check = runCatching {
            httpGetResponse("$DEFAULT_KAVIRO_BASE$CATALOG_ENDPOINT", userAgent = "okhttp/4.12.0", socksPort = tunnelPort)
        }.getOrNull()

        if (check != null && check.code in 200..499 && check.code != 502 && check.code != 503) {
            return DEFAULT_KAVIRO_BASE
        }

        // Try mirror discovery
        val discovered = discoverDomainFromMirrors(tunnelPort)
        if (discovered != null) {
            return "https://$discovered"
        }

        return DEFAULT_KAVIRO_BASE
    }

    private fun discoverDomainFromMirrors(tunnelPort: Int? = null): String? {
        val urls = listOf(MIRROR_DISCOVERY_URL_1, MIRROR_DISCOVERY_URL_2)
        for (url in urls) {
            val text = runCatching {
                httpGetResponse(url, userAgent = "Mozilla/5.0", socksPort = tunnelPort).body
            }.getOrNull() ?: continue

            val match = DOMAIN_EXTRACTOR_REGEX.find(text.replace('\u00A0', ' '))
            if (match != null) {
                val d1 = match.groupValues.getOrNull(1)?.trim()
                val d2 = match.groupValues.getOrNull(2)?.trim()
                if (!d1.isNullOrBlank() && !d2.isNullOrBlank()) {
                    return "$d1.$d2".lowercase()
                }
            }
        }
        return null
    }

    /**
     * Fetches subscription servers from catalog or hive endpoint, falling back to discovered mirrors or bootstrap configs.
     */
    suspend fun fetchSubscriptionConfigs(url: String, userAgent: String, tunnelPort: Int? = null): List<String> {
        val cleanUrl = url.trim()
        val targetPath = when {
            cleanUrl.contains("/device/hive") -> HIVE_ENDPOINT
            cleanUrl.contains("/device/route") -> ROUTE_ENDPOINT
            else -> CATALOG_ENDPOINT
        }

        val direct = runCatching {
            fetchEndpoint(cleanUrl, userAgent, tunnelPort)
        }.getOrNull()

        if (!direct.isNullOrEmpty()) {
            val parsed = parseCatalogResponse(direct)
            if (parsed.isNotEmpty()) return parsed
        }

        // Try resolved mirror domain
        val activeBase = resolveActiveBaseUrl(tunnelPort)
        val mirrorUrl = "$activeBase$targetPath"
        if (mirrorUrl != cleanUrl) {
            val mirrorResp = runCatching {
                fetchEndpoint(mirrorUrl, userAgent, tunnelPort)
            }.getOrNull()
            if (!mirrorResp.isNullOrEmpty()) {
                val parsed = parseCatalogResponse(mirrorResp)
                if (parsed.isNotEmpty()) return parsed
            }
        }

        // Fallback to bootstrap configs so the user has immediate access to SmartCore servers
        return getBootstrapSmartCoreLinks()
    }

    private suspend fun fetchEndpoint(url: String, userAgent: String, tunnelPort: Int?): String? {
        val headers = mapOf(
            "User-Agent" to userAgent,
            "Accept" to "application/json",
            "Content-Type" to "application/json"
        )
        // Catalog and Hive accept POST with JSON envelope or GET
        val postAttempt = runCatching {
            val reqBody = """{"version":1,"audience":"com.yamyam.vpn.catalog.v1"}"""
            httpJson("POST", url, headers, reqBody, socksPort = tunnelPort)
        }.getOrNull()

        if (postAttempt != null && postAttempt.code == 200 && postAttempt.body.isNotBlank()) {
            return postAttempt.body
        }

        val getAttempt = runCatching {
            httpGetResponse(url, userAgent = userAgent, socksPort = tunnelPort)
        }.getOrNull()

        if (getAttempt != null && getAttempt.code == 200 && getAttempt.body.isNotBlank()) {
            return getAttempt.body
        }

        return null
    }

    /**
     * Parses catalog or hive JSON payload into a list of canonical links
     * covering hysteria2x, vless-raw-reality, vless-grpc-reality, vless-xhttp, trojan, and samizdat.
     */
    fun parseCatalogResponse(body: String): List<String> {
        val trimmed = body.trim()
        val rootElement = runCatching { json.parseToJsonElement(trimmed) }.getOrNull() ?: return emptyList()

        val results = mutableListOf<String>()

        fun extractFromArray(array: JsonArray) {
            for (elem in array) {
                if (elem is JsonObject) {
                    serverObjectToLink(elem)?.let { results.add(it) }
                }
            }
        }

        if (rootElement is JsonArray) {
            extractFromArray(rootElement)
        } else if (rootElement is JsonObject) {
            // Hive response has "servers": [...]
            rootElement["servers"]?.let { if (it is JsonArray) extractFromArray(it) }
            // Catalog envelope may have "data" or "catalog"
            rootElement["data"]?.let { if (it is JsonArray) extractFromArray(it) }
            rootElement["catalog"]?.let { if (it is JsonArray) extractFromArray(it) }
        }

        return results
    }

    private fun serverObjectToLink(obj: JsonObject): String? {
        val host = obj["host"]?.jsonPrimitive?.contentOrNull
            ?: obj["address"]?.jsonPrimitive?.contentOrNull
            ?: return null
        val port = obj["port"]?.jsonPrimitive?.intOrNull ?: 443
        val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: "smartcore"
        val country = obj["countryCode"]?.jsonPrimitive?.contentOrNull ?: "IR"
        val remark = "SmartCore · $country ($id)"

        val protocol = obj["protocol"]?.jsonPrimitive?.contentOrNull?.lowercase()
            ?: when {
                obj.containsKey("hysteria") || obj.containsKey("hysteria2") -> "hysteria2"
                obj.containsKey("xhttp") -> "vless-xhttp"
                obj.containsKey("grpc") -> "vless-grpc-reality"
                obj.containsKey("samizdat") -> "samizdat"
                obj.containsKey("trojan") -> "trojan"
                obj.containsKey("vless") -> "vless-raw-reality"
                else -> "vless-raw-reality"
            }

        val tlsServerName = obj["tlsServerName"]?.jsonPrimitive?.contentOrNull
            ?: obj["sni"]?.jsonPrimitive?.contentOrNull
            ?: host

        return when (protocol) {
            "hysteria2", "hysteria2x", "hy2", "hy2x" -> {
                val h2Obj = obj["hysteria2"]?.jsonObject ?: obj["hysteria"]?.jsonObject ?: obj
                val auth = h2Obj["auth"]?.jsonPrimitive?.contentOrNull ?: obj["auth"]?.jsonPrimitive?.contentOrNull ?: id
                val ports = h2Obj["ports"]?.jsonPrimitive?.contentOrNull ?: obj["ports"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val hopInterval = h2Obj["hopInterval"]?.jsonPrimitive?.contentOrNull ?: "30s"
                val obfs = h2Obj["obfs"]?.jsonPrimitive?.contentOrNull ?: "salamander"
                val obfsPass = h2Obj["obfsPassword"]?.jsonPrimitive?.contentOrNull.orEmpty()

                val mportParam = if (ports.isNotBlank()) "&mport=$ports" else ""
                val hopParam = if (ports.isNotBlank()) "&hopInterval=$hopInterval" else ""
                val obfsParam = if (obfs.isNotBlank()) "&obfs=$obfs&obfs-password=$obfsPass" else ""

                "hysteria2x://$auth@$host:$port/?sni=$tlsServerName$mportParam$hopParam$obfsParam#$remark"
            }

            "vless-grpc-reality" -> {
                val uuid = obj["uuid"]?.jsonPrimitive?.contentOrNull ?: obj["id"]?.jsonPrimitive?.contentOrNull ?: id
                val pbk = obj["publicKey"]?.jsonPrimitive?.contentOrNull ?: obj["pbk"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val sid = obj["shortId"]?.jsonPrimitive?.contentOrNull ?: obj["sid"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val serviceName = obj["serviceName"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val flow = obj["flow"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val flowParam = if (flow.isNotBlank()) "&flow=$flow" else ""

                "vless-grpc-reality://$uuid@$host:$port/?security=reality&sni=$tlsServerName&fp=chrome&pbk=$pbk&sid=$sid&type=grpc&serviceName=$serviceName$flowParam#$remark"
            }

            "vless-xhttp" -> {
                val uuid = obj["uuid"]?.jsonPrimitive?.contentOrNull ?: obj["id"]?.jsonPrimitive?.contentOrNull ?: id
                val path = obj["path"]?.jsonPrimitive?.contentOrNull ?: "/"
                val mode = obj["mode"]?.jsonPrimitive?.contentOrNull ?: "auto"

                "vless-xhttp://$uuid@$host:$port/?security=tls&sni=$tlsServerName&type=xhttp&path=$path&mode=$mode#$remark"
            }

            "samizdat" -> {
                val auth = obj["auth"]?.jsonPrimitive?.contentOrNull ?: obj["token"]?.jsonPrimitive?.contentOrNull ?: id
                "samizdat://$auth@$host:$port/?sni=$tlsServerName#$remark"
            }

            "trojan" -> {
                val pass = obj["password"]?.jsonPrimitive?.contentOrNull ?: obj["auth"]?.jsonPrimitive?.contentOrNull ?: id
                "trojan://$pass@$host:$port/?security=tls&sni=$tlsServerName#$remark"
            }

            else -> {
                // Default to vless-raw-reality
                val uuid = obj["uuid"]?.jsonPrimitive?.contentOrNull ?: obj["id"]?.jsonPrimitive?.contentOrNull ?: id
                val pbk = obj["publicKey"]?.jsonPrimitive?.contentOrNull ?: obj["pbk"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val sid = obj["shortId"]?.jsonPrimitive?.contentOrNull ?: obj["sid"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val flow = obj["flow"]?.jsonPrimitive?.contentOrNull ?: "xtls-rprx-vision"

                "vless-raw-reality://$uuid@$host:$port/?security=reality&sni=$tlsServerName&fp=chrome&pbk=$pbk&sid=$sid&flow=$flow#$remark"
            }
        }
    }

    /**
     * Bootstrap fallback configurations for SmartCore / Kaviro Hive.
     * Guaranteed high-speed connectivity when network endpoints are under active filtering.
     */
    fun getBootstrapSmartCoreLinks(): List<String> = listOf(
        "hysteria2x://smartcore@kaviro.ir:443/?sni=kaviro.ir&obfs=salamander&obfs-password=kaviro-obfs#اسمارت‌کور · Hysteria 2X (هایو)",
        "vless-raw-reality://48ff2b70-e180-582f-8866-d9a2edeed5f5@51.158.206.20:23576/?security=reality&sni=fuck.rkn&fp=chrome&pbk=1y5h2FGWKXTJ9xLPCqPo6Mw7RxoZzh6fGkEQKNxpZ3s&sid=01&flow=xtls-rprx-vision#اسمارت‌کور · Reality Vision 1",
        "vless-raw-reality://48ff2b70-e180-582f-8866-d9a2edeed5f5@51.158.206.97:23576/?security=reality&sni=fuck.rkn&fp=chrome&pbk=1y5h2FGWKXTJ9xLPCqPo6Mw7RxoZzh6fGkEQKNxpZ3s&sid=01&flow=xtls-rprx-vision#اسمارت‌کور · Reality Vision 2",
        "vless-grpc-reality://48ff2b70-e180-582f-8866-d9a2edeed5f5@51.158.206.103:23576/?security=reality&sni=fuck.rkn&fp=chrome&pbk=1y5h2FGWKXTJ9xLPCqPo6Mw7RxoZzh6fGkEQKNxpZ3s&sid=01&type=grpc&serviceName=smartcore-grpc#اسمارت‌کور · gRPC Reality",
        "vless-xhttp://48ff2b70-e180-582f-8866-d9a2edeed5f5@51.158.206.26:443/?security=tls&sni=fuck.rkn&type=xhttp&path=/&mode=auto#اسمارت‌کور · XHTTP Stream",
        "samizdat://samizdat-smart-token@kaviro.ir:443/?sni=kaviro.ir#اسمارت‌کور · Samizdat"
    )
}
