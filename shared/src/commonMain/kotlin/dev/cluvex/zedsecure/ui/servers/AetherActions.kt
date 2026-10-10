package dev.cluvex.zedsecure.ui.servers

import androidx.compose.runtime.compositionLocalOf
import dev.cluvex.zedsecure.domain.config.AetherProfile

/**
 * Interface between Compose UI and the native Android Aether implementation.
 * Provides scanning, live log output streaming, WARP key registration and carrier status.
 */
interface AetherActions {

    /** Scans for reachable endpoints of [profile], streaming stdout/stderr to [onOutput]. */
    suspend fun scan(profile: AetherProfile, onOutput: (String) -> Unit): AetherScanUiResult?

    /** Cancels an ongoing scan process. */
    fun cancelScan()

    /** Registers WARP keys anew, streaming progress to [onOutput]. */
    suspend fun registerKeys(protocol: String, onOutput: (String) -> Unit): Boolean

    /** Lists key files with their parsed identities (device_id, IPv4, IPv6). */
    suspend fun keys(): List<AetherKeyUiEntry>

    /** Clears Psiphon learned cache files. */
    suspend fun clearPsiphonData(): Boolean

    /** Returns available Psiphon region codes. */
    suspend fun psiphonRegions(): List<String>

    fun isCoreAvailable(): Boolean
    fun isPsiphonAvailable(): Boolean
    fun isTorTransportsAvailable(): Boolean
}

data class AetherScanUiResult(val endpoint: String, val innerHop: String? = null)

data class AetherKeyUiEntry(val file: String, val deviceId: String?, val ipv4: String?, val ipv6: String?)

data class AetherLogUiEntry(val id: Long, val priority: Int, val text: String)

val LocalAetherActions = compositionLocalOf<AetherActions?> { null }
