package com.shterneregen.securelan.common.model;

import java.util.Objects;

public record FileTransferRequest(
        String transferId,
        String senderId,
        String recipientId,
        String fileName,
        long fileSize
) {
    public FileTransferRequest {
        Objects.requireNonNull(transferId, "transferId must not be null");
        Objects.requireNonNull(senderId, "senderId must not be null");
        Objects.requireNonNull(recipientId, "recipientId must not be null");
        Objects.requireNonNull(fileName, "fileName must not be null");
        if (fileName.isBlank()) {
            throw new IllegalArgumentException("fileName must not be blank");
        }
        if (fileSize < 0) {
            throw new IllegalArgumentException("fileSize must not be negative");
        }
    }
}
