package com.shterneregen.securelan.filetransfer.service

/**
 * Service providing outgoing encrypted file transfer operations to remote peers.
 */
interface FileTransferClientService {
    /**
     * Sends a file synchronously according to the specified request.
     *
     * @param request Transfer request parameters.
     * @return Unique transfer session identifier upon successful completion.
     * @throws IllegalStateException If the transfer fails, times out, or is rejected.
     */
    fun sendFile(request: FileTransferClientRequest): String

    /**
     * Creates an asynchronous or controllable [FileTransferOperation] instance.
     *
     * @param request Transfer request parameters.
     * @param listener Callback listener receiving progressive status updates.
     * @return An executable and cancelable [FileTransferOperation].
     * @throws UnsupportedOperationException If operation-based lifecycle is not supported.
     */
    fun createOperation(
        request: FileTransferClientRequest,
        listener: FileTransferOperationListener = FileTransferOperationListener { },
    ): FileTransferOperation = throw UnsupportedOperationException("Operation lifecycle is not supported by this implementation")
}
