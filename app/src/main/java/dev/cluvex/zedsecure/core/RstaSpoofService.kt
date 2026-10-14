package dev.cluvex.zedsecure.core

import android.app.Service
import android.content.Intent
import android.os.IBinder
import dev.cluvex.zedsecure.core.AppLog as Log

/**
 * Runs in an isolated Linux process (`:snispoof`) to host the Go runtime of `libsnispf.so`.
 * This prevents the Go runtime in `libsnispf.so` from clashing with the Go runtime in `libgojni.so`
 * (sing-box / Xray), which would otherwise crash with `fatal error: addspecial on invalid pointer` (SIGABRT).
 */
class RstaSpoofService : Service() {

    companion object {
        private const val TAG = "RstaSpoofService"
        const val ACTION_START = "dev.cluvex.zedsecure.snispoof.START"
        const val ACTION_STOP = "dev.cluvex.zedsecure.snispoof.STOP"
        const val EXTRA_LISTEN_PORT = "listen_port"
        const val EXTRA_IP = "ip"
        const val EXTRA_PORT = "port"
        const val EXTRA_FAKE_SNI = "fake_sni"
        const val EXTRA_METHOD = "method"

        private var activeSessionId = 0L

        @Volatile
        var isRunning = false
            private set
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val listenPort = intent.getIntExtra(EXTRA_LISTEN_PORT, 0)
                val ip = intent.getStringExtra(EXTRA_IP).orEmpty()
                val port = intent.getIntExtra(EXTRA_PORT, 0)
                val fakeSni = intent.getStringExtra(EXTRA_FAKE_SNI).orEmpty()
                val method = intent.getStringExtra(EXTRA_METHOD) ?: "split"

                if (activeSessionId != 0L) {
                    RstaSpoofEngine.stop(activeSessionId)
                    activeSessionId = 0L
                }

                Log.i(TAG, "Starting RstaSpoofEngine in isolated process :snispoof on port $listenPort -> $ip:$port")
                activeSessionId = RstaSpoofEngine.start(listenPort, ip, port, fakeSni, method)
                isRunning = activeSessionId != 0L
            }
            ACTION_STOP -> {
                Log.i(TAG, "Stopping RstaSpoofEngine")
                if (activeSessionId != 0L) {
                    RstaSpoofEngine.stop(activeSessionId)
                    activeSessionId = 0L
                }
                isRunning = false
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        if (activeSessionId != 0L) {
            RstaSpoofEngine.stop(activeSessionId)
            activeSessionId = 0L
        }
        isRunning = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
