package com.shterneregen.securelan.filetransfer.service.impl

import com.shterneregen.securelan.common.net.transport.ClientSocketFactory
import com.shterneregen.securelan.common.net.transport.TransportEndpoint
import com.shterneregen.securelan.crypto.CryptoServices
import com.shterneregen.securelan.filetransfer.event.FileTransferCompletedEvent
import com.shterneregen.securelan.filetransfer.event.FileTransferFailedEvent
import com.shterneregen.securelan.filetransfer.event.FileTransferStartedEvent
import com.shterneregen.securelan.filetransfer.protocol.FileTransferSession
import com.shterneregen.securelan.filetransfer.service.*
import java.io.IOException
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.nio.file.Files
import java.util.UUID
import java.util.concurrent.CancellationException
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class DefaultFileTransferClientService @JvmOverloads constructor(
    private val eventPublisher: FileTransferEventPublisher,
    cryptoServices: CryptoServices = CryptoServices.createDefault(),
    private val clientSocketFactory: ClientSocketFactory? = null,
    private val timeouts: FileTransferTimeouts = FileTransferTimeouts(),
) : FileTransferClientService {
    private val handshake = SecureFileTransferHandshake(cryptoServices)

    override fun sendFile(request: FileTransferClientRequest): String =
        when (val outcome = createOperation(request).execute()) {
            is FileTransferOutcome.Completed -> outcome.transferId
            is FileTransferOutcome.Failed -> throw IllegalStateException("Unable to send file: ${outcome.kind}", outcome.cause)
        }

    override fun createOperation(request: FileTransferClientRequest, listener: FileTransferOperationListener): FileTransferOperation =
        Operation(request, listener)

    private inner class Operation(
        private val request: FileTransferClientRequest,
        private val listener: FileTransferOperationListener,
    ) : FileTransferOperation {
        override val transferId: String = UUID.randomUUID().toString()
        private val started = AtomicBoolean()
        private val lock = Any()
        private var socket: Socket? = null
        private var source: InputStream? = null
        private var closed = false
        private var timedOut = false
        private var deadline: ScheduledFuture<*>? = null
        private var deadlineGeneration = 0L
        private var phase = FileTransferPhase.PREPARING
        private var transferred = 0L
        private var size: Long? = null

        override fun close() = synchronized(lock) {
            closed = true
            closeResources()
        }

        private fun closeResources() {
            try { socket?.close() } catch (_: IOException) { }
            try { source?.close() } catch (_: IOException) { }
        }

        private fun checkOpen() = synchronized(lock) {
            if (closed) throw IOException("Transfer operation closed")
        }

        private fun update(next: FileTransferPhase = phase) {
            phase = next
            listener.onUpdate(FileTransferOperationUpdate(transferId, phase, transferred, size))
        }

        override fun execute(): FileTransferOutcome {
            check(started.compareAndSet(false, true)) { "Transfer operation can execute only once" }
            val watchdog = Executors.newSingleThreadScheduledExecutor { runnable ->
                Thread(runnable, "file-transfer-watchdog").apply { isDaemon = true }
            }
            fun arm(millis: Long) = synchronized(lock) {
                checkOpen()
                deadline?.cancel(false)
                val generation = ++deadlineGeneration
                deadline = watchdog.schedule({
                    synchronized(lock) {
                        if (generation == deadlineGeneration && !closed) {
                            timedOut = true
                            closed = true
                            closeResources()
                        }
                    }
                }, millis, TimeUnit.MILLISECONDS)
            }
            var eofSubmitted = false
            var failureKind = FileTransferFailureKind.SOURCE
            val fileName = request.file.fileName.toString()
            lateinit var outcome: FileTransferOutcome
            try {
                checkOpen()
                update()
                size = Files.size(request.file)
                val input = Files.newInputStream(request.file)
                synchronized(lock) {
                    if (closed) { input.close(); checkOpen() }
                    source = input
                }
                eventPublisher.publish(FileTransferStartedEvent(transferId, fileName, size!!, true))
                failureKind = FileTransferFailureKind.CONNECTION
                arm(timeouts.connectMillis.toLong())
                val connected = if (clientSocketFactory == null) {
                    val owned = Socket()
                    synchronized(lock) {
                        if (closed) { owned.close(); checkOpen() }
                        socket = owned
                    }
                    owned.connect(InetSocketAddress(request.host, request.port), timeouts.connectMillis)
                    owned
                } else {
                    clientSocketFactory.connect(TransportEndpoint.of(request.host, request.port)).also { owned ->
                        synchronized(lock) {
                            if (closed) { owned.close(); checkOpen() }
                            socket = owned
                        }
                    }
                }
                arm(timeouts.inactivityMillis)
                failureKind = FileTransferFailureKind.PROTOCOL
                FileTransferSession(connected).use { session ->
                    handshake.performClientHandshake(session, request.senderId, request.recipientId,
                        request.sessionPassword, fileName, size!!, transferId) {
                        arm(timeouts.acceptanceMillis)
                        update(FileTransferPhase.WAITING_FOR_ACCEPTANCE)
                    }
                    arm(timeouts.inactivityMillis)
                    update(FileTransferPhase.SENDING)
                    val progress = ThrottledFileTransferProgressPublisher(eventPublisher, transferId, size!!, true)
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        checkOpen()
                        failureKind = FileTransferFailureKind.SOURCE
                        val read = input.read(buffer)
                        if (read == -1) break
                        if (read.toLong() > size!! - transferred) throw IOException("Source size changed")
                        failureKind = FileTransferFailureKind.CONNECTION
                        session.writeEncryptedBytes(buffer.copyOf(read))
                        transferred += read
                        arm(timeouts.inactivityMillis)
                        progress.report(transferred)
                        update()
                    }
                    if (transferred != size) throw IOException("Source size changed")
                    // Even a failed EOF write can have reached the receiver; do not promise non-delivery.
                    eofSubmitted = true
                    session.writeEncryptedBytes(ByteArray(0))
                    arm(timeouts.confirmationMillis)
                    update(FileTransferPhase.WAITING_FOR_CONFIRMATION)
                    if (session.readEncryptedText() != "DONE") throw IOException("Missing transfer confirmation")
                }
                outcome = FileTransferOutcome.Completed(transferId)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                val kind = synchronized(lock) {
                    when {
                        eofSubmitted -> FileTransferFailureKind.OUTCOME_UNKNOWN
                        timedOut || error is SocketTimeoutException -> FileTransferFailureKind.TIMEOUT
                        closed -> FileTransferFailureKind.ABORTED
                        error is TransferRejectedException -> FileTransferFailureKind.REJECTED
                        else -> failureKind
                    }
                }
                outcome = FileTransferOutcome.Failed(transferId, kind, error)
            } finally {
                synchronized(lock) {
                    ++deadlineGeneration
                    deadline?.cancel(false)
                    closed = true
                    closeResources()
                }
                watchdog.shutdownNow()
            }
            when (outcome) {
                is FileTransferOutcome.Completed -> {
                    update(FileTransferPhase.COMPLETED)
                    eventPublisher.publish(FileTransferCompletedEvent(transferId, fileName, request.file, size!!, true))
                }
                is FileTransferOutcome.Failed -> eventPublisher.publish(
                    FileTransferFailedEvent(transferId, fileName, "File transfer failed: ${outcome.kind}", outcome.cause, true),
                )
            }
            return outcome
        }
    }
}
