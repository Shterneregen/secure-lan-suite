package com.shterneregen.securelan.common.model

import java.util.Objects

@JvmRecord
data class FileTransferRequest(
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
}
