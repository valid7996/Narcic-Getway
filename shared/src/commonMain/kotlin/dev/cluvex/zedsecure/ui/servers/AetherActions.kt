package dev.cluvex.zedsecure.ui.servers

import androidx.compose.runtime.compositionLocalOf
import dev.cluvex.zedsecure.domain.config.AetherProfile

/**
 * What the Aether editor asks of the platform: the endpoint scanner, the WARP keys and their
 * registration. Android answers, relaying every core line into the app log; where Aether does not
 * run, [LocalAetherActions] is null and the editor hides what nothing can answer.
 */
interface AetherActions {

    /** Scans for a reachable endpoint of [profile]; null when a session blocks the scan or none was found. */
    suspend fun scan(profile: AetherProfile): AetherScanUiResult?

    /** Registers the WARP keys of [protocol] anew; true once every key of the kind is saved. */
    suspend fun registerKeys(protocol: String): Boolean

    /** Every key of the identity folder, with the identity it holds. */
    suspend fun keys(): List<AetherKeyUiEntry>
}

data class AetherScanUiResult(val endpoint: String, val innerHop: String? = null)

data class AetherKeyUiEntry(val file: String, val deviceId: String?, val ipv4: String?, val ipv6: String?)

val LocalAetherActions = compositionLocalOf<AetherActions?> { null }
