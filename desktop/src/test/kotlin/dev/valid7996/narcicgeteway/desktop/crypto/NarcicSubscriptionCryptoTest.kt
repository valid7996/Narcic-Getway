package dev.valid7996.narcicgeteway.desktop.crypto

import dev.valid7996.narcicgeteway.crypto.NarcicSubscriptionCrypto
import dev.valid7996.narcicgeteway.data.config.DefaultSubscriptionVault
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NarcicSubscriptionCryptoTest {

    @Test
    fun default_subscription_vault_decodes_all_endpoints_correctly() {
        assertEquals(
            "https://raw.githubusercontent.com/validbv7996/Narcic_APK/refs/heads/main/vlessirancell.txt",
            DefaultSubscriptionVault.irancellUrl
        )
        assertEquals(
            "https://raw.githubusercontent.com/validbv7996/Narcic_APK/refs/heads/main/vlessmci.txt",
            DefaultSubscriptionVault.mciUrl
        )
        assertEquals(
            "https://raw.githubusercontent.com/validbv7996/Narcic_APK/refs/heads/main/ngwirguard.txt",
            DefaultSubscriptionVault.wireguardUrl
        )
        assertEquals(
            "https://raw.githubusercontent.com/validbv7996/Narcic_APK/refs/heads/main/windscribewg.txt",
            DefaultSubscriptionVault.windscribeWgUrl
        )
        assertEquals(
            "https://raw.githubusercontent.com/validbv7996/Narcic_APK/refs/heads/main/windscribe_openvpn.json",
            DefaultSubscriptionVault.windscribeOpenvpnUrl
        )
        assertEquals(
            "https://raw.githubusercontent.com/validbv7996/Narcic_APK/refs/heads/main/windscribe_ikev2.txt",
            DefaultSubscriptionVault.windscribeIkev2Url
        )
        assertEquals(
            "https://raw.githubusercontent.com/validbv7996/Narcic_APK/refs/heads/main/Narcic-Spooof.txt",
            DefaultSubscriptionVault.spoofUrl
        )
        assertEquals(
            "https://raw.githubusercontent.com/hamedp-71/N_sub_cheker/refs/heads/patch-1/final.txt",
            DefaultSubscriptionVault.hamedvpnsUrl
        )
    }

    @Test
    fun encrypt_and_decrypt_subscription_roundtrip_with_multiline_content() {
        val configs = listOf(
            "vless://433740ea-3d53-43bf-8887-a90af78ae753@172.67.217.240:443?security=tls#Server1",
            "wireguard://abc@5.6.7.8:51820?publickey=xyz#Server2",
            "vmess://eyJ2IjoiMiIsInBzIjoiVGVzdCJ9"
        ).joinToString("\n")

        val encrypted = NarcicSubscriptionCrypto.encryptSubscription(configs)
        assertTrue(encrypted.startsWith("NRGSUB1:"))
        assertTrue(NarcicSubscriptionCrypto.isEncryptedSubscription(encrypted))
        assertFalse(NarcicSubscriptionCrypto.isEncryptedSubscription("vless://normal-link"))

        val decrypted = NarcicSubscriptionCrypto.decryptSubscription(encrypted)
        assertEquals(configs, decrypted)
    }
}
