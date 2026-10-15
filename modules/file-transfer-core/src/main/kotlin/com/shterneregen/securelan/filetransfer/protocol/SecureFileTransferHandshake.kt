package com.shterneregen.securelan.filetransfer.protocol

import com.shterneregen.securelan.crypto.protocol.PasswordAuthenticatedHandshake
import com.shterneregen.securelan.filetransfer.protocol.FileTransferMetadata
import com.shterneregen.securelan.filetransfer.protocol.FileTransferPeerAuthentication
import com.shterneregen.securelan.filetransfer.protocol.FileTransferSession
import java.io.IOException

/**
 * V2 mutually authenticated PAKE followed by both peers' room-certified signatures over metadata
 * and the channel binding. A password holder cannot impersonate another room participant.
 * Acceptance is outside the short cryptographic handshake deadline.
 */
class SecureFileTransferHandshake {
    fun performClientHandshake(
        session: FileTransferSession, senderId: String, recipientId: String, sessionPassword: String,
        fileName: String, fileSize: Long, transferId: String,
        authentication: FileTransferPeerAuthentication,
        awaitingAcceptance: () -> Unit = {},
    ): FileTransferMetadata {
        val auth = authentication
        if (senderId != auth.localId) throw IOException("Invalid local file sender")
        val metadata = FileTransferMetadata(transferId, senderId, recipientId, fileName, fileSize)
        session.handshake {
            session.writeUtf(PROTOCOL)
            val keys = PasswordAuthenticatedHandshake.client(sessionPassword, PROTOCOL, session::writeUtf, session::readUtf)
            session.enableTransportEncryption(keys, true)
            session.writeEncryptedText(metadata.compactSerialize())
            session.writeEncryptedText(auth.identity.sign(proofPayload(session, metadata, "sender")))
            auth.verify(recipientId, proofPayload(session, metadata, "receiver"), session.readEncryptedText())
            session.writeEncryptedText("PEER_VERIFIED")
        }
        awaitingAcceptance()
        val response = session.readEncryptedText(300_000)
        if (response != ACCEPTED) {
            if (response.startsWith(REJECTED_PREFIX)) throw TransferRejectedException("File transfer rejected by receiver")
            throw IOException("Unexpected transfer handshake response")
        }
        return metadata
    }

    fun performServerHandshake(
        session: FileTransferSession, expectedPassword: String, authentication: FileTransferPeerAuthentication,
    ): FileTransferMetadata = session.handshake {
        val auth = authentication
        if (session.readUtf() != PROTOCOL) throw IOException("Unsupported file transfer protocol; update both clients")
        val keys = PasswordAuthenticatedHandshake.server(expectedPassword, PROTOCOL, session::writeUtf, session::readUtf)
        session.enableTransportEncryption(keys, false)
        val metadata = try { FileTransferMetadata.deserializeCompact(session.readEncryptedText()) } catch (error: Exception) {
            throw IOException("Invalid transfer metadata", error)
        }
        if (metadata.recipientId != auth.localId) throw IOException("Invalid file recipient")
        auth.verify(metadata.senderId, proofPayload(session, metadata, "sender"), session.readEncryptedText())
        session.writeEncryptedText(auth.identity.sign(proofPayload(session, metadata, "receiver")))
        if (session.readEncryptedText() != "PEER_VERIFIED") throw IOException("Peer confirmation failed")
        metadata
    }

    fun acceptTransfer(session: FileTransferSession) = session.writeEncryptedText(ACCEPTED)
    fun rejectTransfer(session: FileTransferSession, reason: String?) = session.writeEncryptedText(REJECTED_PREFIX + (reason ?: "Transfer rejected"))

    private fun proofPayload(session: FileTransferSession, metadata: FileTransferMetadata, role: String): ByteArray =
        session.binding() + "$PROTOCOL:$role\u0000".toByteArray(Charsets.UTF_8) + metadata.compactSerialize().toByteArray(Charsets.UTF_8)

    companion object {
        const val PROTOCOL = "SECURE_FILE_TRANSFER_V2"
        private const val ACCEPTED = "ACCEPTED"
        private const val REJECTED_PREFIX = "REJECTED:"
    }
}

class TransferRejectedException(message: String = "File transfer rejected by receiver") : IOException(message)
