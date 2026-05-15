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
import java.util.concurrent.atomic.AtomicBoolean;
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

    @Test
    void shouldRejectIncomingFileWhenReceiverDeclines() throws Exception {
        Path tempDir = Files.createTempDirectory("file-transfer-reject");
        Path inbox = tempDir.resolve("inbox");
        Files.createDirectories(inbox);
        Path sourceFile = tempDir.resolve("sample.txt");
        Files.writeString(sourceFile, "rejected payload");

        var serverEvents = new CopyOnWriteArrayList<String>();
        FileTransferEventPublisher serverPublisher = event -> serverEvents.add(event.getClass().getSimpleName());
        DefaultFileTransferServerService server = new DefaultFileTransferServerService(serverPublisher);
        int port = findAvailablePort();
        server.start(new FileTransferServerConfig(port, inbox, "files-pass", (metadata, remoteAddress) -> false));
        try {
            DefaultFileTransferClientService client = new DefaultFileTransferClientService(event -> {
            });
            try {
                client.sendFile(new FileTransferClientRequest("127.0.0.1", port, "alice", "bob", "files-pass", sourceFile));
            } catch (IllegalStateException expected) {
                // Receiver rejection is surfaced to the sender as a failed transfer.
            }

            TimeUnit.MILLISECONDS.sleep(250);
            assertFalse(Files.exists(inbox.resolve("sample.txt")));
            assertTrue(serverEvents.contains("FileTransferFailedEvent"));
        } finally {
            server.stop();
        }
    }

    @Test
    void shouldInvokeAcceptanceHandlerBeforeReceivingBytes() throws Exception {
        Path tempDir = Files.createTempDirectory("file-transfer-accept-handler");
        Path inbox = tempDir.resolve("inbox");
        Files.createDirectories(inbox);
        Path sourceFile = tempDir.resolve("sample.txt");
        Files.writeString(sourceFile, "accepted payload");

        AtomicBoolean invoked = new AtomicBoolean(false);
        DefaultFileTransferServerService server = new DefaultFileTransferServerService(event -> {
        });
        int port = findAvailablePort();
        server.start(new FileTransferServerConfig(port, inbox, "files-pass", (metadata, remoteAddress) -> {
            invoked.set(true);
            assertEquals("alice", metadata.senderId());
            assertEquals("bob", metadata.recipientId());
            assertEquals("sample.txt", metadata.fileName());
            assertFalse(remoteAddress.isBlank());
            return true;
        }));
        try {
            DefaultFileTransferClientService client = new DefaultFileTransferClientService(event -> {
            });
            client.sendFile(new FileTransferClientRequest("127.0.0.1", port, "alice", "bob", "files-pass", sourceFile));

            TimeUnit.MILLISECONDS.sleep(250);
            assertTrue(invoked.get());
            assertEquals(Files.readString(sourceFile), Files.readString(inbox.resolve("sample.txt")));
        } finally {
            server.stop();
        }
    }

    @Test
    void shouldSendFileWithLongNameWithoutOversizedRsaHandshakePayload() throws Exception {
        Path tempDir = Files.createTempDirectory("file-transfer-long-name");
        Path inbox = tempDir.resolve("inbox");
        Files.createDirectories(inbox);
        String longName = "Screenshot_2026-05-18-10-57-14-520_com.shterneregen.securelan.androidclient_"
                + "abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789.jpg";
        Path sourceFile = tempDir.resolve(longName);
        byte[] payload = new byte[419_172];
        for (int index = 0; index < payload.length; index++) {
            payload[index] = (byte) (index % 251);
        }
        Files.write(sourceFile, payload);

        DefaultFileTransferServerService server = new DefaultFileTransferServerService(event -> {
        });
        int port = findAvailablePort();
        server.start(new FileTransferServerConfig(port, inbox, "files-pass"));
        try {
            DefaultFileTransferClientService client = new DefaultFileTransferClientService(event -> {
            });
            client.sendFile(new FileTransferClientRequest("127.0.0.1", port, "alice", "bob", "files-pass", sourceFile));

            TimeUnit.MILLISECONDS.sleep(250);
            assertTrue(Files.exists(inbox.resolve(longName)));
            assertEquals(Files.size(sourceFile), Files.size(inbox.resolve(longName)));
        } finally {
            server.stop();
        }
    }

    @Test
    void shouldTransferFilesBidirectionallyBetweenTwoPeers() throws Exception {
        Path tempDir = Files.createTempDirectory("file-transfer-bidirectional");
        Path aliceInbox = tempDir.resolve("alice-inbox");
        Path bobInbox = tempDir.resolve("bob-inbox");
        Files.createDirectories(aliceInbox);
        Files.createDirectories(bobInbox);
        Path aliceFile = tempDir.resolve("alice-to-bob.txt");
        Path bobFile = tempDir.resolve("bob-to-alice.txt");
        Files.writeString(aliceFile, "hello from alice");
        Files.writeString(bobFile, "hello from bob");

        DefaultFileTransferServerService aliceReceiver = new DefaultFileTransferServerService(event -> {
        });
        DefaultFileTransferServerService bobReceiver = new DefaultFileTransferServerService(event -> {
        });
        int alicePort = findAvailablePort();
        int bobPort = findAvailablePort();
        aliceReceiver.start(new FileTransferServerConfig(alicePort, aliceInbox, "files-pass"));
        bobReceiver.start(new FileTransferServerConfig(bobPort, bobInbox, "files-pass"));
        try {
            DefaultFileTransferClientService aliceSender = new DefaultFileTransferClientService(event -> {
            });
            DefaultFileTransferClientService bobSender = new DefaultFileTransferClientService(event -> {
            });

            aliceSender.sendFile(new FileTransferClientRequest("127.0.0.1", bobPort, "alice", "bob", "files-pass", aliceFile));
            bobSender.sendFile(new FileTransferClientRequest("127.0.0.1", alicePort, "bob", "alice", "files-pass", bobFile));

            TimeUnit.MILLISECONDS.sleep(250);
            assertEquals(Files.readString(aliceFile), Files.readString(bobInbox.resolve("alice-to-bob.txt")));
            assertEquals(Files.readString(bobFile), Files.readString(aliceInbox.resolve("bob-to-alice.txt")));
        } finally {
            aliceReceiver.stop();
            bobReceiver.stop();
        }
    }

    private static int findAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
