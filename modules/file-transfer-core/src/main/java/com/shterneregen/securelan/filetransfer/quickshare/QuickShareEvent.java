package com.shterneregen.securelan.filetransfer.quickshare;

import java.util.Objects;

public record QuickShareEvent(
        String shareId,
        QuickShareSnapshot snapshot,
        String message,
        String remoteAddress
) {
    public QuickShareEvent {
        shareId = shareId == null ? "" : shareId;
        Objects.requireNonNull(message, "message must not be null");
        remoteAddress = remoteAddress == null ? "" : remoteAddress;
    }
}
