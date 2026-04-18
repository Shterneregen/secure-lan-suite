package com.shterneregen.securelan.filetransfer.event;

import java.nio.file.Path;

public record FileTransferCompletedEvent(
        String transferId,
        String fileName,
        Path path,
        long totalBytes,
        boolean outgoing
) implements FileTransferEvent {
}
