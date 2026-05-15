package com.shterneregen.securelan.filetransfer.quickshare;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record QuickShareSnapshot(
        String id,
        QuickShareType type,
        String displayName,
        String fileName,
        long fileSize,
        Instant createdAt,
        Instant expiresAt,
        int accessLimit,
        int accessCount,
        QuickShareStatus status,
        List<String> urls
) {
    public QuickShareSnapshot {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(displayName, "displayName must not be null");
        Objects.requireNonNull(fileName, "fileName must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(urls, "urls must not be null");
        if (accessLimit < 1) {
            throw new IllegalArgumentException("accessLimit must be at least 1");
        }
        if (accessCount < 0) {
            throw new IllegalArgumentException("accessCount must not be negative");
        }
        urls = List.copyOf(urls);
    }

    public boolean active() {
        return status == QuickShareStatus.ACTIVE;
    }

    public String primaryUrl() {
        return urls.isEmpty() ? "" : urls.getFirst();
    }
}
