package com.shterneregen.securelan.filetransfer.event

/**
 * Event published when a file transfer operation has commenced.
 *
 * @property transferId Unique transfer session identifier.
 * @property fileName Name of the file being transferred.
 * @property totalBytes Declared total size of the file in bytes.
 * @property outgoing `true` if the local peer is sending the file; `false` if receiving.
 */
@JvmRecord
data class FileTransferStartedEvent(
    override val transferId: String?,
    val fileName: String?,
    val totalBytes: Long,
    val outgoing: Boolean,
) : FileTransferEvent
