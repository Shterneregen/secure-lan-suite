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
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultFileTransferServerService implements FileTransferServerService {
    private static final int MAX_SAFE_FILE_NAME_LENGTH = 160;
    private static final String INVALID_FILE_NAME_CHARS = "<>:\"/\\|?*";
    private static final Set<String> WINDOWS_RESERVED_FILE_NAMES = Set.of(
            "CON", "PRN", "AUX", "NUL",
            "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
            "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
    );

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
            eventPublisher.publish(new FileTransferFailedEvent("incoming", "", failureMessage(e, "Incoming file transfer failed"), e, false));
        }
    }

    private Path createUniqueTargetPath(String fileName, FileTransferServerConfig activeConfig) throws IOException {
        Path storageDirectory = activeConfig.storageDirectory().normalize();
        String safeFileName = sanitizeFileName(fileName);
        Path candidate = storageDirectory.resolve(safeFileName).normalize();
        if (!candidate.startsWith(storageDirectory)) {
            throw new IOException("Invalid target file path");
        }
        if (!Files.exists(candidate)) {
            return candidate;
        }
        String baseName = safeFileName;
        String extension = "";
        int dot = safeFileName.lastIndexOf('.');
        if (dot > 0) {
            baseName = safeFileName.substring(0, dot);
            extension = safeFileName.substring(dot);
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

    private String sanitizeFileName(String fileName) {
        String baseName = Objects.requireNonNullElse(fileName, "incoming-file")
                .replace('\\', '/');
        int slash = baseName.lastIndexOf('/');
        if (slash >= 0) {
            baseName = baseName.substring(slash + 1);
        }
        StringBuilder sanitized = new StringBuilder(baseName.length());
        for (int index = 0; index < baseName.length(); index++) {
            char character = baseName.charAt(index);
            sanitized.append(character < 32 || INVALID_FILE_NAME_CHARS.indexOf(character) >= 0 ? '_' : character);
        }
        String safeName = trimUnsafeFileNameEdges(sanitized.toString().trim());
        if (safeName.isBlank()) {
            safeName = "incoming-file";
        }
        safeName = avoidReservedWindowsFileName(safeName);
        if (safeName.length() <= MAX_SAFE_FILE_NAME_LENGTH) {
            return safeName;
        }
        int dot = safeName.lastIndexOf('.');
        String extension = dot > 0 && safeName.length() - dot <= 16 ? safeName.substring(dot) : "";
        int prefixLength = Math.max(1, MAX_SAFE_FILE_NAME_LENGTH - extension.length());
        return safeName.substring(0, prefixLength) + extension;
    }

    private String trimUnsafeFileNameEdges(String fileName) {
        int end = fileName.length();
        while (end > 0 && (fileName.charAt(end - 1) == '.' || fileName.charAt(end - 1) == ' ')) {
            end--;
        }
        return fileName.substring(0, end);
    }

    private String avoidReservedWindowsFileName(String fileName) {
        int dot = fileName.indexOf('.');
        String stem = dot > 0 ? fileName.substring(0, dot) : fileName;
        if (WINDOWS_RESERVED_FILE_NAMES.contains(stem.toUpperCase())) {
            return "_" + fileName;
        }
        return fileName;
    }

    private String failureMessage(Exception exception, String fallback) {
        if (exception.getMessage() != null && !exception.getMessage().isBlank()) {
            return exception.getMessage();
        }
        return fallback + ": " + exception.getClass().getSimpleName();
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
