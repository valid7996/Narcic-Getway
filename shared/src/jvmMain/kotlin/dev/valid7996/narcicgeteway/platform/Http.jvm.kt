package dev.valid7996.narcicgeteway.platform

import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

internal actual fun httpGetText(
    url: String,
    userAgent: String,
    connectTimeoutMs: Int,
    readTimeoutMs: Int,
): String {
    var currentUrl = url
    var hops = 0
    val maxHops = 10
    while (hops < maxHops) {
        hops++
        val target = URL(currentUrl)
        val connection = (target.openConnection() as HttpURLConnection).apply {
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            instanceFollowRedirects = false
            setRequestProperty("User-Agent", userAgent)
            setRequestProperty("Accept", "*/*")
        }
        if (connection is HttpsURLConnection) {
            connection.sslSocketFactory = permissiveSslSocketFactory
            connection.hostnameVerifier = permissiveHostnameVerifier
        }
        try {
            val code = connection.responseCode
            if (code in 300..399) {
                val loc = connection.getHeaderField("Location")
                if (!loc.isNullOrBlank()) {
                    currentUrl = URL(target, loc).toString()
                    continue
                }
            }
            if (code !in 200..299) {
                throw IllegalStateException("HTTP $code")
            }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } catch (e: NullPointerException) {
            throw platformHttpFailure(e)
        } finally {
            connection.disconnectQuietly()
        }
    }
    throw IllegalStateException("Too many redirects ($hops)")
}

internal actual fun httpGetResponse(
    url: String,
    userAgent: String,
    socksPort: Int?,
    connectTimeoutMs: Int,
    readTimeoutMs: Int,
): HttpTextResponse {
    if (socksPort != null && JvmRuntime.resolvesThroughProxy) {
        val response = rawHttpViaSocks("GET", url, mapOf("User-Agent" to userAgent, "Accept" to "*/*"), null, socksPort, connectTimeoutMs, readTimeoutMs)
        if (response.code !in 200..299) throw IllegalStateException("HTTP ${response.code}")
        return HttpTextResponse(response.code, response.body, response.headers)
    }

    var currentUrl = url
    var hops = 0
    val maxHops = 10
    while (hops < maxHops) {
        hops++
        val target = URL(currentUrl)
        val connection = (
            if (socksPort != null) {
                target.openConnection(
                    java.net.Proxy(java.net.Proxy.Type.SOCKS, java.net.InetSocketAddress.createUnresolved("127.0.0.1", socksPort)),
                )
            } else {
                val opener = VpnNet.urlOpener
                if (opener != null && dev.valid7996.narcicgeteway.core.VpnManager.deviceVpnActive()) {
                    opener(target)
                } else {
                    target.openConnection()
                }
            }
        ) as HttpURLConnection

        if (connection is HttpsURLConnection) {
            connection.sslSocketFactory = permissiveSslSocketFactory
            connection.hostnameVerifier = permissiveHostnameVerifier
        }

        connection.apply {
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            instanceFollowRedirects = false
            setRequestProperty("User-Agent", userAgent)
            setRequestProperty("Accept", "*/*")
        }
        try {
            val code = connection.responseCode
            val headers = connection.headerFields.orEmpty().entries
                .mapNotNull { (name, values) ->
                    val key = name?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                    val value = values?.firstOrNull() ?: return@mapNotNull null
                    key to value
                }
                .toMap()

            if (code in 300..399) {
                val loc = headers["location"]
                if (!loc.isNullOrBlank()) {
                    currentUrl = URL(target, loc).toString()
                    continue
                }
            }

            if (code !in 200..299) throw IllegalStateException("HTTP $code")

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            return HttpTextResponse(code, body, headers)
        } catch (e: NullPointerException) {
            throw platformHttpFailure(e)
        } finally {
            connection.disconnectQuietly()
        }
    }
    throw IllegalStateException("Too many redirects ($hops)")
}

internal actual suspend fun httpJson(
    method: String,
    url: String,
    headers: Map<String, String>,
    body: String?,
    connectTimeoutMs: Int,
    readTimeoutMs: Int,
    socksPort: Int?,
): HttpTextResponse = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    if (socksPort != null && JvmRuntime.resolvesThroughProxy) {
        val response = rawHttpViaSocks(method, url, headers, body?.encodeToByteArray(), socksPort, connectTimeoutMs, readTimeoutMs)
        return@withContext HttpTextResponse(response.code, response.body, response.headers)
    }
    val target = URL(url)

    val raw = if (socksPort != null) {
        target.openConnection(
            java.net.Proxy(java.net.Proxy.Type.SOCKS, java.net.InetSocketAddress.createUnresolved("127.0.0.1", socksPort)),
        )
    } else {
        target.openConnection()
    }
    val connection = (raw as HttpURLConnection).apply {
        requestMethod = method
        connectTimeout = connectTimeoutMs
        readTimeout = readTimeoutMs
        instanceFollowRedirects = true
        headers.forEach { (k, v) -> setRequestProperty(k, v) }
    }
    if (connection is HttpsURLConnection) {
        connection.sslSocketFactory = permissiveSslSocketFactory
        connection.hostnameVerifier = permissiveHostnameVerifier
    }
    try {
        if (body != null) {
            connection.doOutput = true
            connection.outputStream.use { it.write(body.encodeToByteArray()) }
        }
        val code = connection.responseCode

        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        val responseHeaders = connection.headerFields.orEmpty().entries
            .mapNotNull { (name, values) ->
                val key = name?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                val value = values?.firstOrNull() ?: return@mapNotNull null
                key to value
            }
            .toMap()
        HttpTextResponse(code, text, responseHeaders)
    } catch (e: NullPointerException) {
        throw platformHttpFailure(e)
    } finally {
        connection.disconnectQuietly()
    }
}
