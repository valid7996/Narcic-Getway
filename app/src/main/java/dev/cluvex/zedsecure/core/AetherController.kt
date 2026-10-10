package dev.cluvex.zedsecure.core

import android.content.Context
import android.os.Build
import kotlin.concurrent.Volatile
import dev.cluvex.zedsecure.core.AppLog as Log
import dev.cluvex.zedsecure.domain.config.AetherCommands
import dev.cluvex.zedsecure.domain.config.AetherExitJson
import dev.cluvex.zedsecure.domain.config.AetherProfile
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Owns the Aether core process of a session, ported from PattNG: the core is the libaether.so
 * shipped in the APK's jniLibs, run as a process with a SOCKS listener on the loopback. A minimal
 * Xray exit proxy runs beside it, and the core dials out through it, so the profile's finalMask and
 * dialMode apply and a carrier of its own keeps working; without Xray the core dials out directly.
 * Missing WARP keys are registered first, the tunnel starts once the listener answers a SOCKS
 * greeting, and the output is relayed into the app log. Leftover processes of a killed app are reaped.
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
        val binary = AetherSupport.binary(context)
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
            AetherSupport.reapStale(context, "127.0.0.1:$socksPort")
            val workDir = AetherSupport.workDir(context).apply { mkdirs() }
            AetherIdentityStore.settle(workDir)

            val missing = AetherIdentityStore.missingKeys(workDir, profile)
            if (missing.isNotEmpty()) {
                val upstream = AetherSupport.startExitProxy(profile)
                try {
                    LogBus.append("I/aether registering missing WARP keys: ${missing.joinToString()}")
                    val registered = AetherIdentityStore.registerBlocking(
                        context = context,
                        protocol = profile.protocol,
                        upstreamPort = upstream,
                        workDir = workDir,
                        onOutput = ::relayOutput,
                    )
                    if (!registered) {
                        onStopped("WARP key registration failed")
                        return
                    }
                } finally {
                    AetherSupport.stopExitProxy()
                }
            }

            var arguments = AetherCommands.runArguments(profile, socksPort)
            val upstream = if (AetherCommands.UPSTREAM !in arguments) {
                AetherSupport.startExitProxy(profile)
            } else {
                null
            }
            if (upstream != null) {
                arguments = arguments + listOf(AetherCommands.UPSTREAM, "socks5://127.0.0.1:$upstream")
            }

            val p = try {
                AetherSupport.startProcess(context, arguments, workDir)
            } catch (e: Exception) {
                Log.e("AetherController", "failed to spawn core", e)
                AetherSupport.stopExitProxy()
                onStopped(e.message)
                return
            }
            process = p

            val reader = Thread({
                try {
                    p.inputStream.bufferedReader().forEachLine { line ->
                        relayOutput(line)
                    }
                } catch (_: IOException) {
                }
            }, "aether-log").apply { isDaemon = true }
            reader.start()

            val deadline = System.currentTimeMillis() + START_TIMEOUT_MS
            var ready = false
            while (!stopped && System.currentTimeMillis() < deadline) {
                if (answersSocks()) {
                    ready = true
                    break
                }
                if (!p.isAlive) break
                Thread.sleep(POLL_INTERVAL_MS)
            }

            if (!ready || stopped) {
                p.destroy()
                AetherSupport.stopExitProxy()
                onStopped(if (stopped) null else "Aether core listener did not respond within ${START_TIMEOUT_MS / 1000}s")
                return
            }

            onEstablished()

            p.waitFor()
            AetherSupport.stopExitProxy()
            if (!stopped) onStopped("Aether core exited unexpectedly with code ${p.exitValue()}")
        } catch (t: Throwable) {
            Log.e("AetherController", "worker threw", t)
            AetherSupport.stopExitProxy()
            if (!stopped) onStopped(t.message)
        }
    }

    private fun relayOutput(line: String) {
        val clean = line.trim()
        if (clean.isNotEmpty()) {
            LogBus.append("${AetherSupport.levelOf(clean)}/aether $clean")
        }
    }

    private fun answersSocks(): Boolean = AetherSupport.answersSocks(socksPort)

    companion object {
        private const val START_TIMEOUT_MS = 60_000L
        private const val POLL_INTERVAL_MS = 300L
    }
}

/** Shared process plumbing of the Aether core, used by the session, the scanner and the identity store. */
object AetherSupport {

    const val BINARY_NAME = "libaether.so"
    const val PSIPHON_BINARY_NAME = "libpsiphon-tunnel-core.so"
    const val TRANSPORT_BINARY_NAME = "liblyrebird.so"

    private const val OWNER_ENV = "NARCIC_AETHER_OWNER"
    private const val CERT_DIR_ENV = "SSL_CERT_DIR"
    const val PSIPHON_BIN_ENV = "AETHER_PSIPHON_BIN"
    const val TOR_PT_ENV = "AETHER_TOR_PT"
    const val PSIPHON_DIR_ENV = "AETHER_PSIPHON_DIR"
    const val PSIPHON_CONFIG_ENV = "AETHER_PSIPHON_CONFIG"

    private val torTransports = listOf("obfs4", "snowflake", "webtunnel", "meek_lite", "obfs3", "scramblesuit")

    private const val PSIPHON_OVERLAY = """{"DNSResolverAlternateServers": ["1.1.1.1", "1.0.0.1", "8.8.8.8", "8.8.4.4"]}"""

    fun binary(context: Context): File = File(context.applicationInfo.nativeLibraryDir, BINARY_NAME)

    fun psiphonBinary(context: Context): File = File(context.applicationInfo.nativeLibraryDir, PSIPHON_BINARY_NAME)

    fun transportBinary(context: Context): File = File(context.applicationInfo.nativeLibraryDir, TRANSPORT_BINARY_NAME)

    fun isCoreSupported(context: Context): Boolean = binary(context).canExecute()

    fun isPsiphonSupported(context: Context): Boolean = psiphonBinary(context).canExecute()

    fun isTorTransportsSupported(context: Context): Boolean = transportBinary(context).canExecute()

    fun workDir(context: Context): File = File(context.filesDir, "aether")

    fun psiphonDir(context: Context): File = File(context.filesDir, "aether-psiphon")

    fun environment(context: Context, workDir: File): MutableMap<String, String> = mutableMapOf(
        "HOME" to workDir.absolutePath,
        "TMPDIR" to context.cacheDir.absolutePath,
        "AETHER_CONFIG" to File(workDir, AetherIdentityStore.BASE_FILE).absolutePath,
        "AETHER_MASQUE_CONFIG" to File(workDir, AetherIdentityStore.MASQUE_FILE).absolutePath,
        "AETHER_WG_CONFIG" to File(workDir, AetherIdentityStore.WIREGUARD_FILE).absolutePath,
        OWNER_ENV to android.os.Process.myPid().toString(),
        PSIPHON_DIR_ENV to psiphonDir(context).apply { mkdirs() }.absolutePath,
    ).apply {
        certificateDirectories()?.let { put(CERT_DIR_ENV, it) }
        psiphonBinary(context).takeIf { it.canExecute() }?.let { put(PSIPHON_BIN_ENV, it.absolutePath) }
        transportBinary(context).takeIf { it.canExecute() }?.let { transport ->
            put(TOR_PT_ENV, torTransports.joinToString(";") { "$it=${transport.absolutePath}" })
        }
        psiphonOverlay(workDir)?.let { put(PSIPHON_CONFIG_ENV, it.absolutePath) }
    }

    fun startProcess(context: Context, arguments: List<String>, workDir: File): Process {
        val builder = ProcessBuilder(listOf(binary(context).absolutePath) + arguments)
            .directory(workDir)
            .redirectErrorStream(true)
        builder.environment().putAll(environment(context, workDir))
        return builder.start()
    }

    fun levelOf(line: String): Char = when {
        line.startsWith("Error:") || line.startsWith("E/") -> 'E'
        line.startsWith("Warn:") || line.startsWith("W/") -> 'W'
        line.startsWith("[") && line.contains("] ") ->
            when (line.substringAfter('[').substringBefore(']').split(' ').getOrNull(1)) {
                "ERROR" -> 'E'
                "WARN" -> 'W'
                "DEBUG", "TRACE" -> 'D'
                else -> 'I'
            }

        else -> 'I'
    }

    fun answersSocks(port: Int): Boolean = try {
        Socket().use { socket ->
            socket.connect(InetSocketAddress("127.0.0.1", port), PROBE_TIMEOUT_MS)
            socket.soTimeout = PROBE_TIMEOUT_MS
            socket.getOutputStream().write(byteArrayOf(5, 1, 0))
            socket.getInputStream().read() == 5
        }
    } catch (_: Exception) {
        false
    }

    fun findFreePort(): Int = ServerSocket(0).use { socket ->
        socket.localPort.takeIf { it in 1024..65535 } ?: 10892
    }

    fun scanPort(profile: AetherProfile): Int =
        if (profile.tor == AetherProfile.CARRIER_REVERSE) findFreePort() else 0

    @Volatile private var exitRunning = false

    fun startExitProxy(profile: AetherProfile): Int? {
        if (exitRunning) return AetherExitJson.EXIT_SOCKS_PORT
        val config = AetherExitJson.exitConfig(profile.finalMask, profile.dialMode)
        val ok = try {
            XrayController.start(config)
        } catch (t: Throwable) {
            Log.w("AetherSupport", "aether: the exit proxy failed to start", t)
            false
        }
        if (!ok || !runBlockingWait { answersSocks(AetherExitJson.EXIT_SOCKS_PORT) }) {
            LogBus.append("W/aether the exit proxy did not come up; the core dials out directly")
            runCatching { XrayController.stop() }
            return null
        }
        exitRunning = true
        LogBus.append("I/aether the exit proxy is up on 127.0.0.1:${AetherExitJson.EXIT_SOCKS_PORT}")
        return AetherExitJson.EXIT_SOCKS_PORT
    }

    fun stopExitProxy() {
        if (exitRunning) {
            exitRunning = false
            runCatching { XrayController.stop() }
        }
    }

    private fun runBlockingWait(probe: () -> Boolean): Boolean {
        var ok = false
        val deadline = System.nanoTime() + EXIT_START_TIMEOUT_MS * 1_000_000
        while (System.nanoTime() < deadline) {
            if (probe()) {
                ok = true
                break
            }
            try {
                Thread.sleep(READY_POLL_MS)
            } catch (_: InterruptedException) {
                break
            }
        }
        return ok
    }

    fun reapStale(context: Context, bindAddress: String?) {
        val binaryPath = binary(context).absolutePath
        val procDir = File("/proc")
        val entries = procDir.listFiles() ?: return
        for (entry in entries) {
            val pid = entry.name.toIntOrNull() ?: continue
            val argv = readNulSeparated(File(entry, "cmdline")) ?: continue
            if (argv.firstOrNull() != binaryPath) continue
            val environ = readNulSeparated(File(entry, "environ"))
            val owner = environ?.firstOrNull { it.startsWith("$OWNER_ENV=") }?.substringAfter('=')?.toIntOrNull()
            val ownerAlive = owner?.let { File(procDir, it.toString()).isDirectory }
            val holdsBind = bindAddress != null && argv.contains(bindAddress)
            if (ownerAlive == false || holdsBind) {
                Log.w("AetherSupport", "aether: killing leftover core process, pid=$pid ownerAlive=$ownerAlive holdsBind=$holdsBind")
                runCatching { android.os.Process.killProcess(pid) }
            }
        }
    }

    private fun certificateDirectories(): String? = listOf(
        "/apex/com.android.conscrypt/cacerts",
        "/system/etc/security/cacerts",
    ).filter { File(it).isDirectory }.takeIf { it.isNotEmpty() }?.joinToString(":")

    private fun psiphonOverlay(workDir: File): File? = try {
        File(workDir, "psiphon-overlay.json").apply { if (!isFile || readText() != PSIPHON_OVERLAY) writeText(PSIPHON_OVERLAY) }
    } catch (_: IOException) {
        null
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

    private const val READY_POLL_MS = 500L
    private const val PROBE_TIMEOUT_MS = 1_000
    private const val EXIT_START_TIMEOUT_MS = 10_000L
}

/** Handles reading and registering WARP identity keys via the Rust core. */
object AetherIdentityStore {

    const val BASE_FILE = "aether.toml"
    const val MASQUE_FILE = "aether-masque.toml"
    const val MASQUE_INNER_FILE = "aether-masque-secondary.toml"
    const val WIREGUARD_FILE = "aether-wg.toml"
    const val WIREGUARD_INNER_FILE = "aether-wg-secondary.toml"

    private val identityField = Regex("""^(device_id|ipv4|ipv6)\s*=\s*"([^"]*)"$""")

    data class AetherIdentity(val deviceId: String, val ipv4: String, val ipv6: String)
    data class KeyEntry(val file: String, val identity: AetherIdentity?)

    fun filesOf(protocol: String): List<String> = when (protocol) {
        AetherProfile.PROTO_MASQUE -> listOf(MASQUE_FILE)
        AetherProfile.PROTO_WG -> listOf(WIREGUARD_FILE)
        AetherProfile.PROTO_GOOL -> listOf(WIREGUARD_FILE, WIREGUARD_INNER_FILE)
        AetherProfile.PROTO_MIM -> listOf(MASQUE_FILE, MASQUE_INNER_FILE)
        AetherProfile.PROTO_WG_OVER_MASQUE -> listOf(WIREGUARD_FILE, MASQUE_FILE)
        else -> listOf(WIREGUARD_FILE)
    }

    fun read(file: File): AetherIdentity? = try {
        if (file.isFile) parse(file.readText()) else null
    } catch (_: Exception) {
        null
    }

    private fun parse(text: String): AetherIdentity? {
        val fields = mutableMapOf<String, String>()
        for (raw in text.lineSequence()) {
            identityField.matchEntire(raw.trim())?.destructured?.let { (key, value) ->
                fields.putIfAbsent(key, value)
            }
        }
        val deviceId = fields["device_id"]?.takeIf { it.isNotBlank() } ?: return null
        return AetherIdentity(deviceId, fields["ipv4"].orEmpty(), fields["ipv6"].orEmpty())
    }

    fun keys(context: Context): List<KeyEntry> {
        val workDir = AetherSupport.workDir(context)
        return listOf(WIREGUARD_FILE, WIREGUARD_INNER_FILE, MASQUE_FILE, MASQUE_INNER_FILE).map { name ->
            KeyEntry(name, read(File(workDir, name)))
        }
    }

    fun missingKeys(workDir: File, profile: AetherProfile): List<String> =
        filesOf(profile.protocol).filter { read(File(workDir, it)) == null }

    suspend fun register(
        context: Context,
        protocol: String,
        upstreamPort: Int?,
        workDir: File,
        onOutput: (String) -> Unit,
    ): Boolean = withContext(Dispatchers.IO) {
        registerBlocking(context, protocol, upstreamPort, workDir, onOutput)
    }

    fun registerBlocking(
        context: Context,
        protocol: String,
        upstreamPort: Int?,
        workDir: File,
        onOutput: (String) -> Unit,
    ): Boolean {
        var arguments = listOf(AetherCommands.REGISTER, protocol, "--log-level", "info")
        if (upstreamPort != null) {
            arguments = arguments + listOf(AetherCommands.UPSTREAM, "socks5://127.0.0.1:$upstreamPort")
        }
        return runUntil(
            context = context,
            arguments = arguments,
            workDir = workDir,
            timeoutMs = REGISTER_TIMEOUT_MS,
            source = "aether-key",
            onOutput = onOutput,
            match = { line -> if (registeredWord.containsMatchIn(line)) line else null },
        ) != null
    }

    internal fun <T> runUntil(
        context: Context,
        arguments: List<String>,
        workDir: File,
        timeoutMs: Long,
        source: String,
        onOutput: (String) -> Unit,
        match: (String) -> T?,
    ): T? {
        AetherSupport.reapStale(context, null)
        val process = try {
            AetherSupport.startProcess(context, arguments, workDir)
        } catch (e: IOException) {
            Log.w("AetherIdentity", "$source: failed to launch", e)
            return null
        }
        var found: T? = null
        val output = Thread({
            try {
                process.inputStream.bufferedReader().forEachLine { line ->
                    val text = line.trim()
                    if (text.isNotEmpty()) {
                        onOutput(text)
                        val hit = match(text)
                        if (hit != null && found == null) found = hit
                    }
                }
            } catch (_: IOException) {
            }
        }, "$source-output").apply { isDaemon = true }
        output.start()
        val deadline = System.nanoTime() + timeoutMs * 1_000_000
        while (found == null && System.nanoTime() < deadline) {
            if (runCatching { process.isAlive }.getOrNull() != true) {
                output.join(1_000)
                break
            }
            try {
                Thread.sleep(200)
            } catch (_: InterruptedException) {
                break
            }
        }
        process.destroy()
        runCatching { process.waitFor() }
        AetherSupport.reapStale(context, null)
        return found
    }

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

    private val registeredWord = Regex("""identities ready: \S""")
    private const val REGISTER_TIMEOUT_MS = 4 * 60_000L
}

/** Runs the Aether scanner using libaether.so with full PattNG regex matching. */
object AetherScanner {

    data class ScanResult(val endpoint: String, val innerHop: String? = null)

    private const val SCAN_TIMEOUT_MS = 8 * 60_000L

    private val masqueGateway = Regex("""selected MASQUE gateway (\S+)""")
    private val wireguardEndpoint = Regex("""selected WireGuard endpoint (\S+)""")
    private val goolHops = Regex("""using cloudflare edge (\S+) \(outer\) and (\S+) \(inner\)""")
    private val mimHops = Regex("""masque-in-masque ready: (\S+) \(outer\) and (\S+) \(inner\)""")
    private val exitAccepted = Regex("""exit location \S+ accepted""")
    private val exitRejected = Regex("""exit location \S+ rejected""")

    @Volatile private var activeProcess: Process? = null

    fun cancel() {
        runCatching { activeProcess?.destroy() }
    }

    suspend fun scan(
        context: Context,
        profile: AetherProfile,
        onOutput: (String) -> Unit = {},
    ): ScanResult? = withContext(Dispatchers.IO) {
        val workDir = AetherSupport.workDir(context).apply { mkdirs() }
        AetherIdentityStore.settle(workDir)
        val upstream = AetherSupport.startExitProxy(profile)
        try {
            var arguments = AetherCommands.buildArguments(profile, AetherSupport.scanPort(profile), scan = true)
            if (upstream != null) {
                arguments = arguments + listOf(AetherCommands.UPSTREAM, "socks5://127.0.0.1:$upstream")
            }
            val matcher = matcher(profile, exitRuled = profile.exitLoc.isNotBlank())
            AetherSupport.reapStale(context, null)
            val p = try {
                AetherSupport.startProcess(context, arguments, workDir)
            } catch (e: Exception) {
                onOutput("E/aether-scan failed to launch scanner: ${e.message}")
                return@withContext null
            }
            activeProcess = p
            var found: ScanResult? = null
            val reader = Thread({
                try {
                    p.inputStream.bufferedReader().forEachLine { line ->
                        val text = line.trim()
                        if (text.isNotEmpty()) {
                            onOutput(text)
                            val hit = matcher(text)
                            if (hit != null && found == null) found = hit
                        }
                    }
                } catch (_: IOException) {
                }
            }, "scan-reader").apply { isDaemon = true }
            reader.start()

            val deadline = System.currentTimeMillis() + SCAN_TIMEOUT_MS
            while (found == null && System.currentTimeMillis() < deadline && p.isAlive) {
                Thread.sleep(250)
            }
            p.destroy()
            runCatching { p.waitFor() }
            activeProcess = null
            AetherSupport.reapStale(context, null)
            found
        } finally {
            AetherSupport.stopExitProxy()
        }
    }

    internal fun matcher(profile: AetherProfile, exitRuled: Boolean): (String) -> ScanResult? {
        val plain = { line: String -> parse(profile, line) }
        if (!exitRuled || profile.protocol == AetherProfile.PROTO_MIM) return plain
        var named: ScanResult? = null
        return { line ->
            val found = parse(profile, line)
            when {
                found != null -> {
                    named = found
                    null
                }
                exitAccepted.containsMatchIn(line) -> named
                exitRejected.containsMatchIn(line) -> {
                    named = null
                    null
                }
                else -> null
            }
        }
    }

    fun parse(profile: AetherProfile, line: String): ScanResult? = when (profile.protocol) {
        AetherProfile.PROTO_GOOL -> hopsOf(goolHops, line)
        AetherProfile.PROTO_MIM -> hopsOf(mimHops, line)
        AetherProfile.PROTO_MASQUE -> masqueGateway.find(line)?.let { ScanResult(it.groupValues[1]) }
        else -> wireguardEndpoint.find(line)?.let { ScanResult(it.groupValues[1]) }
    }

    private fun hopsOf(hops: Regex, line: String): ScanResult? = hops.find(line)?.let { match ->
        val outer = match.groupValues[1]
        val inner = match.groupValues[2]
        if (outer.isNotEmpty() && inner.isNotEmpty()) ScanResult(outer, inner) else null
    }
}
