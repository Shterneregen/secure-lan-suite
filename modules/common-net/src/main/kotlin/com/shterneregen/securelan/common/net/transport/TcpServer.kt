package com.shterneregen.securelan.common.net.transport

import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.ExecutorService
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Multi-threaded TCP server that binds to a port, accepts incoming connections on a background
 * accept thread, and dispatches each accepted socket to a bounded worker pool without a backlog.
 *
 * Lifecycle:
 * - Call [start] to bind the server socket and begin accepting connections.
 * - Call [close] to stop accepting connections and shut down the client executor.
 *
 * @param threadNamePrefix prefix used for accept and worker thread names.
 * @param serverSocketFactory factory used to open the bound [ServerSocket].
 */
class TcpServer(
    private val threadNamePrefix: String,
    private val serverSocketFactory: ServerSocketFactory,
    private val maxClients: Int = 32,
) : AutoCloseable {
    private val running = AtomicBoolean(false)
    private val lifecycleLock = Any()
    private var generation = 0L
    private val activeSockets = java.util.concurrent.ConcurrentHashMap.newKeySet<Socket>()

    init { require(maxClients in 1..256) { "Invalid client limit" } }

    @Volatile
    private var serverSocket: ServerSocket? = null

    @Volatile
    private var clientExecutor: ExecutorService? = null

    private var acceptGate: Any? = null

    /**
     * Creates a new [TcpServer] using the default system [ServerSocketFactory].
     *
     * @param threadNamePrefix prefix used for server thread names.
     */
    constructor(threadNamePrefix: String) : this(threadNamePrefix, ServerSocketFactory.systemDefault())

    /**
     * Starts the TCP server on the given [port].
     *
     * If the server is already running, this method returns immediately without effect.
     *
     * @param port the TCP port to listen on (0 for ephemeral, or 1..65535).
     * @param socketHandler handler called for each accepted client socket.
     * @param errorHandler handler called for accept loop or task scheduling errors.
     * @throws IllegalArgumentException if [port] is outside the range 0..65535.
     * @throws IOException if binding the server socket fails.
     */
    @Throws(IOException::class)
    fun start(port: Int, socketHandler: SocketHandler, errorHandler: ErrorHandler) {
        require(port in 0..65_535) { "port must be between 0 and 65535" }
        val token = synchronized(lifecycleLock) {
            if (!running.compareAndSet(false, true)) return
            ++generation
        }
        var createdSocket: ServerSocket? = null
        var createdExecutor: ExecutorService? = null
        try {
            createdSocket = serverSocketFactory.open(port)
            createdExecutor = java.util.concurrent.ThreadPoolExecutor(
                0, maxClients, 30, java.util.concurrent.TimeUnit.SECONDS,
                java.util.concurrent.SynchronousQueue(),
                { runnable -> Thread(runnable, "$threadNamePrefix-client").apply { isDaemon = true } },
            )
            synchronized(lifecycleLock) {
                if (generation != token || !running.get()) {
                    SocketClose.closeQuietly(createdSocket)
                    createdExecutor.shutdownNow()
                    return
                }
                serverSocket = createdSocket
                clientExecutor = createdExecutor
                val activeSocket = createdSocket
                val activeExecutor = createdExecutor
                val gate = Any()
                acceptGate = gate
                Thread(
                    { acceptLoop(activeSocket, activeExecutor, gate, socketHandler, errorHandler) },
                    "$threadNamePrefix-accept",
                ).apply {
                    isDaemon = true
                    start()
                }
            }
        } catch (error: Throwable) {
            rollback(token, createdSocket, createdExecutor)
            throw error
        }
    }

    /**
     * Returns `true` if the server is currently started and accepting connections.
     */
    fun isRunning(): Boolean = running.get()

    /**
     * Returns the local port to which the server socket is bound, or `-1` if not running.
     */
    fun localPort(): Int = serverSocket?.localPort ?: -1

    /**
     * Returns the local port to which the server socket is bound, or `-1` if not running.
     * Alias for [localPort].
     */
    fun port(): Int = localPort()

    /**
     * Stops the server and waits for the accept operation to release the listening socket before
     * returning, so the caller can immediately rebind its port. Client workers are interrupted.
     */
    override fun close() {
        val gate = synchronized(lifecycleLock) {
            val gate = acceptGate
            generation++
            running.set(false)
            stop(serverSocket, clientExecutor)
            serverSocket = null
            clientExecutor = null
            acceptGate = null
            gate
        }
        // A JDK socket close can defer releasing its descriptor until accept() aborts.
        // Wait only for that operation, outside the lifecycle lock and application callbacks.
        if (gate != null) synchronized(gate) { }
    }

    private fun rollback(token: Long, socket: ServerSocket?, executor: ExecutorService?) {
        synchronized(lifecycleLock) {
            SocketClose.closeQuietly(socket)
            executor?.shutdownNow()
            if (generation == token) {
                running.set(false)
                serverSocket = null
                clientExecutor = null
                acceptGate = null
            }
        }
    }

    private fun acceptLoop(
        activeSocket: ServerSocket,
        activeExecutor: ExecutorService,
        gate: Any,
        socketHandler: SocketHandler,
        errorHandler: ErrorHandler,
    ) {
        while (isActive(activeSocket, activeExecutor)) {
            try {
                val socket = synchronized(gate) {
                    if (!isActive(activeSocket, activeExecutor)) return
                    activeSocket.accept()
                }
                activeSockets.add(socket)
                if (!isActive(activeSocket, activeExecutor)) {
                    activeSockets.remove(socket)
                    SocketClose.closeQuietly(socket)
                    continue
                }
                try {
                    activeExecutor.submit {
                        try {
                            socketHandler.handle(socket)
                        } catch (e: Throwable) {
                            if (isActive(activeSocket, activeExecutor)) {
                                errorHandler.onError("Unhandled error in client socket handler", e)
                            }
                        } finally { activeSockets.remove(socket); SocketClose.closeQuietly(socket) }
                    }
                } catch (e: RejectedExecutionException) {
                    activeSockets.remove(socket)
                    SocketClose.closeQuietly(socket)
                    if (isActive(activeSocket, activeExecutor)) {
                        errorHandler.onError("Unable to schedule TCP client handler", e)
                    }
                }
            } catch (e: SocketException) {
                if (isActive(activeSocket, activeExecutor)) {
                    errorHandler.onError("TCP server socket error", e)
                }
            } catch (e: IOException) {
                if (isActive(activeSocket, activeExecutor)) {
                    errorHandler.onError("Unable to accept TCP client", e)
                }
            }
        }
    }

    private fun isActive(activeSocket: ServerSocket, activeExecutor: ExecutorService): Boolean =
        running.get() &&
            serverSocket === activeSocket &&
            clientExecutor === activeExecutor &&
            !activeSocket.isClosed &&
            !activeExecutor.isShutdown

    private fun stop(socket: ServerSocket?, executor: ExecutorService?) {
        SocketClose.closeQuietly(socket)
        activeSockets.forEach(SocketClose::closeQuietly)
        activeSockets.clear()
        executor?.shutdownNow()
    }

    /**
     * Callback interface for processing an accepted client [Socket].
     */
    fun interface SocketHandler {
        /**
         * Invoked to handle an accepted client connection.
         *
         * @param socket the connected client socket.
         */
        fun handle(socket: Socket)
    }

    /**
     * Callback interface for handling server accept loop errors.
     */
    fun interface ErrorHandler {
        /**
         * Invoked when an error occurs during client acceptance or handler dispatch.
         *
         * @param message human-readable error description.
         * @param cause the underlying exception.
         */
        fun onError(message: String, cause: Throwable)
    }
}
