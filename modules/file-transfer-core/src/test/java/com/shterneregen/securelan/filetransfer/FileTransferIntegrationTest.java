package com.shterneregen.securelan.filetransfer;

import com.shterneregen.securelan.filetransfer.service.FileTransferClientRequest;
import com.shterneregen.securelan.filetransfer.service.FileTransferEventPublisher;
import com.shterneregen.securelan.filetransfer.service.FileTransferServerConfig;
import com.shterneregen.securelan.filetransfer.service.impl.DefaultFileTransferClientService;
import com.shterneregen.securelan.filetransfer.service.impl.DefaultFileTransferServerService;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileTransferIntegrationTest {
    @Test
    void shouldSendAndReceiveEncryptedFile() throws Exception {
        Path tempDir = Files.createTempDirectory("file-transfer-it");
        Path inbox = tempDir.resolve("inbox");
        Files.createDirectories(inbox);
        Path sourceFile = tempDir.resolve("sample.txt");
        Files.writeString(sourceFile, "secure file transfer payload");

        var serverEvents = new CopyOnWriteArrayList<String>();
        var clientEvents = new CopyOnWriteArrayList<String>();
        FileTransferEventPublisher serverPublisher = event -> serverEvents.add(event.getClass().getSimpleName());
        FileTransferEventPublisher clientPublisher = event -> clientEvents.add(event.getClass().getSimpleName());

        DefaultFileTransferServerService server = new DefaultFileTransferServerService(serverPublisher);
        int port = findAvailablePort();
        server.start(new FileTransferServerConfig(port, inbox, "files-pass"));
        try {
            DefaultFileTransferClientService client = new DefaultFileTransferClientService(clientPublisher);
            String transferId = client.sendFile(new FileTransferClientRequest(
                    "127.0.0.1",
                    port,
                    "alice",
                    "bob",
                    "files-pass",
                    sourceFile
            ));
            assertFalse(transferId.isBlank());
            TimeUnit.MILLISECONDS.sleep(250);

            Path receivedFile = inbox.resolve("sample.txt");
            assertTrue(Files.exists(receivedFile));
            assertEquals(Files.readString(sourceFile), Files.readString(receivedFile));
            assertTrue(serverEvents.contains("FileTransferStartedEvent"));
            assertTrue(serverEvents.contains("FileTransferCompletedEvent"));
            assertTrue(clientEvents.contains("FileTransferStartedEvent"));
            assertTrue(clientEvents.contains("FileTransferCompletedEvent"));
        } finally {
            server.stop();
        }
    }

    @Test
    void shouldRestartServerAfterStop() throws Exception {
        Path tempDir = Files.createTempDirectory("file-transfer-restart");
        Path inbox = tempDir.resolve("inbox");
        Files.createDirectories(inbox);
        FileTransferEventPublisher serverPublisher = event -> {
        };

        DefaultFileTransferServerService server = new DefaultFileTransferServerService(serverPublisher);
        int port = findAvailablePort();
        try {
            server.start(new FileTransferServerConfig(port, inbox, "files-pass"));
            assertTrue(server.isRunning());

            server.stop();
            assertFalse(server.isRunning());

            assertDoesNotThrow(() -> server.start(new FileTransferServerConfig(port, inbox, "files-pass")));
            assertTrue(server.isRunning());
        } finally {
            server.stop();
        }
    }

    private static int findAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
