package com.shterneregen.securelan.chat.service

/**
 * Server-side service that manages the chat room lifecycle.
 *
 * A single instance represents one chat room.  Peers connect, exchange messages, and disconnect
 * while the server is running.  Events such as user joins, leaves, and errors are reported via the
 * [ChatEventPublisher] provided at construction time.
 *
 * ## Lifecycle
 * 1. Call [start] to bind the TCP server socket and begin accepting connections.
 * 2. Query [isRunning] and [connectedUsers] while the server is active.
 * 3. Call [stop] to close all connections and release resources.
 *
 * Implementations must be thread-safe.
 */
interface ChatServerService {

    /**
     * Starts the server and begins accepting client connections on the port specified by [config].
     *
     * If the server is already running, this call is a no-op.
     *
     * @param config The server configuration (port, session password).
     * @throws IllegalStateException if the underlying TCP server fails to bind to the port.
     */
    fun start(config: ChatServerConfig)

    /**
     * Stops the server, forcibly closes all active client sessions, and releases the server socket.
     *
     * After this call, [isRunning] returns `false`.
     * Safe to call even if the server is not running.
     */
    fun stop()

    /**
     * Returns `true` if the server is currently running and accepting connections.
     */
    fun isRunning(): Boolean

    /**
     * Returns the number of clients that have completed the handshake and are currently connected.
     */
    fun connectedUsers(): Int
}
