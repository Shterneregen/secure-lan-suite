package com.shterneregen.securelan.filetransfer.service.impl

import com.shterneregen.securelan.crypto.CryptoServices
import com.shterneregen.securelan.filetransfer.protocol.FileTransferMetadata
import com.shterneregen.securelan.filetransfer.protocol.FileTransferSession
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * Handles the multi-step handshake protocol for authenticated and encrypted file transfers.
 *
 * Handshake sequence:
 * 1. Protocol banner verification (`SECURE_FILE_TRANSFER_V1`).
 * 2. Receiver provides an ephemeral RSA public key.
 * 3. Sender generates a symmetric AES-256 session key, encrypts it and the session password
 *    using receiver's RSA public key, and sends it.
 * 4. Both peers activate AES-GCM transport encryption on the [FileTransferSession].
 * 5. Sender transmits compact [FileTransferMetadata] payload.
 * 6. Receiver verifies password and consults acceptance policy before responding with `ACCEPTED` or `REJECTED:<reason>`.
 *
 * @param cryptoServices Cryptographic services provider.
 */
internal class SecureFileTransferHandshake(private val cryptoServices: CryptoServices) {
    /**
     * Executes the sender side of the handshake.
     *
     * @param session Connected transport session.
     * @param senderId Local sender identifier.
     * @param recipientId Target recipient identifier.
     * @param sessionPassword Shared secret / room password.
     * @param fileName Name of the file being sent.
     * @param fileSize Size of the file in bytes.
     * @param transferId Unique transfer session identifier.
     * @param awaitingAcceptance Callback invoked when metadata is sent and the client waits for receiver approval.
     * @return Negotiated [FileTransferMetadata].
     * @throws IOException If handshake fails, connection drops, or receiver rejects the transfer.
     */
    @Throws(IOException::class)
    fun performClientHandshake(
        session: FileTransferSession,
        senderId: String,
        recipientId: String,
        sessionPassword: String,
        fileName: String,
        fileSize: Long,
        transferId: String,
        awaitingAcceptance: () -> Unit = {},
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
        awaitingAcceptance()
        val response = session.readEncryptedText()
        if (ACCEPTED != response) {
            if (response.startsWith(REJECTED_PREFIX)) {
                val reason = response.removePrefix(REJECTED_PREFIX).trim()
                throw TransferRejectedException(if (reason.isNotBlank()) "File transfer rejected by receiver: $reason" else "File transfer rejected by receiver")
            }
            throw IOException("Unexpected transfer handshake response")
        }
        return metadata
    }

    /**
     * Sends an acceptance confirmation to the sender over the encrypted session.
     *
     * @param session The active encrypted transfer session.
     * @throws IOException If writing to the session fails.
     */
    @Throws(IOException::class)
    fun acceptTransfer(session: FileTransferSession) {
        session.writeEncryptedText(ACCEPTED)
    }

    /**
     * Sends a rejection notice to the sender over the encrypted session.
     *
     * @param session The active encrypted transfer session.
     * @param reason Optional human-readable rejection explanation.
     * @throws IOException If writing to the session fails.
     */
    @Throws(IOException::class)
    fun rejectTransfer(session: FileTransferSession, reason: String?) {
        session.writeEncryptedText(REJECTED_PREFIX + if (reason.isNullOrBlank()) "Transfer rejected" else reason)
    }

    /**
     * Executes the receiver side of the handshake, verifying protocol version and password.
     *
     * @param session Connected transport session.
     * @param expectedPassword Shared password expected from the client.
     * @return Decoded [FileTransferMetadata] from the sender.
     * @throws IOException If protocol is unsupported, password is wrong, or payload is malformed.
     */
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
        val parts = payload.split("\n", limit = 7)
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
    private fun decodeMetadata(parts: List<String>): FileTransferMetadata =
        if (parts[2].contains("|")) {
            decodeCompactMetadata(parts[2])
        } else {
            val size = parts[6].toLongOrNull() ?: throw IOException("Invalid file size in transfer payload")
            FileTransferMetadata(parts[2], parts[3], parts[4], parts[5], size)
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

/**
 * Exception thrown when the remote receiver explicitly rejects an incoming file transfer.
 *
 * @param message Rejection explanation message.
 */
internal class TransferRejectedException(
    message: String = "File transfer rejected by receiver",
) : IOException(message)
