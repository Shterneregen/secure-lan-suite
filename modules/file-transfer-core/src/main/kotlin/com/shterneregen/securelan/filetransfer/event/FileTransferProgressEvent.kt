package com.shterneregen.securelan.filetransfer.event

import com.shterneregen.securelan.common.model.FileTransferProgress

@JvmRecord
data class FileTransferProgressEvent(
    override val transferId: String?,
    val progress: FileTransferProgress?,
    val outgoing: Boolean,
) : FileTransferEvent
