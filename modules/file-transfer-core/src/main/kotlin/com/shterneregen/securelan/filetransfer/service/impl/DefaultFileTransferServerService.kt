package com.shterneregen.securelan.filetransfer.service.impl

import com.shterneregen.securelan.common.net.transport.TcpServer
import com.shterneregen.securelan.crypto.CryptoServices
import com.shterneregen.securelan.filetransfer.event.FileTransferCompletedEvent
import com.shterneregen.securelan.filetransfer.event.FileTransferFailedEvent
import com.shterneregen.securelan.filetransfer.event.FileTransferStartedEvent
import com.shterneregen.securelan.filetransfer.protocol.FileTransferSession
import com.shterneregen.securelan.filetransfer.protocol.FileTransferMetadata
import com.shterneregen.securelan.filetransfer.service.FileTransferTimeouts
import com.shterneregen.securelan.filetransfer.service.FileTransferEventPublisher
import com.shterneregen.securelan.filetransfer.service.FileTransferServerConfig
import com.shterneregen.securelan.filetransfer.service.FileTransferServerService
import java.io.IOException
import java.net.Socket
import java.nio.file.Files
import java.nio.file.FileAlreadyExistsException
import java.nio.file.FileSystemException
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.Locale
import java.util.Objects
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class DefaultFileTransferServerService @JvmOverloads constructor(
    eventPublisher: FileTransferEventPublisher,
    cryptoServices: CryptoServices = CryptoServices.createDefault(),
    private val timeouts: FileTransferTimeouts = FileTransferTimeouts(),
) : FileTransferServerService {
    private val eventPublisher: FileTransferEventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher")
    private val handshake = SecureFileTransferHandshake(Objects.requireNonNull(cryptoServices, "cryptoServices"))
    private val lifecycleLock = Any()
    private val running = AtomicBoolean(false)
    private val tcpServer = TcpServer("file-transfer-server")
    private val activeSockets = mutableSetOf<Socket>()
    private var generation = 0L

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
                val activeGeneration = ++generation
                tcpServer.start(config.port(), { socket -> handleClient(socket, activeConfig, activeGeneration) }, this::publishAcceptError)
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

    private fun handleClient(socket: Socket, activeConfig: FileTransferServerConfig, activeGeneration: Long) {
        synchronized(lifecycleLock) {
            if (!running.get() || generation != activeGeneration) {
                socket.close()
                return
            }
            activeSockets.add(socket)
        }
        var knownMetadata: FileTransferMetadata? = null
        var temporary: Path? = null
        val deadline = AtomicLong(System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeouts.inactivityMillis))
        fun arm(millis: Long) { deadline.set(System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(millis)) }
        val watchdog = Executors.newSingleThreadScheduledExecutor { runnable ->
            Thread(runnable, "file-receive-watchdog").apply { isDaemon = true }
        }
        watchdog.scheduleWithFixedDelay({
            if (System.nanoTime() - deadline.get() >= 0) {
                try { socket.close() } catch (_: IOException) { }
            }
        }, 1, minOf(1_000L, timeouts.inactivityMillis, timeouts.acceptanceMillis), TimeUnit.MILLISECONDS)
        socket.use { activeSocket ->
            try {
                FileTransferSession(activeSocket).use { session ->
                    val metadata = handshake.performServerHandshake(session, activeConfig.sessionPassword())
                    knownMetadata = metadata
                    if (metadata.fileSize < 0) throw IOException("Invalid file size")
                    arm(timeouts.acceptanceMillis)
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
                    arm(timeouts.inactivityMillis)
                    handshake.acceptTransfer(session)
                    eventPublisher.publish(FileTransferStartedEvent(metadata.transferId, metadata.fileName, metadata.fileSize, false))

                    val staging = Files.createTempFile(activeConfig.storageDirectory(), ".securelan-", ".part")
                    temporary = staging
                    var transferred = 0L
                    val progressPublisher = ThrottledFileTransferProgressPublisher(
                        eventPublisher,
                        metadata.transferId,
                        metadata.fileSize,
                        false,
                    )
                    Files.newOutputStream(staging, StandardOpenOption.WRITE).use { outputStream ->
                        while (true) {
                            val chunk = session.readEncryptedBytes()
                            if (chunk.isEmpty()) {
                                break
                            }
                            if (metadata.fileSize > 0 && chunk.size.toLong() > metadata.fileSize - transferred) {
                                throw IOException("Received file exceeds declared size")
                            }
                            outputStream.write(chunk)
                            transferred = Math.addExact(transferred, chunk.size.toLong())
                            arm(timeouts.inactivityMillis)
                            progressPublisher.report(transferred)
                        }
                    }
                    if (metadata.fileSize > 0 && transferred != metadata.fileSize) {
                        throw IOException("Received file length differs from declared size")
                    }
                    val target = publishUniqueTarget(staging, metadata.fileName, activeConfig)
                    Files.deleteIfExists(staging)
                    temporary = null
                    arm(timeouts.confirmationMillis)
                    session.writeEncryptedText("DONE")
                    eventPublisher.publish(FileTransferCompletedEvent(metadata.transferId, target.fileName.toString(), target, transferred, false))
                }
            } catch (e: Exception) {
                eventPublisher.publish(FileTransferFailedEvent(knownMetadata?.transferId ?: "incoming", knownMetadata?.fileName ?: "", "Incoming file transfer failed", e, false))
            } finally {
                watchdog.shutdownNow()
                temporary?.let { path ->
                    try { Files.deleteIfExists(path) } catch (_: IOException) { }
                }
                synchronized(lifecycleLock) { activeSockets.remove(socket) }
            }
        }
    }

    @Throws(IOException::class)
    private fun publishUniqueTarget(staging: Path, fileName: String?, activeConfig: FileTransferServerConfig): Path {
        val storageDirectory = activeConfig.storageDirectory().normalize()
        val safeFileName = sanitizeFileName(fileName)
        val candidate = storageDirectory.resolve(safeFileName).normalize()
        if (!candidate.startsWith(storageDirectory)) {
            throw IOException("Invalid target file path")
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
        var index = 0
        while (true) {
            val alternative = if (index == 0) candidate else storageDirectory.resolve("$baseName-$index$extension").normalize()
            try {
                // Link creation atomically publishes complete bytes and fails on collision.
                // ATOMIC_MOVE is unsuitable: providers may replace an existing target.
                Files.createLink(alternative, staging)
                return alternative
            } catch (_: FileAlreadyExistsException) {
                index++
            } catch (_: UnsupportedOperationException) {
                return moveToUniqueTarget(staging, storageDirectory, candidate, baseName, extension, index)
            } catch (_: FileSystemException) {
                // FAT/exFAT, SMB, and some custom providers cannot create hard links.
                return moveToUniqueTarget(staging, storageDirectory, candidate, baseName, extension, index)
            }
        }
    }

    private fun moveToUniqueTarget(
        staging: Path,
        storageDirectory: Path,
        candidate: Path,
        baseName: String,
        extension: String,
        initialIndex: Int,
    ): Path {
        var index = initialIndex
        while (true) {
            val alternative = if (index == 0) candidate else storageDirectory.resolve("$baseName-$index$extension").normalize()
            try {
                // A same-directory move preserves the no-overwrite contract on providers
                // where hard links are unavailable and consumes the staging file.
                return Files.move(staging, alternative)
            } catch (_: FileAlreadyExistsException) {
                index++
            }
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

    override fun stop() {
        synchronized(lifecycleLock) {
            running.set(false)
            activeSockets.forEach { try { it.close() } catch (_: IOException) { } }
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
