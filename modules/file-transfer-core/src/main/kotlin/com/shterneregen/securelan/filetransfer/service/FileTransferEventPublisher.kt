package com.shterneregen.securelan.filetransfer.service

import com.shterneregen.securelan.filetransfer.event.FileTransferEvent

/**
 * Functional callback interface for publishing [FileTransferEvent] notifications.
 */
fun interface FileTransferEventPublisher {
    /**
     * Publishes a file transfer event.
     *
     * @param event The [FileTransferEvent] to publish.
     */
    fun publish(event: FileTransferEvent)
}
