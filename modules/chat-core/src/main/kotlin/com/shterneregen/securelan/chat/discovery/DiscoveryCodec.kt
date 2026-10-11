package com.shterneregen.securelan.chat.discovery

import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.Objects

/**
 * Codec that serializes and deserializes peer discovery broadcast messages.
 *
 * ## Wire format
 * A discovery message is encoded as a pipe-delimited text string:
 * ```
 * SECURELAN_DISCOVERY_V1|<peerId-b64url>|<nickname-b64url>|<chatPort>|<filePort>
 * ```
 * All text fields are encoded with URL-safe Base64 (no padding) to safely carry arbitrary
 * Unicode characters across a plain-UDP datagram.
 *
 * The codec is symmetric: `decode(encode(m)) == m` for any valid [Message].
 */
object DiscoveryCodec {
    private const val PREFIX = "SECURELAN_DISCOVERY_V1"

    /**
     * Encodes [message] to its wire-format string representation.
     *
     * @param message The [Message] to encode; must not be `null`.
     * @return A pipe-delimited, single-line wire string.
     * @throws NullPointerException if [message] is `null`.
     */
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

    /**
     * Decodes a wire-format string back into a [Message].
     *
     * @param payload The wire string to decode; may be `null`.
     * @return The decoded [Message].
     * @throws IllegalArgumentException if [payload] is `null`, does not start with the expected
     *                                  prefix, does not contain exactly 5 pipe-separated fields,
     *                                  or contains port values outside the 1–65 535 range.
     */
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

    /**
     * Convenience function that converts [message] directly to a UTF-8 byte array.
     *
     * Equivalent to `encode(message).toByteArray(StandardCharsets.UTF_8)`.
     *
     * @param message The [Message] to convert; must not be `null`.
     * @return The UTF-8 encoded wire bytes.
     */
    @JvmStatic
    fun toBytes(message: Message): ByteArray = encode(message).toByteArray(StandardCharsets.UTF_8)

    /**
     * Convenience function that decodes a [Message] from the first [length] bytes of [data].
     *
     * @param data   The raw byte array; must not be `null`.
     * @param length The number of bytes to consider; must be in `0..data.size`.
     * @return The decoded [Message].
     * @throws IllegalArgumentException if [length] is negative or exceeds `data.size`, or if the
     *                                  resulting string is not a valid discovery message.
     */
    @JvmStatic
    fun fromBytes(data: ByteArray, length: Int): Message {
        Objects.requireNonNull(data, "data")
        require(length in 0..data.size) { "length must be between 0 and data length" }
        return decode(String(data, 0, length, StandardCharsets.UTF_8))
    }

    private fun encodeText(value: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun decodeText(value: String): String =
        String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)

    private fun parsePort(value: String, fieldName: String): Int = try {
        val port = value.toInt()
        require(port in 1..65_535) { "$fieldName must be between 1 and 65535" }
        port
    } catch (e: NumberFormatException) {
        throw IllegalArgumentException("$fieldName must be a valid TCP/UDP port", e)
    }

    /**
     * A peer's discovery advertisement, containing the information needed to identify and
     * connect to the peer.
     *
     * @property peerId    A stable unique identifier for the peer.
     * @property nickname  The human-readable display name.
     * @property chatPort  The TCP chat port (1–65 535).
     * @property filePort  The TCP file-transfer port (1–65 535).
     * @throws IllegalArgumentException if either port is outside the 1–65 535 range.
     * @throws NullPointerException     if [peerId] or [nickname] is `null`.
     */
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
