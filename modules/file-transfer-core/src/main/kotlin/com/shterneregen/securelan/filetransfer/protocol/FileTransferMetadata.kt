package com.shterneregen.securelan.filetransfer.protocol

import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.Objects

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

    fun serialize(): String = listOf(transferId, senderId, recipientId, fileName, fileSize.toString()).joinToString(SEPARATOR)

    fun compactSerialize(): String = listOf(
        encodeText(transferId),
        encodeText(senderId),
        encodeText(recipientId),
        encodeText(fileName),
        fileSize.toString(),
    ).joinToString("|")

    companion object {
        private const val SEPARATOR = "\n"

        @JvmStatic
        fun deserialize(value: String): FileTransferMetadata {
            val parts = value.split(SEPARATOR, limit = 5).toTypedArray()
            if (parts.size != 5) {
                throw IllegalArgumentException("Malformed metadata payload")
            }
            return FileTransferMetadata(parts[0], parts[1], parts[2], parts[3], parts[4].toLong())
        }

        @JvmStatic
        fun deserializeCompact(value: String): FileTransferMetadata {
            val parts = value.split("|", limit = 5).toTypedArray()
            if (parts.size != 5) {
                throw IllegalArgumentException("Malformed compact metadata payload")
            }
            return FileTransferMetadata(
                decodeText(parts[0]),
                decodeText(parts[1]),
                decodeText(parts[2]),
                decodeText(parts[3]),
                parts[4].toLong(),
            )
        }

        private fun encodeText(value: String): String =
            Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(StandardCharsets.UTF_8))

        private fun decodeText(value: String): String = String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
    }
}
