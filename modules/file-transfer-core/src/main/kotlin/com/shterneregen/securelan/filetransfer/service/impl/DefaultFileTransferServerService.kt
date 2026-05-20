package com.shterneregen.securelan.filetransfer.service.impl

import com.shterneregen.securelan.common.model.FileTransferProgress
import com.shterneregen.securelan.common.model.TransferStatus
import com.shterneregen.securelan.common.net.transport.TcpServer
import com.shterneregen.securelan.crypto.CryptoServices
import com.shterneregen.securelan.filetransfer.event.FileTransferCompletedEvent
import com.shterneregen.securelan.filetransfer.event.FileTransferFailedEvent
import com.shterneregen.securelan.filetransfer.event.FileTransferProgressEvent
import com.shterneregen.securelan.filetransfer.event.FileTransferStartedEvent
import com.shterneregen.securelan.filetransfer.protocol.FileTransferSession
import com.shterneregen.securelan.filetransfer.service.FileTransferEventPublisher
import com.shterneregen.securelan.filetransfer.service.FileTransferServerConfig
import com.shterneregen.securelan.filetransfer.service.FileTransferServerService
import java.io.IOException
import java.net.Socket
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.Locale
import java.util.Objects
import java.util.concurrent.atomic.AtomicBoolean

class DefaultFileTransferServerService @JvmOverloads constructor(
    eventPublisher: FileTransferEventPublisher,
    cryptoServices: CryptoServices = CryptoServices.createDefault(),
) : FileTransferServerService {
    private val eventPublisher: FileTransferEventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher")
    private val handshake = SecureFileTransferHandshake(Objects.requireNonNull(cryptoServices, "cryptoServices"))
    private val lifecycleLock = Any()
    private val running = AtomicBoolean(false)
    private val tcpServer = TcpServer("file-transfer-server")

    @Volatile
    private var config: FileTransferServerConfig? = null

    override fun start(config: FileTransferServerConfig) {
        Objects.requireNonNull(config, "config")
        synchronized(lifecycleLock) {
            if (running.get()) {
                return
            }
            try {
                Files.createDirectories(config.storageDirectory())
                this.config = config
                running.set(true)
                val activeConfig = config
                tcpServer.start(config.port(), { socket -> handleClient(socket, activeConfig) }, this::publishAcceptError)
            } catch (e: Exception) {
                if (e is IOException || e is RuntimeException) {
                    cleanupFailedStart()
                    throw IllegalStateException("Unable to start file transfer server", e)
                }
                throw e
            }
        }
    }

    private fun publishAcceptError(message: String, cause: Throwable) {
        if (running.get()) {
            eventPublisher.publish(FileTransferFailedEvent("server", "", message, cause, false))
        }
    }

    private fun handleClient(socket: Socket, activeConfig: FileTransferServerConfig) {
        socket.use { activeSocket ->
            try {
                FileTransferSession(activeSocket).use { session ->
                    val metadata = handshake.performServerHandshake(session, activeConfig.sessionPassword())
                    if (!activeConfig.acceptanceHandler().accept(metadata, session.remoteAddress())) {
                        handshake.rejectTransfer(session, "File transfer rejected by receiver")
                        eventPublisher.publish(
                            FileTransferFailedEvent(
                                metadata.transferId,
                                metadata.fileName,
                                "Rejected incoming file from ${metadata.senderId}",
                                null,
                                false,
                            ),
                        )
                        return
                    }
                    handshake.acceptTransfer(session)
                    eventPublisher.publish(FileTransferStartedEvent(metadata.transferId, metadata.fileName, metadata.fileSize, false))

                    val target = createUniqueTargetPath(metadata.fileName, activeConfig)
                    var transferred = 0L
                    Files.newOutputStream(target, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE).use { outputStream ->
                        while (true) {
                            val chunk = session.readEncryptedBytes()
                            if (chunk.isEmpty()) {
                                break
                            }
                            outputStream.write(chunk)
                            transferred += chunk.size.toLong()
                            eventPublisher.publish(
                                FileTransferProgressEvent(
                                    metadata.transferId,
                                    FileTransferProgress(metadata.transferId, transferred, metadata.fileSize, TransferStatus.IN_PROGRESS),
                                    false,
                                ),
                            )
                        }
                    }
                    session.writeEncryptedText("DONE")
                    eventPublisher.publish(FileTransferCompletedEvent(metadata.transferId, metadata.fileName, target, metadata.fileSize, false))
                }
            } catch (e: Exception) {
                eventPublisher.publish(FileTransferFailedEvent("incoming", "", failureMessage(e, "Incoming file transfer failed"), e, false))
            }
        }
    }

    @Throws(IOException::class)
    private fun createUniqueTargetPath(fileName: String?, activeConfig: FileTransferServerConfig): Path {
        val storageDirectory = activeConfig.storageDirectory().normalize()
        val safeFileName = sanitizeFileName(fileName)
        val candidate = storageDirectory.resolve(safeFileName).normalize()
        if (!candidate.startsWith(storageDirectory)) {
            throw IOException("Invalid target file path")
        }
        if (!Files.exists(candidate)) {
            return candidate
        }
        val dot = safeFileName.lastIndexOf('.')
        val baseName: String
        val extension: String
        if (dot > 0) {
            baseName = safeFileName.substring(0, dot)
            extension = safeFileName.substring(dot)
        } else {
            baseName = safeFileName
            extension = ""
        }
        var index = 1
        while (true) {
            val alternative = storageDirectory.resolve("$baseName-$index$extension").normalize()
            if (!Files.exists(alternative)) {
                return alternative
            }
            index++
        }
    }

    private fun sanitizeFileName(fileName: String?): String {
        var baseName = (fileName ?: "incoming-file").replace('\\', '/')
        val slash = baseName.lastIndexOf('/')
        if (slash >= 0) {
            baseName = baseName.substring(slash + 1)
        }
        val sanitized = StringBuilder(baseName.length)
        for (character in baseName) {
            sanitized.append(if (character.code < 32 || INVALID_FILE_NAME_CHARS.indexOf(character) >= 0) '_' else character)
        }
        var safeName = trimUnsafeFileNameEdges(sanitized.toString().trim())
        if (safeName.isBlank()) {
            safeName = "incoming-file"
        }
        safeName = avoidReservedWindowsFileName(safeName)
        if (safeName.length <= MAX_SAFE_FILE_NAME_LENGTH) {
            return safeName
        }
        val dot = safeName.lastIndexOf('.')
        val extension = if (dot > 0 && safeName.length - dot <= 16) safeName.substring(dot) else ""
        val prefixLength = maxOf(1, MAX_SAFE_FILE_NAME_LENGTH - extension.length)
        return safeName.substring(0, prefixLength) + extension
    }

    private fun trimUnsafeFileNameEdges(fileName: String): String {
        var end = fileName.length
        while (end > 0 && (fileName[end - 1] == '.' || fileName[end - 1] == ' ')) {
            end--
        }
        return fileName.substring(0, end)
    }

    private fun avoidReservedWindowsFileName(fileName: String): String {
        val dot = fileName.indexOf('.')
        val stem = if (dot > 0) fileName.substring(0, dot) else fileName
        return if (WINDOWS_RESERVED_FILE_NAMES.contains(stem.uppercase(Locale.ROOT))) "_$fileName" else fileName
    }

    private fun failureMessage(exception: Exception, fallback: String): String =
        if (!exception.message.isNullOrBlank()) exception.message!! else "$fallback: ${exception.javaClass.simpleName}"

    override fun stop() {
        synchronized(lifecycleLock) {
            running.set(false)
            tcpServer.close()
            config = null
        }
    }

    override fun isRunning(): Boolean = running.get()

    private fun cleanupFailedStart() {
        running.set(false)
        tcpServer.close()
        config = null
    }

    companion object {
        private const val MAX_SAFE_FILE_NAME_LENGTH = 160
        private const val INVALID_FILE_NAME_CHARS = "<>:\"/\\|?*"
        private val WINDOWS_RESERVED_FILE_NAMES = setOf(
            "CON", "PRN", "AUX", "NUL",
            "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
            "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9",
        )
    }
}
