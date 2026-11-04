package dev.valid7996.narcicgeteway.platform

internal expect suspend fun httpGetViaSocks(
    url: String,
    socksPort: Int?,
    userAgent: String = "NarcicGeteway",
    connectTimeoutMs: Int = 8_000,
    readTimeoutMs: Int = 8_000,
    closeConnection: Boolean = false,
): String

internal class HttpTiming(
    val bytes: Long,
    val wallNanos: Long,
    val ttfbNanos: Long,
    val serverMillis: Double,
)

internal expect suspend fun httpTimedTransfer(
    url: String,
    socksPort: Int?,
    upload: Boolean,
    uploadBytes: Int,
    connectTimeoutMs: Int,
    readTimeoutMs: Int,
): HttpTiming

internal expect suspend fun resolveHostAddress(host: String): String?

internal expect suspend fun tcpConnectMillis(host: String, port: Int, timeoutMs: Int): Long

internal expect suspend fun udpProbeMillis(host: String, port: Int, payload: ByteArray, timeoutMs: Int): Long

internal expect suspend fun tlsConnectMillis(host: String, port: Int, timeoutMs: Int): Long

internal expect suspend fun httpStreamTransfer(
    url: String,
    socksPort: Int?,
    upload: Boolean,
    uploadBytes: Long,
    connectTimeoutMs: Int,
    readTimeoutMs: Int,
    onChunk: (deltaBytes: Long) -> Boolean,
): HttpTiming

internal expect suspend fun dnsOverTcp(
    server: String,
    port: Int,
    query: ByteArray,
    socksPort: Int?,
    timeoutMs: Int,
): ByteArray
