package com.shterneregen.securelan.filetransfer.service.impl

import com.shterneregen.securelan.crypto.CryptoServices
import com.shterneregen.securelan.filetransfer.protocol.FileTransferMetadata
import com.shterneregen.securelan.filetransfer.protocol.FileTransferSession
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.Base64

internal class SecureFileTransferHandshake(private val cryptoServices: CryptoServices) {
    @Throws(IOException::class)
    fun performClientHandshake(
        session: FileTransferSession,
        senderId: String,
        recipientId: String,
        sessionPassword: String,
        fileName: String,
        fileSize: Long,
        transferId: String,
    ): FileTransferMetadata {
        session.writeUtf(PROTOCOL)
        val serverPublicKey = cryptoServices.keyEncodingService().decodePublicKey(Base64.getDecoder().decode(session.readUtf()))
        val sessionKey = cryptoServices.keyGenerationService().generateAesKey()
        val metadata = FileTransferMetadata(transferId, senderId, recipientId, fileName, fileSize)
        val payload = sessionPassword + "\n" +
            Base64.getEncoder().encodeToString(cryptoServices.keyEncodingService().encodeSecretKey(sessionKey))
        val encrypted = cryptoServices.rsaCryptoService().encrypt(payload.toByteArray(StandardCharsets.UTF_8), serverPublicKey)
        session.writeBytes(encrypted)
        session.enableTransportEncryption(sessionKey, cryptoServices.aesGcmCryptoService())
        session.writeEncryptedText(metadata.compactSerialize())
        val response = session.readEncryptedText()
        if (ACCEPTED != response) {
            throw IOException(response)
        }
        return metadata
    }

    @Throws(IOException::class)
    fun acceptTransfer(session: FileTransferSession) {
        session.writeEncryptedText(ACCEPTED)
    }

    @Throws(IOException::class)
    fun rejectTransfer(session: FileTransferSession, reason: String?) {
        session.writeEncryptedText(REJECTED_PREFIX + if (reason.isNullOrBlank()) "Transfer rejected" else reason)
    }

    @Throws(IOException::class)
    fun performServerHandshake(session: FileTransferSession, expectedPassword: String): FileTransferMetadata {
        val protocol = session.readUtf()
        if (PROTOCOL != protocol) {
            throw IOException("Unsupported file transfer protocol")
        }
        val rsaKeyPair = cryptoServices.keyGenerationService().generateRsaKeyPair()
        session.writeUtf(Base64.getEncoder().encodeToString(cryptoServices.keyEncodingService().encodePublicKey(rsaKeyPair.public)))
        val encryptedPayload = session.readBytes()
        val payload = String(cryptoServices.rsaCryptoService().decrypt(encryptedPayload, rsaKeyPair.private), StandardCharsets.UTF_8)
        val parts = payload.split("\n", limit = 7).toTypedArray()
        if (parts.size != 2 && parts.size != 7) {
            throw IOException("Malformed secure file transfer payload")
        }
        val password = parts[0]
        if (expectedPassword != password) {
            throw IOException("Wrong transfer password")
        }
        val sessionKey = cryptoServices.keyEncodingService().decodeAesKey(Base64.getDecoder().decode(parts[1]))
        session.enableTransportEncryption(sessionKey, cryptoServices.aesGcmCryptoService())
        return if (parts.size == 2) decodeCompactMetadata(session.readEncryptedText()) else decodeMetadata(parts)
    }

    @Throws(IOException::class)
    private fun decodeMetadata(parts: Array<String>): FileTransferMetadata =
        if (parts[2].contains("|")) {
            decodeCompactMetadata(parts[2])
        } else {
            FileTransferMetadata(parts[2], parts[3], parts[4], parts[5], parts[6].toLong())
        }

    @Throws(IOException::class)
    private fun decodeCompactMetadata(value: String): FileTransferMetadata = try {
        FileTransferMetadata.deserializeCompact(value)
    } catch (exception: IllegalArgumentException) {
        throw IOException("Malformed compact metadata payload", exception)
    }

    companion object {
        private const val PROTOCOL = "SECURE_FILE_TRANSFER_V1"
        private const val ACCEPTED = "ACCEPTED"
        private const val REJECTED_PREFIX = "REJECTED:"
    }
}
