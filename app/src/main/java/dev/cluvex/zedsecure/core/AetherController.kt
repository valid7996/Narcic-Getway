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
 * greeting — waited for as long as the core lives, for a scan of a hard network takes minutes —
 * and the output is relayed into the app log. Leftover processes of a killed app are reaped.
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

            // The exit proxy first, so the core's first dials have somewhere to go; direct when it cannot run.
            val upstream = AetherSupport.startExitProxy(profile)

            if (AetherIdentityStore.missingKeys(workDir, profile).isNotEmpty()) {
                LogBus.append("I/aether registering WARP keys for ${profile.protocol}")
                val registered = kotlinx.coroutines.runBlocking {
                    AetherIdentityStore.register(
                        context = context,
                        protocol = profile.protocol,
                        upstreamPort = upstream,
                        workDir = workDir,
                        onOutput = ::relay,
                    )
                }
                if (!registered) {
                    if (!stopped) fail("Aether key registration failed")
                    AetherSupport.stopExitProxy()
                    return
                }
            }

            var arguments = AetherCommands.runArguments(profile, socksPort)
            if (AetherCommands.listenerPortOf(arguments) == null) {
                // A hand-written command may name no listener; the app dials the session port, so the core takes it.
                arguments = arguments + listOf(AetherCommands.BIND, "127.0.0.1:$socksPort")
            }
            if (upstream != null && AetherCommands.UPSTREAM !in arguments) {
                arguments = arguments + listOf(AetherCommands.UPSTREAM, "socks5://127.0.0.1:$upstream")
            }
            LogBus.append("I/aether starting the tunnel on 127.0.0.1:$socksPort")

            val started = try {
                AetherSupport.startProcess(context, arguments, workDir)
            } catch (e: IOException) {
                Log.e(TAG, "aether launch failed", e)
                fail("Aether launch failed: ${e.message}")
                AetherSupport.stopExitProxy()
                return
            }
            process = started

            val watcher = Thread({ watch(started) }, "aether-output").apply { isDaemon = true }
            watcher.start()

            // A tunnel without an endpoint scans first, which takes what the network takes; the
            // wait ends when the listener answers, or when the core gives up on its own.
            var listening = false
            while (!stopped) {
                if (runCatching { started.isAlive }.getOrNull() != true) break
                if (answersSocks()) {
                    listening = true
                    break
                }
                try {
                    Thread.sleep(READY_POLL_MS)
                } catch (_: InterruptedException) {
                    break
                }
            }
            if (listening && !stopped) {
                onEstablished()
            } else if (!stopped) {
                fail("Aether listener did not come up")
            }
        } catch (t: Throwable) {
            Log.e(TAG, "aether session crashed", t)
            if (!stopped) fail("Aether error: ${t.message}")
        }
    }

    private fun watch(process: Process) {
        try {
            process.inputStream.bufferedReader().forEachLine { line -> relay(line) }
        } catch (_: IOException) {
        }
        val code = runCatching { process.waitFor() }.getOrDefault(-1)
        this.process = null
        AetherSupport.stopExitProxy()
        if (stopped) return
        LogBus.append("E/aether the core exited on its own with code $code")
        fail("Aether core stopped (code $code)")
    }

    private fun relay(line: String) {
        LogBus.append("${AetherSupport.levelOf(line.trim())}/aether ${line.trim()}")
    }

    private fun fail(reason: String) {
        onStopped(reason)
    }

    private fun answersSocks(): Boolean = AetherSupport.answersSocks(socksPort)

    companion object {
        private const val TAG = "AetherController"
        private const val READY_POLL_MS = 500L
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

    /** The transports lyrebird speaks, as the core names them. */
    private val torTransports = listOf("obfs4", "snowflake", "webtunnel", "meek_lite", "obfs3", "scramblesuit")

    private const val PSIPHON_OVERLAY = """{"DNSResolverAlternateServers": ["1.1.1.1", "1.0.0.1", "8.8.8.8", "8.8.4.4"]}"""

    fun binary(context: Context): File = File(context.applicationInfo.nativeLibraryDir, BINARY_NAME)

    fun psiphonBinary(context: Context): File = File(context.applicationInfo.nativeLibraryDir, PSIPHON_BINARY_NAME)

    fun transportBinary(context: Context): File = File(context.applicationInfo.nativeLibraryDir, TRANSPORT_BINARY_NAME)

    fun workDir(context: Context): File = File(context.filesDir, "aether")

    fun psiphonDir(context: Context): File = File(context.filesDir, "aether-psiphon")

    /**
     * The environment every core runs with: where the identity files are, the owner of the process,
     * and the helpers of the carriers the device ships.
     */
    fun environment(context: Context, workDir: File, psiphonSession: Boolean): MutableMap<String, String> = mutableMapOf(
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
        builder.environment().putAll(environment(context, workDir, psiphonSession = true))
        return builder.start()
    }

    /** The core's line without its log header, and the level it wrote it at, for the app log. */
    fun logLine(line: String): String = line.trim()

    /** [line] as the app log levels it: the core's header decides, an Error: prefix loudest. */
    fun levelOf(line: String): Char = when {
        line.startsWith("Error:") -> 'E'
        line.startsWith("[") && line.contains("] ") ->
            when (line.substringAfter('[').substringBefore(']').split(' ').getOrNull(1)) {
                "ERROR" -> 'E'
                "WARN" -> 'W'
                "DEBUG", "TRACE" -> 'D'
                else -> 'I'
            }

        else -> 'I'
    }

    /** True when a SOCKS server answers on the loopback [port]: the greeting gets its reply. */
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

    /** A free loopback port, for a listener the app does not dial itself. */
    fun findFreePort(): Int = ServerSocket(0).use { socket ->
        socket.localPort.takeIf { it in 1024..65535 } ?: 10892
    }

    /** The port a scan binds: none, unless Tor around the tunnel comes along and needs a real one. */
    fun scanPort(profile: AetherProfile): Int =
        if (profile.tor == AetherProfile.CARRIER_REVERSE) findFreePort() else 0

    /**
     * Starts the minimal Xray exit proxy that a session core, a scan or a registration dials out
     * through, carrying the profile's finalMask and dialMode; null when it cannot run, and the core
     * dials out directly. The core starts once the proxy listens.
     */
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

    /** A short bounded wait on a background thread, for callers that run outside coroutines. */
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

    /**
     * Kills leftover core processes of this app: any whose owning app process is gone and any still
     * holding [bindAddress], for a survivor on the port would make every later start fail.
     */
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
                Log.w("AetherSupport", "aether: killing a leftover core process, pid=$pid ownerAlive=$ownerAlive holdsBind=$holdsBind")
                runCatching { android.os.Process.killProcess(pid) }
            }
        }
    }

    /** The certificate directories of this device that exist, joined the way Go reads them; null without one. */
    private fun certificateDirectories(): String? = listOf(
        "/apex/com.android.conscrypt/cacerts",
        "/system/etc/security/cacerts",
    ).filter { File(it).isDirectory }.takeIf { it.isNotEmpty() }?.joinToString(":")

    /** The Psiphon overlay in [workDir], written when it is missing or says something else; null when it cannot be written. */
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

/** The WARP key files of the identity folder, ported from PattNG's AetherIdentityManager. */
object AetherIdentityStore {

    const val BASE_FILE = "aether.toml"
    const val MASQUE_FILE = "aether-masque.toml"
    const val MASQUE_INNER_FILE = "aether-masque-secondary.toml"
    const val WIREGUARD_FILE = "aether-wg.toml"
    const val WIREGUARD_INNER_FILE = "aether-wg-secondary.toml"

    private val identityField = Regex("""^(device_id|ipv4|ipv6)\s*=\s*"([^"]*)"$""")

    /** The identity a key file holds: the device id and the addresses WARP gave it. */
    data class AetherIdentity(val deviceId: String, val ipv4: String, val ipv6: String)

    data class KeyEntry(val file: String, val identity: AetherIdentity?)

    fun filesOf(protocol: String): List<String> = when (protocol) {
        AetherProfile.PROTO_MASQUE -> listOf(MASQUE_FILE)
        AetherProfile.PROTO_WG -> listOf(WIREGUARD_FILE)
        AetherProfile.PROTO_GOOL -> listOf(WIREGUARD_FILE, WIREGUARD_INNER_FILE)
        AetherProfile.PROTO_MIM -> listOf(MASQUE_FILE, MASQUE_INNER_FILE)
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

    /** Every key of the identity folder, with the identity it holds. */
    fun keys(context: Context): List<KeyEntry> {
        val workDir = AetherSupport.workDir(context)
        return listOf(WIREGUARD_FILE, WIREGUARD_INNER_FILE, MASQUE_FILE, MASQUE_INNER_FILE).map { name ->
            KeyEntry(name, read(File(workDir, name)))
        }
    }

    /** The key files of the profile's protocol that the identity folder lacks. */
    fun missingKeys(workDir: File, profile: AetherProfile): List<String> =
        filesOf(profile.protocol).filter { read(File(workDir, it)) == null }

    /**
     * Registers the keys of [protocol] with a run of the core on --register, which ends at its word
     * that every key is saved. The run dials out through the exit on [upstreamPort] when one is
     * given, and writes the keys into [workDir]. Keys are registered only while no session runs.
     */
    suspend fun register(
        context: Context,
        protocol: String,
        upstreamPort: Int?,
        workDir: File,
        onOutput: (String) -> Unit,
    ): Boolean = withContext(Dispatchers.IO) {
        var arguments = listOf(AetherCommands.REGISTER, protocol, "--log-level", "info")
        if (upstreamPort != null) {
            arguments = arguments + listOf(AetherCommands.UPSTREAM, "socks5://127.0.0.1:$upstreamPort")
        }
        runUntil(
            context = context,
            arguments = arguments,
            workDir = workDir,
            timeoutMs = REGISTER_TIMEOUT_MS,
            source = "aether-key",
            onOutput = onOutput,
            match = { line -> if (registeredWord.containsMatchIn(line)) line else null },
        ) != null
    }

    /** [arguments] as a core of its own, ended once [match] holds or the timeout passes. */
    internal suspend fun <T> runUntil(
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
            kotlinx.coroutines.delay(300)
        }
        process.destroy()
        runCatching { process.waitFor() }
        AetherSupport.reapStale(context, null)
        return found
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

    /** The core's last word in a registration, once every key it was asked for is saved. */
    private val registeredWord = Regex("""identities ready: \S""")

    private const val REGISTER_TIMEOUT_MS = 4 * 60_000L
}

/** A scan of the core for a reachable endpoint, ported from PattNG's AetherScanner. */
object AetherScanner {

    data class ScanResult(val endpoint: String, val innerHop: String? = null)

    private const val SCAN_TIMEOUT_MS = 8 * 60_000L

    private val masqueGateway = Regex("""selected MASQUE gateway (\S+)""")
    private val wireguardEndpoint = Regex("""selected WireGuard endpoint (\S+)""")
    private val goolHops = Regex("""using cloudflare edge (\S+) \(outer\) and (\S+) \(inner\)""")
    private val mimHops = Regex("""masque-in-masque ready: (\S+) \(outer\) and (\S+) \(inner\)""")
    private val exitAccepted = Regex("""exit location \S+ accepted""")
    private val exitRejected = Regex("""exit location \S+ rejected""")

    /**
     * Runs the core on scan arguments: no endpoint, no quick reconnect, a throwaway listener
     * nothing dials. What ends the scan is the line that names the endpoint, or with an exit rule
     * the line that says the rule accepted it. The run dials out through the exit proxy, which this
     * starts for its own and stops after; null when a session is running, which a scan would
     * disturb, or when nothing was found in time.
     */
    suspend fun scan(
        context: Context,
        profile: AetherProfile,
        onOutput: (String) -> Unit = {},
    ): ScanResult? = withContext(Dispatchers.IO) {
        val status = VpnManager.status.value
        if (status.state.isActive || status.state.isTransitioning) return@withContext null
        val workDir = AetherSupport.workDir(context).apply { mkdirs() }
        AetherIdentityStore.settle(workDir)
        val upstream = AetherSupport.startExitProxy(profile)
        try {
            var arguments = AetherCommands.buildArguments(profile, AetherSupport.scanPort(profile), scan = true)
            if (upstream != null) {
                arguments = arguments + listOf(AetherCommands.UPSTREAM, "socks5://127.0.0.1:$upstream")
            }
            val matcher = matcher(profile, exitRuled = profile.exitLoc.isNotBlank())
            val hit = AetherIdentityStore.runUntil(
                context = context,
                arguments = arguments,
                workDir = workDir,
                timeoutMs = SCAN_TIMEOUT_MS,
                source = "aether-scan",
                onOutput = onOutput,
                match = matcher,
            )
            hit?.let { ScanResult(it.endpoint, it.innerHop) }
        } finally {
            AetherSupport.stopExitProxy()
        }
    }

    /** What ends a scan, line by line; with an exit rule, the endpoint named last and accepted counts. */
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
