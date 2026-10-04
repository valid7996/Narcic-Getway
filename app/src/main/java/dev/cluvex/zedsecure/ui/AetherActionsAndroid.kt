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

/** The Aether answers of Android: the scanner, the WARP keys and their registration. */
class AetherActionsAndroid(private val context: Context) : AetherActions {

    override suspend fun scan(profile: AetherProfile): AetherScanUiResult? =
        AetherScanner.scan(context, profile, ::relayScan)?.let { AetherScanUiResult(it.endpoint, it.innerHop) }

    override suspend fun registerKeys(protocol: String): Boolean {
        val workDir = AetherSupport.workDir(context).apply { mkdirs() }
        val upstream = AetherSupport.startExitProxy(AetherProfile())
        return try {
            AetherIdentityStore.register(
                context = context,
                protocol = protocol,
                upstreamPort = upstream,
                workDir = workDir,
                onOutput = ::relayKey,
            )
        } finally {
            AetherSupport.stopExitProxy()
        }
    }

    override suspend fun keys(): List<AetherKeyUiEntry> = AetherIdentityStore.keys(context).map { entry ->
        AetherKeyUiEntry(entry.file, entry.identity?.deviceId, entry.identity?.ipv4, entry.identity?.ipv6)
    }

    private fun relayScan(line: String) {
        LogBus.append("${AetherSupport.levelOf(line)}/aether-scan ${line.trim()}")
    }

    private fun relayKey(line: String) {
        LogBus.append("${AetherSupport.levelOf(line)}/aether-key ${line.trim()}")
    }
}
