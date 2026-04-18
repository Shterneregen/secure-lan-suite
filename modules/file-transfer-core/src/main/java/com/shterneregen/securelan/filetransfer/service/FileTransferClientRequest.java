package com.shterneregen.securelan.filetransfer.service;

import java.nio.file.Path;
import java.util.Objects;

public record FileTransferClientRequest(
        String host,
        int port,
        String senderId,
        String recipientId,
        String sessionPassword,
        Path file
) {
    public FileTransferClientRequest {
        Objects.requireNonNull(host, "host must not be null");
        Objects.requireNonNull(senderId, "senderId must not be null");
        Objects.requireNonNull(recipientId, "recipientId must not be null");
        Objects.requireNonNull(sessionPassword, "sessionPassword must not be null");
        Objects.requireNonNull(file, "file must not be null");
        if (host.isBlank()) {
            throw new IllegalArgumentException("host must not be blank");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
        if (senderId.isBlank()) {
            throw new IllegalArgumentException("senderId must not be blank");
        }
        if (recipientId.isBlank()) {
            throw new IllegalArgumentException("recipientId must not be blank");
        }
    }
}
