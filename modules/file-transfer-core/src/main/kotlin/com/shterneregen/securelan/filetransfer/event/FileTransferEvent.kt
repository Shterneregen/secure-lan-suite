package com.shterneregen.securelan.filetransfer.event

sealed interface FileTransferEvent {
    val transferId: String?
}
