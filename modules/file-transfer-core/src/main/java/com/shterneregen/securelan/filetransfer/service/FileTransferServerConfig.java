package com.shterneregen.securelan.filetransfer.service;

import java.nio.file.Path;
import java.util.Objects;

public record FileTransferServerConfig(
        int port,
        Path storageDirectory,
        String sessionPassword,
        FileTransferAcceptanceHandler acceptanceHandler
) {
    public FileTransferServerConfig(int port, Path storageDirectory, String sessionPassword) {
        this(port, storageDirectory, sessionPassword, FileTransferAcceptanceHandler.acceptAll());
    }

    public FileTransferServerConfig {
        Objects.requireNonNull(storageDirectory, "storageDirectory must not be null");
        Objects.requireNonNull(sessionPassword, "sessionPassword must not be null");
        acceptanceHandler = acceptanceHandler == null ? FileTransferAcceptanceHandler.acceptAll() : acceptanceHandler;
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
        if (sessionPassword.isBlank()) {
            throw new IllegalArgumentException("sessionPassword must not be blank");
        }
    }
}
