package com.shterneregen.securelan.androidclient.network

import android.content.ContentResolver
import androidx.core.net.toUri
import com.shterneregen.securelan.androidclient.protocol.CryptoCompat
import com.shterneregen.securelan.androidclient.protocol.FileTransferMetadata
import com.shterneregen.securelan.androidclient.protocol.FileTransferSession
import com.shterneregen.securelan.androidclient.transfer.TransferAttempt
import com.shterneregen.securelan.androidclient.transfer.TransferPhase
import com.shterneregen.securelan.androidclient.transfer.TransferSource
import com.shterneregen.securelan.androidclient.transfer.stageStream
import com.shterneregen.securelan.androidclient.transfer.validateTransferLength
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
import java.nio.charset.StandardCharsets
import java.util.Base64
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
            if (!context.isActive || System.nanoTime() >= operation.deadline) operation.close()
        }, 100, 100, TimeUnit.MILLISECONDS)
        var staged: File? = null
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
            operation.touch(timeouts.connectMillis)
            socket.connect(InetSocketAddress(host, port), timeouts.connectMillis)
            socket.soTimeout = timeouts.acceptanceMillis
            operation.touch(timeouts.acceptanceMillis)
            FileTransferSession(socket).use { session ->
                publish(TransferPhase.WAITING_FOR_ACCEPTANCE, total = exactSize)
                performHandshake(session, sessionPassword, metadata)
                socket.soTimeout = timeouts.inactivityMillis
                operation.touch(timeouts.inactivityMillis)
                publish(TransferPhase.SENDING)
                requireNotNull(operation.input).use { input ->
                    val buffer = ByteArray(CHUNK_SIZE)
                    var sent = 0L
                    var lastUpdate = 0L
                    while (true) {
                        context.ensureActive()
                        operation.checkOpen()
                        val read = input.read(buffer)
                        if (read == -1) break
                        validateTransferLength(exactSize, Math.addExact(sent, read.toLong()), false)
                        session.writeEncryptedBytes(buffer.copyOf(read))
                        sent += read
                        operation.touch(timeouts.inactivityMillis)
                        val now = System.nanoTime()
                        if (now - lastUpdate >= 100_000_000 || sent == exactSize) {
                            publish(TransferPhase.SENDING, sent)
                            lastUpdate = now
                        }
                    }
                    validateTransferLength(exactSize, sent, true)
                }
                context.ensureActive()
                operation.checkOpen()
                // Once EOF submission begins, a lost response cannot prove non-delivery.
                socket.soTimeout = timeouts.confirmationMillis
                operation.touch(timeouts.confirmationMillis)
                publish(TransferPhase.WAITING_FOR_CONFIRMATION, exactSize)
                session.writeEncryptedBytes(ByteArray(0))
                val result = session.readEncryptedText()
                require(result == "DONE") { "Confirmation unavailable" }
            }
            transferId
        } catch (error: Exception) {
            context.ensureActive()
            throw error
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

    private fun performHandshake(session: FileTransferSession, sessionPassword: String, metadata: FileTransferMetadata) {
        session.writeUtf(PROTOCOL)
        val serverPublicKey = CryptoCompat.decodePublicKey(Base64.getDecoder().decode(session.readUtf()))
        val aesKey = CryptoCompat.generateAesKey()
        val payload = sessionPassword + "\n" +
            Base64.getEncoder().encodeToString(CryptoCompat.encodeSecretKey(aesKey))
        val encrypted = CryptoCompat.rsaEncrypt(payload.toByteArray(StandardCharsets.UTF_8), serverPublicKey)
        session.writeBytes(encrypted)
        session.enableTransportEncryption(aesKey)
        session.writeEncryptedText(metadata.compactSerialize())
        val response = session.readEncryptedText()
        require(response == ACCEPTED) { response }
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
        private const val PROTOCOL = "SECURE_FILE_TRANSFER_V1"
        private const val ACCEPTED = "ACCEPTED"
        private const val CHUNK_SIZE = 16 * 1024
        private const val MAX_FILE_NAME_LENGTH = 160
        private val INVALID_FILE_NAME_CHARS = setOf('<', '>', ':', '"', '/', '\\', '|', '?', '*')
    }
}
