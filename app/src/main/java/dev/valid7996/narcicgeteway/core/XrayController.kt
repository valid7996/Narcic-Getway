package dev.valid7996.narcicgeteway.core

import android.content.Context
import dev.valid7996.narcicgeteway.core.AppLog as Log
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import libv2ray.Libv2ray
import android.os.SystemClock
import java.io.File

object XrayController {
    private const val TAG = "XrayController"

    @Volatile
    private var controller: CoreController? = null

    /** start() and stop() run from different threads (service start vs teardown); they must not interleave. */
    private val lifecycleLock = Any()

    /**
     * The core calls shutdown() when its loop ends - also when we ended it ourselves with stopLoop(). That is not a
     * failure and must not tear the service down (it used to, which killed a session that was just restarted).
     */
    @Volatile
    private var expectedShutdownUntil = 0L

    private fun expectShutdown() {
        expectedShutdownUntil = SystemClock.elapsedRealtime() + 2_000L
    }

    @Volatile
    private var initialized = false

    @Volatile
    private var appContext: Context? = null

    var onCoreShutdown: (() -> Unit)? = null

    @Volatile
    var lastError: String? = null
        private set

    fun runtimeReport(limit: Int = 12): String =
        runCatching { Libv2ray.runtimeReport(limit.toLong()) }.getOrElse { "unavailable: ${it.message}" }

    fun cryptoAcceleration(): String =
        runCatching { Libv2ray.cryptoAcceleration() }.getOrElse { "" }

    @Synchronized
    fun init(context: Context) {
        if (initialized) return

        CoreCrashLog.install(context)
        val assets = File(context.filesDir, "assets").apply { mkdirs() }.absolutePath
        Libv2ray.initCoreEnv(assets, "")
        controller = Libv2ray.newCoreController(object : CoreCallbackHandler {
            override fun onEmitStatus(status: Long, message: String?): Long = 0
            override fun startup(): Long = 0
            override fun shutdown(): Long {
                if (SystemClock.elapsedRealtime() >= expectedShutdownUntil) onCoreShutdown?.invoke()
                return 0
            }
        })
        initialized = true
    }

    fun warmUp(context: Context) {
        appContext = context.applicationContext
        if (initialized) return
        val app = context.applicationContext
        Thread({ runCatching { init(app) } }, "nrg-core-init").apply { isDaemon = true }.start()
    }

    private fun ready(): CoreController? {
        if (!initialized) appContext?.let { runCatching { init(it) } }
        return controller
    }

    val isRunning: Boolean
        get() = controller?.isRunning ?: false

    fun start(configJson: String, tunFd: Int = 0): Boolean = synchronized(lifecycleLock) {
        val c = ready() ?: return@synchronized false
        if (c.isRunning) {
            expectShutdown()
            runCatching { c.stopLoop() }
        }
        lastError = null
        try {
            c.startLoop(configJson, tunFd)
            c.isRunning
        } catch (e: Exception) {
            Log.e(TAG, "startLoop failed", e)
            lastError = e.message?.takeIf { it.isNotBlank() } ?: e.toString()
            false
        }
    }

    fun stop() {
        synchronized(lifecycleLock) {
            runCatching {
                if (controller?.isRunning == true) {
                    expectShutdown()
                    controller?.stopLoop()
                }
            }.onFailure { Log.e(TAG, "stopLoop failed", it) }
        }
    }

    fun readTrafficDelta(): Pair<Long, Long> {
        val raw = runCatching { controller?.queryAllOutboundTrafficStats() }.getOrNull()
            ?: return 0L to 0L
        var down = 0L
        var up = 0L

        val byTag = HashMap<String, LongArray>()
        for (entry in raw.split(';')) {
            if (entry.isEmpty()) continue
            val parts = entry.split(',')
            if (parts.size != 3) continue
            val value = parts[2].toLongOrNull() ?: continue
            if (parts[0] !in NOT_A_METER) {
                val slot = byTag.getOrPut(parts[0]) { LongArray(2) }
                when (parts[1]) {
                    "downlink" -> slot[0] += value
                    "uplink" -> slot[1] += value
                }
            }

            if (parts[0] != "proxy" && !dev.valid7996.narcicgeteway.domain.config.AutoSelectTags.isMember(parts[0])) continue
            when (parts[1]) {
                "downlink" -> down += value
                "uplink" -> up += value
            }
        }
        if (down == 0L && up == 0L && byTag.isNotEmpty()) {
            val busiest = byTag.values.maxByOrNull { it[0] + it[1] }
            if (busiest != null) return busiest[0] to busiest[1]
        }
        return down to up
    }

    private val NOT_A_METER = setOf("direct", "block", "blocked", "dns-out", "dns_out", "api", "fragment")

    fun measureDelay(url: String): Long =
        runCatching { ready()?.measureDelay(url) ?: -1L }.getOrDefault(-1L)

    fun measureDelayDetailed(url: String): dev.valid7996.narcicgeteway.core.CoreProbe.DelayOutcome {
        val c = ready()
        if (c == null || !c.isRunning) {
            return dev.valid7996.narcicgeteway.core.CoreProbe.DelayOutcome(-1L, "core not running")
        }
        return try {
            val ms = c.measureDelay(url)
            if (ms <= 0) {
                dev.valid7996.narcicgeteway.core.CoreProbe.DelayOutcome(-1L, "unreachable")
            } else {
                dev.valid7996.narcicgeteway.core.CoreProbe.DelayOutcome(ms, null)
            }
        } catch (e: Exception) {
            dev.valid7996.narcicgeteway.core.CoreProbe.DelayOutcome(-1L, e.message)
        }
    }
}
