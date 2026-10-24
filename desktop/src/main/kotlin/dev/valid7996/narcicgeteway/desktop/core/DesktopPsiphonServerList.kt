package dev.valid7996.narcicgeteway.desktop.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.util.zip.InflaterInputStream

object DesktopPsiphonServerList {
    private const val ENTRIES_FILE = "psiphon-servers.txt"

    const val SIGNING_KEY =
        "MIICIDANBgkqhkiG9w0BAQEFAAOCAg0AMIICCAKCAgEAt7Ls+/39r+T6zNW7GiVpJfzq/xvL9SBH" +
            "5rIFnk0RXYEYavax3WS6HOD35eTAqn8AniOwiH+DOkvgSKF2caqk/y1dfq47Pdymtwzp9ikpB1C5" +
            "OfAysXzBiwVJlCdajBKvBZDerV1cMvRzCKvKwRmvDmHgphQQ7WfXIGbRbmmk6opMBh3roE42Kcot" +
            "LFtqp0RRwLtcBRNtCdsrVsjiI1Lqz/lH+T61sGjSjQ3CHMuZYSQJZo/KrvzgQXpkaCTdbObxHqb6" +
            "/+i1qaVOfEsvjoiyzTxJADvSytVtcTjijhPEV6XskJVHE1Zgl+7rATr/pDQkw6DPCNBS1+Y6fy7G" +
            "stZALQXwEDN/qhQI9kWkHijT8ns+i1vGg00Mk/6J75arLhqcodWsdeG/M/moWgqQAnlZAGVtJI1O" +
            "geF5fsPpXu4kctOfuZlGjVZXQNW34aOzm8r8S0eVZitPlbhcPiR4gT/aSMz/wd8lZlzZYsje/Jr8" +
            "u/YtlwjjreZrGRmG8KMOzukV3lLmMppXFMvl4bxv6YFEmIuTsOhbLTwFgh7KYNjodLj/LsqRVfwz" +
            "31PgWQFTEPICV7GCvgVlPRxnofqKSjgTWI4mxDhBpVcATvaoBl1L/6WLbFvBsoAUBItWwctO2xal" +
            "KxF5szhGm8lccoc5MZr8kfE0uxMgsxz4er68iCID+rsCAQM="

    private val json = Json { ignoreUnknownKeys = true }

    fun entriesFile(workDir: File): File? {
        val entries = File(workDir, ENTRIES_FILE)
        if (entries.isFile && entries.length() > 1024) return entries

        val stream = DesktopPsiphonServerList::class.java.getResourceAsStream("/psiphon/psiphon_servers.dat")
            ?: DesktopPsiphonServerList::class.java.getResourceAsStream("/psiphon_servers.dat")
        val packed = stream?.use { it.readBytes() }
            ?: File("desktop/src/main/resources/psiphon/psiphon_servers.dat").takeIf { it.isFile }?.readBytes()
            ?: File("app/src/main/assets/psiphon_servers.dat").takeIf { it.isFile }?.readBytes()
            ?: return entries.takeIf { it.isFile }

        return try {
            val text = unpack(packed, SIGNING_KEY)
            workDir.mkdirs()
            entries.writeText(text)
            entries
        } catch (e: Exception) {
            System.err.println("Could not unpack Psiphon server list: ${e.message}")
            entries.takeIf { it.isFile }
        }
    }

    @Throws(IOException::class)
    fun unpack(packed: ByteArray, signingKey: String): String {
        val text = try {
            InflaterInputStream(ByteArrayInputStream(packed)).use { it.readBytes() }.toString(Charsets.UTF_8)
        } catch (e: IOException) {
            throw IOException("the list is not compressed the way Psiphon writes it", e)
        }
        val root = try {
            json.parseToJsonElement(text).jsonObject
        } catch (e: RuntimeException) {
            throw IOException("the list is not the package Psiphon writes", e)
        }
        val data = root["data"]?.jsonPrimitive?.content ?: throw IOException("no data in list")
        val keyDigest = root["signingPublicKeyDigest"]?.jsonPrimitive?.content ?: throw IOException("no key digest")
        val signature = root["signature"]?.jsonPrimitive?.content ?: throw IOException("no signature")

        try {
            val expected = MessageDigest.getInstance("SHA-256").digest(signingKey.toByteArray(Charsets.US_ASCII))
            val b64 = Base64.getDecoder()
            if (!b64.decode(keyDigest).contentEquals(expected)) throw IOException("the list is signed with another key")
            val key = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(b64.decode(signingKey)))
            val verifier = Signature.getInstance("SHA256withRSA")
            verifier.initVerify(key)
            verifier.update(data.toByteArray(Charsets.UTF_8))
            if (!verifier.verify(b64.decode(signature))) throw IOException("signature check failed")
        } catch (e: GeneralSecurityException) {
            throw IOException("signature check error", e)
        }

        return data
    }
}
