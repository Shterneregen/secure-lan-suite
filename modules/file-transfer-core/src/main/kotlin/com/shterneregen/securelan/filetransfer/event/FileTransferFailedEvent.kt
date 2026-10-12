package com.shterneregen.securelan.filetransfer.event

/**
 * Event published when a file transfer operation has failed, was aborted, or was rejected.
 *
 * @property transferId Unique transfer session identifier, or fallback identifier if failed prior to negotiation.
 * @property fileName Name of the file being transferred, if known.
 * @property message Descriptive failure message.
 * @property cause Underlying exception or error, if available.
 * @property outgoing `true` if the local peer was sending; `false` if receiving.
 */
@JvmRecord
data class FileTransferFailedEvent(
    override val transferId: String?,
    val fileName: String?,
    val message: String?,
    val cause: Throwable?,
    val outgoing: Boolean,
) : FileTransferEvent
