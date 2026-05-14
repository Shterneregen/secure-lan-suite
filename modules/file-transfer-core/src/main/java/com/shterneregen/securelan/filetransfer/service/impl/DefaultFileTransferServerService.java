package com.shterneregen.securelan.filetransfer.service.impl;

import com.shterneregen.securelan.common.model.FileTransferProgress;
import com.shterneregen.securelan.common.model.TransferStatus;
import com.shterneregen.securelan.common.net.transport.TcpServer;
import com.shterneregen.securelan.crypto.CryptoServices;
import com.shterneregen.securelan.filetransfer.event.FileTransferCompletedEvent;
import com.shterneregen.securelan.filetransfer.event.FileTransferFailedEvent;
import com.shterneregen.securelan.filetransfer.event.FileTransferProgressEvent;
import com.shterneregen.securelan.filetransfer.event.FileTransferStartedEvent;
import com.shterneregen.securelan.filetransfer.protocol.FileTransferMetadata;
import com.shterneregen.securelan.filetransfer.protocol.FileTransferSession;
import com.shterneregen.securelan.filetransfer.service.FileTransferEventPublisher;
import com.shterneregen.securelan.filetransfer.service.FileTransferServerConfig;
import com.shterneregen.securelan.filetransfer.service.FileTransferServerService;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultFileTransferServerService implements FileTransferServerService {
    private final FileTransferEventPublisher eventPublisher;
    private final SecureFileTransferHandshake handshake;
    private final Object lifecycleLock = new Object();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final TcpServer tcpServer;

    private volatile FileTransferServerConfig config;

    public DefaultFileTransferServerService(FileTransferEventPublisher eventPublisher) {
        this(eventPublisher, CryptoServices.createDefault());
    }

    public DefaultFileTransferServerService(FileTransferEventPublisher eventPublisher, CryptoServices cryptoServices) {
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
        this.handshake = new SecureFileTransferHandshake(Objects.requireNonNull(cryptoServices, "cryptoServices"));
        this.tcpServer = new TcpServer("file-transfer-server");
    }

    @Override
    public void start(FileTransferServerConfig config) {
        Objects.requireNonNull(config, "config");
        synchronized (lifecycleLock) {
            if (running.get()) {
                return;
            }
            try {
                Files.createDirectories(config.storageDirectory());
                this.config = config;
                running.set(true);
                FileTransferServerConfig activeConfig = config;
                tcpServer.start(config.port(), socket -> handleClient(socket, activeConfig), this::publishAcceptError);
            } catch (IOException | RuntimeException e) {
                cleanupFailedStart();
                throw new IllegalStateException("Unable to start file transfer server", e);
            }
        }
    }

    private void publishAcceptError(String message, Throwable cause) {
        if (running.get()) {
            eventPublisher.publish(new FileTransferFailedEvent("server", "", message, cause, false));
        }
    }

    private void handleClient(Socket socket, FileTransferServerConfig activeConfig) {
        try (socket; FileTransferSession session = new FileTransferSession(socket)) {
            FileTransferMetadata metadata = handshake.performServerHandshake(session, activeConfig.sessionPassword());
            if (!activeConfig.acceptanceHandler().accept(metadata, session.remoteAddress())) {
                handshake.rejectTransfer(session, "File transfer rejected by receiver");
                eventPublisher.publish(new FileTransferFailedEvent(metadata.transferId(), metadata.fileName(), "Rejected incoming file from " + metadata.senderId(), null, false));
                return;
            }
            handshake.acceptTransfer(session);
            eventPublisher.publish(new FileTransferStartedEvent(metadata.transferId(), metadata.fileName(), metadata.fileSize(), false));

            Path target = createUniqueTargetPath(metadata.fileName(), activeConfig);
            long transferred = 0;
            try (OutputStream outputStream = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                while (true) {
                    byte[] chunk = session.readEncryptedBytes();
                    if (chunk.length == 0) {
                        break;
                    }
                    outputStream.write(chunk);
                    transferred += chunk.length;
                    eventPublisher.publish(new FileTransferProgressEvent(
                            metadata.transferId(),
                            new FileTransferProgress(metadata.transferId(), transferred, metadata.fileSize(), TransferStatus.IN_PROGRESS),
                            false
                    ));
                }
            }
            session.writeEncryptedText("DONE");
            eventPublisher.publish(new FileTransferCompletedEvent(metadata.transferId(), metadata.fileName(), target, metadata.fileSize(), false));
        } catch (Exception e) {
            eventPublisher.publish(new FileTransferFailedEvent("incoming", "", e.getMessage(), e, false));
        }
    }

    private Path createUniqueTargetPath(String fileName, FileTransferServerConfig activeConfig) throws IOException {
        Path storageDirectory = activeConfig.storageDirectory().normalize();
        Path candidate = storageDirectory.resolve(fileName).normalize();
        if (!candidate.startsWith(storageDirectory)) {
            throw new IOException("Invalid target file path");
        }
        if (!Files.exists(candidate)) {
            return candidate;
        }
        String baseName = fileName;
        String extension = "";
        int dot = fileName.lastIndexOf('.');
        if (dot > 0) {
            baseName = fileName.substring(0, dot);
            extension = fileName.substring(dot);
        }
        int index = 1;
        while (true) {
            Path alternative = storageDirectory.resolve(baseName + "-" + index + extension).normalize();
            if (!Files.exists(alternative)) {
                return alternative;
            }
            index++;
        }
    }

    @Override
    public void stop() {
        synchronized (lifecycleLock) {
            running.set(false);
            tcpServer.close();
            config = null;
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    private void cleanupFailedStart() {
        running.set(false);
        tcpServer.close();
        config = null;
    }
}
