package com.shterneregen.securelan.filetransfer.event

import java.nio.file.Path

@JvmRecord
data class FileTransferCompletedEvent(
    override val transferId: String?,
    val fileName: String?,
    val path: Path?,
    val totalBytes: Long,
    val outgoing: Boolean,
) : FileTransferEvent
