package com.shterneregen.securelan.filetransfer.event;

public record FileTransferFailedEvent(
        String transferId,
        String fileName,
        String message,
        Throwable cause,
        boolean outgoing
) implements FileTransferEvent {
}
