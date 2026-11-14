package dev.valid7996.narcicgeteway.crypto

/**
 * Proprietary Narcic Gateway Subscription Encryption.
 * Protects subscription payloads using AES-256-GCM + Deflate compression
 * so that third-party VPN apps (v2rayNG, Hiddify, sing-box, etc.) cannot
 * parse, inspect, or reuse Narcic subscriptions.
 */
expect object NarcicSubscriptionCrypto {
    fun isEncryptedSubscription(text: String): Boolean
    fun decryptSubscription(text: String): String
    fun encryptSubscription(plainText: String): String
}
