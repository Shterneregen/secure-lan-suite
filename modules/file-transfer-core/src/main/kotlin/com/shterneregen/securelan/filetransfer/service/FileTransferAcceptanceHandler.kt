package com.shterneregen.securelan.filetransfer.service

import com.shterneregen.securelan.filetransfer.protocol.FileTransferMetadata

/**
 * Functional callback invoked by the file transfer server when an incoming file transfer request arrives.
 *
 * Implementations may prompt the local user for interactive confirmation or apply automated rules.
 */
fun interface FileTransferAcceptanceHandler {
    /**
     * Determines whether to accept or reject an incoming file transfer.
     *
     * @param metadata Declared metadata of the incoming file transfer.
     * @param remoteAddress IP/socket address of the remote sender.
     * @return `true` to accept the file and proceed with data transfer; `false` to reject it.
     */
    fun accept(metadata: FileTransferMetadata, remoteAddress: String): Boolean

    companion object {
        /**
         * Returns an acceptance handler that unconditionally accepts all incoming file transfers.
         */
        @JvmStatic
        fun acceptAll(): FileTransferAcceptanceHandler = FileTransferAcceptanceHandler { _, _ -> true }
    }
}
