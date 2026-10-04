package dev.cluvex.zedsecure.core

import android.content.Context
import android.os.Build
import dev.cluvex.zedsecure.core.AppLog as Log
import dev.cluvex.zedsecure.domain.config.AetherCommands
import dev.cluvex.zedsecure.domain.config.AetherProfile
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Owns the Aether core process of a session, ported from PattNG's AetherCoreManager: the core is
 * the libaether.so shipped in the APK's jniLibs, run as a process with a SOCKS listener on the
 * loopback. Missing WARP keys are registered first, the tunnel starts once the listener answers a
 * SOCKS greeting, and the output is relayed into the app log. Leftover processes of a killed app
 * are reaped, for the core has no parent-death handling of its own.
 */
class AetherController(
    private val context: Context,
    private val profile: AetherProfile,
    private val socksPort: Int,
    private val onEstablished: () -> Unit,
    private val onStopped: (String?) -> Unit,
) {
    @Volatile private var process: Process? = null
    @Volatile private var stopped = false
    @Volatile private var worker: Thread? = null

    val isRunning: Boolean get() = worker?.isAlive == true && !stopped

    fun start(): Boolean {
        val binary = binary()
        if (!binary.canExecute()) {
            LogBus.append("E/aether the Aether core (libaether.so) is missing for this device's ABI")
            onStopped("Aether core binary missing")
            return false
        }
        stopped = false
        val t = Thread({ runSession(binary) }, "aether-worker").apply { isDaemon = true }
        worker = t
        t.start()
        return true
    }

    fun stop() {
        stopped = true
        runCatching { process?.destroy() }
        runCatching { worker?.interrupt() }
    }

    private fun runSession(binary: File) {
        try {
            reapStale()
            val workDir = workDir().apply { mkdirs() }
            AetherIdentityStore.settle(workDir)

            if (missingKeys().isNotEmpty()) {
                LogBus.append("I/aether registering WARP keys for ${profile.protocol}")
                if (!registerKeys(binary, workDir)) {
                    if (!stopped) fail("Aether key registration failed")
                    return
                }
            }

            var arguments = AetherCommands.runArguments(profile, socksPort)
            if (AetherCommands.bindPortOf(arguments) == null) {
                // A hand-written command may name no listener; the app dials the session port, so the core takes it.
                arguments = arguments + listOf(AetherCommands.BIND, "127.0.0.1:$socksPort")
            }
            LogBus.append("I/aether starting the tunnel on 127.0.0.1:$socksPort")

            val builder = ProcessBuilder(listOf(binary.absolutePath) + arguments)
                .directory(workDir)
                .redirectErrorStream(true)
            builder.environment().apply {
                put(OWNER_ENV, android.os.Process.myPid().toString())
                put("HOME", workDir.absolutePath)
                put("TMPDIR", context.cacheDir.absolutePath)
                put("AETHER_CONFIG", File(workDir, AetherIdentityStore.BASE_FILE).absolutePath)
                put("AETHER_MASQUE_CONFIG", File(workDir, AetherIdentityStore.MASQUE_FILE).absolutePath)
                put("AETHER_WG_CONFIG", File(workDir, AetherIdentityStore.WIREGUARD_FILE).absolutePath)
                certificateDirectories()?.let { put(CERT_DIR_ENV, it) }
            }

            val started = try {
                builder.start()
            } catch (e: IOException) {
                Log.e(TAG, "aether launch failed", e)
                fail("Aether launch failed: ${e.message}")
                return
            }
            process = started

            val watcher = Thread({ watch(started) }, "aether-output").apply { isDaemon = true }
            watcher.start()

            val timeoutMs = if (profile.endpointText() == null && !profile.isTwoHops) SCAN_READY_TIMEOUT_MS else READY_TIMEOUT_MS
            if (!awaitListening(timeoutMs)) {
                if (!stopped) fail("Aether listener did not come up")
            }
        } catch (t: Throwable) {
            Log.e(TAG, "aether session crashed", t)
            if (!stopped) fail("Aether error: ${t.message}")
        }
    }

    private fun watch(process: Process) {
        try {
            process.inputStream.bufferedReader().forEachLine { line ->
                relay(line)
            }
        } catch (_: IOException) {
        }
        val code = runCatching { process.waitFor() }.getOrDefault(-1)
        processNoLongerHeld()
        if (stopped) return
        LogBus.append("E/aether the core exited on its own with code $code")
        fail("Aether core stopped (code $code)")
    }

    private fun relay(line: String) {
        val text = line.trim()
        if (text.isEmpty()) return
        val level = when {
            text.startsWith("Error:") -> "E"
            text.startsWith("[") && text.contains("] ") -> when (text.substringAfter('[').substringBefore(']').split(' ').getOrNull(1)) {
                "ERROR" -> "E"
                "WARN" -> "W"
                "DEBUG", "TRACE" -> "D"
                else -> "I"
            }

            else -> "I"
        }
        LogBus.append("$level/aether $text")
    }

    /** Waits until the listener the app dials answers a SOCKS5 greeting. */
    private fun awaitListening(timeoutMs: Long): Boolean {
        val deadline = System.nanoTime() + timeoutMs * 1_000_000
        while (!stopped) {
            if (answersSocks()) {
                if (!stopped) onEstablished()
                return true
            }
            val running = runCatching { process?.isAlive }.getOrNull() ?: true
            if (!running) return false
            if (System.nanoTime() >= deadline) return false
            try {
                Thread.sleep(READY_POLL_MS)
            } catch (_: InterruptedException) {
                return false
            }
        }
        return false
    }

    private fun answersSocks(): Boolean = try {
        Socket().use { socket ->
            socket.connect(InetSocketAddress("127.0.0.1", socksPort), PROBE_TIMEOUT_MS)
            socket.soTimeout = PROBE_TIMEOUT_MS
            socket.getOutputStream().write(byteArrayOf(5, 1, 0))
            socket.getInputStream().read() == 5
        }
    } catch (_: Exception) {
        false
    }

    /** Registers the keys [profile] runs on, ending when the core says every key is saved. */
    private fun registerKeys(binary: File, workDir: File): Boolean {
        val arguments = AetherCommands.registerArguments(profile) + listOf("--log-level", "info")
        val builder = ProcessBuilder(listOf(binary.absolutePath) + arguments)
            .directory(workDir)
            .redirectErrorStream(true)
        builder.environment().apply {
            put(OWNER_ENV, android.os.Process.myPid().toString())
            put("HOME", workDir.absolutePath)
            put("TMPDIR", context.cacheDir.absolutePath)
            put("AETHER_CONFIG", File(workDir, AetherIdentityStore.BASE_FILE).absolutePath)
            put("AETHER_MASQUE_CONFIG", File(workDir, AetherIdentityStore.MASQUE_FILE).absolutePath)
            put("AETHER_WG_CONFIG", File(workDir, AetherIdentityStore.WIREGUARD_FILE).absolutePath)
            certificateDirectories()?.let { put(CERT_DIR_ENV, it) }
        }
        val registered = Regex("""identities ready: \S""")
        val started = try {
            builder.start()
        } catch (e: IOException) {
            Log.e(TAG, "aether register launch failed", e)
            return false
        }
        val reader = started.inputStream.bufferedReader()
        while (true) {
            val line = try {
                reader.readLine() ?: break
            } catch (_: IOException) {
                break
            }
            relay(line)
            if (registered.containsMatchIn(line)) return true
        }
        runCatching { started.waitFor() }
        return false
    }

    /** The key files of the profile's protocol that the identity folder lacks. */
    private fun missingKeys(): List<String> =
        AetherIdentityStore.filesOf(profile.protocol).filter { !AetherIdentityStore.reads(File(workDir(), it)) }

    private fun fail(reason: String) {
        onStopped(reason)
    }

    private fun processNoLongerHeld() {
        process = null
    }

    private fun binary(): File = File(context.applicationInfo.nativeLibraryDir, BINARY_NAME)

    private fun workDir(): File = File(context.filesDir, WORK_DIR)

    /** The certificate directories of this device that exist, joined the way Go reads them; null without one. */
    private fun certificateDirectories(): String? = listOf(
        "/apex/com.android.conscrypt/cacerts",
        "/system/etc/security/cacerts",
    ).filter { File(it).isDirectory }.takeIf { it.isNotEmpty() }?.joinToString(":")

    /**
     * Kills leftover core processes of this app: any whose owning app process is gone and any still
     * holding this session's listener address, for a survivor on the port would make every later
     * start fail until a reboot.
     */
    private fun reapStale() {
        val binaryPath = binary().absolutePath
        val procDir = File("/proc")
        val entries = procDir.listFiles() ?: return
        val bindAddress = "127.0.0.1:$socksPort"
        for (entry in entries) {
            val pid = entry.name.toIntOrNull() ?: continue
            val argv = readNulSeparated(File(entry, "cmdline")) ?: continue
            if (argv.firstOrNull() != binaryPath) continue
            val environ = readNulSeparated(File(entry, "environ"))
            val owner = environ?.firstOrNull { it.startsWith("$OWNER_ENV=") }?.substringAfter('=')?.toIntOrNull()
            val ownerAlive = owner?.let { File(procDir, it.toString()).isDirectory }
            val holdsBind = argv.contains(bindAddress)
            if (ownerAlive == false || holdsBind) {
                Log.w(TAG, "aether: killing a leftover core process, pid=$pid ownerAlive=$ownerAlive holdsBind=$holdsBind")
                runCatching { android.os.Process.killProcess(pid) }
            }
        }
    }

    private fun readNulSeparated(file: File): List<String>? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            java.nio.file.Files.readAllBytes(file.toPath()).toString(Charsets.UTF_8).split('\u0000').filter { it.isNotEmpty() }
        } else {
            file.readBytes().toString(Charsets.UTF_8).split('\u0000').filter { it.isNotEmpty() }
        }
    } catch (_: Exception) {
        null
    }

    companion object {
        private const val TAG = "AetherController"
        private const val BINARY_NAME = "libaether.so"
        private const val WORK_DIR = "aether"
        private const val OWNER_ENV = "NARCIC_AETHER_OWNER"
        private const val CERT_DIR_ENV = "SSL_CERT_DIR"

        private const val READY_POLL_MS = 500L
        private const val PROBE_TIMEOUT_MS = 1_000
        private const val READY_TIMEOUT_MS = 60_000L

        /** A tunnel without an endpoint scans first, which can take minutes on a hard network. */
        private const val SCAN_READY_TIMEOUT_MS = 240_000L
    }
}

/** The WARP key files of the identity folder, ported from PattNG's AetherIdentityManager. */
object AetherIdentityStore {
    const val BASE_FILE = "aether.toml"
    const val MASQUE_FILE = "aether-masque.toml"
    const val MASQUE_INNER_FILE = "aether-masque-secondary.toml"
    const val WIREGUARD_FILE = "aether-wg.toml"
    const val WIREGUARD_INNER_FILE = "aether-wg-secondary.toml"

    private val identityField = Regex("""^(device_id|ipv4|ipv6)\s*=\s*"([^"]*)"$""")

    fun filesOf(protocol: String): List<String> = when (protocol) {
        AetherProfile.PROTO_MASQUE -> listOf(MASQUE_FILE)
        AetherProfile.PROTO_WG -> listOf(WIREGUARD_FILE)
        AetherProfile.PROTO_GOOL -> listOf(WIREGUARD_FILE, WIREGUARD_INNER_FILE)
        AetherProfile.PROTO_MIM -> listOf(MASQUE_FILE, MASQUE_INNER_FILE)
        else -> listOf(WIREGUARD_FILE)
    }

    /** Whether [file] reads as a key: it holds a device_id line. */
    fun reads(file: File): Boolean = try {
        file.isFile && parse(file.readText())
    } catch (_: Exception) {
        false
    }

    private fun parse(text: String): Boolean = text.lineSequence().any { line ->
        identityField.matchEntire(line.trim()) != null
    }

    /**
     * Moves the keys of an interrupted renewal into place, ported from PattNG's settle: a renewal
     * marks its folder ready once every key is there, and the keys take effect from that mark.
     */
    fun settle(workDir: File) {
        val renewalDir = File(workDir.parentFile, "aether-renewal")
        if (!File(renewalDir, "ready").isFile) return
        workDir.mkdirs()
        for (name in listOf(WIREGUARD_FILE, WIREGUARD_INNER_FILE, MASQUE_FILE, MASQUE_INNER_FILE)) {
            val renewed = File(renewalDir, name)
            if (renewed.isFile) runCatching { renewed.renameTo(File(workDir, name)) }
        }
        renewalDir.deleteRecursively()
    }
}
