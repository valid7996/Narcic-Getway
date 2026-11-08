package dev.valid7996.narcicgeteway.domain.config

import kotlinx.serialization.Serializable

/**
 * Engine settings shared by every WhiteDNS connection (MTU scan, workers, timeouts, retries ...).
 * Numbers are kept as text so that a half-typed value never loses what the user entered; [resolve] clamps
 * them the way the engine accepts them.
 *
 * The local listener (address, port, SOCKS auth, local DNS) is deliberately not here: Narcic owns the local
 * port and bridges the engine into the tunnel itself.
 */
@Serializable
data class WhiteDnsSettings(
    /** Resolvers used when the connection has none of its own and the global DNS override is off. */
    val resolvers: String = "",

    val balancingStrategy: Int = 3,
    val uploadDuplication: String = "3",
    val downloadDuplication: String = "7",
    val uploadCompression: Int = 2,
    val downloadCompression: Int = 2,
    val baseEncodeData: Boolean = false,

    val minUploadMtu: String = "40",
    val minDownloadMtu: String = "300",
    val maxUploadMtu: String = "140",
    val maxDownloadMtu: String = "3000",

    val mtuTestRetriesResolvers: String = "3",
    val mtuTestTimeoutResolvers: String = "2.5",
    val mtuTestParallelismResolvers: String = "100",
    val mtuTestRetriesLogs: String = "5",
    val mtuTestTimeoutLogs: String = "2.5",
    val mtuTestParallelismLogs: String = "32",

    val rxTxWorkers: String = "4",
    val tunnelProcessWorkers: String = "4",
    val tunnelPacketTimeoutSeconds: String = "10.0",
    val dispatcherIdlePollIntervalSeconds: String = "0.020",
    val txChannelSize: String = "2048",
    val rxChannelSize: String = "2048",
    val resolverUdpConnectionPoolSize: String = "64",
    val streamQueueInitialCapacity: String = "128",
    val orphanQueueInitialCapacity: String = "32",
    val dnsResponseFragmentStoreCapacity: String = "256",
    val maxActiveStreams: String = "2048",

    val localHandshakeTimeoutSeconds: String = "5.0",
    val socksUdpAssociateReadTimeoutSeconds: String = "30.0",
    val clientTerminalStreamRetentionSeconds: String = "45.0",
    val clientCancelledSetupRetentionSeconds: String = "120.0",

    val sessionInitRetryBaseSeconds: String = "1.0",
    val sessionInitRetryStepSeconds: String = "1.0",
    val sessionInitRetryLinearAfter: String = "5",
    val sessionInitRetryMaxSeconds: String = "60.0",
    val sessionInitBusyRetryIntervalSeconds: String = "60.0",

    /** "resolvers" = full scan, "logs" = start from the resolvers of earlier sessions (fast). */
    val startupMode: String = "resolvers",
    val pingWatchdogSeconds: String = "300",
    val logLevel: String = "INFO",
) {
    fun resolve(): WhiteDnsResolved {
        val rxTx = boundedInt(rxTxWorkers, 4, 1, 64)
        val retryBase = boundedDouble(sessionInitRetryBaseSeconds, 1.0, 0.1, 60.0)
        val minUp = boundedInt(minUploadMtu, 40, 1, 65535)
        val minDown = boundedInt(minDownloadMtu, 300, 1, 65535)
        return WhiteDnsResolved(
            balancingStrategy = if (balancingStrategy in 1..4) balancingStrategy else 3,
            uploadDuplication = boundedInt(uploadDuplication, 3, 1, 30),
            downloadDuplication = boundedInt(downloadDuplication, 7, 1, 30),
            uploadCompression = uploadCompression.coerceIn(0, 3),
            downloadCompression = downloadCompression.coerceIn(0, 3),
            baseEncodeData = baseEncodeData,
            minUploadMtu = minUp,
            minDownloadMtu = minDown,
            maxUploadMtu = boundedInt(maxUploadMtu, 140, 1, 65535).coerceAtLeast(minUp),
            maxDownloadMtu = boundedInt(maxDownloadMtu, 3000, 1, 65535).coerceAtLeast(minDown),
            mtuTestRetriesResolvers = boundedInt(mtuTestRetriesResolvers, 3, 1, 100),
            mtuTestTimeoutResolvers = positiveDouble(mtuTestTimeoutResolvers, 2.5),
            mtuTestParallelismResolvers = boundedInt(mtuTestParallelismResolvers, 100, 1, 1024),
            mtuTestRetriesLogs = boundedInt(mtuTestRetriesLogs, 5, 1, 100),
            mtuTestTimeoutLogs = positiveDouble(mtuTestTimeoutLogs, 2.5),
            mtuTestParallelismLogs = boundedInt(mtuTestParallelismLogs, 32, 1, 1024),
            rxTxWorkers = rxTx,
            tunnelProcessWorkers = boundedInt(tunnelProcessWorkers, 4, 1, 64).coerceAtLeast(rxTx),
            tunnelPacketTimeoutSeconds = boundedDouble(tunnelPacketTimeoutSeconds, 10.0, 0.5, 120.0),
            dispatcherIdlePollIntervalSeconds = boundedDouble(dispatcherIdlePollIntervalSeconds, 0.020, 0.001, 1.0),
            txChannelSize = boundedInt(txChannelSize, 2048, 64, 65536),
            rxChannelSize = boundedInt(rxChannelSize, 2048, 64, 65536),
            resolverUdpConnectionPoolSize = boundedInt(resolverUdpConnectionPoolSize, 64, 1, 1024),
            streamQueueInitialCapacity = boundedInt(streamQueueInitialCapacity, 128, 8, 65536),
            orphanQueueInitialCapacity = boundedInt(orphanQueueInitialCapacity, 32, 4, 4096),
            dnsResponseFragmentStoreCapacity = boundedInt(dnsResponseFragmentStoreCapacity, 256, 16, 16384),
            maxActiveStreams = boundedInt(maxActiveStreams, 2048, 1, 65535),
            localHandshakeTimeoutSeconds = boundedDouble(localHandshakeTimeoutSeconds, 5.0, 0.5, 60.0),
            socksUdpAssociateReadTimeoutSeconds = boundedDouble(socksUdpAssociateReadTimeoutSeconds, 30.0, 1.0, 3600.0),
            clientTerminalStreamRetentionSeconds = boundedDouble(clientTerminalStreamRetentionSeconds, 45.0, 1.0, 3600.0),
            clientCancelledSetupRetentionSeconds = boundedDouble(clientCancelledSetupRetentionSeconds, 120.0, 1.0, 3600.0),
            sessionInitRetryBaseSeconds = retryBase,
            sessionInitRetryStepSeconds = boundedDouble(sessionInitRetryStepSeconds, 1.0, 0.0, 60.0),
            sessionInitRetryLinearAfter = boundedInt(sessionInitRetryLinearAfter, 5, 0, 1000),
            sessionInitRetryMaxSeconds = boundedDouble(sessionInitRetryMaxSeconds, 60.0, retryBase, 3600.0),
            sessionInitBusyRetryIntervalSeconds = boundedDouble(sessionInitBusyRetryIntervalSeconds, 60.0, 1.0, 3600.0),
            startupMode = if (startupMode.trim().lowercase() == "logs") "logs" else "resolvers",
            pingWatchdogSeconds = boundedInt(pingWatchdogSeconds, 300, 0, 3600),
            logLevel = normalizeLogLevel(logLevel),
        )
    }

    companion object {
        val BALANCING = listOf(1 to "Random", 2 to "Round Robin", 3 to "Least Loss", 4 to "Lowest Latency")
        val COMPRESSION = listOf(0 to "Off", 1 to "ZSTD", 2 to "LZ4", 3 to "ZLIB")
        val LOG_LEVELS = listOf("DEBUG", "INFO", "WARN", "ERROR")

        fun normalizeLogLevel(raw: String): String {
            val v = raw.trim().uppercase()
            return if (v in LOG_LEVELS) v else "INFO"
        }

        private fun boundedInt(raw: String, default: Int, min: Int, max: Int): Int =
            raw.trim().toIntOrNull()?.coerceIn(min, max) ?: default

        private fun positiveDouble(raw: String, default: Double): Double {
            val v = raw.trim().toDoubleOrNull() ?: return default
            return if (v > 0.0) v else default
        }

        private fun boundedDouble(raw: String, default: Double, min: Double, max: Double): Double {
            val v = raw.trim().toDoubleOrNull() ?: return default
            if (v.isNaN()) return default
            return v.coerceIn(min, max)
        }
    }
}

/** [WhiteDnsSettings] after clamping, ready to be written into the engine config. */
data class WhiteDnsResolved(
    val balancingStrategy: Int,
    val uploadDuplication: Int,
    val downloadDuplication: Int,
    val uploadCompression: Int,
    val downloadCompression: Int,
    val baseEncodeData: Boolean,
    val minUploadMtu: Int,
    val minDownloadMtu: Int,
    val maxUploadMtu: Int,
    val maxDownloadMtu: Int,
    val mtuTestRetriesResolvers: Int,
    val mtuTestTimeoutResolvers: Double,
    val mtuTestParallelismResolvers: Int,
    val mtuTestRetriesLogs: Int,
    val mtuTestTimeoutLogs: Double,
    val mtuTestParallelismLogs: Int,
    val rxTxWorkers: Int,
    val tunnelProcessWorkers: Int,
    val tunnelPacketTimeoutSeconds: Double,
    val dispatcherIdlePollIntervalSeconds: Double,
    val txChannelSize: Int,
    val rxChannelSize: Int,
    val resolverUdpConnectionPoolSize: Int,
    val streamQueueInitialCapacity: Int,
    val orphanQueueInitialCapacity: Int,
    val dnsResponseFragmentStoreCapacity: Int,
    val maxActiveStreams: Int,
    val localHandshakeTimeoutSeconds: Double,
    val socksUdpAssociateReadTimeoutSeconds: Double,
    val clientTerminalStreamRetentionSeconds: Double,
    val clientCancelledSetupRetentionSeconds: Double,
    val sessionInitRetryBaseSeconds: Double,
    val sessionInitRetryStepSeconds: Double,
    val sessionInitRetryLinearAfter: Int,
    val sessionInitRetryMaxSeconds: Double,
    val sessionInitBusyRetryIntervalSeconds: Double,
    val startupMode: String,
    val pingWatchdogSeconds: Int,
    val logLevel: String,
)
