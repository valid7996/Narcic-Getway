package dev.valid7996.narcicgeteway.core

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

object AndroidVpn {
    fun start(
        context: Context,
        configJson: String,
        remark: String,
        socksPort: Int,
        kind: String = VpnManager.KIND_XRAY,

        proxyOnly: Boolean = false,
    ) {
        VpnManager.onStarting(remark)

        val viaFile = configJson.length > INLINE_CONFIG_LIMIT
        val handoff = if (viaFile) {
            runCatching {
                java.io.File(context.filesDir, "pending-config.json").apply { writeText(configJson) }
            }.getOrNull()
        } else {
            null
        }
        val intent = Intent(context, NrgVpnService::class.java).apply {
            putExtra(VpnManager.EXTRA_COMMAND, VpnManager.CMD_START)
            if (handoff != null) putExtra(VpnManager.EXTRA_CONFIG_PATH, handoff.absolutePath)
            else putExtra(VpnManager.EXTRA_CONFIG, configJson)
            putExtra(VpnManager.EXTRA_REMARK, remark)
            putExtra(VpnManager.EXTRA_SOCKS_PORT, socksPort)
            putExtra(VpnManager.EXTRA_KIND, kind)
            putExtra(VpnManager.EXTRA_PROXY_ONLY, proxyOnly)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    private const val INLINE_CONFIG_LIMIT = 48 * 1024

    /**
     * Asks the service to tear the session down. The command is sent whenever there is anything to tear down,
     * not only when the published state says so: callers used to stop the IKEv2 session first (which publishes
     * "Idle") and then call this, so the early return below swallowed the command and the foreground service,
     * its notification and the kill-switch bookkeeping were left running with nobody to stop them.
     */
    fun stop(context: Context) {
        val ikev2Live = Ikev2Controller.isActive
        if (!VpnManager.onStopping() && !ikev2Live) return
        val intent = Intent(context, NrgVpnService::class.java).apply {
            putExtra(VpnManager.EXTRA_COMMAND, VpnManager.CMD_STOP)
        }
        try {
            context.startService(intent)
        } catch (e: Exception) {
            // Android refuses startService() from the background (IllegalStateException). The service cannot be
            // reached, so release what we can directly instead of leaving the app stuck in "Disconnecting".
            AppLog.w(TAG, "could not reach the VPN service to stop it; tearing down directly", e)
            Ikev2Controller.stop(context)
            VpnManager.onDisconnected()
        }
    }

    private const val TAG = "AndroidVpn"
}
