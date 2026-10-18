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
import com.shterneregen.securelan.androidclient.protocol.FileTransferMetadata
import com.shterneregen.securelan.androidclient.protocol.FileTransferSession
import com.shterneregen.securelan.androidclient.transfer.numberedTransferFileName
import com.shterneregen.securelan.filetransfer.protocol.FileTransferStreams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream
import java.util.Locale

/** Platform-independent receiver boundary for workspace lifecycle ownership and correlated events. */
interface IncomingFileReceiver {
    fun setReadinessListener(listener: (Boolean) -> Unit) {}
    fun close()
    suspend fun serve(
        port: Int = SecureLanPorts.DEFAULT_FILE_TRANSFER_PORT,
        sessionPassword: String,
        onStarted: (FileTransferMetadata) -> Unit,
        onProgress: (FileTransferMetadata, Long) -> Unit,
        onCompleted: (FileTransferMetadata, SecureFileReceiver.ReceivedFile) -> Unit,
        onError: (String, Throwable?) -> Unit,
        authentication: com.shterneregen.securelan.filetransfer.protocol.FileTransferPeerAuthentication,
        onTransferError: (FileTransferMetadata, String, Throwable?) -> Unit = { _, message, error -> onError(message, error) },
    )
}

/**
 * Uses bounded concurrent V2 handshakes and one authorized receive slot for Downloads publication.
 * Publishes complete files to Downloads and removes unfinished targets on failure or the next start.
 * Callbacks run on the IO thread; [close] unblocks both the listener and current transfer.
 */
class SecureFileReceiver(private val context: Context) : IncomingFileReceiver {
    @Volatile private var readiness: (Boolean) -> Unit = {}
    override fun setReadinessListener(listener: (Boolean) -> Unit) { readiness = listener }
    private val listener = java.util.concurrent.atomic.AtomicReference<com.shterneregen.securelan.common.net.transport.TcpServer?>()
    private val receiveSlot = java.util.concurrent.Semaphore(1)
    private val pending = context.getSharedPreferences("pending_received_files", Context.MODE_PRIVATE)

    override fun close() {
        listener.getAndSet(null)?.close()
    }

    override suspend fun serve(
        port: Int,
        sessionPassword: String,
        onStarted: (FileTransferMetadata) -> Unit,
        onProgress: (metadata: FileTransferMetadata, received: Long) -> Unit,
        onCompleted: (metadata: FileTransferMetadata, receivedFile: ReceivedFile) -> Unit,
        onError: (String, Throwable?) -> Unit,
        authentication: com.shterneregen.securelan.filetransfer.protocol.FileTransferPeerAuthentication,
        onTransferError: (FileTransferMetadata, String, Throwable?) -> Unit,
    ): Unit = withContext(Dispatchers.IO) {
        require(port in 1..65535) { "File receiver port out of range" }
        check(receiveSlot.tryAcquire()) { "Previous receive is still closing" }
        try { cleanupPending() } finally { receiveSlot.release() }
        val server = com.shterneregen.securelan.common.net.transport.TcpServer(
            "android-file-receiver", com.shterneregen.securelan.common.net.transport.ServerSocketFactory.systemDefault(), 4,
        )
        check(listener.compareAndSet(null, server)) { "File receiver already running" }
        val reportReady = readiness
        val budget = com.shterneregen.securelan.filetransfer.protocol.FileReceiveBudget()
        try {
            server.start(port, { socket ->
                try {
                    socket.soTimeout = 60_000
                    socket.use { handleClient(it, sessionPassword, authentication, budget, onStarted, onProgress, onCompleted, onTransferError) }
                } catch (error: Exception) {
                    onError("Incoming file transfer failed", error)
                }
            }, { _, error -> onError("File receiver unavailable", error) })
            reportReady(true)
            kotlinx.coroutines.awaitCancellation()
        } finally {
            reportReady(false)
            server.close()
            listener.compareAndSet(server, null)
        }
    }

    private fun handleClient(
        socket: java.net.Socket,
        sessionPassword: String,
        authentication: com.shterneregen.securelan.filetransfer.protocol.FileTransferPeerAuthentication,
        budget: com.shterneregen.securelan.filetransfer.protocol.FileReceiveBudget,
        onStarted: (FileTransferMetadata) -> Unit,
        onProgress: (metadata: FileTransferMetadata, received: Long) -> Unit,
        onCompleted: (metadata: FileTransferMetadata, receivedFile: ReceivedFile) -> Unit,
        onTransferError: (FileTransferMetadata, String, Throwable?) -> Unit,
    ) {
        FileTransferSession(socket).use { session ->
            val handshake = com.shterneregen.securelan.filetransfer.protocol.SecureFileTransferHandshake()
            val metadata = handshake.performServerHandshake(session, sessionPassword, authentication)
            check(metadata.fileSize in 0..MAX_FILE_BYTES) { "File exceeds receive budget" }
            check(Environment.getExternalStorageDirectory().usableSpace >= metadata.fileSize + 128L * 1024 * 1024) { "Insufficient receive storage" }
            if (!receiveSlot.tryAcquire()) {
                handshake.rejectTransfer(session, "Receiver busy")
                return
            }
            try {
                budget.reserve(metadata.fileSize).use { quota ->
                handshake.acceptTransfer(session)
                onStarted(metadata)
                val target = createReceiveTarget(metadata.fileName)
                var received = 0L
                var published = false
                try {
                    target.openOutputStream().use { output ->
                        var lastProgressAt: Long? = null
                        received = FileTransferStreams.receive(output, metadata.fileSize, { session.readEncryptedBytes() }) { bytes ->
                            val now = System.nanoTime()
                            if (lastProgressAt == null || bytes == metadata.fileSize || now - lastProgressAt!! >= 100_000_000L) {
                                lastProgressAt = now
                                onProgress(metadata, bytes)
                            }
                        }
                    }
                    val receivedFile = FileTransferStreams.publishAndConfirm(session, {
                        target.publish().also { published = true }
                    }, { quota.commit() })
                    onCompleted(metadata.copy(fileName = receivedFile.savedPath.substringAfterLast('/'), fileSize = received), receivedFile)
                } finally { if (!published) target.discard() }
                }
            } catch (error: Exception) {
                onTransferError(metadata, "Incoming file transfer failed", error)
            } finally { receiveSlot.release() }
        }
    }

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
        private const val MAX_FILE_BYTES = 1024L * 1024 * 1024
        private const val ACCEPTED = "ACCEPTED"
        private const val DONE = "DONE"
        private const val ACCEPT_TIMEOUT_MS = 1000
        private const val DOWNLOAD_SUBDIRECTORY = "SecureLan"
        private const val MAX_FILE_NAME_LENGTH = 160
        private const val MAX_DUPLICATE_FILE_INDEX = 10_000
        private val INVALID_FILE_NAME_CHARS = setOf('<', '>', ':', '"', '/', '\\', '|', '?', '*')
    }
}
