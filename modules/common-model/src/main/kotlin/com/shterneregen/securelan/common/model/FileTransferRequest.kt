package com.shterneregen.securelan.common.model

/**
 * Describes a file offered for transfer between two peers.
 *
 * @property transferId identifier shared by the transfer's events.
 * @property senderId identifier of the sending peer.
 * @property recipientId identifier of the receiving peer.
 * @property fileName name of the offered file; must not be blank.
 * @property fileSize size in bytes; must be non-negative.
 * @throws IllegalArgumentException if [fileName] is blank or [fileSize] is negative.
 */
@JvmRecord
data class FileTransferRequest(
    val transferId: String,
    val senderId: String,
    val recipientId: String,
    val fileName: String,
    val fileSize: Long,
) {
    init {
        require(fileName.isNotBlank()) { "fileName must not be blank" }
        require(fileSize >= 0) { "fileSize must not be negative" }
    }
}
