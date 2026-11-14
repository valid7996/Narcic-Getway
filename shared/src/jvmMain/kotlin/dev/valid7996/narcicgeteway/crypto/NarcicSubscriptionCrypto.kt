package dev.valid7996.narcicgeteway.crypto

import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.zip.Deflater
import java.util.zip.Inflater
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
actual object NarcicSubscriptionCrypto {
    const val PREFIX = "NRGSUB1:"
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BITS = 128
    private const val NONCE_LENGTH_BYTES = 12
    private val AAD = "NRG-SUB-V1".toByteArray(Charsets.UTF_8)

    // Proprietary key derivation for Narcic Gateway subscription payload encryption
    private val SECRET_KEY: ByteArray by lazy {
        MessageDigest.getInstance("SHA-256")
            .digest("NarcicGateway://ProtectedSubscription/v1@validbv7996".toByteArray(Charsets.UTF_8))
    }

    private val rng = SecureRandom()

    actual fun isEncryptedSubscription(text: String): Boolean {
        val t = text.trim()
        return t.startsWith(PREFIX, ignoreCase = true) || t.startsWith("nrgsub://", ignoreCase = true)
    }

    actual fun encryptSubscription(plainText: String): String {
        val raw = plainText.toByteArray(Charsets.UTF_8)
        val compressed = deflate(raw)

        val nonce = ByteArray(NONCE_LENGTH_BYTES)
        rng.nextBytes(nonce)

        val cipher = Cipher.getInstance(ALGORITHM)
        val keySpec = SecretKeySpec(SECRET_KEY, "AES")
        val paramSpec = GCMParameterSpec(TAG_LENGTH_BITS, nonce)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, paramSpec)
        cipher.updateAAD(AAD)
        val ciphertext = cipher.doFinal(compressed)

        val combined = nonce + ciphertext
        val b64 = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT).encode(combined)
        return "$PREFIX$b64"
    }

    actual fun decryptSubscription(text: String): String {
        val t = text.trim()
        val payload = when {
            t.startsWith(PREFIX, ignoreCase = true) -> t.substring(PREFIX.length).trim()
            t.startsWith("nrgsub://", ignoreCase = true) -> t.substring("nrgsub://".length).trim()
            else -> throw IllegalArgumentException("Not an encrypted subscription payload")
        }
        val combined = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL).decode(payload)
        require(combined.size > NONCE_LENGTH_BYTES + 16) { "Encrypted payload too short" }

        val nonce = combined.copyOfRange(0, NONCE_LENGTH_BYTES)
        val ciphertext = combined.copyOfRange(NONCE_LENGTH_BYTES, combined.size)

        val cipher = Cipher.getInstance(ALGORITHM)
        val keySpec = SecretKeySpec(SECRET_KEY, "AES")
        val paramSpec = GCMParameterSpec(TAG_LENGTH_BITS, nonce)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, paramSpec)
        cipher.updateAAD(AAD)
        val compressed = cipher.doFinal(ciphertext)

        val decompressed = inflate(compressed)
        return String(decompressed, Charsets.UTF_8)
    }

    private fun deflate(bytes: ByteArray): ByteArray {
        val deflater = Deflater(Deflater.BEST_COMPRESSION)
        deflater.setInput(bytes)
        deflater.finish()
        val out = ByteArrayOutputStream(bytes.size)
        val buf = ByteArray(8192)
        while (!deflater.finished()) {
            val count = deflater.deflate(buf)
            out.write(buf, 0, count)
        }
        deflater.end()
        return out.toByteArray()
    }

    private fun inflate(bytes: ByteArray): ByteArray {
        val inflater = Inflater()
        inflater.setInput(bytes)
        val out = ByteArrayOutputStream(bytes.size * 2)
        val buf = ByteArray(8192)
        while (!inflater.finished()) {
            val count = inflater.inflate(buf)
            if (count == 0 && inflater.needsInput()) break
            out.write(buf, 0, count)
        }
        inflater.end()
        return out.toByteArray()
    }
}
