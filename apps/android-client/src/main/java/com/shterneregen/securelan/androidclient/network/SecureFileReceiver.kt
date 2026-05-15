package com.shterneregen.securelan.androidclient.network

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.shterneregen.securelan.androidclient.model.SecureLanPorts
import com.shterneregen.securelan.androidclient.protocol.CryptoCompat
import com.shterneregen.securelan.androidclient.protocol.FileTransferMetadata
import com.shterneregen.securelan.androidclient.protocol.FileTransferSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream
import java.net.ServerSocket
import java.net.SocketTimeoutException
import java.nio.charset.StandardCharsets
import java.util.Base64

class SecureFileReceiver(private val context: Context) {
    suspend fun serve(
        port: Int = SecureLanPorts.DEFAULT_FILE_TRANSFER_PORT,
        sessionPassword: String,
        onStarted: (FileTransferMetadata) -> Unit,
        onProgress: (metadata: FileTransferMetadata, received: Long) -> Unit,
        onCompleted: (metadata: FileTransferMetadata, savedPath: String) -> Unit,
        onError: (String, Throwable?) -> Unit,
    ) = withContext(Dispatchers.IO) {
        require(port in 1..65535) { "File receiver port out of range: $port" }
        ServerSocket(port).use { serverSocket ->
            serverSocket.soTimeout = ACCEPT_TIMEOUT_MS
            while (isActive) {
                try {
                    val socket = serverSocket.accept()
                    runCatching {
                        socket.use { handleClient(it, sessionPassword, onStarted, onProgress, onCompleted) }
                    }.onFailure { error -> onError(error.message ?: "Incoming file transfer failed", error) }
                } catch (_: SocketTimeoutException) {
                }
            }
        }
    }

    private fun handleClient(
        socket: java.net.Socket,
        sessionPassword: String,
        onStarted: (FileTransferMetadata) -> Unit,
        onProgress: (metadata: FileTransferMetadata, received: Long) -> Unit,
        onCompleted: (metadata: FileTransferMetadata, savedPath: String) -> Unit,
    ) {
        FileTransferSession(socket).use { session ->
            val metadata = performServerHandshake(session, sessionPassword)
            session.writeEncryptedText(ACCEPTED)
            onStarted(metadata)

            val target = createReceiveTarget(metadata.fileName)
            var received = 0L
            target.openOutputStream().use { output ->
                while (true) {
                    val chunk = session.readEncryptedBytes()
                    if (chunk.isEmpty()) break
                    output.write(chunk)
                    received += chunk.size
                    onProgress(metadata, received)
                }
            }
            session.writeEncryptedText(DONE)
            onCompleted(metadata, target.displayPath)
        }
    }

    private fun performServerHandshake(session: FileTransferSession, expectedPassword: String): FileTransferMetadata {
        val protocol = session.readUtf()
        require(protocol == PROTOCOL) { "Unsupported file transfer protocol" }
        val keyPair = CryptoCompat.generateRsaKeyPair()
        session.writeUtf(Base64.getEncoder().encodeToString(CryptoCompat.encodePublicKey(keyPair.public)))
        val encryptedPayload = session.readBytes()
        val payload = String(CryptoCompat.rsaDecrypt(encryptedPayload, keyPair.private), StandardCharsets.UTF_8)
        val parts = payload.split("\n", limit = 7)
        require(parts.size == 2 || parts.size == 7) { "Malformed secure file transfer payload" }
        require(parts[0] == expectedPassword) { "Wrong transfer password" }
        val sessionKey = CryptoCompat.decodeSecretKey(Base64.getDecoder().decode(parts[1]))
        session.enableTransportEncryption(sessionKey)
        return if (parts.size == 2) decodeCompactMetadata(session.readEncryptedText()) else decodeMetadata(parts)
    }

    private fun decodeMetadata(parts: List<String>): FileTransferMetadata {
        val compactParts = parts[2].split("|", limit = 5)
        if (compactParts.size == 5) {
            return decodeCompactMetadata(parts[2])
        }
        return FileTransferMetadata(parts[2], parts[3], parts[4], parts[5], parts[6].toLong())
    }

    private fun decodeCompactMetadata(value: String): FileTransferMetadata {
        val compactParts = value.split("|", limit = 5)
        require(compactParts.size == 5) { "Malformed compact metadata payload" }
        return FileTransferMetadata(
            transferId = decodeCompactText(compactParts[0]),
            senderId = decodeCompactText(compactParts[1]),
            recipientId = decodeCompactText(compactParts[2]),
            fileName = decodeCompactText(compactParts[3]),
            fileSize = compactParts[4].toLong(),
        )
    }

    private fun decodeCompactText(value: String): String = String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)

    private fun createReceiveTarget(fileName: String): ReceiveTarget {
        val safeName = safeFileName(fileName)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, safeName)
                put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + File.separator + DOWNLOAD_SUBDIRECTORY)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("Unable to create Downloads/$DOWNLOAD_SUBDIRECTORY/$safeName")
            return ReceiveTarget(
                displayPath = "Downloads/$DOWNLOAD_SUBDIRECTORY/$safeName",
                openOutputStream = {
                    resolver.openOutputStream(uri) ?: error("Unable to open Downloads/$DOWNLOAD_SUBDIRECTORY/$safeName")
                },
            )
        }

        val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), DOWNLOAD_SUBDIRECTORY).apply { mkdirs() }
        val target = uniqueTargetFile(directory, safeName)
        return ReceiveTarget(
            displayPath = target.absolutePath,
            openOutputStream = { target.outputStream() },
        )
    }

    private fun safeFileName(fileName: String): String {
        val name = fileName.substringAfterLast('/').substringAfterLast('\\').ifBlank { "incoming-file" }
        return name.map { if (it in INVALID_FILE_NAME_CHARS || it.isISOControl()) '_' else it }
            .joinToString("")
            .take(MAX_FILE_NAME_LENGTH)
            .ifBlank { "incoming-file" }
    }

    private fun uniqueTargetFile(directory: File, safeName: String): File {
        val dot = safeName.lastIndexOf('.')
        val base = if (dot > 0) safeName.substring(0, dot) else safeName
        val extension = if (dot > 0) safeName.substring(dot) else ""
        var candidate = File(directory, safeName)
        var index = 1
        while (candidate.exists()) {
            candidate = File(directory, "$base-$index$extension")
            index++
        }
        return candidate
    }

    private data class ReceiveTarget(
        val displayPath: String,
        val openOutputStream: () -> OutputStream,
    )

    companion object {
        private const val PROTOCOL = "SECURE_FILE_TRANSFER_V1"
        private const val ACCEPTED = "ACCEPTED"
        private const val DONE = "DONE"
        private const val ACCEPT_TIMEOUT_MS = 1000
        private const val DOWNLOAD_SUBDIRECTORY = "SecureLan"
        private const val MAX_FILE_NAME_LENGTH = 160
        private val INVALID_FILE_NAME_CHARS = setOf('<', '>', ':', '"', '/', '\\', '|', '?', '*')
    }
}
