package com.shterneregen.securelan.filetransfer.service

interface FileTransferClientService {
    fun sendFile(request: FileTransferClientRequest): String

    fun createOperation(
        request: FileTransferClientRequest,
        listener: FileTransferOperationListener = FileTransferOperationListener { },
    ): FileTransferOperation = throw UnsupportedOperationException("Operation lifecycle is not supported by this implementation")
}
