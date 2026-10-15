package com.shterneregen.securelan.androidclient.network

import com.shterneregen.securelan.androidclient.protocol.FileTransferSession
import com.shterneregen.securelan.chat.event.ChatUserJoinedEvent
import com.shterneregen.securelan.chat.protocol.handshake.PeerCapabilities
import com.shterneregen.securelan.chat.service.ChatClientConnectRequest
import com.shterneregen.securelan.chat.service.ChatServerConfig
import com.shterneregen.securelan.chat.service.impl.DefaultChatClientService
import com.shterneregen.securelan.chat.service.impl.DefaultChatServerService
import com.shterneregen.securelan.filetransfer.protocol.FileTransferPeerAuthentication
import com.shterneregen.securelan.filetransfer.protocol.SecureFileTransferHandshake
import com.shterneregen.securelan.filetransfer.service.FileTransferAcceptanceHandler
import com.shterneregen.securelan.filetransfer.service.FileTransferServerConfig
import com.shterneregen.securelan.filetransfer.service.impl.DefaultFileTransferServerService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.net.ServerSocket
import java.net.Socket
import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Exercises Android-client identity distribution through a real core host and real V2 file IO. */
class SecureRoomFileCompatibilityTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test(timeout = 15_000)
    fun androidChatIdentityAuthorizesFileToDesktop() = runBlocking {
        val roomPort = ServerSocket(0).use { it.localPort }
        val filePort = ServerSocket(0).use { it.localPort }
        val certified = ConcurrentHashMap<String, String>()
        val joined = CountDownLatch(1)
        val host = DefaultChatServerService({})
        val desktop = DefaultChatClientService({ event ->
            if (event is ChatUserJoinedEvent && event.nickname == "android") {
                certified["android"] = event.capabilities.identityKey()
                joined.countDown()
            }
        })
        val receiver = DefaultFileTransferServerService({})
        val android = SecureChatClient()
        try {
            host.start(ChatServerConfig(roomPort, "test-password"))
            assertTrue(desktop.connect(ChatClientConnectRequest("127.0.0.1", roomPort, "desktop", "test-password")))
            android.connect("127.0.0.1", roomPort, "android", "test-password", PeerCapabilities.android("test", 0))
            val presence = android.readMessage()!!
            assertEquals("desktop", presence.sender)
            val desktopKey = PeerCapabilities.decode(presence.payload).identityKey()
            assertEquals(desktop.fileIdentity().publicKey, desktopKey)
            assertTrue(joined.await(2, TimeUnit.SECONDS))
            assertEquals(android.fileIdentity.publicKey, certified["android"])
            val inbox = temporary.newFolder("inbox").toPath()
            receiver.start(FileTransferServerConfig(filePort, inbox, "test-password", FileTransferAcceptanceHandler.acceptAll(),
                FileTransferPeerAuthentication("desktop", desktop.fileIdentity()) { certified[it] }))
            Socket("127.0.0.1", filePort).use { socket ->
                socket.soTimeout = 3000
                FileTransferSession(socket).use { session ->
                    val auth = FileTransferPeerAuthentication("android", android.fileIdentity) { if (it == "desktop") desktopKey else null }
                    val payload = "Android to desktop V2".toByteArray()
                    SecureFileTransferHandshake().performClientHandshake(session, "android", "desktop", "test-password", "payload.txt", payload.size.toLong(), "test-transfer", auth)
                    session.writeEncryptedBytes(payload)
                    session.writeEncryptedBytes(ByteArray(0))
                    assertEquals("DONE", session.readEncryptedText())
                }
            }
            assertEquals("Android to desktop V2", String(Files.readAllBytes(inbox.resolve("payload.txt")), Charsets.UTF_8))
        } finally { android.close(); desktop.disconnect(); receiver.stop(); host.stop() }
    }
}
