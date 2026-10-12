package com.shterneregen.securelan.filetransfer.protocol

import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.Objects

/**
 * Immutable metadata describing an initiated file transfer between two peers.
 *
 * This metadata is negotiated during the initial handshake before any payload bytes are transmitted.
 *
 * @property transferId Unique transfer session identifier.
 * @property senderId Identifier of the sending peer.
 * @property recipientId Identifier of the intended recipient peer.
 * @property fileName Name of the file being transferred.
 * @property fileSize Exact size of the file in bytes (must be non-negative).
 */
@JvmRecord
data class FileTransferMetadata(
    val transferId: String,
    val senderId: String,
    val recipientId: String,
    val fileName: String,
    val fileSize: Long,
) {
    init {
        Objects.requireNonNull(transferId, "transferId must not be null")
        Objects.requireNonNull(senderId, "senderId must not be null")
        Objects.requireNonNull(recipientId, "recipientId must not be null")
        Objects.requireNonNull(fileName, "fileName must not be null")
        require(fileName.isNotBlank()) { "fileName must not be blank" }
        require(fileSize >= 0) { "fileSize must not be negative" }
    }

    /**
     * Serializes this metadata into a newline-separated string.
     *
     * @return Newline-delimited metadata string.
     */
    fun serialize(): String = listOf(transferId, senderId, recipientId, fileName, fileSize.toString()).joinToString(SEPARATOR)

    /**
     * Serializes this metadata into a compact URL-safe Base64 pipe-delimited format.
     *
     * This representation avoids exceeding RSA encryption block limits during initial handshake.
     *
     * @return Compact serialized metadata string.
     */
    fun compactSerialize(): String = listOf(
        encodeText(transferId),
        encodeText(senderId),
        encodeText(recipientId),
        encodeText(fileName),
        fileSize.toString(),
    ).joinToString("|")

    companion object {
        private const val SEPARATOR = "\n"

        /**
         * Deserializes metadata from a newline-separated string payload.
         *
         * @param value Serialized metadata string.
         * @return Parsed [FileTransferMetadata] instance.
         * @throws IllegalArgumentException If the payload is malformed or contains invalid data.
         */
        @JvmStatic
        @Throws(IllegalArgumentException::class)
        fun deserialize(value: String): FileTransferMetadata {
            val parts = value.split(SEPARATOR, limit = 5)
            if (parts.size != 5) {
                throw IllegalArgumentException("Malformed metadata payload")
            }
            val size = parts[4].toLongOrNull() ?: throw IllegalArgumentException("Invalid file size in metadata")
            return FileTransferMetadata(parts[0], parts[1], parts[2], parts[3], size)
        }

        /**
         * Deserializes metadata from a compact pipe-delimited Base64 string payload.
         *
         * @param value Compact serialized metadata string.
         * @return Parsed [FileTransferMetadata] instance.
         * @throws IllegalArgumentException If the payload is malformed or cannot be decoded.
         */
        @JvmStatic
        @Throws(IllegalArgumentException::class)
        fun deserializeCompact(value: String): FileTransferMetadata {
            val parts = value.split("|", limit = 5)
            if (parts.size != 5) {
                throw IllegalArgumentException("Malformed compact metadata payload")
            }
            val size = parts[4].toLongOrNull() ?: throw IllegalArgumentException("Invalid file size in compact metadata")
            return FileTransferMetadata(
                decodeText(parts[0]),
                decodeText(parts[1]),
                decodeText(parts[2]),
                decodeText(parts[3]),
                size,
            )
        }

        private fun encodeText(value: String): String =
            Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(StandardCharsets.UTF_8))

        private fun decodeText(value: String): String =
            String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
    }
}
