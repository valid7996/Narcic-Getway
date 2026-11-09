package dev.valid7996.narcicgeteway.core

import android.content.Context
import dev.valid7996.narcicgeteway.core.AppLog as Log
import dev.valid7996.narcicgeteway.domain.config.WhiteDnsConfig
import dev.valid7996.narcicgeteway.domain.config.WhiteDnsProfile
import dev.valid7996.narcicgeteway.domain.config.WhiteDnsSettings
import dev.valid7996.narcicgeteway.platform.InAppLog
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Runs one WhiteDNS connection: the StormDNS or CottenDNS client that ships with the app as a native
 * executable. It is started as a child process with a generated config, its output goes line by line to the
 * app log under the WhiteDns tag, and its local SOCKS port is what the tunnel is bridged to.
 */
class WhiteDnsController(
    private val context: Context,
    private val profile: WhiteDnsProfile,
    private val settings: WhiteDnsSettings,
    /** The resolvers to scan, one entry per item (IP, IP:port or CIDR). */
    private val resolvers: List<String>,
    private val listenPort: Int,
    private val maxListenPort: Int = listenPort + 9,
    /** The phone network's own DNS servers; only the Soar engine reads them. */
    private val systemResolvers: List<String> = emptyList(),
    private val listenHost: String = "127.0.0.1",
) {
    @Volatile private var process: Process? = null
    @Volatile private var started = false
    @Volatile private var ready = false
    @Volatile private var boundPort = 0
    @Volatile private var tunnelReady = false
    @Volatile private var lastProbeAt = 0L
    @Volatile private var lastProbeOk = true
    private var drain: Thread? = null
    private var configFile: File? = null
    private var resolversFile: File? = null

    sealed interface Failure {
        data class NoUsableResolver(val tried: Int) : Failure

        data class SessionRefused(val usable: Int, val reason: String) : Failure

        data class Stalled(val checked: Int, val total: Int, val usable: Int) : Failure

        data class Exited(val code: Int?) : Failure

        data class Other(val reason: String) : Failure
    }

    @Volatile var failure: Failure? = null
        private set

    @Volatile var stopped = false
        private set

    @Volatile private var scanChecked = 0
    @Volatile private var scanTotal = 0
    @Volatile private var scanUsable = 0
    @Volatile private var noUsableResolver = false
    @Volatile private var sessionFailures = 0
    @Volatile private var sessionError: String? = null
    @Volatile private var lastErrorLine: String? = null
    @Volatile private var soarError: String? = null
    @Volatile private var lastActivityAt = 0L

    val isReady: Boolean get() = ready

    val isRunning: Boolean
        get() {
            if (!started) return false
            if (process?.isAlive != true) return false
            if (!ready) return true
            val now = System.currentTimeMillis()
            if (now - lastProbeAt < PROBE_CACHE_MS) return lastProbeOk
            lastProbeAt = now
            lastProbeOk = portAccepts(boundPort, 800)
            return lastProbeOk
        }

    /** Starts the engine. Returns the local SOCKS port it will open, or -1 with [failure] set. */
    @Synchronized
    fun start(): Int {
        failure = null
        if (stopped) return -1
        val soar = profile.isSoar
        if (!profile.isValid) {
            return fail(if (soar) "domain and a base64 32-byte server public key are required" else "domain and encryption key are required")
        }
        if (resolvers.isEmpty() && !(soar && profile.soar.discovery)) {
            return fail(if (soar) "add resolvers or turn on Soar discovery" else "no resolvers to scan")
        }

        val binary = File(
            context.applicationInfo.nativeLibraryDir,
            when {
                soar -> "libsoar_client.so"
                profile.isCottenDns -> "libcottendns_client.so"
                else -> "libstormdns_client.so"
            },
        )
        if (!binary.exists()) {
            return fail(
                if (soar) {
                    "Soar engine is not in this build (${binary.name}). Build it with tools/build-soar-engine.sh or the android-natives workflow"
                } else {
                    "${profile.engineLabel} engine is missing from this build (${binary.name})"
                },
            )
        }
        if (!binary.canExecute()) return fail("${profile.engineLabel} engine is not executable (${binary.name})")

        val port = firstFreePort(listenPort)
        if (port < 0) return fail("no free local port near $listenPort")

        val config = try {
            if (soar) {
                WhiteDnsConfig.renderSoarJson(profile, listenHost, port, systemResolvers)
            } else {
                WhiteDnsConfig.renderRuntimeToml(profile, settings, listenHost, port)
            }
        } catch (e: IllegalArgumentException) {
            return fail(e.message ?: "invalid configuration")
        }

        val dir = File(context.filesDir, "whitedns/${profile.normalized().engine}").apply { mkdirs() }
        cleanStale(dir)
        val id = UUID.randomUUID().toString()
        val cfg = File(dir, if (soar) ".wd-$id.json" else ".wd-$id.toml")
        val res = File(dir, ".wd-$id.resolvers")
        return try {
            cfg.writeText(config)
            res.writeText(WhiteDnsConfig.renderResolvers(resolvers))
            configFile = cfg
            resolversFile = res
            resetProgress()
            tunnelReady = !profile.usesCottenMax && !soar

            val p = ProcessBuilder(binary.absolutePath, "-config", cfg.absolutePath, "-resolvers", res.absolutePath)
                .directory(dir)
                .redirectErrorStream(true)
                .start()
            process = p
            started = true
            boundPort = port
            startDrain(p)
            note("${profile.engineLabel} started; scanning ${resolvers.size} resolver entries")
            Log.i(TAG, "${profile.engineLabel} started; SOCKS $port opens after the MTU scan and session setup")
            port
        } catch (e: IOException) {
            Log.e(TAG, "${profile.engineLabel} start failed", e)
            cleanFiles()
            fail("could not start ${profile.engineLabel}: ${e.message}")
        } catch (e: SecurityException) {
            cleanFiles()
            fail("could not start ${profile.engineLabel}: ${e.message}")
        }
    }

    /** Waits until the local SOCKS port answers and the tunnel is up. False with [failure] set otherwise. */
    fun awaitReady(idleTimeoutMs: Long, maxTimeoutMs: Long): Boolean {
        val begin = System.currentTimeMillis()
        lastActivityAt = begin
        while (true) {
            if (stopped || !started) return false
            if (tunnelReady && portAccepts(boundPort, 500)) {
                ready = true
                lastProbeAt = System.currentTimeMillis()
                lastProbeOk = true
                Log.i(TAG, "${profile.engineLabel} SOCKS ready on $boundPort")
                note("SOCKS ready on $boundPort")
                return true
            }
            val p = process
            if (p == null || !p.isAlive) {
                failure = failure ?: when {
                    soarError != null -> Failure.Other(soarError.orEmpty())
                    noUsableResolver -> Failure.NoUsableResolver(maxOf(scanTotal, scanChecked))
                    else -> Failure.Exited(exitCodeOrNull(p))
                }
                return false
            }
            if (noUsableResolver) {
                failure = Failure.NoUsableResolver(maxOf(scanTotal, scanChecked))
                return false
            }
            if (sessionFailures >= SESSION_FAILURES_TO_GIVE_UP) {
                failure = Failure.SessionRefused(scanUsable, sessionError.orEmpty())
                return false
            }
            val now = System.currentTimeMillis()
            if (now - lastActivityAt > idleTimeoutMs || now - begin > maxTimeoutMs) {
                failure = if (sessionFailures > 0) {
                    Failure.SessionRefused(scanUsable, sessionError.orEmpty())
                } else {
                    Failure.Stalled(scanChecked, scanTotal, scanUsable)
                }
                return false
            }
            try { Thread.sleep(500) } catch (_: InterruptedException) { return false }
        }
    }

    @Synchronized
    fun stop() {
        stopped = true
        val p = process
        process = null
        started = false
        ready = false
        lastProbeAt = 0L
        lastProbeOk = true
        if (p != null) {
            runCatching { p.destroy() }
            val exited = runCatching { p.waitFor(1_500, TimeUnit.MILLISECONDS) }.getOrDefault(false)
            if (!exited) {
                runCatching { p.destroyForcibly() }
                runCatching { p.waitFor(1_500, TimeUnit.MILLISECONDS) }
            }
        }
        drain?.interrupt()
        drain = null
        boundPort = 0
        cleanFiles()
    }

    private fun startDrain(p: Process) {
        val pid = android.os.Process.myPid()
        drain = Thread({
            try {
                p.inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { raw -> if (raw.isNotBlank()) mirror(raw, pid) }
                }
            } catch (_: IOException) {
                // The stream closes when the process is destroyed on purpose.
            } finally {
                if (!stopped) cleanFiles()
            }
        }, "whitedns-output").apply { isDaemon = true; start() }
    }

    private fun mirror(raw: String, pid: Int) {
        val line = ANSI.replace(raw, "").trim()
        if (line.isEmpty()) return
        val m = FILE_LINE.find(line)
        val message = m?.groupValues?.get(2) ?: line
        val level = when (m?.groupValues?.get(1) ?: levelOf(line)) {
            "DEBUG" -> 'D'
            "WARN" -> 'W'
            "ERROR" -> 'E'
            else -> 'I'
        }
        InAppLog.write(level, LOG_TAG, message, null, pid)
        track(level, message)
    }

    private fun levelOf(line: String): String = when {
        line.startsWith("SOAR_ERROR") || " ERROR" in line || "[ERROR]" in line -> "ERROR"
        " WARN" in line || "[WARN]" in line -> "WARN"
        "[DEBUG]" in line -> "DEBUG"
        else -> "INFO"
    }

    private fun track(level: Char, message: String) {
        lastActivityAt = System.currentTimeMillis()
        PROGRESS.find(message)?.let { match ->
            val fields = FIELD.findAll(match.groupValues[1]).associate { it.groupValues[1] to it.groupValues[2] }
            fields["completed"]?.toIntOrNull()?.let { scanChecked = it }
            fields["total"]?.toIntOrNull()?.let { scanTotal = it }
            fields["valid"]?.toIntOrNull()?.let { scanUsable = it }
            if (fields["phase"].equals("connected", ignoreCase = true)) tunnelReady = true
        }
        SCAN_PROGRESS.find(message)?.let { p ->
            scanChecked = p.groupValues[2].toInt()
            scanTotal = p.groupValues[3].toInt()
            scanUsable = p.groupValues[4].toInt()
        }
        if (message.startsWith("SOAR_READY")) tunnelReady = true
        if (message.startsWith("SOAR_ERROR:")) soarError = message.removePrefix("SOAR_ERROR:").trim().take(200)
        val lower = message.lowercase()
        when {
            NO_RESOLVER.any { it in lower } -> noUsableResolver = true
            "session initialized successfully" in lower -> {
                sessionFailures = 0
                sessionError = null
            }
            else -> SESSION_FAILED.find(message)?.let {
                sessionFailures++
                sessionError = it.groupValues[1].trim().take(160)
            }
        }
        if (level == 'E') lastErrorLine = message.take(200)
    }

    private fun resetProgress() {
        scanChecked = 0
        scanTotal = 0
        scanUsable = 0
        noUsableResolver = false
        sessionFailures = 0
        sessionError = null
        lastErrorLine = null
        soarError = null
        lastActivityAt = System.currentTimeMillis()
        ready = false
    }

    private fun fail(reason: String): Int {
        failure = Failure.Other(reason)
        Log.e(TAG, reason)
        note(reason)
        return -1
    }

    private fun note(message: String) {
        InAppLog.write('I', LOG_TAG, message, null, android.os.Process.myPid())
    }

    private fun exitCodeOrNull(p: Process?): Int? = runCatching { p?.exitValue() }.getOrNull()

    private fun cleanFiles() {
        runCatching { configFile?.delete() }
        runCatching { resolversFile?.delete() }
        configFile = null
        resolversFile = null
    }

    private fun cleanStale(dir: File) {
        val cutoff = System.currentTimeMillis() - STALE_MS
        dir.listFiles()?.forEach { f ->
            if (f.isFile && STALE_FILE.matches(f.name) && f.lastModified() < cutoff) runCatching { f.delete() }
        }
    }

    private fun firstFreePort(from: Int): Int {
        for (p in from..maxListenPort) {
            try {
                ServerSocket().use { it.bind(InetSocketAddress(listenHost, p)); return p }
            } catch (_: Exception) { }
        }
        return -1
    }

    private fun portAccepts(port: Int, timeoutMs: Int): Boolean {
        if (port <= 0) return false
        return try {
            Socket().use { it.connect(InetSocketAddress(listenHost, port), timeoutMs) }
            true
        } catch (_: Exception) {
            false
        }
    }

    private companion object {
        const val TAG = "WhiteDnsController"
        const val LOG_TAG = "WhiteDns"

        val ANSI = Regex("\u001B\\[[;?0-9]*[ -/]*[@-~]")
        val FILE_LINE = Regex("""^\d{4}/\d{2}/\d{2} \d{2}:\d{2}:\d{2} \[(DEBUG|INFO|WARN|ERROR)] ?(.*)$""")
        val PROGRESS = Regex("""WD_PROGRESS\s+(.*)$""")
        val FIELD = Regex("""(\w+)=([^\s]+)""")
        val SCAN_PROGRESS = Regex("""(Accepted|Rejected) \((\d+)/(\d+)\).*?valid=(\d+)""")
        val SESSION_FAILED = Regex("""Session initialization failed:\s*(.*)$""")
        val NO_RESOLVER = listOf(
            "no valid connections found after mtu testing",
            "mtu tests failed: no valid connections",
            "no valid connections after mtu testing",
        )
        val STALE_FILE = Regex("""\.wd-[A-Za-z0-9-]+\.(toml|json|resolvers)""")

        const val SESSION_FAILURES_TO_GIVE_UP = 3
        const val PROBE_CACHE_MS = 5_000L
        const val STALE_MS = 24L * 60L * 60L * 1_000L
    }
}
