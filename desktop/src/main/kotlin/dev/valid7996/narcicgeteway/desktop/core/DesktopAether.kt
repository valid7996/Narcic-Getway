package dev.valid7996.narcicgeteway.desktop.core

import dev.valid7996.narcicgeteway.core.LogBus
import dev.valid7996.narcicgeteway.domain.config.AetherCommands
import dev.valid7996.narcicgeteway.domain.config.AetherProfile
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class DesktopAether(
    private val profile: AetherProfile,
    private val workDir: File,
    val socksPort: Int,
) {
    @Volatile private var process: Process? = null

    fun start(waitSec: Long = CONNECT_WAIT_SEC): Result<Unit> {
        val bin = BundledBinary.extract(workDir, "aether", "aether.exe")
            ?: return Result.failure(IllegalStateException("Aether is not bundled for ${Os.current}"))
        val serverList = DesktopPsiphonServerList.entriesFile(workDir)
        var runArgs = AetherCommands.runArguments(profile, socksPort)
        runArgs = AetherCommands.withShippedList(runArgs, serverList?.absolutePath)
        val overlayContent = AetherCommands.overlayOf(runArgs)
        val overlayFile = overlayContent?.let {
            File(workDir, "psiphon-overlay.json").apply { writeText(it) }
        }
        runArgs = AetherCommands.withOverlay(runArgs, overlayFile?.absolutePath)
        val args = listOf(bin.absolutePath) + runArgs
        val started = try {
            ProcessBuilder(args)
                .redirectErrorStream(true)
                .directory(workDir)
                .start()
        } catch (e: Exception) {
            return Result.failure(IllegalStateException("Aether failed to start: ${e.message}"))
        }
        process = started

        val connected = AtomicBoolean(false)
        val settled = CountDownLatch(1)
        val lastError = StringBuilder()
        Thread {
            runCatching {
                started.inputStream.bufferedReader().forEachLine { line ->
                    LogBus.append("I/Aether: $line")
                    if (line.contains("identities ready") || line.contains("connected") || line.contains("listening on") || line.contains("warp enabled")) {
                        if (connected.compareAndSet(false, true)) settled.countDown()
                    }
                    if (line.contains("ERROR") || line.contains("error") || line.contains("failed")) {
                        lastError.setLength(0)
                        lastError.append(line)
                    }
                }
            }
            settled.countDown()
        }.apply { isDaemon = true; name = "aether-output" }.start()

        settled.await(waitSec, TimeUnit.SECONDS)
        if (started.isAlive) {
            connected.set(true)
            return Result.success(Unit)
        }
        val reason = if (lastError.isNotEmpty()) lastError.toString() else "Aether exited unexpectedly"
        stop()
        return Result.failure(IllegalStateException(reason))
    }

    fun stop() {
        val running = process ?: return
        process = null
        XrayCore.stopProcess(running)
    }

    companion object {
        private const val CONNECT_WAIT_SEC = 6L

        fun registerKeys(workDir: File, request: AetherCommands.KeysRequest, onLine: (String) -> Unit): Result<Unit> {
            val bin = BundledBinary.extract(workDir, "aether", "aether.exe")
                ?: return Result.failure(IllegalStateException("Aether is not bundled for ${Os.current}"))
            val args = listOf(bin.absolutePath) + AetherCommands.keysArguments(request)
            return try {
                val proc = ProcessBuilder(args)
                    .redirectErrorStream(true)
                    .directory(workDir)
                    .start()
                proc.inputStream.bufferedReader().forEachLine { line ->
                    onLine(line)
                    LogBus.append("I/AetherKeys: $line")
                }
                val code = proc.waitFor()
                if (code == 0) Result.success(Unit) else Result.failure(IllegalStateException("Aether exited with code $code"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
