package dev.cluvex.zedsecure.domain.config

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Builds the minimal Xray exit configuration that the Aether core process dials out through,
 * so that finalMask and dialMode apply to what leaves the device.
 * Ported from PattNG's CoreOutboundBuilder.toOutboundAetherExit.
 */
object AetherExitJson {

    const val EXIT_SOCKS_PORT = 10891

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun exitConfig(finalMaskJson: String?, dialMode: String?): String {
        val maskObj = finalMaskJson?.trim()?.takeIf { it.isNotEmpty() }?.let { raw ->
            runCatching { json.parseToJsonElement(raw) as? JsonObject }.getOrNull()
        }
        val dial = dialMode?.trim()?.takeIf { it.isNotEmpty() }

        val root = buildJsonObject {
            putJsonObject("log") {
                put("loglevel", "warning")
            }
            putJsonArray("inbounds") {
                add(
                    buildJsonObject {
                        put("tag", "socks-exit")
                        put("port", EXIT_SOCKS_PORT)
                        put("listen", "127.0.0.1")
                        put("protocol", "socks")
                        putJsonObject("settings") {
                            put("auth", "noauth")
                            put("udp", true)
                        }
                    },
                )
            }
            putJsonArray("outbounds") {
                add(
                    buildJsonObject {
                        put("tag", "freedom-exit")
                        put("protocol", "freedom")
                        putJsonObject("settings") {
                            if (dial != null) put("dialMode", dial)
                            if (maskObj != null) put("finalMask", maskObj)
                        }
                    },
                )
            }
        }
        return root.toString()
    }
}
