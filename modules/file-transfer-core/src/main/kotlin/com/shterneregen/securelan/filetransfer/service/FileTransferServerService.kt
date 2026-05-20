package com.shterneregen.securelan.filetransfer.service

interface FileTransferServerService {
    fun start(config: FileTransferServerConfig)
    fun stop()
    fun isRunning(): Boolean
}
