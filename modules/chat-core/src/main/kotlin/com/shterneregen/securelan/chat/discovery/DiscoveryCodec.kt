package com.shterneregen.securelan.chat.discovery

import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.Objects

object DiscoveryCodec {
    private const val PREFIX = "SECURELAN_DISCOVERY_V1"

    @JvmStatic
    fun encode(message: Message): String {
        Objects.requireNonNull(message, "message")
        return listOf(
            PREFIX,
            encodeText(message.peerId),
            encodeText(message.nickname),
            message.chatPort.toString(),
            message.filePort.toString(),
        ).joinToString("|")
    }

    @JvmStatic
    fun decode(payload: String?): Message {
        val parts = payload?.split("|", ignoreCase = false, limit = 0)?.toTypedArray() ?: emptyArray()
        if (parts.size != 5 || PREFIX != parts[0]) {
            throw IllegalArgumentException("Unsupported discovery message")
        }
        val peerId = decodeText(parts[1])
        val nickname = decodeText(parts[2])
        val chatPort = parsePort(parts[3], "chatPort")
        val filePort = parsePort(parts[4], "filePort")
        return Message(peerId, nickname, chatPort, filePort)
    }

    @JvmStatic
    fun toBytes(message: Message): ByteArray = encode(message).toByteArray(StandardCharsets.UTF_8)

    @JvmStatic
    fun fromBytes(data: ByteArray, length: Int): Message {
        Objects.requireNonNull(data, "data")
        require(length in 0..data.size) { "length must be between 0 and data length" }
        return decode(String(data, 0, length, StandardCharsets.UTF_8))
    }

    private fun encodeText(value: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun decodeText(value: String): String = String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)

    private fun parsePort(value: String, fieldName: String): Int = try {
        val port = value.toInt()
        require(port in 1..65_535) { "$fieldName must be between 1 and 65535" }
        port
    } catch (e: NumberFormatException) {
        throw IllegalArgumentException("$fieldName must be a valid TCP/UDP port", e)
    }

    @JvmRecord
    data class Message(
        val peerId: String,
        val nickname: String,
        val chatPort: Int,
        val filePort: Int,
    ) {
        init {
            Objects.requireNonNull(peerId, "peerId")
            Objects.requireNonNull(nickname, "nickname")
            require(chatPort in 1..65_535) { "chatPort must be between 1 and 65535" }
            require(filePort in 1..65_535) { "filePort must be between 1 and 65535" }
        }
    }
}
