package com.shterneregen.securelan.filetransfer.service;

public interface FileTransferServerService {
    void start(FileTransferServerConfig config);
    void stop();
    boolean isRunning();
}
