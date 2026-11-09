@file:OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)

package dev.valid7996.narcicgeteway.domain.config

import dev.valid7996.narcicgeteway.crypto.ZsxCrypto
import dev.valid7996.narcicgeteway.crypto.ZsxSealRequest
import kotlin.io.encoding.Base64
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Ikev2Profile(

    val server: String = "",

    val remoteId: String = "",

    val localId: String = "",

    val authType: Ikev2Auth = Ikev2Auth.EAP_MSCHAPV2,

    val username: String = "",

    val password: String = "",

    val psk: String = "",

    val caCertPem: String = "",

    val userCertAlias: String = "",

    val ikeProposal: String = "",

    val espProposal: String = "",

    val mtu: Int = 1280,
) {
    val effectiveRemoteId: String get() = remoteId.ifBlank { server }

    val effectiveAuth: Ikev2Auth
        get() = if (authType == Ikev2Auth.EAP_TLS) Ikev2Auth.CERTIFICATE else authType

    val isValid: Boolean
        get() = server.isNotBlank() && when (effectiveAuth) {
            Ikev2Auth.EAP_MSCHAPV2 -> username.isNotBlank() && password.isNotBlank()
            Ikev2Auth.CERTIFICATE -> userCertAlias.isNotBlank()
            Ikev2Auth.EAP_TLS -> false
            Ikev2Auth.PSK -> psk.isNotBlank()
        }

    fun plainPassword(): String = openSecret(password)

    fun plainPsk(): String = openSecret(psk)

    companion object {
        const val SEALED_PREFIX = "zsx:"

        fun sealPassword(plain: String): String = sealSecret(plain)

        fun sealPsk(plain: String): String = sealSecret(plain)

        private fun sealSecret(plain: String): String {
            if (plain.isBlank()) return ""
            if (plain.startsWith(SEALED_PREFIX)) return plain
            return runCatching {
                val sealed = ZsxCrypto.seal(
                    ZsxSealRequest(
                        configPayload = plain,
                        nameEn = "", nameFa = "", note = "", expiresAt = null, password = null,
                    ),
                )
                SEALED_PREFIX + Base64.encode(sealed)
            }.getOrDefault(plain)
        }

        private fun openSecret(stored: String): String {
            if (!stored.startsWith(SEALED_PREFIX)) return stored
            val b64 = stored.removePrefix(SEALED_PREFIX)
            return runCatching { ZsxCrypto.open(Base64.decode(b64), password = null) }.getOrDefault("")
        }

        fun fromLink(link: String): Pair<String, Ikev2Profile>? {
            val trimmed = link.trim().trim('\uFEFF').trim()
            if (!trimmed.startsWith("ikev2://", ignoreCase = true)) return null
            return runCatching {
                val withoutScheme = trimmed.substring(8)
                val fragment = if (withoutScheme.contains('#')) withoutScheme.substringAfter('#') else ""
                val name = if (fragment.isNotEmpty()) runCatching { ConfigParser.percentDecode(fragment) }.getOrDefault(fragment) else ""
                val mainPart = withoutScheme.substringBefore('#')
                val userInfo = if (mainPart.contains('@')) mainPart.substringBefore('@') else ""
                val hostAndQuery = if (mainPart.contains('@')) mainPart.substringAfter('@') else mainPart
                val hostPort = hostAndQuery.substringBefore('?')
                val query = if (hostAndQuery.contains('?')) hostAndQuery.substringAfter('?') else ""

                val username = if (userInfo.contains(':')) {
                    runCatching { ConfigParser.percentDecode(userInfo.substringBefore(':')) }.getOrDefault(userInfo.substringBefore(':'))
                } else {
                    userInfo
                }
                val password = if (userInfo.contains(':')) {
                    runCatching { ConfigParser.percentDecode(userInfo.substringAfter(':')) }.getOrDefault(userInfo.substringAfter(':'))
                } else {
                    ""
                }

                val server = if (hostPort.startsWith("[")) hostPort.substringBefore(']') + "]" else hostPort.substringBefore(':')
                val queryParams = query.split('&').filter { it.contains('=') }.associate {
                    it.substringBefore('=').lowercase() to runCatching { ConfigParser.percentDecode(it.substringAfter('=')) }.getOrDefault(it.substringAfter('='))
                }
                val remoteId = queryParams["remoteid"] ?: server
                val localId = queryParams["localid"] ?: ""
                val mtu = queryParams["mtu"]?.toIntOrNull() ?: 1280

                name to Ikev2Profile(
                    server = server,
                    remoteId = remoteId,
                    localId = localId,
                    authType = Ikev2Auth.EAP_MSCHAPV2,
                    username = username,
                    password = password,
                    mtu = mtu,
                )
            }.getOrNull()
        }
    }
}

@Serializable
enum class Ikev2Auth {
    @SerialName("eap_mschapv2")
    EAP_MSCHAPV2,

    @SerialName("certificate")
    CERTIFICATE,

    @SerialName("eap_tls")
    EAP_TLS,

    @SerialName("psk")
    PSK,
}
