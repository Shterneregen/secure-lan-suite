package com.shterneregen.securelan.filetransfer.service.impl

import com.shterneregen.securelan.common.model.FileTransferProgress
import com.shterneregen.securelan.common.model.TransferStatus
import com.shterneregen.securelan.common.net.transport.ClientSocketFactory
import com.shterneregen.securelan.common.net.transport.TransportEndpoint
import com.shterneregen.securelan.crypto.CryptoServices
import com.shterneregen.securelan.filetransfer.event.FileTransferCompletedEvent
import com.shterneregen.securelan.filetransfer.event.FileTransferFailedEvent
import com.shterneregen.securelan.filetransfer.event.FileTransferProgressEvent
import com.shterneregen.securelan.filetransfer.event.FileTransferStartedEvent
import com.shterneregen.securelan.filetransfer.protocol.FileTransferSession
import com.shterneregen.securelan.filetransfer.service.FileTransferClientRequest
import com.shterneregen.securelan.filetransfer.service.FileTransferClientService
import com.shterneregen.securelan.filetransfer.service.FileTransferEventPublisher
import java.io.IOException
import java.nio.file.Files
import java.util.Arrays
import java.util.Objects
import java.util.UUID

class DefaultFileTransferClientService @JvmOverloads constructor(
    eventPublisher: FileTransferEventPublisher,
    cryptoServices: CryptoServices = CryptoServices.createDefault(),
    clientSocketFactory: ClientSocketFactory = ClientSocketFactory.systemDefault(),
) : FileTransferClientService {
    private val eventPublisher: FileTransferEventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher")
    private val handshake = SecureFileTransferHandshake(Objects.requireNonNull(cryptoServices, "cryptoServices"))
    private val clientSocketFactory: ClientSocketFactory = Objects.requireNonNull(clientSocketFactory, "clientSocketFactory")

    override fun sendFile(request: FileTransferClientRequest): String {
        val file = request.file
        val transferId = UUID.randomUUID().toString()
        val fileName = file.fileName.toString()
        try {
            val fileSize = Files.size(file)
            eventPublisher.publish(FileTransferStartedEvent(transferId, fileName, fileSize, true))
            clientSocketFactory.connect(TransportEndpoint.of(request.host, request.port)).use { socket ->
                FileTransferSession(socket).use { session ->
                    Files.newInputStream(file).use { inputStream ->
                        val metadata = handshake.performClientHandshake(
                            session,
                            request.senderId,
                            request.recipientId,
                            request.sessionPassword,
                            fileName,
                            fileSize,
                            transferId,
                        )
                        val buffer = ByteArray(CHUNK_SIZE)
                        var transferred = 0L
                        while (true) {
                            val read = inputStream.read(buffer)
                            if (read == -1) {
                                break
                            }
                            val chunk = if (read == buffer.size) buffer.clone() else Arrays.copyOf(buffer, read)
                            session.writeEncryptedBytes(chunk)
                            transferred += read.toLong()
                            eventPublisher.publish(
                                FileTransferProgressEvent(
                                    metadata.transferId,
                                    FileTransferProgress(metadata.transferId, transferred, metadata.fileSize, TransferStatus.IN_PROGRESS),
                                    true,
                                ),
                            )
                        }
                        session.writeEncryptedBytes(ByteArray(0))
                        val result = session.readEncryptedText()
                        if ("DONE" != result) {
                            throw IOException(result)
                        }
                        eventPublisher.publish(FileTransferCompletedEvent(metadata.transferId, metadata.fileName, file, metadata.fileSize, true))
                        return metadata.transferId
                    }
                }
            }
        } catch (ex: Exception) {
            eventPublisher.publish(FileTransferFailedEvent(transferId, fileName, ex.message, ex, true))
            throw IllegalStateException("Unable to send file", ex)
        }
    }

    companion object {
        private const val CHUNK_SIZE = 16 * 1024
    }
}
