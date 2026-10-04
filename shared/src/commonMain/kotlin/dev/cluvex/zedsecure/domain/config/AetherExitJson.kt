package dev.cluvex.zedsecure.domain.config

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * The Xray configuration that carries the exit-node an Aether core dials out through: a SOCKS
 * inbound on the loopback and one freedom outbound, tagged exit-node, that carries the profile's
 * finalMask and dialMode. The core is told the inbound as its --upstream, so its own dials leave
 * Xray by the exit-node. Ported from PattNG's CoreOutboundBuilder.toOutboundAetherExit.
 */
object AetherExitJson {

    const val EXIT_SOCKS_PORT = 10891

    /** The exit-node outbound for [finalMask] and [dialMode], as the core's --upstream leaves Xray by. */
    fun exitOutbound(finalMask: String?, dialMode: String?): JsonObject = buildJsonObject {
        put("tag", "exit-node")
        put("protocol", "freedom")
        val mask = finalMask?.trim().orEmpty()
        val mode = dialMode?.trim().orEmpty()
        if (mask.isNotEmpty() || mode.isNotEmpty()) {
            putJsonObject("streamSettings") {
                // A freedom outbound has no transport; the stream settings carry the mask and the sockopt alone.
                if (mask.isNotEmpty()) {
                    runCatching { Json.parseToJsonElement(mask).jsonObject }.getOrNull()?.let { put("finalmask", it) }
                }
                if (mode.isNotEmpty()) {
                    putJsonObject("sockopt") {
                        put("dialMode", mode)
                    }
                }
            }
        }
    }

    /** The whole configuration the exit Xray runs on loopback [port]. */
    fun exitConfig(finalMask: String?, dialMode: String?, port: Int = EXIT_SOCKS_PORT): String {
        val config = buildJsonObject {
            putJsonObject("log") {
                put("loglevel", "warning")
            }
            put("inbounds", kotlinx.serialization.json.buildJsonArray {
                add(buildJsonObject {
                    put("listen", "127.0.0.1")
                    put("port", port)
                    put("protocol", "socks")
                    putJsonObject("settings") {
                        put("udp", true)
                    }
                })
            })
            put("outbounds", kotlinx.serialization.json.buildJsonArray {
                add(exitOutbound(finalMask, dialMode))
            })
        }
        return config.toString()
    }
}
