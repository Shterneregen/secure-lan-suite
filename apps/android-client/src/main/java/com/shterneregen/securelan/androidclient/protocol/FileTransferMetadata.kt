package com.shterneregen.securelan.androidclient.protocol

typealias FileTransferMetadata = com.shterneregen.securelan.filetransfer.protocol.FileTransferMetadata

val FileTransferMetadata.transferId: String
    get() = transferId()

val FileTransferMetadata.senderId: String
    get() = senderId()

val FileTransferMetadata.recipientId: String
    get() = recipientId()

val FileTransferMetadata.fileName: String
    get() = fileName()

val FileTransferMetadata.fileSize: Long
    get() = fileSize()
