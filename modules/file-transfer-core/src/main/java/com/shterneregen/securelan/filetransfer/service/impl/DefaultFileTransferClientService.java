package com.shterneregen.securelan.filetransfer.service.impl;

import com.shterneregen.securelan.common.model.FileTransferProgress;
import com.shterneregen.securelan.common.model.TransferStatus;
import com.shterneregen.securelan.crypto.CryptoServices;
import com.shterneregen.securelan.filetransfer.event.FileTransferCompletedEvent;
import com.shterneregen.securelan.filetransfer.event.FileTransferFailedEvent;
import com.shterneregen.securelan.filetransfer.event.FileTransferProgressEvent;
import com.shterneregen.securelan.filetransfer.event.FileTransferStartedEvent;
import com.shterneregen.securelan.filetransfer.protocol.FileTransferMetadata;
import com.shterneregen.securelan.filetransfer.protocol.FileTransferSession;
import com.shterneregen.securelan.filetransfer.service.FileTransferClientRequest;
import com.shterneregen.securelan.filetransfer.service.FileTransferClientService;
import com.shterneregen.securelan.filetransfer.service.FileTransferEventPublisher;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class DefaultFileTransferClientService implements FileTransferClientService {
    private static final int CHUNK_SIZE = 16 * 1024;

    private final FileTransferEventPublisher eventPublisher;
    private final SecureFileTransferHandshake handshake;

    public DefaultFileTransferClientService(FileTransferEventPublisher eventPublisher) {
        this(eventPublisher, CryptoServices.createDefault());
    }

    public DefaultFileTransferClientService(FileTransferEventPublisher eventPublisher, CryptoServices cryptoServices) {
        this.eventPublisher = eventPublisher;
        this.handshake = new SecureFileTransferHandshake(cryptoServices);
    }

    @Override
    public String sendFile(FileTransferClientRequest request) {
        Path file = request.file();
        String transferId = UUID.randomUUID().toString();
        String fileName = file.getFileName().toString();
        try {
            long fileSize = Files.size(file);
            eventPublisher.publish(new FileTransferStartedEvent(transferId, fileName, fileSize, true));
            try (Socket socket = new Socket(request.host(), request.port());
                 FileTransferSession session = new FileTransferSession(socket);
                 InputStream inputStream = Files.newInputStream(file)) {
                FileTransferMetadata metadata = handshake.performClientHandshake(
                        session,
                        request.senderId(),
                        request.recipientId(),
                        request.sessionPassword(),
                        fileName,
                        fileSize,
                        transferId
                );
                byte[] buffer = new byte[CHUNK_SIZE];
                long transferred = 0;
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    byte[] chunk = read == buffer.length ? buffer.clone() : java.util.Arrays.copyOf(buffer, read);
                    session.writeEncryptedBytes(chunk);
                    transferred += read;
                    eventPublisher.publish(new FileTransferProgressEvent(
                            metadata.transferId(),
                            new FileTransferProgress(metadata.transferId(), transferred, metadata.fileSize(), TransferStatus.IN_PROGRESS),
                            true
                    ));
                }
                session.writeEncryptedBytes(new byte[0]);
                String result = session.readEncryptedText();
                if (!"DONE".equals(result)) {
                    throw new IOException(result);
                }
                eventPublisher.publish(new FileTransferCompletedEvent(metadata.transferId(), metadata.fileName(), file, metadata.fileSize(), true));
                return metadata.transferId();
            }
        } catch (Exception ex) {
            eventPublisher.publish(new FileTransferFailedEvent(transferId, fileName, ex.getMessage(), ex, true));
            throw new IllegalStateException("Unable to send file", ex);
        }
    }
}
