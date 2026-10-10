package dev.cluvex.zedsecure.ui

import android.content.Context
import dev.cluvex.zedsecure.core.AetherIdentityStore
import dev.cluvex.zedsecure.core.AetherScanner
import dev.cluvex.zedsecure.core.AetherSupport
import dev.cluvex.zedsecure.core.LogBus
import dev.cluvex.zedsecure.domain.config.AetherProfile
import dev.cluvex.zedsecure.ui.servers.AetherActions
import dev.cluvex.zedsecure.ui.servers.AetherKeyUiEntry
import dev.cluvex.zedsecure.ui.servers.AetherScanUiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The Aether answers of Android: the scanner, the WARP keys, Psiphon / Tor status and registration. */
class AetherActionsAndroid(private val context: Context) : AetherActions {

    override suspend fun scan(profile: AetherProfile, onOutput: (String) -> Unit): AetherScanUiResult? =
        AetherScanner.scan(context, profile) { line ->
            onOutput(line)
            relayScan(line)
        }?.let { AetherScanUiResult(it.endpoint, it.innerHop) }

    override fun cancelScan() {
        AetherScanner.cancel()
    }

    override suspend fun registerKeys(protocol: String, onOutput: (String) -> Unit): Boolean {
        val workDir = AetherSupport.workDir(context).apply { mkdirs() }
        val upstream = AetherSupport.startExitProxy(AetherProfile())
        return try {
            AetherIdentityStore.register(
                context = context,
                protocol = protocol,
                upstreamPort = upstream,
                workDir = workDir,
            ) { line ->
                onOutput(line)
                relayKey(line)
            }
        } finally {
            AetherSupport.stopExitProxy()
        }
    }

    override suspend fun keys(): List<AetherKeyUiEntry> = AetherIdentityStore.keys(context).map { entry ->
        AetherKeyUiEntry(entry.file, entry.identity?.deviceId, entry.identity?.ipv4, entry.identity?.ipv6)
    }

    override suspend fun clearPsiphonData(): Boolean = withContext(Dispatchers.IO) {
        val dir = AetherSupport.psiphonDir(context)
        dir.deleteRecursively()
    }

    override suspend fun psiphonRegions(): List<String> = emptyList()

    override fun isCoreAvailable(): Boolean = AetherSupport.isCoreSupported(context)

    override fun isPsiphonAvailable(): Boolean = AetherSupport.isPsiphonSupported(context)

    override fun isTorTransportsAvailable(): Boolean = AetherSupport.isTorTransportsSupported(context)

    private fun relayScan(line: String) {
        LogBus.append("${AetherSupport.levelOf(line)}/aether-scan ${line.trim()}")
    }

    private fun relayKey(line: String) {
        LogBus.append("${AetherSupport.levelOf(line)}/aether-key ${line.trim()}")
    }
}
