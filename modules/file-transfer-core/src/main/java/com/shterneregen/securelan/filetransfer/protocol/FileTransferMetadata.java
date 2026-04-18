package com.shterneregen.securelan.filetransfer.protocol;

import java.util.Objects;

public record FileTransferMetadata(
        String transferId,
        String senderId,
        String recipientId,
        String fileName,
        long fileSize
) {
    private static final String SEPARATOR = "\n";

    public FileTransferMetadata {
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

    public String serialize() {
        return String.join(SEPARATOR,
                transferId,
                senderId,
                recipientId,
                fileName,
                Long.toString(fileSize)
        );
    }

    public static FileTransferMetadata deserialize(String value) {
        String[] parts = value.split(SEPARATOR, 5);
        if (parts.length != 5) {
            throw new IllegalArgumentException("Malformed metadata payload");
        }
        return new FileTransferMetadata(parts[0], parts[1], parts[2], parts[3], Long.parseLong(parts[4]));
    }
}
