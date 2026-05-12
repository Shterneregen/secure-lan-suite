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
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultFileTransferServerService implements FileTransferServerService {
    private final FileTransferEventPublisher eventPublisher;
    private final SecureFileTransferHandshake handshake;
    private final Object lifecycleLock = new Object();
    private final AtomicBoolean running = new AtomicBoolean(false);

    private volatile ServerSocket serverSocket;
    private volatile FileTransferServerConfig config;
    private volatile ExecutorService executor;

    public DefaultFileTransferServerService(FileTransferEventPublisher eventPublisher) {
        this(eventPublisher, CryptoServices.createDefault());
    }

    public DefaultFileTransferServerService(FileTransferEventPublisher eventPublisher, CryptoServices cryptoServices) {
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
        this.handshake = new SecureFileTransferHandshake(cryptoServices);
    }

    @Override
    public void start(FileTransferServerConfig config) {
        Objects.requireNonNull(config, "config");
        synchronized (lifecycleLock) {
            if (running.get()) {
                return;
            }
            ServerSocket createdSocket = null;
            ExecutorService createdExecutor = null;
            try {
                Files.createDirectories(config.storageDirectory());
                createdSocket = new ServerSocket(config.port());
                createdExecutor = Executors.newCachedThreadPool();
                this.serverSocket = createdSocket;
                this.config = config;
                this.executor = createdExecutor;
                running.set(true);
                ServerSocket activeSocket = createdSocket;
                ExecutorService activeExecutor = createdExecutor;
                FileTransferServerConfig activeConfig = config;
                activeExecutor.submit(() -> acceptLoop(activeSocket, activeExecutor, activeConfig));
            } catch (IOException | RuntimeException e) {
                cleanupFailedStart(createdSocket, createdExecutor);
                throw new IllegalStateException("Unable to start file transfer server", e);
            }
        }
    }

    private void acceptLoop(ServerSocket activeSocket, ExecutorService activeExecutor, FileTransferServerConfig activeConfig) {
        while (isActive(activeSocket, activeExecutor)) {
            try {
                Socket socket = activeSocket.accept();
                try {
                    activeExecutor.submit(() -> handleClient(socket, activeConfig));
                } catch (RejectedExecutionException e) {
                    closeQuietly(socket);
                    if (isActive(activeSocket, activeExecutor)) {
                        eventPublisher.publish(new FileTransferFailedEvent("server", "", "Unable to handle file transfer client", e, false));
                    }
                }
            } catch (IOException e) {
                if (isActive(activeSocket, activeExecutor)) {
                    eventPublisher.publish(new FileTransferFailedEvent("server", "", "Unable to accept file transfer client", e, false));
                }
            }
        }
    }

    private boolean isActive(ServerSocket activeSocket, ExecutorService activeExecutor) {
        return running.get()
                && serverSocket == activeSocket
                && executor == activeExecutor
                && !activeSocket.isClosed()
                && !activeExecutor.isShutdown();
    }

    private void handleClient(Socket socket, FileTransferServerConfig activeConfig) {
        try (socket; FileTransferSession session = new FileTransferSession(socket)) {
            FileTransferMetadata metadata = handshake.performServerHandshake(session, activeConfig.sessionPassword());
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
            closeQuietly(serverSocket);
            if (executor != null) {
                executor.shutdownNow();
            }
            serverSocket = null;
            config = null;
            executor = null;
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    private void cleanupFailedStart(ServerSocket socket, ExecutorService executor) {
        running.set(false);
        closeQuietly(socket);
        if (executor != null) {
            executor.shutdownNow();
        }
        serverSocket = null;
        config = null;
        this.executor = null;
    }

    private static void closeQuietly(ServerSocket socket) {
        if (socket == null) {
            return;
        }
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    private static void closeQuietly(Socket socket) {
        if (socket == null) {
            return;
        }
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
