package dev.valid7996.narcicgeteway.core

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.net.VpnManager as PlatformVpnManager
import android.net.VpnProfileState
import dev.valid7996.narcicgeteway.R
import dev.valid7996.narcicgeteway.core.AppLog as Log

/**
 * Service to receive [PlatformVpnManager.ACTION_VPN_MANAGER_EVENT] on Android 13+ (Tiramisu).
 * The platform delivers this event via startService to the app associated with the active VPN profile.
 */
class Ikev2EventService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && intent.action == PlatformVpnManager.ACTION_VPN_MANAGER_EVENT) {
            handleEvent(intent)
        }
        stopSelf(startId)
        return START_NOT_STICKY
    }

    private fun handleEvent(intent: Intent) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val category = intent.categories?.firstOrNull()
        val errorClass = intent.getIntExtra(PlatformVpnManager.EXTRA_ERROR_CLASS, -1)
        val errorCode = intent.getIntExtra(PlatformVpnManager.EXTRA_ERROR_CODE, -1)
        val state = (intent.getParcelableExtra(
            PlatformVpnManager.EXTRA_VPN_PROFILE_STATE, VpnProfileState::class.java,
        ))?.state
        Log.i(TAG, "VPN service event category=$category errorClass=$errorClass errorCode=$errorCode state=$state")
        LogBus.append("I/IKEv2 platform event: ${category ?: "state"} state=$state")

        val reason: String? = when (category) {
            PlatformVpnManager.CATEGORY_EVENT_IKE_ERROR ->
                getString(R.string.ikev2_ike_error)
            PlatformVpnManager.CATEGORY_EVENT_NETWORK_ERROR ->
                getString(networkErrorRes(errorCode))
            PlatformVpnManager.CATEGORY_EVENT_DEACTIVATED_BY_USER -> {
                Log.i(TAG, "IKEv2 deactivated by user from system settings")
                null
            }
            PlatformVpnManager.CATEGORY_EVENT_ALWAYS_ON_STATE_CHANGED -> null
            else -> null
        }

        val recoverable = errorClass == PlatformVpnManager.ERROR_CLASS_RECOVERABLE
        if (reason != null && recoverable) {
            LogBus.append("W/IKEv2 recoverable: $reason (platform will retry)")
            return
        }
        Ikev2Controller.onPlatformEvent(this, state, reason)
    }

    private fun networkErrorRes(code: Int): Int = when (code) {
        PlatformVpnManager.ERROR_CODE_NETWORK_UNKNOWN_HOST -> R.string.ikev2_err_unknown_host
        PlatformVpnManager.ERROR_CODE_NETWORK_PROTOCOL_TIMEOUT -> R.string.ikev2_err_timeout
        PlatformVpnManager.ERROR_CODE_NETWORK_LOST -> R.string.ikev2_err_network_lost
        else -> R.string.ikev2_network_error
    }

    companion object {
        private const val TAG = "Ikev2EventService"
    }
}
