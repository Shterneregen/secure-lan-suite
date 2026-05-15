package com.shterneregen.securelan.androidclient.protocol

data class FileTransferMetadata(
    val transferId: String,
    val senderId: String,
    val recipientId: String,
    val fileName: String,
    val fileSize: Long,
) {
    fun serialize(): String = listOf(transferId, senderId, recipientId, fileName, fileSize.toString()).joinToString(SEPARATOR)

    fun compactSerialize(): String = listOf(
        encodeText(transferId),
        encodeText(senderId),
        encodeText(recipientId),
        encodeText(fileName),
        fileSize.toString(),
    ).joinToString(COMPACT_SEPARATOR)

    companion object {
        private const val SEPARATOR = "\n"
        private const val COMPACT_SEPARATOR = "|"

        private fun encodeText(value: String): String = java.util.Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(value.toByteArray(Charsets.UTF_8))
    }
}
