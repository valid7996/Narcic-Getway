package dev.valid7996.narcicgeteway.desktop.core

import dev.valid7996.narcicgeteway.domain.config.SstpProfile
import java.io.File

class DesktopSstp(
    private val profile: SstpProfile,
    private val workDir: File,
    private val os: Os = Os.current,
) {
    fun connect(): Result<Unit> {
        if (!profile.isValid) return fail("Invalid SSTP profile (server or username is missing)")
        return when (os) {
            Os.WINDOWS -> windows()
            else -> fail("MS-SSTP on desktop is currently supported on Windows")
        }
    }

    fun disconnect() {
        when (os) {
            Os.WINDOWS -> {
                exec("rasdial", NAME, "/disconnect", timeoutSec = 20)
                exec(
                    "powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command",
                    "Remove-VpnConnection -Name '$NAME' -Force -ErrorAction SilentlyContinue",
                    timeoutSec = 20,
                )
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
        val script = File(dir, "sstp.ps1").apply { writeText(windowsScript()) }
        val serverAddr = if (profile.effectivePort() == 443) profile.serverHost() else "${profile.serverHost()}:${profile.effectivePort()}"
        val result = execWithEnv(
            listOf("powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", script.absolutePath),
            mapOf(
                "ZS_NAME" to NAME,
                "ZS_SERVER" to serverAddr,
                "ZS_USER" to profile.username,
                "ZS_PASS" to profile.password,
            ),
            timeoutSec = 120,
        )
        script.delete()
        if (result.first == 0) return Result.success(Unit)
        return fail(windowsError(result.first, result.second))
    }

    internal fun windowsScript(): String {
        val auth = when (profile.authProtocol) {
            SstpProfile.AUTH_PAP -> "Pap"
            SstpProfile.AUTH_CHAP -> "Chap"
            else -> "MSChapv2"
        }
        return """
            |${'$'}ErrorActionPreference = 'Stop'
            |${'$'}name = ${'$'}env:ZS_NAME
            |if (Get-VpnConnection -Name ${'$'}name -ErrorAction SilentlyContinue) {
            |  rasdial ${'$'}name /disconnect | Out-Null
            |  Remove-VpnConnection -Name ${'$'}name -Force
            |}
            |Add-VpnConnection -Name ${'$'}name -ServerAddress ${'$'}env:ZS_SERVER -TunnelType Sstp -EncryptionLevel Optional -AuthenticationMethod $auth -RememberCredential -Force
            |${'$'}ErrorActionPreference = 'Continue'
            |rasdial ${'$'}name ${'$'}env:ZS_USER ${'$'}env:ZS_PASS
            |exit ${'$'}LASTEXITCODE
            """.trimMargin() + "\n"
    }

    private fun windowsError(code: Int, output: String): String = when (code) {
        691 -> "The server refused the username or password (Error 691)"
        800 -> "Unable to create the SSTP connection. Check the server address or firewall (Error 800)"
        809 -> "The network connection between your computer and the VPN server could not be established (Error 809)"
        868 -> "The remote connection was not made because the name of the remote access server did not resolve (Error 868)"
        else -> "SSTP failed (rasdial $code): " + output.lineSequence().map { it.trim() }.lastOrNull { it.isNotEmpty() }.orEmpty()
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

    private fun fail(message: String): Result<Unit> = Result.failure(IllegalStateException(message))

    companion object {
        const val NAME = "Narcic Getway SSTP"
    }
}
