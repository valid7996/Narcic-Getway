package dev.valid7996.narcicgeteway.core

import android.content.Context
import dev.valid7996.narcicgeteway.core.AppLog as Log
import dev.valid7996.narcicgeteway.domain.config.WhiteDnsProfile
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class WhiteDnsController(
    private val context: Context,
    private val profile: WhiteDnsProfile,
    val socksPort: Int,
) {
    @Volatile private var process: Process? = null
    @Volatile private var stopped = false
    @Volatile private var drainThread: Thread? = null

    val isRunning: Boolean
        get() = process?.isAlive == true && !stopped

    fun start(): Boolean {
        stop()
        stopped = false
        return try {
            val runtimeDir = File(context.filesDir, "whitedns").apply { mkdirs() }
            val executable = prepareExecutable(profile.engine)

            val isSoar = profile.engine.equals(WhiteDnsProfile.ENGINE_SOAR, ignoreCase = true)
            val configFile = File(runtimeDir, if (isSoar) "config_soar.json" else "config_${profile.engine}.toml")
            configFile.writeText(profile.toConfigFile(port = socksPort))

            val resolversFile = File(runtimeDir, "resolvers_${profile.engine}.txt")
            prepareResolvers(resolversFile)

            Log.i("WhiteDNS", "Starting WhiteDNS engine ${profile.engine} on SOCKS5 127.0.0.1:$socksPort")

            val pb = if (isSoar) {
                ProcessBuilder(
                    executable.absolutePath,
                    "-config", configFile.absolutePath,
                )
            } else {
                ProcessBuilder(
                    executable.absolutePath,
                    "-config", configFile.absolutePath,
                    "-resolvers", resolversFile.absolutePath,
                )
            }.apply {
                directory(runtimeDir)
                redirectErrorStream(true)
            }

            val p = pb.start()
            process = p

            val reader = Thread({
                runCatching {
                    BufferedReader(InputStreamReader(p.inputStream)).use { br ->
                        while (!stopped) {
                            val line = br.readLine() ?: break
                            val trimmed = line.trim()
                            if (trimmed.isNotBlank()) {
                                if (trimmed.contains("error", ignoreCase = true) ||
                                    trimmed.contains("failed", ignoreCase = true) ||
                                    trimmed.contains("fatal", ignoreCase = true) ||
                                    trimmed.contains("panic", ignoreCase = true)) {
                                    Log.e("WhiteDNS", trimmed)
                                } else {
                                    Log.i("WhiteDNS", trimmed)
                                }
                            }
                        }
                    }
                }
            }, "whitedns-output-drain").apply {
                isDaemon = true
                start()
            }
            drainThread = reader
            true
        } catch (t: Throwable) {
            Log.e("WhiteDNS", "Failed to launch WhiteDNS process", t)
            stop()
            false
        }
    }

    fun awaitReady(timeoutMs: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (stopped) return false
            val p = process
            if (p != null && !p.isAlive) {
                val code = runCatching { p.exitValue() }.getOrNull()
                Log.e("WhiteDNS", "WhiteDNS process died with exit code $code")
                return false
            }
            if (portAccepts("127.0.0.1", socksPort, 350)) {
                Log.i("WhiteDNS", "WhiteDNS SOCKS listener on 127.0.0.1:$socksPort is ready!")
                return true
            }
            try {
                Thread.sleep(150)
            } catch (_: InterruptedException) {
                return false
            }
        }
        Log.e("WhiteDNS", "WhiteDNS SOCKS port $socksPort did not open within ${timeoutMs}ms")
        return false
    }

    fun stop() {
        stopped = true
        val p = process
        process = null
        if (p != null && p.isAlive) {
            p.destroy()
            try {
                if (!p.waitFor(1_200, TimeUnit.MILLISECONDS)) {
                    p.destroyForcibly()
                }
            } catch (_: InterruptedException) {
                p.destroyForcibly()
            }
        }
        drainThread = null
    }

    private fun prepareExecutable(engine: String): File {
        val libraryName = when (engine.trim().lowercase()) {
            WhiteDnsProfile.ENGINE_STORMDNS -> "libstormdns_client.so"
            WhiteDnsProfile.ENGINE_SOAR -> "libsoar_client.so"
            else -> "libcottendns_client.so"
        }

        val nativeDir = File(context.applicationInfo.nativeLibraryDir)
        val inNativeDir = File(nativeDir, libraryName)
        if (inNativeDir.exists()) {
            inNativeDir.setExecutable(true, false)
            return inNativeDir
        }

        // Copy to app private bin directory to ensure execution permission
        val binDir = context.getDir("whitedns_bin", Context.MODE_PRIVATE).apply { mkdirs() }
        val target = File(binDir, libraryName)

        if (!target.exists() || (inNativeDir.exists() && target.length() != inNativeDir.length())) {
            if (inNativeDir.exists()) {
                inNativeDir.inputStream().use { input ->
                    FileOutputStream(target).use { output -> input.copyTo(output) }
                }
            } else {
                // Try reading from assets if present
                runCatching {
                    context.assets.open(libraryName).use { input ->
                        FileOutputStream(target).use { output -> input.copyTo(output) }
                    }
                }
            }
        }

        if (!target.exists()) {
            throw java.io.FileNotFoundException("WhiteDNS binary $libraryName not found in ${nativeDir.absolutePath} or assets")
        }

        target.setReadable(true, false)
        target.setExecutable(true, false)
        return target
    }

    private fun prepareResolvers(targetFile: File) {
        val custom = profile.resolversList()
        if (!profile.useBundledResolvers && custom.isNotEmpty()) {
            targetFile.writeText(custom.joinToString("\n"))
            return
        }

        // Use bundled 4,785 working resolvers
        runCatching {
            context.assets.open("whitedns_resolvers.txt").use { input ->
                FileOutputStream(targetFile).use { output -> input.copyTo(output) }
            }
        }.onFailure {
            // Fallback to high reputation public resolvers
            targetFile.writeText(
                listOf(
                    "8.8.8.8", "8.8.4.4", "1.1.1.1", "1.0.0.1",
                    "9.9.9.9", "149.112.112.112", "208.67.222.222", "208.67.220.220",
                ).joinToString("\n"),
            )
        }
    }

    private fun portAccepts(host: String, port: Int, timeoutMs: Int): Boolean = runCatching {
        Socket().use {
            it.connect(InetSocketAddress(host, port), timeoutMs)
            true
        }
    }.getOrDefault(false)

    companion object {
        private const val TAG = "WhiteDnsController"
    }
}
