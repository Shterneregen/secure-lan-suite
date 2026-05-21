package com.shterneregen.securelan.filetransfer

import com.shterneregen.securelan.filetransfer.service.FileTransferAcceptanceHandler
import com.shterneregen.securelan.filetransfer.service.FileTransferClientRequest
import com.shterneregen.securelan.filetransfer.service.FileTransferEventPublisher
import com.shterneregen.securelan.filetransfer.service.FileTransferServerConfig
import com.shterneregen.securelan.filetransfer.service.impl.DefaultFileTransferClientService
import com.shterneregen.securelan.filetransfer.service.impl.DefaultFileTransferServerService
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.ServerSocket
import java.nio.file.Files
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class FileTransferIntegrationTest {
    @Test
    fun shouldSendAndReceiveEncryptedFile() {
        val tempDir = Files.createTempDirectory("file-transfer-it")
        val inbox = tempDir.resolve("inbox")
        Files.createDirectories(inbox)
        val sourceFile = tempDir.resolve("sample.txt")
        Files.writeString(sourceFile, "secure file transfer payload")

        val serverEvents = CopyOnWriteArrayList<String>()
        val clientEvents = CopyOnWriteArrayList<String>()
        val serverPublisher = FileTransferEventPublisher { event -> serverEvents.add(event.javaClass.simpleName) }
        val clientPublisher = FileTransferEventPublisher { event -> clientEvents.add(event.javaClass.simpleName) }

        val server = DefaultFileTransferServerService(serverPublisher)
        val port = findAvailablePort()
        server.start(FileTransferServerConfig(port, inbox, "files-pass"))
        try {
            val client = DefaultFileTransferClientService(clientPublisher)
            val transferId = client.sendFile(
                FileTransferClientRequest(
                    "127.0.0.1",
                    port,
                    "alice",
                    "bob",
                    "files-pass",
                    sourceFile,
                ),
            )
            assertFalse(transferId.isBlank())
            TimeUnit.MILLISECONDS.sleep(250)

            val receivedFile = inbox.resolve("sample.txt")
            assertTrue(Files.exists(receivedFile))
            assertEquals(Files.readString(sourceFile), Files.readString(receivedFile))
            assertTrue(serverEvents.contains("FileTransferStartedEvent"))
            assertTrue(serverEvents.contains("FileTransferCompletedEvent"))
            assertTrue(clientEvents.contains("FileTransferStartedEvent"))
            assertTrue(clientEvents.contains("FileTransferCompletedEvent"))
        } finally {
            server.stop()
        }
    }

    @Test
    fun shouldRestartServerAfterStop() {
        val tempDir = Files.createTempDirectory("file-transfer-restart")
        val inbox = tempDir.resolve("inbox")
        Files.createDirectories(inbox)
        val serverPublisher = FileTransferEventPublisher { }

        val server = DefaultFileTransferServerService(serverPublisher)
        val port = findAvailablePort()
        try {
            server.start(FileTransferServerConfig(port, inbox, "files-pass"))
            assertTrue(server.isRunning())

            server.stop()
            assertFalse(server.isRunning())

            assertDoesNotThrow { server.start(FileTransferServerConfig(port, inbox, "files-pass")) }
            assertTrue(server.isRunning())
        } finally {
            server.stop()
        }
    }

    @Test
    fun shouldRejectIncomingFileWhenReceiverDeclines() {
        val tempDir = Files.createTempDirectory("file-transfer-reject")
        val inbox = tempDir.resolve("inbox")
        Files.createDirectories(inbox)
        val sourceFile = tempDir.resolve("sample.txt")
        Files.writeString(sourceFile, "rejected payload")

        val serverEvents = CopyOnWriteArrayList<String>()
        val serverPublisher = FileTransferEventPublisher { event -> serverEvents.add(event.javaClass.simpleName) }
        val server = DefaultFileTransferServerService(serverPublisher)
        val port = findAvailablePort()
        server.start(FileTransferServerConfig(port, inbox, "files-pass", FileTransferAcceptanceHandler { _, _ -> false }))
        try {
            val client = DefaultFileTransferClientService(FileTransferEventPublisher { })
            try {
                client.sendFile(FileTransferClientRequest("127.0.0.1", port, "alice", "bob", "files-pass", sourceFile))
            } catch (_: IllegalStateException) {
                // Receiver rejection is surfaced to the sender as a failed transfer.
            }

            TimeUnit.MILLISECONDS.sleep(250)
            assertFalse(Files.exists(inbox.resolve("sample.txt")))
            assertTrue(serverEvents.contains("FileTransferFailedEvent"))
        } finally {
            server.stop()
        }
    }

    @Test
    fun shouldInvokeAcceptanceHandlerBeforeReceivingBytes() {
        val tempDir = Files.createTempDirectory("file-transfer-accept-handler")
        val inbox = tempDir.resolve("inbox")
        Files.createDirectories(inbox)
        val sourceFile = tempDir.resolve("sample.txt")
        Files.writeString(sourceFile, "accepted payload")

        val invoked = AtomicBoolean(false)
        val server = DefaultFileTransferServerService(FileTransferEventPublisher { })
        val port = findAvailablePort()
        server.start(
            FileTransferServerConfig(
                port,
                inbox,
                "files-pass",
                FileTransferAcceptanceHandler { metadata, remoteAddress ->
                    invoked.set(true)
                    assertEquals("alice", metadata.senderId)
                    assertEquals("bob", metadata.recipientId)
                    assertEquals("sample.txt", metadata.fileName)
                    assertFalse(remoteAddress.isBlank())
                    true
                },
            ),
        )
        try {
            val client = DefaultFileTransferClientService(FileTransferEventPublisher { })
            client.sendFile(FileTransferClientRequest("127.0.0.1", port, "alice", "bob", "files-pass", sourceFile))

            TimeUnit.MILLISECONDS.sleep(250)
            assertTrue(invoked.get())
            assertEquals(Files.readString(sourceFile), Files.readString(inbox.resolve("sample.txt")))
        } finally {
            server.stop()
        }
    }

    @Test
    fun shouldSendFileWithLongNameWithoutOversizedRsaHandshakePayload() {
        val tempDir = Files.createTempDirectory("file-transfer-long-name")
        val inbox = tempDir.resolve("inbox")
        Files.createDirectories(inbox)
        val longName = "Screenshot_2026-05-18-10-57-14-520_com.shterneregen.securelan.androidclient_" +
            "abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789.jpg"
        val sourceFile = tempDir.resolve(longName)
        val payload = ByteArray(419_172)
        for (index in payload.indices) {
            payload[index] = (index % 251).toByte()
        }
        Files.write(sourceFile, payload)

        val server = DefaultFileTransferServerService(FileTransferEventPublisher { })
        val port = findAvailablePort()
        server.start(FileTransferServerConfig(port, inbox, "files-pass"))
        try {
            val client = DefaultFileTransferClientService(FileTransferEventPublisher { })
            client.sendFile(FileTransferClientRequest("127.0.0.1", port, "alice", "bob", "files-pass", sourceFile))

            TimeUnit.MILLISECONDS.sleep(250)
            assertTrue(Files.exists(inbox.resolve(longName)))
            assertEquals(Files.size(sourceFile), Files.size(inbox.resolve(longName)))
        } finally {
            server.stop()
        }
    }

    @Test
    fun shouldTransferFilesBidirectionallyBetweenTwoPeers() {
        val tempDir = Files.createTempDirectory("file-transfer-bidirectional")
        val aliceInbox = tempDir.resolve("alice-inbox")
        val bobInbox = tempDir.resolve("bob-inbox")
        Files.createDirectories(aliceInbox)
        Files.createDirectories(bobInbox)
        val aliceFile = tempDir.resolve("alice-to-bob.txt")
        val bobFile = tempDir.resolve("bob-to-alice.txt")
        Files.writeString(aliceFile, "hello from alice")
        Files.writeString(bobFile, "hello from bob")

        val aliceReceiver = DefaultFileTransferServerService(FileTransferEventPublisher { })
        val bobReceiver = DefaultFileTransferServerService(FileTransferEventPublisher { })
        val alicePort = findAvailablePort()
        val bobPort = findAvailablePort()
        aliceReceiver.start(FileTransferServerConfig(alicePort, aliceInbox, "files-pass"))
        bobReceiver.start(FileTransferServerConfig(bobPort, bobInbox, "files-pass"))
        try {
            val aliceSender = DefaultFileTransferClientService(FileTransferEventPublisher { })
            val bobSender = DefaultFileTransferClientService(FileTransferEventPublisher { })

            aliceSender.sendFile(FileTransferClientRequest("127.0.0.1", bobPort, "alice", "bob", "files-pass", aliceFile))
            bobSender.sendFile(FileTransferClientRequest("127.0.0.1", alicePort, "bob", "alice", "files-pass", bobFile))

            TimeUnit.MILLISECONDS.sleep(250)
            assertEquals(Files.readString(aliceFile), Files.readString(bobInbox.resolve("alice-to-bob.txt")))
            assertEquals(Files.readString(bobFile), Files.readString(aliceInbox.resolve("bob-to-alice.txt")))
        } finally {
            aliceReceiver.stop()
            bobReceiver.stop()
        }
    }

    private fun findAvailablePort(): Int = ServerSocket(0).use { it.localPort }
}
