package com.shterneregen.securelan.filetransfer

import com.shterneregen.securelan.crypto.protocol.PeerIdentity
import com.shterneregen.securelan.filetransfer.protocol.*
import com.shterneregen.securelan.filetransfer.service.*
import com.shterneregen.securelan.filetransfer.service.impl.*
import com.shterneregen.securelan.filetransfer.protocol.SecureFileTransferHandshake
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class FileTransferSecurityTest {
    @TempDir lateinit var directory: Path
    private val alice = PeerIdentity()
    private val bob = PeerIdentity()
    private val mallory = PeerIdentity()
    private fun auth(id: String, identity: PeerIdentity = if (id == "alice") alice else bob) =
        FileTransferPeerAuthentication(id, identity) { when (it) { "alice" -> alice.publicKey; "bob" -> bob.publicKey; else -> null } }

    private fun withServer(maxBytes: Long = 1024, receiver: FileTransferPeerAuthentication = auth("bob"), run: (Int, Path, CountDownLatch) -> Unit) {
        val inbox = Files.createDirectories(directory.resolve("inbox"))
        val finished = CountDownLatch(1)
        val port = ServerSocket(0).use { it.localPort }
        val server = DefaultFileTransferServerService({ if (it is com.shterneregen.securelan.filetransfer.event.FileTransferFailedEvent) finished.countDown() })
        server.start(FileTransferServerConfig(port, inbox, "password", FileTransferAcceptanceHandler.acceptAll(), receiver, maxBytes))
        try { run(port, inbox, finished) } finally { server.stop() }
    }

    private fun request(port: Int, sender: String = "alice", recipient: String = "bob", identity: FileTransferPeerAuthentication = auth("alice")): FileTransferClientRequest {
        val source = directory.resolve("source.txt")
        Files.writeString(source, "test-payload")
        return FileTransferClientRequest("127.0.0.1", port, sender, recipient, "password", source, identity)
    }

    @Test fun chunkBurstUsesSameThrottleForOperationUpdatesAndPreservesFinalPhase() = withServer(maxBytes = 2_000_000) { port, inbox, _ ->
        val source = directory.resolve("burst.bin")
        Files.write(source, ByteArray(1024 * 1024) { it.toByte() })
        val events = mutableListOf<com.shterneregen.securelan.filetransfer.event.FileTransferProgressEvent>()
        val updates = mutableListOf<FileTransferOperationUpdate>()
        val client = DefaultFileTransferClientService({ if (it is com.shterneregen.securelan.filetransfer.event.FileTransferProgressEvent) events += it })
        val operation = client.createOperation(FileTransferClientRequest("127.0.0.1", port, "alice", "bob", "password", source, auth("alice")), updates::add)
        assertTrue(operation.execute() is FileTransferOutcome.Completed)
        val sending = updates.filter { it.phase == FileTransferPhase.SENDING }
        assertEquals(events.size + 1, sending.size, "only the phase transition may bypass the shared throttle")
        assertEquals(1024L * 1024, sending.last().transferredBytes)
        assertTrue(updates.any { it.phase == FileTransferPhase.WAITING_FOR_CONFIRMATION })
        assertEquals(1024L * 1024, Files.size(inbox.resolve("burst.bin")))
    }

    @Test fun validRoomPeerCompletesTransfer() = withServer { port, inbox, _ ->
        DefaultFileTransferClientService({}).sendFile(request(port))
        assertEquals("test-payload", Files.readString(inbox.resolve("source.txt")))
    }

    @Test fun senderCannotImpersonateAnotherParticipant() = withServer { port, inbox, finished ->
        assertThrows(IllegalStateException::class.java) { DefaultFileTransferClientService({}).sendFile(request(port, identity = auth("alice", mallory))) }
        assertTrue(finished.await(2, TimeUnit.SECONDS))
        assertFalse(Files.exists(inbox.resolve("source.txt")))
    }

    @Test fun wrongRecipientAndUnknownPeerAreRejected() = withServer { port, inbox, finished ->
        assertThrows(IllegalStateException::class.java) { DefaultFileTransferClientService({}).sendFile(request(port, recipient = "carol")) }
        assertThrows(IllegalStateException::class.java) { DefaultFileTransferClientService({}).sendFile(request(port, sender = "mallory", identity = auth("mallory", mallory))) }
        assertTrue(finished.await(2, TimeUnit.SECONDS))
        assertFalse(Files.exists(inbox.resolve("source.txt")))
    }

    @Test fun senderAuthenticatesReceiverBeforeSendingFileBytes() = withServer(receiver = auth("bob", mallory)) { port, inbox, finished ->
        assertThrows(IllegalStateException::class.java) { DefaultFileTransferClientService({}).sendFile(request(port)) }
        assertTrue(finished.await(2, TimeUnit.SECONDS))
        assertFalse(Files.exists(inbox.resolve("source.txt")))
    }

    @Test fun oversizedTransferIsRejectedBeforeCreatingPartialFile() = withServer(maxBytes = 2) { port, inbox, finished ->
        assertThrows(IllegalStateException::class.java) { DefaultFileTransferClientService({}).sendFile(request(port)) }
        assertTrue(finished.await(2, TimeUnit.SECONDS))
        Files.list(inbox).use { assertEquals(0, it.count()) }
    }

    @Test fun exactZeroDoesNotAllowNonEmptyFileAndCleansUp() = withServer { port, inbox, finished ->
        Socket("127.0.0.1", port).use { socket ->
            socket.soTimeout = 3000
            FileTransferSession(socket).use { session ->
                SecureFileTransferHandshake().performClientHandshake(session, "alice", "bob", "password", "zero.bin", 0, "zero-transfer", auth("alice"))
                session.writeEncryptedBytes("unexpected".toByteArray())
                assertThrows(IOException::class.java) { session.readEncryptedText() }
            }
        }
        assertTrue(finished.await(2, TimeUnit.SECONDS))
        // Wait for handler cleanup rather than treating the error event as cleanup completion.
        val end = System.nanoTime() + TimeUnit.SECONDS.toNanos(2)
        while (System.nanoTime() < end && Files.list(inbox).use { it.count() > 0 }) Thread.sleep(10)
        Files.list(inbox).use { assertEquals(0, it.count()) }
    }

    @Test fun fileAuthorizationFailsClosedWithoutRoomIdentity() {
        val port = ServerSocket(0).use { it.localPort }
        val source = Files.writeString(directory.resolve("source.txt"), "payload")
        val constructor = FileTransferClientRequest::class.java.declaredConstructors.single { it.parameterCount == 7 }
        val failure = assertThrows(java.lang.reflect.InvocationTargetException::class.java) {
            constructor.newInstance("127.0.0.1", port, "alice", "bob", "password", source, null)
        }
        assertTrue(failure.cause is NullPointerException)
    }

    @Test fun replayedFileChunkCannotProduceDoneAndPartialFileIsRemoved() = withServer { port, inbox, finished ->
        val captured = java.io.ByteArrayOutputStream()
        val socket = object : Socket("127.0.0.1", port) {
            override fun getOutputStream(): java.io.OutputStream {
                val actual = super.getOutputStream()
                return object : java.io.OutputStream() {
                    override fun write(value: Int) { captured.write(value); actual.write(value) }
                    override fun write(bytes: ByteArray, offset: Int, length: Int) { captured.write(bytes, offset, length); actual.write(bytes, offset, length) }
                    override fun flush() = actual.flush()
                }
            }
        }
        socket.use {
            socket.soTimeout = 3000
            FileTransferSession(socket).use { session ->
                SecureFileTransferHandshake().performClientHandshake(session, "alice", "bob", "password", "replay.bin", 8, "replay-transfer", auth("alice"))
                captured.reset()
                session.writeEncryptedBytes("AAAA".toByteArray())
                val replay = captured.toByteArray()
                socket.getOutputStream().apply { write(replay); flush() }
                assertThrows(IOException::class.java) { session.readEncryptedText() }
            }
        }
        assertTrue(finished.await(2, TimeUnit.SECONDS))
        val end = System.nanoTime() + TimeUnit.SECONDS.toNanos(2)
        while (System.nanoTime() < end && Files.list(inbox).use { it.count() > 0 }) Thread.sleep(10)
        Files.list(inbox).use { assertEquals(0, it.count()) }
    }

    @Test fun traversalNameIsConfinedToInboxAndExactEmptyFileCompletes() = withServer { port, inbox, _ ->
        Socket("127.0.0.1", port).use { socket ->
            socket.soTimeout = 3000
            FileTransferSession(socket).use { session ->
                SecureFileTransferHandshake().performClientHandshake(session, "alice", "bob", "password", "../outside.txt", 0, "empty-transfer", auth("alice"))
                session.writeEncryptedBytes(ByteArray(0))
                assertEquals("DONE", session.readEncryptedText())
            }
        }
        assertEquals(0, Files.size(inbox.resolve("outside.txt")))
        assertFalse(Files.exists(directory.resolve("outside.txt")))
    }
}
