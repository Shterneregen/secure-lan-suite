package com.shterneregen.securelan.filetransfer.event;

import com.shterneregen.securelan.common.model.FileTransferProgress;

public record FileTransferProgressEvent(
        String transferId,
        FileTransferProgress progress,
        boolean outgoing
) implements FileTransferEvent {
}
