package dev.valid7996.narcicgeteway.desktop.core

import dev.valid7996.narcicgeteway.domain.config.SstpProfile
import java.io.File

class DesktopSstp(
    private val profile: SstpProfile,
    private val workDir: File,
    private val os: Os = Os.current,
) {
    fun connect(): Result<Unit> {
        if (!profile.isValid) return Result.failure(IllegalStateException("The SSTP profile is missing server or username"))
        return when (os) {
            Os.WINDOWS -> windows()
            else -> Result.failure(IllegalStateException("MS-SSTP is natively supported on Windows"))
        }
    }

    fun disconnect() {
        when (os) {
            Os.WINDOWS -> {
                exec("rasdial", NAME, "/disconnect", timeoutSec = 15)
                exec("powershell", "-NoProfile", "-Command", "Remove-VpnConnection -Name '$NAME' -Force -ErrorAction SilentlyContinue", timeoutSec = 15)
            }
            else -> Unit
        }
    }

    fun isUp(): Boolean = when (os) {
        Os.WINDOWS -> exec("rasdial", timeoutSec = 10).second.lineSequence().any { it.trim().equals(NAME, ignoreCase = true) }
        else -> false
    }

    private fun windows(): Result<Unit> {
        val dir = workDir.also { it.mkdirs() }
        val script = File(dir, "sstp.ps1").apply {
            writeText(
                """
                |${'$'}ErrorActionPreference = 'Stop'
                |${'$'}name = ${'$'}env:ZS_NAME
                |if (Get-VpnConnection -Name ${'$'}name -ErrorAction SilentlyContinue) {
                |  rasdial ${'$'}name /disconnect | Out-Null
                |  Remove-VpnConnection -Name ${'$'}name -Force
                |}
                |Add-VpnConnection -Name ${'$'}name -ServerAddress ${'$'}env:ZS_SERVER -TunnelType Sstp -EncryptionLevel Optional -AuthenticationMethod MsChapv2,Pap,Chap -RememberCredential -Force
                |${'$'}ErrorActionPreference = 'Continue'
                |rasdial ${'$'}name ${'$'}env:ZS_USER ${'$'}env:ZS_PASS
                |exit ${'$'}LASTEXITCODE
                """.trimMargin() + "\n",
            )
        }
        val result = execWithEnv(
            listOf("powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", script.absolutePath),
            mapOf(
                "ZS_NAME" to NAME,
                "ZS_SERVER" to profile.serverHost(),
                "ZS_USER" to profile.username,
                "ZS_PASS" to profile.password,
            ),
            timeoutSec = 120,
        )
        script.delete()
        if (result.first == 0) return Result.success(Unit)
        return Result.failure(IllegalStateException(windowsError(result.first, result.second)))
    }

    private fun windowsError(code: Int, output: String): String = when (code) {
        691 -> "The server refused the username or password"
        800 -> "Unable to establish SSTP connection to the server"
        809 -> "The server did not answer or blocked SSTP port 443"
        else -> output.trim().ifEmpty { "SSTP connection failed (code $code)" }
    }

    private fun execWithEnv(cmd: List<String>, env: Map<String, String>, timeoutSec: Long): Pair<Int, String> = try {
        val p = ProcessBuilder(cmd).redirectErrorStream(true).also { it.environment().putAll(env) }.start()
        val out = p.inputStream.bufferedReader().readText()
        if (!p.waitFor(timeoutSec, java.util.concurrent.TimeUnit.SECONDS)) {
            p.destroyForcibly()
            -1 to out
        } else {
            p.exitValue() to out
        }
    } catch (e: Exception) {
        -1 to (e.message ?: "exec failed")
    }

    companion object {
        const val NAME = "Narcic Getway SSTP"
    }
}
