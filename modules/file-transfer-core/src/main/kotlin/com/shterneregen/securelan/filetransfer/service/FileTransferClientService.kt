package com.shterneregen.securelan.filetransfer.service

interface FileTransferClientService {
    fun sendFile(request: FileTransferClientRequest): String
}
