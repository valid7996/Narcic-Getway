package dev.valid7996.narcicgeteway.domain.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
class WhiteDnsConfigTest {
    private val storm = WhiteDnsProfile(domains = "t.example.com", encryptionKey = "k3y", encryptionMethod = 2)
    private val cotten = storm.copy(engine = WhiteDnsProfile.ENGINE_COTTEN)

    private fun lines(toml: String): Map<String, String> =
        WhiteDnsConfig.parseAssignments(toml).associate { it.key to it.raw }

    @Test
    fun `storm config carries the server and the client block`() {
        val t = lines(WhiteDnsConfig.renderRuntimeToml(storm, WhiteDnsSettings(), "127.0.0.1", 10910))
        assertEquals("[\"t.example.com\"]", t["DOMAINS"])
        assertEquals("2", t["DATA_ENCRYPTION_METHOD"])
        assertEquals("\"k3y\"", t["ENCRYPTION_KEY"])
        assertEquals("10910", t["LISTEN_PORT"])
        assertEquals("\"SOCKS5\"", t["PROTOCOL_TYPE"])
        assertEquals("false", t["SOCKS5_AUTH"])
        assertEquals("false", t["LOCAL_DNS_ENABLED"])
        assertEquals("\"INFO\"", t["LOG_LEVEL"])
    }

    @Test
    fun `storm config has no cotten keys`() {
        val t = lines(WhiteDnsConfig.renderRuntimeToml(storm, WhiteDnsSettings(), "127.0.0.1", 10910))
        assertFalse(t.containsKey("CONFIG_PRESET"))
        assertFalse(t.containsKey("QUERY_TYPES"))
        assertFalse(t.containsKey("TUNNEL_MODE"))
    }

    @Test
    fun `cotten default preset asks for TXT over auto transport`() {
        val t = lines(WhiteDnsConfig.renderRuntimeToml(cotten, WhiteDnsSettings(), "127.0.0.1", 10910))
        assertEquals("\"default\"", t["CONFIG_PRESET"])
        assertEquals("false", t["LEGACY_SESSION_ID"])
        assertEquals("\"legacy\"", t["TUNNEL_MODE"])
        assertEquals("[\"TXT\"]", t["QUERY_TYPES"])
        assertEquals("\"auto\"", t["RESOLVER_TRANSPORT"])
    }

    @Test
    fun `master-storm preset is a legacy-session udp client`() {
        val p = cotten.copy(configPreset = WhiteDnsProfile.PRESET_MASTER_STORM, tunnelMode = "cottenmax")
        val t = lines(WhiteDnsConfig.renderRuntimeToml(p, WhiteDnsSettings(), "127.0.0.1", 10910))
        assertEquals("\"default\"", t["CONFIG_PRESET"])
        assertEquals("true", t["LEGACY_SESSION_ID"])
        assertEquals("\"udp\"", t["RESOLVER_TRANSPORT"])
        assertFalse(t.containsKey("TUNNEL_MODE"))
        assertFalse(t.containsKey("COTTENMAX_TRAFFIC_PROFILE"))
    }

    @Test
    fun `cottenmax writes its options and stays out of other modes`() {
        val max = cotten.copy(tunnelMode = "cottenmax")
        val t = lines(WhiteDnsConfig.renderRuntimeToml(max, WhiteDnsSettings(), "127.0.0.1", 10910))
        assertEquals("\"full\"", t["COTTENMAX_TRAFFIC_PROFILE"])
        assertEquals("false", t["RESOLVER_DISCOVERY_ENABLED"])
        val legacy = lines(WhiteDnsConfig.renderRuntimeToml(cotten, WhiteDnsSettings(), "127.0.0.1", 10910))
        assertFalse(legacy.containsKey("COTTENMAX_TRAFFIC_PROFILE"))
    }

    @Test
    fun `extra keys replace generated ones`() {
        val p = storm.copy(extraToml = "LOG_LEVEL = \"DEBUG\"\nSOME_NEW_KEY = 7")
        val t = lines(WhiteDnsConfig.renderRuntimeToml(p, WhiteDnsSettings(), "127.0.0.1", 10910))
        assertEquals("\"DEBUG\"", t["LOG_LEVEL"])
        assertEquals("7", t["SOME_NEW_KEY"])
        assertEquals(1, WhiteDnsConfig.renderRuntimeToml(p, WhiteDnsSettings(), "127.0.0.1", 10910).lines().count { it.startsWith("LOG_LEVEL ") })
    }

    @Test
    fun `settings are clamped the way the engine accepts them`() {
        val r = WhiteDnsSettings(
            minUploadMtu = "200", maxUploadMtu = "100", rxTxWorkers = "9", tunnelProcessWorkers = "2",
            uploadDuplication = "99", logLevel = "nonsense", mtuTestTimeoutResolvers = "-1",
        ).resolve()
        assertEquals(200, r.maxUploadMtu)
        assertEquals(9, r.tunnelProcessWorkers)
        assertEquals(30, r.uploadDuplication)
        assertEquals("INFO", r.logLevel)
        assertEquals(2.5, r.mtuTestTimeoutResolvers, 0.0)
    }

    @Test
    fun `profile text round trips through the form fields`() {
        val p = cotten.copy(
            tunnelMode = "cottenmax", transportMode = "doh", deliveryMode = "txt-cname", queryRateLimit = "5",
            resolvers = "8.8.8.8\n1.1.1.1:53", extraToml = "SOME_KEY = true",
            max = WhiteDnsMaxOptions(discoveryEnabled = true, discoveryCountry = "ir", holdMs = 300),
        ).normalized()
        val text = WhiteDnsConfig.renderProfileToml(p)
        val back = WhiteDnsConfig.applyProfileToml(WhiteDnsProfile(), text).value
        assertEquals(p, back)
    }

    @Test
    fun `editing the text changes the form`() {
        val edited = WhiteDnsConfig.renderProfileToml(storm).replace("DATA_ENCRYPTION_METHOD = 2", "DATA_ENCRYPTION_METHOD = 5")
        assertEquals(5, WhiteDnsConfig.applyProfileToml(storm, edited).value.encryptionMethod)
    }

    @Test
    fun `settings text round trips and reports keys that belong elsewhere`() {
        val s = WhiteDnsSettings(minDownloadMtu = "500", logLevel = "WARN", baseEncodeData = true, uploadCompression = 3)
        val applied = WhiteDnsConfig.applySettingsToml(WhiteDnsSettings(), WhiteDnsConfig.renderSettingsToml(s) + "\nLISTEN_PORT = 1234")
        assertEquals("500", applied.value.minDownloadMtu)
        assertEquals("WARN", applied.value.logLevel)
        assertTrue(applied.value.baseEncodeData)
        assertEquals(3, applied.value.uploadCompression)
        assertEquals(listOf("LISTEN_PORT"), applied.ignoredKeys)
    }

    @Test
    fun `a mistake is reported with its line`() {
        try {
            WhiteDnsConfig.applySettingsToml(WhiteDnsSettings(), "RX_TX_WORKERS = 4\nMAX_ACTIVE_STREAMS = 0")
            fail("expected a range error")
        } catch (e: WhiteDnsConfigException) {
            assertTrue(e.message!!, e.message!!.startsWith("line 2"))
        }
    }

    @Test
    fun `comments and multi-line arrays are read`() {
        val parsed = WhiteDnsConfig.parseAssignments(
            "# note\nDOMAINS = [\n  \"a.example.com\", # first\n  \"b.example.com\"\n]\nKEY = \"has # inside\" # tail",
        )
        assertEquals(2, parsed.size)
        assertEquals("\"has # inside\"", parsed[1].raw)
        val p = WhiteDnsConfig.applyProfileToml(WhiteDnsProfile(), "DOMAINS = [\n\"a.example.com\",\n\"b.example.com\"\n]").value
        assertEquals(listOf("a.example.com", "b.example.com"), p.domainList())
    }

    @Test
    fun `a key with quotes and backslashes survives`() {
        val p = storm.copy(encryptionKey = "a\"b\\c")
        val t = lines(WhiteDnsConfig.renderRuntimeToml(p, WhiteDnsSettings(), "127.0.0.1", 10910))
        assertEquals("\"a\\\"b\\\\c\"", t["ENCRYPTION_KEY"])
        assertEquals("a\"b\\c", WhiteDnsConfig.applyProfileToml(WhiteDnsProfile(), WhiteDnsConfig.renderProfileToml(p)).value.encryptionKey)
    }

    private fun link(scheme: String, json: String) =
        "$scheme://" + Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT).encode(json.encodeToByteArray())

    @Test
    fun `stormdns link imports`() {
        val l = link(
            "stormdns",
            """{"profile":{"name":"Office","server":{"domain":"t.example.com.","encryption_key":"abc","encryption_method":3}}}""",
        )
        assertTrue(WhiteDnsConfig.isLink(l))
        val (name, p) = WhiteDnsConfig.parseLink(l)!!
        assertEquals("Office", name)
        assertEquals(WhiteDnsProfile.ENGINE_STORM, p.engine)
        assertEquals("t.example.com", p.domains)
        assertEquals(3, p.encryptionMethod)
    }

    @Test
    fun `cottendns link imports its settings`() {
        val l = link(
            "cottendns",
            """{"profile":{"name":"C","server":{"domain":"c.example.com","encryption_key":"abc"},
               "cottenSettings":{"tunnelMode":"cottenmax","configPreset":"survival","transportMode":"doh",
               "cottenMaxOptions":{"trafficProfile":"low","holdMs":300}}}}""",
        )
        val p = WhiteDnsConfig.parseLink(l)!!.second
        assertTrue(p.isCottenDns)
        assertTrue(p.usesCottenMax)
        assertEquals("survival", p.configPreset)
        assertEquals("doh", p.transportMode)
        assertEquals("low", p.max.trafficProfile)
        assertEquals(300, p.max.holdMs)
    }

    @Test
    fun `broken links are refused`() {
        assertNull(WhiteDnsConfig.parseLink("stormdns://%%%"))
        assertNull(WhiteDnsConfig.parseLink(link("stormdns", """{"profile":{"server":{"domain":"","encryption_key":""}}}""")))
        assertNotNull(WhiteDnsConfig.parseLink(link("stormdns", """{"profile":{"server":{"domain":"a.b","encryption_key":"k"}}}""")))
    }

    @Test
    fun `profile needs a domain and a key`() {
        assertFalse(WhiteDnsProfile().isValid)
        assertFalse(WhiteDnsProfile(domains = "a.b").isValid)
        assertTrue(WhiteDnsProfile(domains = "a.b", encryptionKey = "k").isValid)
    }

    private val soarKey = "A".repeat(43) + "="
    private val soar = WhiteDnsProfile(engine = WhiteDnsProfile.ENGINE_SOAR, domains = "s.example.com", encryptionKey = soarKey)

    @Test
    fun `soar needs a zone and a 32 byte public key`() {
        assertTrue(soar.isValid)
        assertFalse(soar.copy(encryptionKey = "short").isValid)
        assertFalse(soar.copy(domains = "a.example.com, b.example.com").isValid)
        assertFalse(soar.copy(domains = "x".repeat(60) + ".com").isValid)
        assertFalse(soar.copy(domains = "nodot").isValid)
    }

    @Test
    fun `soar json has exactly the keys the engine accepts`() {
        val json = WhiteDnsConfig.renderSoarJson(soar, "127.0.0.1", 10910, listOf("8.8.8.8", "8.8.8.8", "1.1.1.1"))
        val root = kotlinx.serialization.json.Json.parseToJsonElement(json) as kotlinx.serialization.json.JsonObject
        assertEquals(
            setOf("zone", "server_public_key", "system_resolvers", "listen", "username", "password", "soar"),
            root.keys,
        )
        assertEquals("127.0.0.1:10910", (root["listen"] as kotlinx.serialization.json.JsonPrimitive).content)
        assertEquals(2, (root["system_resolvers"] as kotlinx.serialization.json.JsonArray).size)
        val soarObj = root["soar"] as kotlinx.serialization.json.JsonObject
        assertEquals(
            setOf(
                "transport", "discovery", "country", "discoveryWant", "discoveryConcurrency", "discoveryPaceMs",
                "safeQName", "maxQName", "maxAnswer", "holdMs", "maxInflight", "maxPathWindow", "maxStreams",
                "streamWindowKiB", "sendBufferKiB", "sessionIdleSeconds", "dialTimeoutSeconds",
            ),
            soarObj.keys,
        )
    }

    @Test
    fun `soar json refuses an invalid server`() {
        try {
            WhiteDnsConfig.renderSoarJson(soar.copy(encryptionKey = "nope"), "127.0.0.1", 10910, emptyList())
            fail("expected a refusal")
        } catch (_: WhiteDnsConfigException) {
        }
    }

    @Test
    fun `soar options are clamped and presets are recognised`() {
        val o = WhiteDnsSoarOptions(maxQName = 500, safeQName = 50, sessionIdleSeconds = 500, transport = "weird").normalized()
        assertEquals(253, o.maxQName)
        assertEquals(101, o.safeQName)
        assertEquals(110, o.sessionIdleSeconds)
        assertEquals("auto", o.transport)
        assertEquals("balanced", WhiteDnsSoarOptions().presetName())
        val conservative = WhiteDnsSoarOptions().withPreset("conservative")
        assertEquals(32, conservative.maxInflight)
        assertEquals("conservative", conservative.presetName())
        assertEquals("custom", conservative.copy(maxStreams = 99).presetName())
    }

    @Test
    fun `soar profile text lists soar keys only and round trips`() {
        val p = soar.copy(soar = WhiteDnsSoarOptions(discovery = false, country = "RU", maxInflight = 64)).normalized()
        val text = WhiteDnsConfig.renderProfileToml(p)
        assertTrue(text.contains("SOAR_MAX_INFLIGHT = 64"))
        assertFalse(text.contains("COTTENMAX_"))
        assertFalse(text.contains("DATA_ENCRYPTION_METHOD"))
        assertEquals(p, WhiteDnsConfig.applyProfileToml(WhiteDnsProfile(), text).value)
    }

    @Test
    fun `soar link imports its key resolvers and settings`() {
        val l = link(
            "soar",
            """{"profile":{"name":"S","server":{"domain":"s.example.com","public_key":"$soarKey"},
               "resolvers":["8.8.8.8","1.1.1.1:53","10.0.0.0/8"],
               "soarSettings":{"transport":"only","discovery":false,"maxInflight":64}}}""",
        )
        val (name, p) = WhiteDnsConfig.parseLink(l)!!
        assertEquals("S", name)
        assertTrue(p.isSoar)
        assertEquals(soarKey, p.encryptionKey)
        assertEquals(listOf("8.8.8.8", "1.1.1.1:53"), p.resolverList())
        assertEquals("only", p.soar.transport)
        assertFalse(p.soar.discovery)
        assertEquals(64, p.soar.maxInflight)
    }

    @Test
    fun `soar link with a bad key is refused`() {
        val l = link("soar", """{"profile":{"name":"S","server":{"domain":"s.example.com","public_key":"bad"}}}""")
        assertNull(WhiteDnsConfig.parseLink(l))
    }
}
