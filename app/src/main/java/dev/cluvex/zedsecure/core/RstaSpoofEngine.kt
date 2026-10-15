package dev.cluvex.zedsecure.core

import com.rstagit.androidasdd.core.protocol.GoNativeBridge
import dev.cluvex.zedsecure.core.AppLog as Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

/**
 * The no-root SNI spoof: the Rsta engine, a Go library (libsnispf.so, through the JNI bridge of libandroidasdd_jni.so) that
 * listens on a loopback port and forwards each connection to the real server address, splitting and disguising the first
 * packets of the TLS handshake so that the server name a filter reads is the fake one. It needs no root: it is plain
 * sockets with the splitting of the ClientHello, the way the Rsta app uses it.
 *
 * A session is one listener; several can run at once on different ports (the real ping starts one for each profile it measures).
 */
object RstaSpoofEngine {

    private const val TAG = "SniSpoof"
    private const val BIND_TIMEOUT_MS = 4_000L

    private val sessions = ConcurrentHashMap<Long, String>()

    @Volatile private var poller: Thread? = null

    /** One ping at a time: the engine's log is one queue, and the sessions of a batch would answer for each other. */
    private val probeLock = Mutex()

    fun isAvailable(): Boolean = try {
        GoNativeBridge.isAvailable()
    } catch (_: Throwable) {
        false
    }

    fun loadError(): String? = try {
        GoNativeBridge.getLoadError()?.message
    } catch (_: Throwable) {
        null
    }

    /** Starts a listener on [listenPort] that forwards to [ip]:[port] with [fakeSni] and [method]; 0 when it did not start. */
    @Synchronized
    fun start(listenPort: Int, ip: String, port: Int, fakeSni: String, method: String): Long {
        if (!isAvailable()) {
            Log.e(TAG, "engine unavailable: ${loadError() ?: "unknown"}")
            return 0L
        }
        return try {
            val remote = if (ip.contains(':') && !ip.startsWith("[")) "[$ip]:$port" else "$ip:$port"
            val id = GoNativeBridge.spfStart(listenPort, remote, fakeSni, method)
            if (id == 0L) {
                Log.e(TAG, "spfStart returned 0: port $listenPort may be in use")
            } else {
                sessions[id] = "$listenPort -> $remote"
                LogBus.append("I/sni-spoof started: 127.0.0.1:$listenPort -> $remote (sni=$fakeSni, method=$method)")
                startPolling()
            }
            id
        } catch (t: Throwable) {
            Log.e(TAG, "failed to start", t)
            0L
        }
    }

    @Synchronized
    fun stop(id: Long) {
        if (id == 0L) return
        sessions.remove(id)
        try {
            GoNativeBridge.spfStop(id)
        } catch (t: Throwable) {
            Log.e(TAG, "failed to stop cleanly", t)
        }
    }

    /** True once something answers on [port]; waits up to [BIND_TIMEOUT_MS] for the listener to come up. */
    fun awaitListener(port: Int): Boolean {
        val deadline = System.currentTimeMillis() + BIND_TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            val ok = runCatching { Socket().use { it.connect(InetSocketAddress("127.0.0.1", port), 500); true } }.getOrDefault(false)
            if (ok) return true
            try { Thread.sleep(150) } catch (_: InterruptedException) { return false }
        }
        return false
    }

    /**
     * The delay of a profile, really measured: a session on a free port to the real server address, the block
     * run against it (it points the profile's proxy at that port and times a request through the whole chain), and
     * the session closed again. -1 when the engine did not come up.
     */
    suspend fun probe(ip: String, port: Int, fakeSni: String, method: String, block: suspend (Int) -> Long): Long =
        probeLock.withLock {
            withContext(Dispatchers.IO) {
                if (!isAvailable()) return@withContext -1L
                val listen = AetherSupport.findFreePort()
                val id = start(listen, ip, port, fakeSni, method)
                if (id == 0L) return@withContext -1L
                try {
                    if (awaitListener(listen)) block(listen) else -1L
                } finally {
                    stop(id)
                }
            }
        }

    private val logBuffer = java.util.ArrayDeque<String>()
    private val logLock = Any()
    private const val LOG_BUFFER_MAX_LINES = 200

    val isRunning: Boolean
        get() = sessions.isNotEmpty()

    fun statusSummary(): String {
        if (!isAvailable()) return "unavailable: ${loadError() ?: "native bridge failed"}"
        return if (isRunning) {
            "active (${sessions.values.joinToString(", ")})"
        } else {
            "ready / inactive"
        }
    }

    fun recentLogLines(): List<String> = synchronized(logLock) { logBuffer.toList() }

    private fun appendLog(line: String) {
        synchronized(logLock) {
            logBuffer.addLast(line)
            while (logBuffer.size > LOG_BUFFER_MAX_LINES) {
                logBuffer.removeFirst()
            }
        }
    }

    private fun startPolling() {
        if (poller?.isAlive == true) return
        poller = Thread({
            while (sessions.isNotEmpty()) {
                val line = try { GoNativeBridge.spfPollLog() } catch (_: Throwable) { null }
                if (line.isNullOrBlank()) {
                    try { Thread.sleep(300) } catch (_: InterruptedException) { return@Thread }
                } else {
                    val clean = line.trim()
                    appendLog(clean)
                    val level = if (clean.startsWith("[ERR]")) "E" else "I"
                    LogBus.append("$level/sni-spoof $clean")
                }
            }
        }, "rsta-spoof-log").apply { isDaemon = true; start() }
    }
}
