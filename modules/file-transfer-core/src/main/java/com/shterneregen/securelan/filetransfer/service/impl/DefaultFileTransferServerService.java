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
import com.shterneregen.securelan.filetransfer.service.FileTransferEventPublisher;
import com.shterneregen.securelan.filetransfer.service.FileTransferServerConfig;
import com.shterneregen.securelan.filetransfer.service.FileTransferServerService;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultFileTransferServerService implements FileTransferServerService {
    private final FileTransferEventPublisher eventPublisher;
    private final SecureFileTransferHandshake handshake;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ExecutorService executor = Executors.newCachedThreadPool();

    private volatile ServerSocket serverSocket;
    private volatile FileTransferServerConfig config;

    public DefaultFileTransferServerService(FileTransferEventPublisher eventPublisher) {
        this(eventPublisher, CryptoServices.createDefault());
    }

    public DefaultFileTransferServerService(FileTransferEventPublisher eventPublisher, CryptoServices cryptoServices) {
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
        this.handshake = new SecureFileTransferHandshake(cryptoServices);
    }

    @Override
    public void start(FileTransferServerConfig config) {
        if (running.get()) {
            return;
        }
        try {
            Files.createDirectories(config.storageDirectory());
            this.serverSocket = new ServerSocket(config.port());
            this.config = config;
            running.set(true);
            executor.submit(this::acceptLoop);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to start file transfer server", e);
        }
    }

    private void acceptLoop() {
        while (running.get()) {
            try {
                Socket socket = serverSocket.accept();
                executor.submit(() -> handleClient(socket));
            } catch (IOException e) {
                if (running.get()) {
                    eventPublisher.publish(new FileTransferFailedEvent("server", "", "Unable to accept file transfer client", e, false));
                }
            }
        }
    }

    private void handleClient(Socket socket) {
        try (socket; FileTransferSession session = new FileTransferSession(socket)) {
            FileTransferMetadata metadata = handshake.performServerHandshake(session, config.sessionPassword());
            eventPublisher.publish(new FileTransferStartedEvent(metadata.transferId(), metadata.fileName(), metadata.fileSize(), false));

            Path target = createUniqueTargetPath(metadata.fileName());
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

    private Path createUniqueTargetPath(String fileName) throws IOException {
        Path candidate = config.storageDirectory().resolve(fileName).normalize();
        if (!candidate.startsWith(config.storageDirectory().normalize())) {
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
            Path alternative = config.storageDirectory().resolve(baseName + "-" + index + extension).normalize();
            if (!Files.exists(alternative)) {
                return alternative;
            }
            index++;
        }
    }

    @Override
    public void stop() {
        running.set(false);
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
        }
        executor.shutdownNow();
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }
}
