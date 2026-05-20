package com.shterneregen.securelan.stego.service.impl.internal

import com.shterneregen.securelan.stego.model.StegoContentType
import java.nio.ByteBuffer
import java.util.Objects

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

    fun write(): ByteArray = ByteBuffer.allocate(BYTE_LENGTH)
        .putInt(MAGIC)
        .put(VERSION)
        .put(if (encrypted) ENCRYPTED_FLAG else 0.toByte())
        .put(contentType.code())
        .put(0.toByte())
        .putInt(payloadLength)
        .array()

    companion object {
        const val BYTE_LENGTH: Int = 12

        private const val MAGIC = 0x534C5347
        private const val VERSION: Byte = 1
        private const val ENCRYPTED_FLAG: Byte = 0x01

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
