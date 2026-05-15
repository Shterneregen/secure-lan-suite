package com.shterneregen.securelan.filetransfer.quickshare;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

public record QuickShareCreateRequest(
        QuickShareType type,
        String displayName,
        Path file,
        String text,
        Duration expiresAfter,
        int accessLimit
) {
    public QuickShareCreateRequest {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(displayName, "displayName must not be null");
        Objects.requireNonNull(expiresAfter, "expiresAfter must not be null");
        if (displayName.isBlank()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
        if (expiresAfter.isZero() || expiresAfter.isNegative()) {
            throw new IllegalArgumentException("expiresAfter must be positive");
        }
        if (accessLimit < 1) {
            throw new IllegalArgumentException("accessLimit must be at least 1");
        }
        if (type == QuickShareType.FILE) {
            Objects.requireNonNull(file, "file must not be null for file shares");
            if (!Files.isRegularFile(file) || !Files.isReadable(file)) {
                throw new IllegalArgumentException("file must be a readable regular file");
            }
            text = "";
        } else if (type == QuickShareType.TEXT) {
            Objects.requireNonNull(text, "text must not be null for text shares");
            if (text.isBlank()) {
                throw new IllegalArgumentException("text must not be blank");
            }
            file = null;
        }
    }

    public static QuickShareCreateRequest file(Path file, String displayName, Duration expiresAfter, int accessLimit) {
        Objects.requireNonNull(file, "file must not be null");
        String resolvedName = displayName == null || displayName.isBlank()
                ? file.getFileName().toString()
                : displayName;
        return new QuickShareCreateRequest(QuickShareType.FILE, resolvedName, file, "", expiresAfter, accessLimit);
    }

    public static QuickShareCreateRequest text(String text, String displayName, Duration expiresAfter, int accessLimit) {
        String resolvedName = displayName == null || displayName.isBlank() ? "shared-text" : displayName;
        return new QuickShareCreateRequest(QuickShareType.TEXT, resolvedName, null, text, expiresAfter, accessLimit);
    }
}
