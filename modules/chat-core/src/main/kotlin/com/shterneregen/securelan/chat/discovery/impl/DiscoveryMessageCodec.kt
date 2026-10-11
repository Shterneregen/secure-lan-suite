package com.shterneregen.securelan.chat.discovery.impl

import com.shterneregen.securelan.chat.discovery.DiscoveryCodec

/**
 * Internal codec that translates between the internal [DiscoveryMessage] type and the public
 * [DiscoveryCodec] wire format.
 *
 * This object is a thin adapter: it converts [DiscoveryMessage] to/from [DiscoveryCodec.Message]
 * and delegates all encoding/decoding logic to [DiscoveryCodec], keeping the `impl` package
 * insulated from external callers.
 *
 * All methods mirror their [DiscoveryCodec] counterparts; refer to that class for wire-format
 * details and exception contracts.
 */
internal object DiscoveryMessageCodec {

    /**
     * Encodes [message] to the wire-format string.
     *
     * @param message The internal message to encode.
     * @return A pipe-delimited discovery wire string.
     */
    @JvmStatic
    fun encode(message: DiscoveryMessage): String = DiscoveryCodec.encode(toPublicMessage(message))

    /**
     * Decodes a wire-format string into a [DiscoveryMessage].
     *
     * @param payload The wire string to decode; may be `null`.
     * @return The decoded [DiscoveryMessage].
     * @throws IllegalArgumentException if the payload is invalid.
     */
    @JvmStatic
    fun decode(payload: String?): DiscoveryMessage = fromPublicMessage(DiscoveryCodec.decode(payload))

    /**
     * Encodes [message] directly to a UTF-8 byte array.
     *
     * @param message The internal message to encode.
     * @return The UTF-8 encoded wire bytes.
     */
    @JvmStatic
    fun toBytes(message: DiscoveryMessage): ByteArray = DiscoveryCodec.toBytes(toPublicMessage(message))

    /**
     * Decodes a [DiscoveryMessage] from the first [length] bytes of [data].
     *
     * @param data   The raw byte buffer.
     * @param length The number of bytes to read from [data].
     * @return The decoded [DiscoveryMessage].
     * @throws IllegalArgumentException if the payload is invalid or [length] is out of range.
     */
    @JvmStatic
    fun fromBytes(data: ByteArray, length: Int): DiscoveryMessage =
        fromPublicMessage(DiscoveryCodec.fromBytes(data, length))

    private fun toPublicMessage(message: DiscoveryMessage): DiscoveryCodec.Message =
        DiscoveryCodec.Message(message.peerId, message.nickname, message.chatPort, message.filePort)

    private fun fromPublicMessage(message: DiscoveryCodec.Message): DiscoveryMessage =
        DiscoveryMessage(message.peerId, message.nickname, message.chatPort, message.filePort)
}
