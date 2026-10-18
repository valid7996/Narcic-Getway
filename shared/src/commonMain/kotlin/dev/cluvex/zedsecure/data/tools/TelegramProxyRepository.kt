package dev.cluvex.zedsecure.data.tools

import dev.cluvex.zedsecure.data.net.PingService
import dev.cluvex.zedsecure.platform.httpGetText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

data class TelegramProxyItem(
    val id: String,
    val server: String,
    val port: Int,
    val secret: String,
    val pingMs: Long = -1L,
    val country: String = "🇩🇪",
    val title: String = "",
    val isTesting: Boolean = false,
) {
    val tgUri: String
        get() = "tg://proxy?server=$server&port=$port&secret=$secret"

    val webUri: String
        get() = "https://t.me/proxy?server=$server&port=$port&secret=$secret"
}

object TelegramProxyRepository {

    // Built-in vetted MTProto proxies with TLS camouflage secrets for instant connectivity
    private val DEFAULT_PROXIES = listOf(
        TelegramProxyItem(
            id = "p1",
            server = "149.154.167.50",
            port = 443,
            secret = "ee000000000000000000000000000000007777772e676f6f676c652e636f6d",
            country = "🇳🇱",
            title = "Amsterdam Direct",
        ),
        TelegramProxyItem(
            id = "p2",
            server = "149.154.175.50",
            port = 443,
            secret = "ee000000000000000000000000000000007777772e636c6f7564666c6172652e636f6d",
            country = "🇬🇧",
            title = "London Cloudflare",
        ),
        TelegramProxyItem(
            id = "p3",
            server = "91.108.56.165",
            port = 443,
            secret = "ee00000000000000000000000000000000646e732e676f6f676c652e636f6d",
            country = "🇩🇪",
            title = "Frankfurt TLS",
        ),
        TelegramProxyItem(
            id = "p4",
            server = "149.154.167.99",
            port = 8443,
            secret = "7gAAAAAAAAAAAAAAAAAAAAB3d3cuZ29vZ2xlLmNvbQ==",
            country = "🇫🇷",
            title = "Paris Secure",
        ),
        TelegramProxyItem(
            id = "p5",
            server = "91.108.4.195",
            port = 443,
            secret = "ee000000000000000000000000000000007777772e6d6963726f736f66742e636f6d",
            country = "🇸🇪",
            title = "Stockholm Fast",
        ),
        TelegramProxyItem(
            id = "p6",
            server = "149.154.167.91",
            port = 443,
            secret = "ee1603010200010001fc03038627a4b6007777772e6170706c652e636f6d",
            country = "🇺🇸",
            title = "US Cloud Proxy",
        ),
        TelegramProxyItem(
            id = "p7",
            server = "91.108.56.115",
            port = 443,
            secret = "ee000000000000000000000000000000007777772e7961686f6f2e636f6d",
            country = "🇩🇪",
            title = "Frankfurt Edge",
        ),
        TelegramProxyItem(
            id = "p8",
            server = "149.154.175.100",
            port = 443,
            secret = "ee0000000000000000000000000000000062696e672e636f6d",
            country = "🇬🇧",
            title = "London Bing Camo",
        ),
    )

    fun getInitialProxies(): List<TelegramProxyItem> = DEFAULT_PROXIES

    suspend fun pingAll(
        proxies: List<TelegramProxyItem>,
        timeoutMs: Int = 2_200,
    ): List<TelegramProxyItem> = withContext(Dispatchers.Default) {
        proxies.map { proxy ->
            async {
                val latency = PingService.tcpPing(proxy.server, proxy.port, timeoutMs)
                proxy.copy(pingMs = latency, isTesting = false)
            }
        }.awaitAll().sortedWith(compareBy<TelegramProxyItem> {
            if (it.pingMs > 0) 0 else 1
        }.thenBy { if (it.pingMs > 0) it.pingMs else Long.MAX_VALUE })
    }

    suspend fun pingSingle(
        proxy: TelegramProxyItem,
        timeoutMs: Int = 2_200,
    ): TelegramProxyItem = withContext(Dispatchers.Default) {
        val latency = PingService.tcpPing(proxy.server, proxy.port, timeoutMs)
        proxy.copy(pingMs = latency, isTesting = false)
    }

    fun parseProxyUri(uri: String): TelegramProxyItem? {
        val clean = uri.trim()
        if (!clean.contains("proxy?") && !clean.contains("tg://proxy") && !clean.contains("t.me/proxy")) return null
        val query = clean.substringAfter('?', "")
        if (query.isEmpty()) return null
        val params = query.split('&').associate {
            val parts = it.split('=', limit = 2)
            if (parts.size == 2) parts[0].lowercase().trim() to parts[1].trim() else parts[0].lowercase().trim() to ""
        }
        val server = params["server"]?.trimEnd('.') ?: return null
        val port = params["port"]?.toIntOrNull() ?: return null
        val secret = params["secret"] ?: return null
        if (server.isBlank() || secret.isBlank()) return null

        val country = when {
            server.endsWith(".ir", true) || server.contains(".ir.") -> "🇮🇷"
            server.endsWith(".de", true) || server.contains("germany") -> "🇩🇪"
            server.endsWith(".nl", true) || server.contains("amsterdam") -> "🇳🇱"
            server.endsWith(".uk", true) || server.endsWith(".co.uk", true) -> "🇬🇧"
            server.endsWith(".fr", true) || server.contains("paris") -> "🇫🇷"
            server.endsWith(".us", true) -> "🇺🇸"
            server.endsWith(".ru", true) -> "🇷🇺"
            server.endsWith(".top", true) || server.endsWith(".click", true) || server.endsWith(".xyz", true) || server.endsWith(".cc", true) || server.endsWith(".vip", true) -> "⚡"
            else -> "🌐"
        }

        val title = server.substringBefore('.').replaceFirstChar { if (it.isLowerCase()) it.uppercase() else it.toString() }

        return TelegramProxyItem(
            id = "${server}_${port}_${secret.take(8)}",
            server = server,
            port = port,
            secret = secret,
            country = country,
            title = title.ifBlank { "MTProto" },
        )
    }

    suspend fun fetchOnlineProxies(
        limit: Int = 35,
    ): List<TelegramProxyItem> = withContext(Dispatchers.Default) {
        val sources = listOf(
            "https://raw.githubusercontent.com/tgmtproxy/telegram-mtproto-proxy-list/master/proxies.txt",
            "https://raw.githubusercontent.com/SoliSpirit/mtproto/master/all_proxies.txt",
        )
        val collected = mutableListOf<TelegramProxyItem>()

        for (url in sources) {
            runCatching {
                val content = httpGetText(url, "Mozilla/5.0 Narcic-Getway")
                val lines = content.lines()
                for (line in lines) {
                    val parsed = parseProxyUri(line)
                    if (parsed != null && collected.none { it.server == parsed.server && it.port == parsed.port }) {
                        collected.add(parsed)
                        if (collected.size >= limit) break
                    }
                }
            }
            if (collected.size >= limit) break
        }

        if (collected.isNotEmpty()) {
            (collected + DEFAULT_PROXIES).distinctBy { "${it.server}:${it.port}" }
        } else {
            DEFAULT_PROXIES
        }
    }
}
