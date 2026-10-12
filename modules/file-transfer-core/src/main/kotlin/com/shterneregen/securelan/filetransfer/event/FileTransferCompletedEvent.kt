package com.shterneregen.securelan.filetransfer.event

import java.nio.file.Path

/**
 * Event published when a file transfer operation has completed successfully.
 *
 * @property transferId Unique transfer session identifier.
 * @property fileName Name of the transferred file.
 * @property path Filesystem path to the transferred file (the local source on send or stored destination on receive).
 * @property totalBytes Total number of bytes successfully transferred.
 * @property outgoing `true` if the local peer sent the file; `false` if it was received.
 */
@JvmRecord
data class FileTransferCompletedEvent(
    override val transferId: String?,
    val fileName: String?,
    val path: Path?,
    val totalBytes: Long,
    val outgoing: Boolean,
) : FileTransferEvent
