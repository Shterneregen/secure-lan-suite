package com.shterneregen.securelan.filetransfer.service;

import com.shterneregen.securelan.filetransfer.event.FileTransferEvent;

@FunctionalInterface
public interface FileTransferEventPublisher {
    void publish(FileTransferEvent event);
}
