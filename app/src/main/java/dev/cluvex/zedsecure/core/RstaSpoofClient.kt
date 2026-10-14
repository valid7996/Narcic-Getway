package dev.cluvex.zedsecure.core

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Client for managing the isolated RstaSpoofService from the main process.
 * By keeping this separate from RstaSpoofEngine, the main process never loads
 * `libsnispf.so` and avoids Go runtime clashing with `libgojni.so`.
 */
object RstaSpoofClient {

    private val probeLock = Mutex()

    fun start(context: Context, listenPort: Int, ip: String, port: Int, fakeSni: String, method: String) {
        val intent = Intent(context, RstaSpoofService::class.java).apply {
            action = RstaSpoofService.ACTION_START
            putExtra(RstaSpoofService.EXTRA_LISTEN_PORT, listenPort)
            putExtra(RstaSpoofService.EXTRA_IP, ip)
            putExtra(RstaSpoofService.EXTRA_PORT, port)
            putExtra(RstaSpoofService.EXTRA_FAKE_SNI, fakeSni)
            putExtra(RstaSpoofService.EXTRA_METHOD, method)
        }
        context.startService(intent)
    }

    fun stop(context: Context) {
        val intent = Intent(context, RstaSpoofService::class.java).apply {
            action = RstaSpoofService.ACTION_STOP
        }
        context.startService(intent)
    }

    fun awaitListener(port: Int, timeoutMs: Long = 4_000L): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val ok = runCatching {
                Socket().use {
                    it.connect(InetSocketAddress("127.0.0.1", port), 500)
                    true
                }
            }.getOrDefault(false)
            if (ok) return true
            try { Thread.sleep(150) } catch (_: InterruptedException) { return false }
        }
        return false
    }

    suspend fun probe(
        context: Context,
        ip: String,
        port: Int,
        fakeSni: String,
        method: String,
        block: suspend (Int) -> Long
    ): Long = probeLock.withLock {
        withContext(Dispatchers.IO) {
            val listen = AetherSupport.findFreePort()
            start(context, listen, ip, port, fakeSni, method)
            try {
                if (awaitListener(listen)) block(listen) else -1L
            } finally {
                stop(context)
            }
        }
    }
}
