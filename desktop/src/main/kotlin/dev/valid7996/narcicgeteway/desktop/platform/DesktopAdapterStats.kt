package dev.valid7996.narcicgeteway.desktop.platform

import dev.valid7996.narcicgeteway.desktop.core.Os
import java.io.File
import java.util.concurrent.TimeUnit

class DesktopAdapterStats(private val interfaceName: String) {
    private var baseline: Pair<Long, Long>? = null

    fun read(): Pair<Long, Long>? = runCatching {
        val raw = readRaw(interfaceName) ?: return null
        if (baseline == null) {
            baseline = raw
            return 0L to 0L
        }
        val (baseIn, baseOut) = baseline!!
        val down = (raw.first - baseIn).coerceAtLeast(0L)
        val up = (raw.second - baseOut).coerceAtLeast(0L)
        down to up
    }.getOrNull()

    companion object {
        fun readRaw(name: String): Pair<Long, Long>? = runCatching {
            when (Os.current) {
                Os.WINDOWS -> readWindows(name)
                Os.LINUX -> readLinux(name)
                Os.MACOS -> readMac(name)
                else -> null
            }
        }.getOrNull()

        private fun readWindows(name: String): Pair<Long, Long>? {
            val proc = ProcessBuilder("netsh", "interface", "ipv4", "show", "subinterfaces")
                .redirectErrorStream(true)
                .start()
            val text = proc.inputStream.bufferedReader().use { it.readText() }
            proc.waitFor(2, TimeUnit.SECONDS)
            for (line in text.lineSequence()) {
                if (line.contains(name, ignoreCase = true)) {
                    val tokens = line.trim().split(Regex("\\s+"))
                    if (tokens.size >= 5) {
                        val bytesIn = tokens[2].toLongOrNull() ?: continue
                        val bytesOut = tokens[3].toLongOrNull() ?: continue
                        return bytesIn to bytesOut
                    }
                }
            }
            return null
        }

        private fun readLinux(name: String): Pair<Long, Long>? {
            val procNetDev = File("/proc/net/dev")
            if (procNetDev.exists()) {
                procNetDev.forEachLine { line ->
                    if (line.contains(":")) {
                        val iface = line.substringBefore(":").trim()
                        if (iface.contains(name, ignoreCase = true) || name.contains(iface, ignoreCase = true)) {
                            val parts = line.substringAfter(":").trim().split(Regex("\\s+"))
                            if (parts.size >= 10) {
                                val rx = parts[0].toLongOrNull() ?: 0L
                                val tx = parts[8].toLongOrNull() ?: 0L
                                return rx to tx
                            }
                        }
                    }
                }
            }
            return null
        }

        private fun readMac(name: String): Pair<Long, Long>? {
            val proc = ProcessBuilder("netstat", "-ibn")
                .redirectErrorStream(true)
                .start()
            val text = proc.inputStream.bufferedReader().use { it.readText() }
            proc.waitFor(2, TimeUnit.SECONDS)
            for (line in text.lineSequence()) {
                if (line.contains(name, ignoreCase = true) || line.contains("ppp", ignoreCase = true) || line.contains("ipsec", ignoreCase = true)) {
                    val parts = line.trim().split(Regex("\\s+"))
                    if (parts.size >= 10) {
                        val ibytes = parts[6].toLongOrNull() ?: continue
                        val obytes = parts[9].toLongOrNull() ?: continue
                        return ibytes to obytes
                    }
                }
            }
            return null
        }
    }
}
