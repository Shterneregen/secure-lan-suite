package com.shterneregen.securelan.androidclient.network

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.core.net.toUri
import com.shterneregen.securelan.androidclient.model.SecureLanPorts
import com.shterneregen.securelan.androidclient.protocol.CryptoCompat
import com.shterneregen.securelan.androidclient.protocol.FileTransferMetadata
import com.shterneregen.securelan.androidclient.protocol.FileTransferSession
import com.shterneregen.securelan.androidclient.transfer.numberedTransferFileName
import com.shterneregen.securelan.androidclient.transfer.validateTransferLength
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream
import java.net.ServerSocket
import java.net.SocketTimeoutException
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Receives files sequentially on the IO dispatcher using the desktop encrypted transfer protocol.
 * Publishes complete files to Downloads and removes unfinished targets on failure or the next start.
 * Callbacks run on the IO thread; [close] unblocks both the listener and current transfer.
 */
class SecureFileReceiver(private val context: Context) {
    @Volatile private var listener: ServerSocket? = null
    @Volatile private var accepted: java.net.Socket? = null
    private val pending = context.getSharedPreferences("pending_received_files", Context.MODE_PRIVATE)

    fun close() {
        runCatching { listener?.close() }
        runCatching { accepted?.close() }
    }

    suspend fun serve(
        port: Int = SecureLanPorts.DEFAULT_FILE_TRANSFER_PORT,
        sessionPassword: String,
        onStarted: (FileTransferMetadata) -> Unit,
        onProgress: (metadata: FileTransferMetadata, received: Long) -> Unit,
        onCompleted: (metadata: FileTransferMetadata, receivedFile: ReceivedFile) -> Unit,
        onError: (String, Throwable?) -> Unit,
    ) = withContext(Dispatchers.IO) {
        require(port in 1..65535) { "File receiver port out of range: $port" }
        val coroutine = currentCoroutineContext()
        val watchdog = Executors.newSingleThreadScheduledExecutor { runnable -> Thread(runnable, "file-receiver-watchdog").apply { isDaemon = true } }
        watchdog.scheduleWithFixedDelay({ if (!coroutine.isActive) close() }, 100, 100, TimeUnit.MILLISECONDS)
        try {
            cleanupPending()
            ServerSocket(port).use { serverSocket ->
                listener = serverSocket
                serverSocket.soTimeout = ACCEPT_TIMEOUT_MS
                while (isActive) {
                    try {
                        val socket = serverSocket.accept()
                        accepted = socket
                        socket.soTimeout = 60_000
                        try {
                            coroutine.ensureActive()
                            socket.use { handleClient(it, sessionPassword, onStarted, onProgress, onCompleted) }
                        } catch (error: Exception) {
                            coroutine.ensureActive()
                            onError("Incoming file transfer failed", error)
                        } finally {
                            accepted = null
                            socket.close()
                        }
                    } catch (_: SocketTimeoutException) {
                    }
                }
            }
        } finally {
            close()
            listener = null
            watchdog.shutdownNow()
        }
    }

    private fun handleClient(
        socket: java.net.Socket,
        sessionPassword: String,
        onStarted: (FileTransferMetadata) -> Unit,
        onProgress: (metadata: FileTransferMetadata, received: Long) -> Unit,
        onCompleted: (metadata: FileTransferMetadata, receivedFile: ReceivedFile) -> Unit,
    ) {
        FileTransferSession(socket).use { session ->
            val metadata = performServerHandshake(session, sessionPassword)
            require(metadata.fileSize >= 0) { "Invalid file length" }
            session.writeEncryptedText(ACCEPTED)
            onStarted(metadata)

            val target = createReceiveTarget(metadata.fileName)
            var received = 0L
            var published = false
            try {
                target.openOutputStream().use { output ->
                    while (true) {
                        val chunk = session.readEncryptedBytes()
                        if (chunk.isEmpty()) break
                        validateTransferLength(metadata.fileSize, Math.addExact(received, chunk.size.toLong()), false, legacyUnknown = true)
                        output.write(chunk)
                        received += chunk.size
                        onProgress(metadata, received)
                    }
                }
                validateTransferLength(metadata.fileSize, received, true, legacyUnknown = true)
                val receivedFile = target.publish()
                published = true
                onCompleted(
                    metadata.copy(fileName = receivedFile.savedPath.substringAfterLast('/'), fileSize = received),
                    receivedFile,
                )
                session.writeEncryptedText(DONE)
            } finally {
                if (!published) target.discard()
            }
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
            decodeCompactText(compactParts[0]),
            decodeCompactText(compactParts[1]),
            decodeCompactText(compactParts[2]),
            decodeCompactText(compactParts[3]),
            compactParts[4].toLong(),
        )
    }

    private fun decodeCompactText(value: String): String = String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)

    // This recovery journal must reach disk before file IO; asynchronous apply() can lose it on process death.
    private fun createReceiveTarget(fileName: String): ReceiveTarget {
        val safeName = safeFileName(fileName)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, safeName)
                put(MediaStore.Downloads.MIME_TYPE, mimeTypeForFileName(safeName))
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + File.separator + DOWNLOAD_SUBDIRECTORY)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("Unable to create Downloads/$DOWNLOAD_SUBDIRECTORY/$safeName")
            pending.edit(commit = true) { putString("uri", uri.toString()) }
            val displayName = try {
                stabilizeMediaStoreDisplayName(resolver, uri, safeName)
            } catch (error: Exception) {
                resolver.delete(uri, null, null)
                pending.edit(commit = true) { remove("uri") }
                throw error
            }
            return ReceiveTarget(
                openOutputStream = {
                    resolver.openOutputStream(uri) ?: error("Unable to open Downloads/$DOWNLOAD_SUBDIRECTORY/$displayName")
                },
                publish = {
                    check(resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null) == 1)
                    pending.edit(commit = true) { remove("uri") }
                    val actualName = resolver.query(uri, arrayOf(MediaStore.Downloads.DISPLAY_NAME), null, null, null)?.use {
                        if (it.moveToFirst()) it.getString(0) else null
                    } ?: displayName
                    ReceivedFile(
                        savedPath = "Downloads/$DOWNLOAD_SUBDIRECTORY/$actualName",
                        contentUri = uri,
                    )
                },
                discard = { resolver.delete(uri, null, null); pending.edit(commit = true) { remove("uri") } },
            )
        }

        val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), DOWNLOAD_SUBDIRECTORY).apply { mkdirs() }
        val temp = File.createTempFile(".securelan-incoming-", ".tmp", directory)
        pending.edit(commit = true) { putString("temp", temp.absolutePath) }
        return ReceiveTarget(
            openOutputStream = { temp.outputStream() },
            publish = {
                // createNewFile reserves the final name atomically; rename replaces only our reservation.
                val target = uniqueTargetFile(directory, safeName)
                try {
                    val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", target)
                    check(temp.renameTo(target)) { "Unable to publish received file" }
                    pending.edit(commit = true) { remove("temp") }
                    ReceivedFile(savedPath = target.absolutePath, contentUri = contentUri)
                } catch (error: Exception) {
                    target.delete()
                    throw error
                }
            },
            discard = { temp.delete(); pending.edit(commit = true) { remove("temp") } },
        )
    }

    private fun safeFileName(fileName: String): String {
        val name = fileName.substringAfterLast('/').substringAfterLast('\\').ifBlank { "incoming-file" }
        return name.map { if (it in INVALID_FILE_NAME_CHARS || it.isISOControl()) '_' else it }
            .joinToString("")
            .take(MAX_FILE_NAME_LENGTH)
            .trim().trimEnd('.', ' ')
            .ifBlank { "incoming-file" }
    }

    private fun uniqueTargetFile(directory: File, safeName: String): File {
        var index = 0
        var candidate = File(directory, numberedTransferFileName(safeName, index))
        while (!candidate.createNewFile()) {
            index++
            candidate = File(directory, numberedTransferFileName(safeName, index))
        }
        return candidate
    }

    private fun mimeTypeForFileName(fileName: String): String =
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileName.substringAfterLast('.', "").lowercase(Locale.ROOT))
            ?: "application/octet-stream"

    private fun stabilizeMediaStoreDisplayName(
        resolver: ContentResolver,
        uri: Uri,
        safeName: String,
    ): String {
        fun currentName(): String? = resolver.query(
            uri,
            arrayOf(MediaStore.Downloads.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }

        if (currentName() == safeName) return safeName
        for (index in 1..MAX_DUPLICATE_FILE_INDEX) {
            val candidate = numberedTransferFileName(safeName, index)
            check(
                resolver.update(
                    uri,
                    ContentValues().apply { put(MediaStore.Downloads.DISPLAY_NAME, candidate) },
                    null,
                    null,
                ) == 1,
            ) { "Unable to assign a unique received file name" }
            if (currentName() == candidate) return candidate
        }
        error("Unable to assign a unique received file name")
    }

    private data class ReceiveTarget(
        val openOutputStream: () -> OutputStream,
        val publish: () -> ReceivedFile,
        val discard: () -> Unit,
    )

    /** Published download with a display path and a shareable content URI, never a partial target. */
    data class ReceivedFile(
        val savedPath: String,
        val contentUri: Uri,
    )

    // Recovery journal changes are persisted synchronously on the IO thread.
    private fun cleanupPending() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            pending.getString("uri", null)?.let { value ->
                val uri = value.toUri()
                runCatching {
                    val isPending = context.contentResolver.query(uri, arrayOf(MediaStore.Downloads.IS_PENDING), null, null, null)?.use { it.moveToFirst() && it.getInt(0) == 1 } == true
                    if (isPending) context.contentResolver.delete(uri, null, null)
                }
            }
        }
        pending.getString("temp", null)?.let { path ->
            val file = File(path)
            if (file.name.startsWith(".securelan-incoming-") && file.parentFile == File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), DOWNLOAD_SUBDIRECTORY)) file.delete()
        }
        pending.edit(commit = true) { clear() }
    }

    companion object {
        private const val PROTOCOL = "SECURE_FILE_TRANSFER_V1"
        private const val ACCEPTED = "ACCEPTED"
        private const val DONE = "DONE"
        private const val ACCEPT_TIMEOUT_MS = 1000
        private const val DOWNLOAD_SUBDIRECTORY = "SecureLan"
        private const val MAX_FILE_NAME_LENGTH = 160
        private const val MAX_DUPLICATE_FILE_INDEX = 10_000
        private val INVALID_FILE_NAME_CHARS = setOf('<', '>', ':', '"', '/', '\\', '|', '?', '*')
    }
}
