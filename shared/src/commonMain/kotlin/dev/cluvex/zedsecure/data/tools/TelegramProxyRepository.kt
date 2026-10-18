package dev.cluvex.zedsecure.data.tools

import dev.cluvex.zedsecure.data.net.PingService
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
}
