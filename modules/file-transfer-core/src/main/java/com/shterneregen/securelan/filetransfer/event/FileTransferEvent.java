package com.shterneregen.securelan.filetransfer.event;

public sealed interface FileTransferEvent permits FileTransferStartedEvent, FileTransferProgressEvent,
        FileTransferCompletedEvent, FileTransferFailedEvent {
    String transferId();
}
