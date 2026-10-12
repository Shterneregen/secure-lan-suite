package com.shterneregen.securelan.stego.service.impl.internal

import com.shterneregen.securelan.stego.model.StegoContentType
import java.nio.ByteBuffer
import java.util.Objects

/**
 * Protocol header embedded before the payload in steganography carrier images.
 *
 * Header structure (12 bytes):
 * - Magic (4 bytes): `0x534C5347` ('SLSG')
 * - Version (1 byte): `0x01`
 * - Flags (1 byte): bit 0 indicates encryption
 * - Content Type (1 byte): [StegoContentType.code]
 * - Reserved (1 byte): `0x00`
 * - Payload Length (4 bytes): big-endian length of the following payload
 *
 * @property contentType The [StegoContentType] of the embedded payload.
 * @property encrypted Whether the payload is encrypted.
 * @property payloadLength Length of the embedded payload in bytes.
 */
@JvmRecord
data class StegoHeader(
    val contentType: StegoContentType,
    val encrypted: Boolean,
    val payloadLength: Int,
) {
    init {
        Objects.requireNonNull(contentType, "contentType")
        require(payloadLength >= 0) { "payloadLength must not be negative" }
    }

    /**
     * Serializes this header into its 12-byte binary protocol representation.
     *
     * @return A 12-byte array containing the serialized header.
     */
    fun write(): ByteArray = ByteBuffer.allocate(BYTE_LENGTH)
        .putInt(MAGIC)
        .put(VERSION)
        .put(if (encrypted) ENCRYPTED_FLAG else 0.toByte())
        .put(contentType.code())
        .put(0.toByte())
        .putInt(payloadLength)
        .array()

    companion object {
        /** Size of the header in bytes. */
        const val BYTE_LENGTH: Int = 12

        private const val MAGIC = 0x534C5347
        private const val VERSION: Byte = 1
        private const val ENCRYPTED_FLAG: Byte = 0x01

        /**
         * Parses and validates a [StegoHeader] from a 12-byte array.
         *
         * @param bytes The raw header bytes to parse.
         * @return The parsed [StegoHeader].
         * @throws IllegalArgumentException If the byte length is invalid, magic number does not match,
         *                                  or version/content type is unsupported.
         */
        @JvmStatic
        fun read(bytes: ByteArray): StegoHeader {
            Objects.requireNonNull(bytes, "bytes")
            if (bytes.size != BYTE_LENGTH) {
                throw IllegalArgumentException("Invalid steganography header length")
            }
            val buffer = ByteBuffer.wrap(bytes)
            val magic = buffer.int
            if (magic != MAGIC) {
                throw IllegalArgumentException("No SecureLanSuite steganography payload found")
            }
            val version = buffer.get()
            if (version != VERSION) {
                throw IllegalArgumentException("Unsupported steganography payload version: $version")
            }
            val flags = buffer.get()
            val encrypted = (flags.toInt() and ENCRYPTED_FLAG.toInt()) == ENCRYPTED_FLAG.toInt()
            val contentType = buffer.get()
            buffer.get()
            val payloadLength = buffer.int
            return StegoHeader(StegoContentType.fromCode(contentType), encrypted, payloadLength)
        }
    }
}
