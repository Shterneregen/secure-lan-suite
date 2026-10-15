package com.shterneregen.securelan.androidclient.network

import android.content.ContentResolver
import androidx.core.net.toUri
import com.shterneregen.securelan.androidclient.protocol.FileTransferMetadata
import com.shterneregen.securelan.androidclient.protocol.FileTransferSession
import com.shterneregen.securelan.androidclient.transfer.TransferAttempt
import com.shterneregen.securelan.androidclient.transfer.TransferPhase
import com.shterneregen.securelan.androidclient.transfer.TransferSource
import com.shterneregen.securelan.androidclient.transfer.stageStream
import com.shterneregen.securelan.filetransfer.protocol.FileTransferStreams
import com.shterneregen.securelan.filetransfer.protocol.TransferRejectedException
import com.shterneregen.securelan.filetransfer.protocol.TransferSourceException
import com.shterneregen.securelan.filetransfer.service.FileTransferAttemptException
import com.shterneregen.securelan.filetransfer.service.FileTransferFailureKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Per-stage IO deadlines in milliseconds; acceptance includes a remote user's approval wait. */
data class FileTransferTimeouts(val connectMillis: Int = 10_000, val acceptanceMillis: Int = 300_000, val inactivityMillis: Int = 60_000, val confirmationMillis: Int = 60_000)

/**
 * Streams one encrypted file at a time on the IO dispatcher using the desktop transfer protocol.
 * Unknown sizes are staged in private cache; deadlines and cancellation close owned streams/sockets.
 * Returning from [sendFile] means the receiver acknowledged `DONE`, not just that bytes were sent.
 */
class SecureFileSender(private val contentResolver: ContentResolver, private val cacheDirectory: File, private val timeouts: FileTransferTimeouts = FileTransferTimeouts()) {
    @Volatile private var active: Operation? = null

    /** Interrupts the current operation; future sends may reuse this sender. */
    fun close() { active?.close() }

    /** Reports transfer stages from the IO thread and returns the confirmed wire transfer ID. */
    suspend fun sendFile(
        host: String,
        port: Int,
        senderId: String,
        recipientId: String,
        sessionPassword: String,
        selectedFile: TransferSource,
        onUpdate: (TransferAttempt) -> Unit,
        authentication: com.shterneregen.securelan.filetransfer.protocol.FileTransferPeerAuthentication,
    ): String = withContext(Dispatchers.IO) {
        val context = currentCoroutineContext()
        val transferId = UUID.randomUUID().toString()
        val operation = Operation()
        check(active == null) { "A transfer is already active" }
        active = operation
        var attempt = TransferAttempt(wireId = transferId, total = selectedFile.size)
        fun publish(phase: TransferPhase, bytes: Long = attempt.bytes, total: Long? = attempt.total) {
            attempt = attempt.copy(phase = phase, bytes = bytes, total = total)
            onUpdate(attempt)
        }
        val watchdog = Executors.newSingleThreadScheduledExecutor { runnable -> Thread(runnable, "file-transfer-watchdog").apply { isDaemon = true } }
        watchdog.scheduleWithFixedDelay({
            if (!context.isActive) operation.close() else if (System.nanoTime() >= operation.deadline) operation.expire()
        }, 100, 100, TimeUnit.MILLISECONDS)
        var staged: File? = null
        var failureKind = FileTransferFailureKind.SOURCE
        try {
            publish(TransferPhase.PREPARING)
            operation.touch(timeouts.inactivityMillis)
            cacheDirectory.mkdirs()
            // This directory belongs exclusively to the single sender slot.
            cacheDirectory.listFiles()?.filter { it.name.startsWith("outgoing-") }?.forEach { it.delete() }
            var size = selectedFile.size
            val original = requireNotNull(contentResolver.openInputStream(selectedFile.reference.toUri())) { "Source unavailable" }
            operation.own(original)
            if (size == null) {
                val temp = File.createTempFile("outgoing-", ".tmp", cacheDirectory)
                staged = temp
                original.use { input ->
                    temp.outputStream().use { output ->
                        size = stageStream(input, output) { context.ensureActive(); operation.checkOpen(); operation.touch(timeouts.inactivityMillis) }
                    }
                }
                operation.own(temp.inputStream())
            }
            val exactSize = requireNotNull(size)
            val metadata = FileTransferMetadata(transferId, senderId, recipientId, safeTransferFileName(selectedFile.name), exactSize)
            operation.checkOpen()
            val socket = operation.socket
            failureKind = FileTransferFailureKind.CONNECTION
            operation.touch(timeouts.connectMillis)
            socket.connect(InetSocketAddress(host, port), timeouts.connectMillis)
            socket.soTimeout = timeouts.acceptanceMillis
            operation.touch(timeouts.acceptanceMillis)
            FileTransferSession(socket).use { session ->
                publish(TransferPhase.WAITING_FOR_ACCEPTANCE, total = exactSize)
                failureKind = FileTransferFailureKind.PROTOCOL
                com.shterneregen.securelan.filetransfer.protocol.SecureFileTransferHandshake().performClientHandshake(
                    session, senderId, recipientId, sessionPassword, metadata.fileName, metadata.fileSize, transferId, authentication,
                )
                socket.soTimeout = timeouts.inactivityMillis
                operation.touch(timeouts.inactivityMillis)
                publish(TransferPhase.SENDING)
                requireNotNull(operation.input).use { input ->
                    var lastUpdate = 0L
                    FileTransferStreams.send(input, exactSize, { chunk ->
                        failureKind = FileTransferFailureKind.CONNECTION
                        session.writeEncryptedBytes(chunk)
                    }, { context.ensureActive(); operation.checkOpen() }) { sent ->
                        operation.touch(timeouts.inactivityMillis)
                        val now = System.nanoTime()
                        if (now - lastUpdate >= 100_000_000 || sent == exactSize) {
                            publish(TransferPhase.SENDING, sent)
                            lastUpdate = now
                        }
                    }
                }
                context.ensureActive()
                operation.checkOpen()
                FileTransferStreams.finishSending(session, {
                    failureKind = FileTransferFailureKind.OUTCOME_UNKNOWN
                    publish(TransferPhase.WAITING_FOR_CONFIRMATION, exactSize)
                    socket.soTimeout = timeouts.confirmationMillis
                    operation.touch(timeouts.confirmationMillis)
                }, {})
            }
            transferId
        } catch (error: Exception) {
            context.ensureActive()
            throw FileTransferAttemptException(when {
                failureKind == FileTransferFailureKind.OUTCOME_UNKNOWN -> failureKind
                operation.timedOut || error is java.net.SocketTimeoutException -> FileTransferFailureKind.TIMEOUT
                operation.stopped -> FileTransferFailureKind.ABORTED
                error is TransferRejectedException -> FileTransferFailureKind.REJECTED
                error is TransferSourceException -> FileTransferFailureKind.SOURCE
                else -> failureKind
            }, error)
        } finally {
            operation.close()
            watchdog.shutdownNow()
            staged?.delete()
            active = null
        }
    }

    private class Operation : Closeable {
        val socket = Socket()
        @Volatile var input: InputStream? = null
        @Volatile var deadline: Long = Long.MAX_VALUE
        @Volatile private var closed = false
        val stopped: Boolean get() = closed
        @Volatile var timedOut = false; private set
        fun expire() { timedOut = true; close() }
        fun touch(millis: Int) { deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(millis.toLong()) }
        @Synchronized fun own(stream: InputStream) {
            if (closed) { stream.close(); error("Transfer stopped") }
            input = stream
        }
        fun checkOpen() { check(!closed) { "Transfer stopped" } }
        override fun close() {
            closed = true
            runCatching { socket.close() }
            runCatching { input?.close() }
        }
    }

    private fun safeTransferFileName(fileName: String): String {
        val baseName = fileName
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .trim()
        val sanitized = buildString(baseName.length) {
            for (character in baseName) {
                append(if (character.code < 32 || character in INVALID_FILE_NAME_CHARS) '_' else character)
            }
        }.trim().trimEnd('.', ' ')
        val fallback = sanitized.ifBlank { "selected-file" }
        if (fallback.length <= MAX_FILE_NAME_LENGTH) return fallback

        val dot = fallback.lastIndexOf('.')
        val extension = if (dot > 0 && fallback.length - dot <= 16) fallback.substring(dot) else ""
        val prefixLength = (MAX_FILE_NAME_LENGTH - extension.length).coerceAtLeast(1)
        return fallback.take(prefixLength) + extension
    }

    companion object {
        private const val CHUNK_SIZE = 16 * 1024
        private const val MAX_FILE_NAME_LENGTH = 160
        private val INVALID_FILE_NAME_CHARS = setOf('<', '>', ':', '"', '/', '\\', '|', '?', '*')
    }
}
