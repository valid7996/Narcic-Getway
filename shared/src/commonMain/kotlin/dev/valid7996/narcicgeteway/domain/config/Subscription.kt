package dev.valid7996.narcicgeteway.domain.config

import kotlinx.serialization.Serializable

@Serializable
data class Subscription(
    val id: String,
    val name: String,
    val url: String,
    val enabled: Boolean = true,
    val lastUpdated: Long = 0,
    val serverCount: Int = 0,

    val userAgent: String? = null,

    val upload: Long? = null,

    val download: Long? = null,

    val total: Long? = null,

    val expireAt: Long? = null,

    val title: String? = null,

    val updateIntervalHours: Int? = null,
    val preset: SubscriptionPreset = SubscriptionPreset(),

    val supportUrl: String? = null,

    val webPageUrl: String? = null,
) {
    val used: Long? get() = if (upload == null && download == null) null else (upload ?: 0L) + (download ?: 0L)

    val isQuotaExhausted: Boolean get() = (total != null && total > 0 && (used ?: 0L) >= total)

    val isExpired: Boolean get() = (expireAt != null && expireAt <= dev.valid7996.narcicgeteway.platform.currentTimeMillis())

    fun formatQuotaBadge(): String? {
        val u = used
        val t = total
        val exp = expireAt
        if (t == null && exp == null && u == null) return null

        val now = dev.valid7996.narcicgeteway.platform.currentTimeMillis()
        val parts = mutableListOf<String>()

        if (t != null && t > 0) {
            val totalGb = t.toDouble() / (1024.0 * 1024.0 * 1024.0)
            if (u != null) {
                val rem = (t - u).coerceAtLeast(0L)
                val remGb = rem.toDouble() / (1024.0 * 1024.0 * 1024.0)
                if (totalGb >= 1.0) {
                    parts.add("${roundOne(remGb)}/${roundOne(totalGb)} GB")
                } else {
                    val remMb = rem.toDouble() / (1024.0 * 1024.0)
                    val totMb = t.toDouble() / (1024.0 * 1024.0)
                    parts.add("${roundOne(remMb)}/${roundOne(totMb)} MB")
                }
            } else {
                parts.add("${roundOne(totalGb)} GB")
            }
        } else if (u != null && u > 0) {
            val usedGb = u.toDouble() / (1024.0 * 1024.0 * 1024.0)
            if (usedGb >= 1.0) {
                parts.add("${roundOne(usedGb)} GB")
            } else {
                val usedMb = u.toDouble() / (1024.0 * 1024.0)
                parts.add("${roundOne(usedMb)} MB")
            }
        }

        if (exp != null && exp > 0) {
            val days = ((exp - now) / 86_400_000L).toInt()
            when {
                days < 0 -> parts.add("منقضی")
                days == 0 -> parts.add("امروز")
                else -> parts.add("$days روز")
            }
        }

        return parts.takeIf { it.isNotEmpty() }?.joinToString(" • ")
    }

    private fun roundOne(v: Double): String {
        val r = kotlin.math.round(v * 10.0) / 10.0
        return if (r % 1.0 == 0.0) r.toLong().toString() else r.toString()
    }
}

object SubscriptionParser {
    fun extractLinks(body: String): List<String> {
        val direct = splitLines(body)
        if (direct.isNotEmpty()) return direct

        val decoded = runCatching { ConfigParser.decodeBase64Utf8(body) }.getOrNull()
        if (decoded != null) {
            val fromDecoded = splitLines(decoded)
            if (fromDecoded.isNotEmpty()) return fromDecoded
        }

        return body.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { line ->
                if (ConfigParser.isSupportedLink(line)) line
                else runCatching { ConfigParser.decodeBase64Utf8(line) }.getOrNull()?.trim()?.takeIf { ConfigParser.isSupportedLink(it) }
            }
            .toList()
    }

    private fun splitLines(text: String): List<String> =
        text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && ConfigParser.isSupportedLink(it) }
            .toList()
}
