package com.shterneregen.securelan.filetransfer.event

/**
 * Base marker interface for all file transfer lifecycle events within SecureLanSuite.
 *
 * Implementations represent distinct stages of a transfer operation (start, progress, completion,
 * failure) and are emitted by both client (sending) and server (receiving) components.
 */
sealed interface FileTransferEvent {
    /**
     * Unique identifier of the file transfer session, or `null` if the event is emitted
     * prior to session ID negotiation or at a global service level.
     */
    val transferId: String?
}
