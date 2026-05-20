package com.shterneregen.securelan.filetransfer.service

import com.shterneregen.securelan.filetransfer.event.FileTransferEvent

fun interface FileTransferEventPublisher {
    fun publish(event: FileTransferEvent)
}
