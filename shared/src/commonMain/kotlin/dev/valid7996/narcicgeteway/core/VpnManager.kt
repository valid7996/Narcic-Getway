package dev.valid7996.narcicgeteway.core

import dev.valid7996.narcicgeteway.domain.model.ConnectionState
import kotlin.concurrent.Volatile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object VpnManager {
    data class Status(
        val state: ConnectionState = ConnectionState.Idle,
        val durationSeconds: Int = 0,
        val downloadBps: Long = 0,
        val uploadBps: Long = 0,
        val totalDownload: Long = 0,
        val totalUpload: Long = 0,
        val serverName: String? = null,
        val error: String? = null,

        val sessionId: Int = 0,
    )

    private val _status = MutableStateFlow(Status())
    val status: StateFlow<Status> = _status.asStateFlow()

    @Volatile
    var activeSocksPort: Int? = null

    @Volatile
    var deviceVpnProbe: () -> Boolean = { false }

    fun deviceVpnActive(): Boolean = runCatching { deviceVpnProbe() }.getOrDefault(false)

    private val _activeKind = MutableStateFlow<String?>(null)
    val activeKind: StateFlow<String?> = _activeKind.asStateFlow()

    fun setActiveKind(kind: String?) {
        _activeKind.value = kind
    }

    const val EXTRA_COMMAND = "nrg.command"
    const val EXTRA_CONFIG = "nrg.config"

    const val EXTRA_CONFIG_PATH = "nrg.config.path"
    const val EXTRA_REMARK = "nrg.remark"
    const val EXTRA_SOCKS_PORT = "nrg.socksPort"
    const val EXTRA_KIND = "nrg.kind"

    const val EXTRA_PROXY_ONLY = "nrg.proxyOnly"
    const val CMD_START = "start"
    const val CMD_STOP = "stop"

    const val CMD_START_ACTIVE = "start_active"

    const val KIND_XRAY = "xray"
    const val KIND_PSIPHON = "psiphon"

    const val KIND_DNS_TUNNEL = "dns_tunnel"

    const val KIND_MASTERDNS = "master_dns"

    const val KIND_MHRV = "mhrv_relay"

    const val KIND_TOR = "tor"

    const val KIND_SSH = "ssh"

    const val KIND_SNISPOOF = "sni_spoof"

    const val KIND_OPENCONNECT = "openconnect"


    const val KIND_IKEV2 = "ikev2"

    const val KIND_CROSS_CHAIN = "cross_chain"
    const val KIND_AETHER = "aether"

    const val KIND_SINGBOX = "sing_box"

    const val KIND_SSTP = "sstp"

    // Every transition below is a single atomic update: the status is written from the main thread, the
    // service's worker threads, the metrics loops and binder callbacks at once. A plain read-copy-write lost
    // updates (e.g. a late metrics tick re-publishing "Connected" over a just-published "Idle"), which left the
    // UI showing a live session that no longer existed.

    fun onStarting(remark: String) {
        activeSocksPort = null
        _status.update { prev ->
            Status(
                state = ConnectionState.Connecting,
                serverName = remark,
                sessionId = prev.sessionId + 1,
            )
        }
    }

    fun onStopping(): Boolean {
        while (true) {
            val prev = _status.value
            if (prev.state == ConnectionState.Idle) return false
            if (_status.compareAndSet(prev, prev.copy(state = ConnectionState.Disconnecting))) return true
        }
    }

    fun onConnected(remark: String?) {
        _status.update { prev ->
            // A user stop is already under way: a late "connected" from the engine must not resurrect the session.
            if (prev.state == ConnectionState.Disconnecting) prev
            else prev.copy(
                state = ConnectionState.Connected,
                serverName = remark ?: prev.serverName,
                error = null,
            )
        }
    }

    fun onMetrics(durationSeconds: Int, downBps: Long, upBps: Long, totalDown: Long, totalUp: Long) {
        _status.update { prev ->
            if (prev.state != ConnectionState.Connected) prev
            else prev.copy(
                durationSeconds = durationSeconds,
                downloadBps = downBps,
                uploadBps = upBps,
                totalDownload = totalDown,
                totalUpload = totalUp,
            )
        }
    }

    fun onError(reason: String) {
        _status.update { prev ->
            Status(
                state = ConnectionState.Error,
                error = reason,
                sessionId = prev.sessionId,
            )
        }
    }

    fun onDisconnected() {
        activeSocksPort = null
        _activeKind.value = null
        _status.update { prev ->
            Status(
                state = ConnectionState.Idle,
                serverName = prev.serverName,
                sessionId = prev.sessionId,
            )
        }
    }
}
