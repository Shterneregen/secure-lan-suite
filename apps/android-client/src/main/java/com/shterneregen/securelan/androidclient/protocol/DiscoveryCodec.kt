package com.shterneregen.securelan.androidclient.protocol

import com.shterneregen.securelan.androidclient.model.DiscoveredPeer
import java.net.InetAddress
import java.nio.charset.StandardCharsets
import java.util.Base64

object DiscoveryCodec {
    private const val PREFIX = "SECURELAN_DISCOVERY_V1"

    fun encode(peerId: String, nickname: String, chatPort: Int, filePort: Int): ByteArray {
        val text = listOf(PREFIX, encodeText(peerId), encodeText(nickname), chatPort.toString(), filePort.toString())
            .joinToString("|")
        return text.toByteArray(StandardCharsets.UTF_8)
    }

    fun decode(bytes: ByteArray, length: Int, address: InetAddress): DiscoveredPeer {
        val payload = String(bytes, 0, length, StandardCharsets.UTF_8)
        val parts = payload.split("|", ignoreCase = false, limit = 5)
        require(parts.size == 5 && parts[0] == PREFIX) { "Unsupported discovery message" }
        return DiscoveredPeer(
            peerId = decodeText(parts[1]),
            nickname = decodeText(parts[2]),
            host = address.hostAddress ?: address.hostName,
            chatPort = parsePort(parts[3]),
            filePort = parsePort(parts[4]),
        )
    }

    private fun encodeText(value: String): String = Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun decodeText(value: String): String = String(
        Base64.getUrlDecoder().decode(value),
        StandardCharsets.UTF_8,
    )

    private fun parsePort(value: String): Int = value.toInt().also { port ->
        require(port in 1..65535) { "Port out of range" }
    }
}
