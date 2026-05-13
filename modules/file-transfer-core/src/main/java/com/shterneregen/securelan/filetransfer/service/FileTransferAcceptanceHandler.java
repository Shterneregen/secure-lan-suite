package com.shterneregen.securelan.filetransfer.service;

import com.shterneregen.securelan.filetransfer.protocol.FileTransferMetadata;

@FunctionalInterface
public interface FileTransferAcceptanceHandler {
    boolean accept(FileTransferMetadata metadata, String remoteAddress);

    static FileTransferAcceptanceHandler acceptAll() {
        return (metadata, remoteAddress) -> true;
    }
}
