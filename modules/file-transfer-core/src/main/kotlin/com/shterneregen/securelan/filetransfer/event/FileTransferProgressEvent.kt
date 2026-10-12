package com.shterneregen.securelan.filetransfer.event

import com.shterneregen.securelan.common.model.FileTransferProgress

/**
 * Event published periodically during an ongoing file transfer operation to report progress.
 *
 * @property transferId Unique transfer session identifier.
 * @property progress Transfer progress snapshot containing transferred bytes and total bytes.
 * @property outgoing `true` if the local peer is sending the file; `false` if receiving.
 */
@JvmRecord
data class FileTransferProgressEvent(
    override val transferId: String?,
    val progress: FileTransferProgress?,
    val outgoing: Boolean,
) : FileTransferEvent
