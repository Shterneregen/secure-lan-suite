package com.shterneregen.securelan.desktop.compose

import com.shterneregen.securelan.chat.discovery.*
import com.shterneregen.securelan.chat.service.*
import com.shterneregen.securelan.common.model.rtc.RtcSignalEnvelope
import com.shterneregen.securelan.crypto.protocol.PeerIdentity
import com.shterneregen.securelan.filetransfer.protocol.FileTransferPeerAuthentication
import com.shterneregen.securelan.filetransfer.service.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class RoomSessionControllerTest {
    @TempDir lateinit var directory: Path

    private class Fixture {
        var failAt = ""
        var serverRunning = false
        var filesRunning = false
        var connected = false
        var discoveryRunning = true
        val operations = mutableListOf<String>()
        private fun step(name: String) { operations += name; if (failAt == name) error("Injected $name failure") }
        val server = object : ChatServerService {
            override fun start(config: ChatServerConfig) { serverRunning = true; step("chat.start") }
            override fun stop() { serverRunning = false; step("chat.stop") }
            override fun isRunning() = serverRunning
            override fun connectedUsers() = 0
        }
        val client = object : ChatClientService {
            override fun connect(request: ChatClientConnectRequest): Boolean { connected = true; step("client.connect"); return true }
            override fun disconnect() { connected = false; step("client.disconnect") }
            override fun sendMessage(text: String?) = ChatSendOutcome.WRITTEN
            override fun sendSignal(signal: RtcSignalEnvelope?) = Unit
            override fun isConnected() = connected
        }
        val files = object : FileTransferServerService {
            override fun start(config: FileTransferServerConfig) { filesRunning = true; step("files.start") }
            override fun stop() { filesRunning = false; step("files.stop") }
            override fun isRunning() = filesRunning
        }
        val discovery = object : PeerDiscoveryService {
            override fun start(config: PeerDiscoveryConfig, listener: PeerDiscoveryListener) { discoveryRunning = true; step(if (config.announceEnabled) "discovery.start" else "discovery.restore") }
            override fun stop() { discoveryRunning = false; step("discovery.stop") }
            override fun isRunning() = discoveryRunning
            override fun snapshot() = emptyList<DiscoveredPeer>()
            override fun setAnnounceEnabled(announceEnabled: Boolean) = Unit
        }
        val controller = RoomSessionController(server, client, files, discovery)
    }

    private fun start(f: Fixture, token: Long = f.controller.operationGeneration()) {
        val identity = FileTransferPeerAuthentication("alice", PeerIdentity()) { null }
        f.controller.start(ChatServerConfig(5050, "password"), FileTransferServerConfig(5051, directory, "password", null, identity),
            ChatClientConnectRequest("127.0.0.1", 5050, "alice", "password"),
            PeerDiscoveryConfig.defaults("2da1e450-798a-401f-990b-cb1cc4f61d92", "alice", 5050, 5051, true), PeerDiscoveryConfig.listenOnly("2da1e450-798a-401f-990b-cb1cc4f61d92", "alice"),
            object : PeerDiscoveryListener {
                override fun onPeerDiscovered(peer: DiscoveredPeer) = Unit
                override fun onPeerExpired(peer: DiscoveredPeer) = Unit
                override fun onDiscoveryError(message: String, cause: Throwable) = Unit
            }, token)
    }

    @Test fun everyStartupFailureRollsBackAndAllowsRetry() {
        for (failure in listOf("chat.start", "files.start", "client.connect")) {
            val f = Fixture().apply { failAt = failure }
            assertThrows(IllegalStateException::class.java) { start(f) }
            assertFalse(f.serverRunning, failure)
            assertFalse(f.filesRunning, failure)
            assertFalse(f.connected, failure)
            f.failAt = ""
            start(f)
            assertTrue(f.connected)
        }
    }

    @Test fun discoveryFailureLeavesReadyRoomAvailableForManualAndQrJoin() {
        val f = Fixture().apply { failAt = "discovery.start" }
        start(f)
        assertTrue(f.serverRunning)
        assertTrue(f.filesRunning)
        assertTrue(f.connected)
        assertNotNull(f.controller.hostedRoom)
        f.controller.close()
        assertNull(f.controller.hostedRoom)
        assertFalse(f.serverRunning)
    }

    @Test fun cancelledWorkCannotAcquireResourcesWhenItStartsLate() {
        val f = Fixture()
        val token = f.controller.operationGeneration()
        f.controller.invalidate()
        assertThrows(java.util.concurrent.CancellationException::class.java) { start(f, token) }
        assertTrue(f.operations.isEmpty())
    }

    @Test fun preexistingConnectionIsNeverAcquiredOrClosedByRollback() {
        val f = Fixture().apply { connected = true }
        assertThrows(IllegalStateException::class.java) { start(f) }
        assertTrue(f.connected)
        assertTrue(f.operations.isEmpty())
    }
}
