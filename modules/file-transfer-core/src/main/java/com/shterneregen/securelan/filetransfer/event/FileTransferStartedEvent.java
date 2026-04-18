package com.shterneregen.securelan.filetransfer.event;

public record FileTransferStartedEvent(
        String transferId,
        String fileName,
        long totalBytes,
        boolean outgoing
) implements FileTransferEvent {
}
