package com.shterneregen.securelan.common.model

import java.util.Objects

@JvmRecord
data class FileTransferProgress(
    val transferId: String,
    val transferredBytes: Long,
    val totalBytes: Long,
    val status: TransferStatus,
) {
    init {
        Objects.requireNonNull(transferId, "transferId must not be null")
        Objects.requireNonNull(status, "status must not be null")
        require(transferredBytes >= 0) { "transferredBytes must not be negative" }
        require(totalBytes >= 0) { "totalBytes must not be negative" }
        require(!(totalBytes > 0 && transferredBytes > totalBytes)) { "transferredBytes must not exceed totalBytes" }
    }

    fun percent(): Int = if (totalBytes <= 0) 0 else ((transferredBytes * 100) / totalBytes).toInt()
}
