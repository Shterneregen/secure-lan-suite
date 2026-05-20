package com.shterneregen.securelan.filetransfer.event

@JvmRecord
data class FileTransferFailedEvent(
    override val transferId: String?,
    val fileName: String?,
    val message: String?,
    val cause: Throwable?,
    val outgoing: Boolean,
) : FileTransferEvent
