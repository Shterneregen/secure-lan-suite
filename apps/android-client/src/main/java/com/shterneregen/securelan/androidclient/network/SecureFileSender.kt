package com.shterneregen.securelan.androidclient.network

import android.content.ContentResolver
import com.shterneregen.securelan.androidclient.model.SelectedFile
import com.shterneregen.securelan.androidclient.protocol.CryptoCompat
import com.shterneregen.securelan.androidclient.protocol.FileTransferMetadata
import com.shterneregen.securelan.androidclient.protocol.FileTransferSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.UUID

class SecureFileSender(private val contentResolver: ContentResolver) {
    suspend fun sendFile(
        host: String,
        port: Int,
        senderId: String,
        recipientId: String,
        sessionPassword: String,
        selectedFile: SelectedFile,
        onProgress: (sent: Long, total: Long) -> Unit,
    ): String = withContext(Dispatchers.IO) {
        val transferId = UUID.randomUUID().toString()
        val metadata = FileTransferMetadata(transferId, senderId, recipientId, safeTransferFileName(selectedFile.name), selectedFile.size)
        Socket(host, port).use { socket ->
            FileTransferSession(socket).use { session ->
                performHandshake(session, sessionPassword, metadata)
                contentResolver.openInputStream(selectedFile.uri).use { input ->
                    requireNotNull(input) { "Unable to open selected file" }
                    val buffer = ByteArray(CHUNK_SIZE)
                    var sent = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        session.writeEncryptedBytes(buffer.copyOf(read))
                        sent += read
                        onProgress(sent, selectedFile.size)
                    }
                }
                session.writeEncryptedBytes(ByteArray(0))
                val result = session.readEncryptedText()
                require(result == "DONE") { result }
            }
        }
        transferId
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
