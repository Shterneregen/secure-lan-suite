package com.shterneregen.securelan.common.net.transport

import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.Objects
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean

class TcpServer(
    threadNamePrefix: String,
    serverSocketFactory: ServerSocketFactory,
) : AutoCloseable {
    private val threadNamePrefix: String = Objects.requireNonNull(threadNamePrefix, "threadNamePrefix")
    private val serverSocketFactory: ServerSocketFactory = Objects.requireNonNull(serverSocketFactory, "serverSocketFactory")
    private val running = AtomicBoolean(false)

    @Volatile
    private var serverSocket: ServerSocket? = null

    @Volatile
    private var clientExecutor: ExecutorService? = null

    @Volatile
    private var acceptThread: Thread? = null

    constructor(threadNamePrefix: String) : this(threadNamePrefix, ServerSocketFactory.systemDefault())

    @Throws(IOException::class)
    fun start(port: Int, socketHandler: SocketHandler, errorHandler: ErrorHandler) {
        Objects.requireNonNull(socketHandler, "socketHandler")
        Objects.requireNonNull(errorHandler, "errorHandler")
        if (!running.compareAndSet(false, true)) {
            return
        }
        var createdSocket: ServerSocket? = null
        var createdExecutor: ExecutorService? = null
        try {
            createdSocket = serverSocketFactory.open(port)
            createdExecutor = Executors.newCachedThreadPool { runnable ->
                Thread(runnable, "$threadNamePrefix-client").apply { isDaemon = true }
            }
            serverSocket = createdSocket
            clientExecutor = createdExecutor
            val activeSocket = createdSocket
            val activeExecutor = createdExecutor
            acceptThread = Thread(
                { acceptLoop(activeSocket, activeExecutor, socketHandler, errorHandler) },
                "$threadNamePrefix-accept",
            ).apply {
                isDaemon = true
                start()
            }
        } catch (e: IOException) {
            running.set(false)
            stop(createdSocket, createdExecutor)
            serverSocket = null
            clientExecutor = null
            acceptThread = null
            throw e
        } catch (e: RuntimeException) {
            running.set(false)
            stop(createdSocket, createdExecutor)
            serverSocket = null
            clientExecutor = null
            acceptThread = null
            throw e
        }
    }

    fun isRunning(): Boolean = running.get()

    override fun close() {
        running.set(false)
        stop(serverSocket, clientExecutor)
        serverSocket = null
        clientExecutor = null
        acceptThread = null
    }

    private fun acceptLoop(
        activeSocket: ServerSocket,
        activeExecutor: ExecutorService,
        socketHandler: SocketHandler,
        errorHandler: ErrorHandler,
    ) {
        while (isActive(activeSocket, activeExecutor)) {
            try {
                val socket = activeSocket.accept()
                try {
                    activeExecutor.submit { socketHandler.handle(socket) }
                } catch (e: RejectedExecutionException) {
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
        executor?.shutdownNow()
    }

    fun interface SocketHandler {
        fun handle(socket: Socket)
    }

    fun interface ErrorHandler {
        fun onError(message: String, cause: Throwable)
    }
}
