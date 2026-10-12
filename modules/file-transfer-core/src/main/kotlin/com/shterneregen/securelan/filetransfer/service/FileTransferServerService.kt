package com.shterneregen.securelan.filetransfer.service

/**
 * Service managing the local listening server for receiving incoming secure file transfers.
 */
interface FileTransferServerService {
    /**
     * Starts listening for incoming file transfers with the provided configuration.
     *
     * @param config Server port, destination directory, password, and acceptance handler.
     * @throws IllegalStateException If the server fails to bind or initialize.
     */
    fun start(config: FileTransferServerConfig)

    /**
     * Stops the listening server and disconnects active incoming transfers.
     */
    fun stop()

    /**
     * Returns `true` if the server is actively listening for incoming connections.
     */
    fun isRunning(): Boolean
}
