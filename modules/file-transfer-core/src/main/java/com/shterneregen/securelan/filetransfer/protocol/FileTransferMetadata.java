package com.shterneregen.securelan.filetransfer.protocol;

import java.util.Objects;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

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

    public String compactSerialize() {
        return String.join("|",
                encodeText(transferId),
                encodeText(senderId),
                encodeText(recipientId),
                encodeText(fileName),
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

    public static FileTransferMetadata deserializeCompact(String value) {
        String[] parts = value.split("\\|", 5);
        if (parts.length != 5) {
            throw new IllegalArgumentException("Malformed compact metadata payload");
        }
        return new FileTransferMetadata(
                decodeText(parts[0]),
                decodeText(parts[1]),
                decodeText(parts[2]),
                decodeText(parts[3]),
                Long.parseLong(parts[4])
        );
    }

    private static String encodeText(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeText(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }
}
