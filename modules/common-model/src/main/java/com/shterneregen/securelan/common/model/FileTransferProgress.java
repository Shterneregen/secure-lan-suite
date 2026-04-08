package com.shterneregen.securelan.common.model;

import java.util.Objects;

public record FileTransferProgress(
        String transferId,
        long transferredBytes,
        long totalBytes,
        TransferStatus status
) {
    public FileTransferProgress {
        Objects.requireNonNull(transferId, "transferId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        if (transferredBytes < 0) {
            throw new IllegalArgumentException("transferredBytes must not be negative");
        }
        if (totalBytes < 0) {
            throw new IllegalArgumentException("totalBytes must not be negative");
        }
        if (totalBytes > 0 && transferredBytes > totalBytes) {
            throw new IllegalArgumentException("transferredBytes must not exceed totalBytes");
        }
    }

    public int percent() {
        if (totalBytes <= 0) {
            return 0;
        }
        return (int) ((transferredBytes * 100) / totalBytes);
    }
}
