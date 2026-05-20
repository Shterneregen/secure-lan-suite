package com.shterneregen.securelan.filetransfer.event

@JvmRecord
data class FileTransferStartedEvent(
    override val transferId: String?,
    val fileName: String?,
    val totalBytes: Long,
    val outgoing: Boolean,
) : FileTransferEvent
