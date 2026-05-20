package com.shterneregen.securelan.filetransfer.service

import com.shterneregen.securelan.filetransfer.protocol.FileTransferMetadata

fun interface FileTransferAcceptanceHandler {
    fun accept(metadata: FileTransferMetadata, remoteAddress: String): Boolean

    companion object {
        @JvmStatic
        fun acceptAll(): FileTransferAcceptanceHandler = FileTransferAcceptanceHandler { _, _ -> true }
    }
}
