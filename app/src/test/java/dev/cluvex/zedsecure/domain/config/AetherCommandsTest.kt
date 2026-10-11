package dev.cluvex.zedsecure.domain.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AetherCommandsTest {

    private val psiphonChain = AetherProfile(protocol = AetherProfile.PROTO_MASQUE, psiphon = AetherProfile.CARRIER_CHAIN)

    @Test
    fun exitProxyNeverSharesAPortWithTheCoreListeners() {
        val port = LocalPorts.AETHER_SOCKS
        val arguments = AetherCommands.buildArguments(
            AetherProfile(tor = AetherProfile.CARRIER_REVERSE, psiphon = AetherProfile.CARRIER_CHAIN, protocol = AetherProfile.PROTO_MASQUE),
            port,
        )
        val bound = listOf(AetherCommands.BIND, AetherCommands.TOR_BIND, AetherCommands.PSIPHON_BIND)
            .mapNotNull { flag -> arguments.indexOf(flag).takeIf { it >= 0 }?.let { arguments[it + 1] } }
            .map { it.substringAfterLast(':').toInt() }
        assertTrue(bound.isNotEmpty())
        assertFalse(AetherExitJson.EXIT_SOCKS_PORT in bound)
        assertEquals(port + 3, AetherExitJson.EXIT_SOCKS_PORT)
    }

    @Test
    fun shippedWordIsReplacedByTheListOrTheFlagGoes() {
        val arguments = listOf("--psiphon", AetherCommands.PSIPHON_SERVER_ENTRIES, AetherCommands.SHIPPED_LIST, "--log-level", "info")
        assertEquals(
            listOf("--psiphon", AetherCommands.PSIPHON_SERVER_ENTRIES, "/data/list.txt", "--log-level", "info"),
            AetherCommands.withShippedList(arguments, "/data/list.txt"),
        )
        assertEquals(listOf("--psiphon", "--log-level", "info"), AetherCommands.withShippedList(arguments, null))
        val own = listOf(AetherCommands.PSIPHON_SERVER_ENTRIES, "/mine.txt")
        assertEquals(own, AetherCommands.withShippedList(own, null))
    }

    @Test
    fun theBundledListFlagIsWrittenOnlyWithPsiphon() {
        val withPsiphon = AetherCommands.buildArguments(psiphonChain, LocalPorts.AETHER_SOCKS)
        assertTrue(AetherCommands.PSIPHON_SERVER_ENTRIES in withPsiphon)
        assertFalse(AetherCommands.PSIPHON_SERVER_ENTRIES in AetherCommands.buildArguments(AetherProfile(), LocalPorts.AETHER_SOCKS))
    }

    @Test
    fun psiphonSessionWaitsForTheReadyWordAndTheInfoLevel() {
        val arguments = AetherCommands.buildArguments(psiphonChain, LocalPorts.AETHER_SOCKS)
        assertTrue(AetherCommands.readyNeedsWord(arguments))
        assertFalse(AetherCommands.readyNeedsWord(AetherCommands.buildArguments(AetherProfile(), LocalPorts.AETHER_SOCKS)))
        assertTrue(AetherCommands.isReadyWord("[2026-01-01 INFO] psiphon is ready"))
        val quiet = listOf("--psiphon", "--log-level", "error")
        assertTrue(AetherCommands.showsInfo(AetherCommands.withInfoLines(quiet)))
    }

    @Test
    fun customCommandGetsAListenerAndAnInfoLevelWhenItNamesNone() {
        val profile = AetherProfile(command = "aether --protocol wg --psiphon")
        val arguments = AetherCommands.runArguments(profile, 10890)
        assertEquals(10890, AetherCommands.listenerPortOf(arguments))
        assertEquals(AetherCommands.PSIPHON_BIND, AetherCommands.listenerFlagOf(arguments))
        assertTrue(AetherCommands.LOG_LEVEL in arguments)
        // A listener the command names stays.
        val named = AetherCommands.runArguments(AetherProfile(command = "--bind 127.0.0.1:1234"), 10890)
        assertEquals(1234, AetherCommands.listenerPortOf(named))
    }

    @Test
    fun keysRequestCarriesItsSettings() {
        val arguments = AetherCommands.keysArguments(
            AetherCommands.KeysRequest(kind = "masque", enrollAddress = "1.2.3.4:443", fragment = true, fragmentSize = "8-16", ech = true),
        )
        assertEquals(listOf(AetherCommands.REGISTER, "masque"), arguments.take(2))
        assertEquals("1.2.3.4:443", arguments[arguments.indexOf("--enroll-address") + 1])
        assertEquals("8-16", arguments[arguments.indexOf("--fragment-size") + 1])
        assertTrue("--ech" in arguments)
    }

    @Test
    fun editorRulesOfPattNgHold() {
        assertNull(AetherCommands.problemOf(AetherProfile()))
        assertEquals(
            AetherCommands.Problem.CARRIER_NEEDS_MASQUE,
            AetherCommands.problemOf(AetherProfile(protocol = AetherProfile.PROTO_WG, psiphon = AetherProfile.CARRIER_REVERSE)),
        )
        assertEquals(
            AetherCommands.Problem.TOR_PSIPHON_CONFLICT,
            AetherCommands.problemOf(AetherProfile(tor = AetherProfile.CARRIER_CHAIN, psiphon = AetherProfile.CARRIER_CHAIN)),
        )
        assertNull(
            AetherCommands.problemOf(
                AetherProfile(protocol = AetherProfile.PROTO_MASQUE, tor = AetherProfile.CARRIER_CHAIN, psiphon = AetherProfile.CARRIER_REVERSE),
            ),
        )
        assertEquals(
            AetherCommands.Problem.TOR_BRIDGES_MISSING,
            AetherCommands.problemOf(AetherProfile(tor = AetherProfile.CARRIER_CHAIN, torBridges = AetherProfile.TOR_BRIDGES_OWN)),
        )
    }
}
