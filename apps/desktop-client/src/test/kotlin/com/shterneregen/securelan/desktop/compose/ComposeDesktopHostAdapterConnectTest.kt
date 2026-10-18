package com.shterneregen.securelan.desktop.compose

import com.shterneregen.securelan.chat.discovery.DiscoveredPeer
import com.shterneregen.securelan.chat.discovery.PeerDiscoveryConfig
import com.shterneregen.securelan.chat.discovery.PeerDiscoveryListener
import com.shterneregen.securelan.chat.discovery.PeerDiscoveryService
import com.shterneregen.securelan.chat.event.ChatDisconnectReasons
import com.shterneregen.securelan.chat.event.ChatDisconnectedEvent
import com.shterneregen.securelan.chat.service.ChatClientConnectRequest
import com.shterneregen.securelan.chat.service.ChatClientService
import com.shterneregen.securelan.chat.service.ChatServerConfig
import com.shterneregen.securelan.chat.service.ChatServerService
import com.shterneregen.securelan.chat.service.RandomNicknameService
import com.shterneregen.securelan.common.model.FileTransferProgress
import com.shterneregen.securelan.common.model.TransferStatus
import com.shterneregen.securelan.common.model.rtc.RtcSignalEnvelope
import com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListState
import com.shterneregen.securelan.desktop.compose.state.transfer.ComposeFileTransferState
import com.shterneregen.securelan.filetransfer.event.FileTransferCompletedEvent
import com.shterneregen.securelan.filetransfer.event.FileTransferFailedEvent
import com.shterneregen.securelan.filetransfer.event.FileTransferProgressEvent
import com.shterneregen.securelan.filetransfer.event.FileTransferStartedEvent
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareCreateRequest
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareEvent
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareServerConfig
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareService
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareSnapshot
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareStatus
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareType
import com.shterneregen.securelan.filetransfer.service.FileTransferServerConfig
import com.shterneregen.securelan.filetransfer.service.FileTransferServerService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.time.Instant
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.logging.Handler
import java.util.logging.LogRecord
import java.util.logging.Logger
import kotlin.system.measureTimeMillis

class ComposeDesktopHostAdapterConnectTest {
    @Test
    fun reconnectShouldUseTheNewFilePortPasswordAndDownloadDirectory() {
        val client = ConnectedChatClient()
        val files = TrackingFileTransferServer()
        val adapter = connectedAdapter(client, files)
        try {
            adapter.connect("127.0.0.1", "desktop-test", "first-password", 50_050, 50_051)
            assertEquals(50_051, files.started.poll(2, TimeUnit.SECONDS)?.port())
            awaitConnected(adapter)
            adapter.disconnect()
            assertFalse(files.isRunning(), "Leaving a remote room must release its file listener")

            val newDirectory = Path.of("new-downloads").toAbsolutePath().normalize()
            adapter.updateDownloadsDirectory(newDirectory)
            adapter.connect("127.0.0.1", "desktop-test", "second-password", 50_060, 50_061)
            val next = files.started.poll(2, TimeUnit.SECONDS)
            assertEquals(50_061, next?.port())
            assertEquals("second-password", next?.sessionPassword())
            assertEquals(newDirectory, next?.storageDirectory())
        } finally {
            adapter.shutdown()
        }
    }

    @Test
    fun remoteDisconnectShouldReleaseTheFileListener() {
        val client = ConnectedChatClient()
        val files = TrackingFileTransferServer()
        val adapter = connectedAdapter(client, files)
        try {
            adapter.connect("127.0.0.1", "desktop-test", "password", 50_050, 50_051)
            assertEquals(50_051, files.started.poll(2, TimeUnit.SECONDS)?.port())
            awaitConnected(adapter)
            client.disconnect()
            adapter.chatEventPublisher.publish(ChatDisconnectedEvent("desktop-test", ChatDisconnectReasons.CONNECTION_LOST))

            assertFalse(files.isRunning())
            assertFalse(adapter.statusState.clientConnected)
        } finally {
            adapter.shutdown()
        }
    }

    @Test
    fun disconnectShouldPreserveTheHostedRoomFileListener() {
        val client = ConnectedChatClient()
        val files = TrackingFileTransferServer()
        val adapter = connectedAdapter(client, files, hosting = true)
        try {
            adapter.connect("127.0.0.1", "desktop-test", "password", 50_050, 50_051)
            assertEquals(50_051, files.started.poll(2, TimeUnit.SECONDS)?.port())
            awaitConnected(adapter)
            adapter.disconnect()
            adapter.chatEventPublisher.publish(ChatDisconnectedEvent("desktop-test", ChatDisconnectReasons.CLIENT_REQUEST))

            assertTrue(files.isRunning(), "The listener belongs to the room while hosting remains active")
        } finally {
            adapter.shutdown()
        }
    }

    @Test
    fun textShareCreationEventsAndRevocationShouldKeepPayloadsOutOfLogs() {
        val secret = "private-text-for-log-test"
        val snapshot = QuickShareSnapshot(
            secret, QuickShareType.TEXT, secret, "", secret.length.toLong(),
            Instant.now(), null, null, 0, QuickShareStatus.ACTIVE,
            listOf("http://127.0.0.1:5053/s/$secret"),
        )
        val adapter = connectedAdapter(ConnectedChatClient(), IdleFileTransferServer(), quickShare = IdleQuickShareService(snapshot))
        val records = mutableListOf<String>()
        val handler = object : Handler() {
            override fun publish(record: LogRecord) { records += record.message }
            override fun flush() = Unit
            override fun close() = Unit
        }
        val logger = Logger.getLogger("SecureLanSuite")
        logger.addHandler(handler)
        try {
            adapter.createTextQuickShare(secret, null, null)
            adapter.quickShareEventPublisher.publish(QuickShareEvent(secret, snapshot, "Quick-share created: $secret", ""))
            adapter.stopQuickShareEntry(secret)

            assertTrue(records.any { it.contains("Text quick-share created") })
            assertTrue(records.any { it.contains("Quick-share stopped") })
            assertFalse(records.any { it.contains(secret) }, "Shared text and text-derived names, IDs, and URLs must stay out of logs")
        } finally {
            logger.removeHandler(handler)
            adapter.shutdown()
        }
    }

    private fun connectedAdapter(
        client: ChatClientService,
        files: FileTransferServerService,
        hosting: Boolean = false,
        quickShare: QuickShareService = IdleQuickShareService(),
        rtc: com.shterneregen.securelan.webrtc.service.RtcSessionService? = null,
        server: ChatServerService = IdleChatServer(hosting),
        discovery: PeerDiscoveryService = IdlePeerDiscovery(),
    ) = ComposeDesktopHostAdapter(
        chatServerService = server,
        chatClientService = client,
        fileTransferServerService = files,
        quickShareService = quickShare,
        rtcSessionService = rtc,
        discoveryService = discovery,
        randomNicknameService = object : RandomNicknameService {
            override fun generate(): String = "desktop-test"
        },
        uiStateDispatcher = { action -> action() },
    )

    @Test fun detailedRoomsWithSameNameStaySeparateFromAuthenticatedPresence() {
        val discovery = object : com.shterneregen.securelan.chat.discovery.RoomDiscoveryService {
            lateinit var publish: (com.shterneregen.securelan.chat.discovery.DiscoverySnapshot) -> Unit
            override fun startSession(request: com.shterneregen.securelan.chat.discovery.DiscoveryStartRequest, listener: PeerDiscoveryListener,
                onSnapshot: (com.shterneregen.securelan.chat.discovery.DiscoverySnapshot) -> Unit) { publish = onSnapshot }
            override fun detailedSnapshot() = com.shterneregen.securelan.chat.discovery.DiscoverySnapshot()
            override fun updateHostedRoom(value: com.shterneregen.securelan.chat.discovery.HostedRoomAdvertisement?) = Unit
            override fun start(config: PeerDiscoveryConfig, listener: PeerDiscoveryListener) = Unit
            override fun stop() = Unit
            override fun isRunning() = true
            override fun snapshot() = emptyList<DiscoveredPeer>()
            override fun setAnnounceEnabled(announceEnabled: Boolean) = Unit
        }
        val adapter = connectedAdapter(ConnectedChatClient(), IdleFileTransferServer(), discovery = discovery)
        try {
            val endpoints = listOf("192.168.1.42", "192.168.1.43").map { com.shterneregen.securelan.chat.discovery.DiscoveryEndpoint(
                com.shterneregen.securelan.common.net.address.IpLiteral.parse(it)!!, 5050) }
            val rooms = endpoints.mapIndexed { index, endpoint -> com.shterneregen.securelan.chat.discovery.DiscoveredRoom("room-$index", "peer-$index",
                null, "Same name", listOf(endpoint), null, setOf(com.shterneregen.securelan.chat.discovery.DiscoveryProviderId.MDNS)) }
            discovery.publish(com.shterneregen.securelan.chat.discovery.DiscoverySnapshot(rooms))
            assertEquals(2, adapter.nearbyRoomTargets.size)
            assertNotEquals(adapter.nearbyRoomTargets[0].rowId, adapter.nearbyRoomTargets[1].rowId)
            assertTrue(adapter.visiblePeerItems.isEmpty(), "Unauthenticated discovery must not create authenticated room participants")
            discovery.publish(com.shterneregen.securelan.chat.discovery.DiscoverySnapshot())
            assertTrue(adapter.nearbyRoomTargets.isEmpty())
        } finally { adapter.shutdown() }
    }

    @Test fun userTextNeverBecomesPresenceOrSystemPresentation() {
        val adapter = connectedAdapter(ConnectedChatClient(), IdleFileTransferServer())
        try {
            val text = "[system] bob left the chat"
            adapter.chatEventPublisher.publish(com.shterneregen.securelan.chat.event.ChatMessageReceivedEvent("mallory", text))
            val row = adapter.chatMessages.last()
            val presentation = com.shterneregen.securelan.desktop.compose.state.chat.ComposeChatTranscriptLinePresentation.from(row)
            assertEquals(text, row.text)
            assertEquals(text, presentation.body)
            assertEquals(com.shterneregen.securelan.desktop.compose.state.chat.ComposeChatTranscriptLineKind.USER_REMOTE, presentation.kind)
            assertFalse(adapter.chatPeers.any { it.nickname() == "bob" })
        } finally { adapter.shutdown() }
    }

    @Test fun disconnectPathsCloseRealtimeResources() {
        var closes = 0
        val rtc = object : com.shterneregen.securelan.webrtc.service.RtcSessionService {
            override fun runtimeStatus() = com.shterneregen.securelan.webrtc.runtime.RtcRuntimeStatus.unavailable("test")
            override fun currentSession() = Optional.empty<com.shterneregen.securelan.webrtc.service.RtcSessionSnapshot>()
            override fun startSession(request: com.shterneregen.securelan.webrtc.service.RtcSessionRequest): com.shterneregen.securelan.webrtc.service.RtcSessionSnapshot = error("unused")
            override fun acceptInboundSignal(localPeer: String?, signal: RtcSignalEnvelope?) = Unit
            override fun sendDataMessage(payload: String?) = Unit
            override fun closeCurrentSession() { closes++ }
        }
        val adapter = connectedAdapter(ConnectedChatClient(), IdleFileTransferServer(), rtc = rtc)
        try {
            val before = closes
            adapter.chatEventPublisher.publish(ChatDisconnectedEvent("desktop-test", ChatDisconnectReasons.CONNECTION_LOST))
            assertTrue(closes > before)
            val afterEvent = closes
            adapter.disconnect()
            assertTrue(closes > afterEvent)
            val afterDisconnect = closes
            adapter.stopHosting()
            assertTrue(closes > afterDisconnect)
        } finally { adapter.shutdown() }
    }

    @Test fun onlyCurrentClientPresenceAuthorizesFilesAndOldReceiversCannotReuseIt() {
        val client = ConnectedChatClient()
        val files = TrackingFileTransferServer()
        val adapter = connectedAdapter(client, files)
        val peer = com.shterneregen.securelan.crypto.protocol.PeerIdentity()
        val payload = "proof".toByteArray()
        val capabilities = com.shterneregen.securelan.chat.protocol.handshake.PeerCapabilities.desktop("test", 5051).withIdentity(peer.publicKey)
        try {
            adapter.connect("127.0.0.1", "desktop-test", "password", 50_050, 50_051)
            val first = files.started.poll(2, TimeUnit.SECONDS)!!
            awaitConnected(adapter)
            adapter.chatEventPublisher.publish(com.shterneregen.securelan.chat.event.ChatUserJoinedEvent("bob", "127.0.0.1:1234", capabilities, serverSide = true))
            org.junit.jupiter.api.Assertions.assertThrows(java.io.IOException::class.java) { first.authentication.verify("bob", payload, peer.sign(payload)) }
            adapter.chatEventPublisher.publish(com.shterneregen.securelan.chat.event.ChatUserJoinedEvent("bob", "", capabilities))
            first.authentication.verify("bob", payload, peer.sign(payload))
            adapter.disconnect()
            adapter.connect("127.0.0.1", "desktop-test", "password", 50_060, 50_061)
            val second = files.started.poll(2, TimeUnit.SECONDS)!!
            awaitConnected(adapter)
            adapter.chatEventPublisher.publish(com.shterneregen.securelan.chat.event.ChatUserJoinedEvent("bob", "", capabilities))
            second.authentication.verify("bob", payload, peer.sign(payload))
            org.junit.jupiter.api.Assertions.assertThrows(java.io.IOException::class.java) { first.authentication.verify("bob", payload, peer.sign(payload)) }
        } finally { adapter.shutdown() }
    }

    @Test
    fun transferUpdatesShouldPublishIndependentStatesForProgressCompletionAndFailure() {
        val adapter = ComposeDesktopHostAdapter(
            chatServerService = IdleChatServer(),
            chatClientService = BlockingChatClient(CountDownLatch(0), CountDownLatch(0)),
            fileTransferServerService = IdleFileTransferServer(),
            quickShareService = IdleQuickShareService(),
            discoveryService = IdlePeerDiscovery(),
            randomNicknameService = object : RandomNicknameService {
                override fun generate(): String = "desktop-test"
            },
            uiStateDispatcher = { action -> action() },
        )
        fun panelState() = ComposeFileTransferState(
            statusState = adapter.statusState,
            peerListState = ComposePeerListState(peers = emptyList()),
            entries = adapter.transferEntries,
        )

        try {
            for (outgoing in listOf(true, false)) {
                val id = "snapshot-$outgoing"
                adapter.fileTransferEventPublisher.publish(FileTransferStartedEvent(id, "file.txt", 100, outgoing))
                val started = panelState()
                adapter.fileTransferEventPublisher.publish(FileTransferProgressEvent(
                    id, FileTransferProgress(id, 50, 100, TransferStatus.IN_PROGRESS), outgoing,
                ))
                val progressing = panelState()

                assertNotEquals(started, progressing, "Nested panel state must change, not just the list container")
                assertEquals(0, started.entries.last().percent)
                assertEquals(50, progressing.activeEntryRows.single().percent)

                adapter.fileTransferEventPublisher.publish(FileTransferCompletedEvent(id, "file.txt", null, 100, outgoing))
                val completed = panelState()
                assertNotEquals(progressing, completed)
                assertTrue(progressing.entries.last().active())
                assertTrue(completed.activeEntryRows.isEmpty())
                assertTrue(completed.recentEntryRows.last().completed)
                assertEquals(100, completed.recentEntryRows.last().percent)
                assertFalse(completed.requiresAttention)

                adapter.fileTransferEventPublisher.publish(FileTransferProgressEvent(
                    id, FileTransferProgress(id, 75, 100, TransferStatus.IN_PROGRESS), outgoing,
                ))
                assertTrue(panelState().activeEntryRows.isEmpty(), "Late progress must not restart completed transfers")
            }

            adapter.fileTransferEventPublisher.publish(FileTransferStartedEvent("failed", "broken.txt", 100, true))
            val beforeFailure = panelState()
            adapter.fileTransferEventPublisher.publish(FileTransferFailedEvent("failed", "broken.txt", "test", null, true))
            val failed = panelState()
            assertNotEquals(beforeFailure, failed)
            assertTrue(beforeFailure.entries.last().active())
            assertTrue(failed.activeEntryRows.isEmpty())
            assertTrue(failed.recentEntryRows.last().failed)
        } finally {
            adapter.shutdown()
        }
    }

    @Test
    fun transferLifecycleShouldUpdateOneTranscriptMessagePerFile() {
        val adapter = ComposeDesktopHostAdapter(
            chatServerService = IdleChatServer(),
            chatClientService = BlockingChatClient(CountDownLatch(0), CountDownLatch(0)),
            fileTransferServerService = IdleFileTransferServer(),
            quickShareService = IdleQuickShareService(),
            discoveryService = IdlePeerDiscovery(),
            randomNicknameService = object : RandomNicknameService {
                override fun generate(): String = "desktop-test"
            },
            uiStateDispatcher = { action -> action() },
        )
        val downloadedFile = Path.of("downloads", "report.pdf").toAbsolutePath().normalize()

        try {
            adapter.fileTransferEventPublisher.publish(FileTransferStartedEvent("receive-1", "report.pdf", 100, false))
            adapter.fileTransferEventPublisher.publish(FileTransferCompletedEvent("receive-1", "report.pdf", downloadedFile, 100, false))
            adapter.fileTransferEventPublisher.publish(FileTransferStartedEvent("send-1", "archive.zip", 200, true))
            adapter.fileTransferEventPublisher.publish(FileTransferCompletedEvent("send-1", "archive.zip", downloadedFile, 200, true))

            assertEquals(2, adapter.chatMessages.size)
            assertEquals("Received report.pdf", adapter.chatMessages[0].displayText)
            assertEquals(downloadedFile, adapter.chatMessages[0].actionPath)
            assertEquals("Sent archive.zip", adapter.chatMessages[1].displayText)
            assertEquals(null, adapter.chatMessages[1].actionPath)
            assertEquals(adapter.chatMessages.map { it.displayText }, adapter.chatTranscript)
        } finally {
            adapter.shutdown()
        }
    }

    @Test
    fun connectShouldNotBlockTheCallingThread() {
        val connectEntered = CountDownLatch(1)
        val releaseConnect = CountDownLatch(1)
        val adapter = ComposeDesktopHostAdapter(
            chatServerService = IdleChatServer(),
            chatClientService = BlockingChatClient(connectEntered, releaseConnect),
            fileTransferServerService = IdleFileTransferServer(),
            quickShareService = IdleQuickShareService(),
            discoveryService = IdlePeerDiscovery(),
            randomNicknameService = object : RandomNicknameService {
                override fun generate(): String = "desktop-test"
            },
            uiStateDispatcher = { action -> action() },
        )

        try {
            CompletableFuture.delayedExecutor(1, TimeUnit.SECONDS).execute { releaseConnect.countDown() }
            val elapsedMillis = measureTimeMillis {
                adapter.connect("127.0.0.1", "desktop-test", "secret", 50_050, 50_051)
            }

            assertTrue(connectEntered.await(500, TimeUnit.MILLISECONDS), "The IO connection task should start")
            assertTrue(elapsedMillis < 500, "connect() blocked its caller for ${elapsedMillis}ms")
            assertTrue(awaitConnectFailure(adapter), "The asynchronous failure should be published")
        } finally {
            releaseConnect.countDown()
            adapter.shutdown()
        }
    }

    @Test fun blockedLocalJoinDoesNotBlockHostActionAndShutdownRollsItBack() {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val closed = CountDownLatch(1)
        val client = BlockingChatClient(entered, release)
        var running = false
        val server = object : ChatServerService {
            override fun start(config: ChatServerConfig) { running = true }
            override fun stop() { running = false; closed.countDown() }
            override fun isRunning() = running
            override fun connectedUsers() = 0
        }
        val adapter = connectedAdapter(client, TrackingFileTransferServer(), server = server)
        try {
            assertTrue(measureTimeMillis { adapter.openRoom("alice", "password", 5050, 5051, true) } < 500)
            assertTrue(entered.await(2, TimeUnit.SECONDS))
            assertTrue(adapter.statusState.operationInProgress)
            assertFalse(adapter.statusState.canOpenRoom)
            adapter.shutdown()
            release.countDown()
            assertTrue(closed.await(2, TimeUnit.SECONDS))
            assertFalse(running)
        } finally { release.countDown(); adapter.shutdown() }
    }

    @Test fun blockedMessageWriteDoesNotBlockUiAndReturnsCorrelatedFailure() {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val results = LinkedBlockingQueue<Pair<String, com.shterneregen.securelan.chat.service.ChatSendOutcome>>()
        val client = ConnectedChatClient {
            entered.countDown(); release.await()
            com.shterneregen.securelan.chat.service.ChatSendOutcome.FAILED
        }
        val files = TrackingFileTransferServer()
        val adapter = connectedAdapter(client, files)
        try {
            adapter.connect("127.0.0.1", "alice", "password", 5050, 5051)
            assertNotEquals(null, files.started.poll(2, TimeUnit.SECONDS))
            awaitConnected(adapter)
            assertTrue(measureTimeMillis { adapter.sendMessage("keep this draft") { id, outcome -> results.add(id to outcome) } } < 500)
            assertTrue(entered.await(2, TimeUnit.SECONDS))
            assertTrue(adapter.sendingMessage)
            release.countDown()
            val result = results.poll(2, TimeUnit.SECONDS)!!
            assertTrue(result.first.isNotBlank())
            assertEquals(com.shterneregen.securelan.chat.service.ChatSendOutcome.FAILED, result.second)
            assertFalse(adapter.sendingMessage)
        } finally { release.countDown(); adapter.shutdown() }
    }

    @Test fun asyncStegoCompletionPreservesTheToolDraft(@org.junit.jupiter.api.io.TempDir directory: Path) {
        val image = java.awt.image.BufferedImage(32, 32, java.awt.image.BufferedImage.TYPE_INT_RGB)
        val cover = directory.resolve("cover.bmp")
        assertTrue(javax.imageio.ImageIO.write(image, "bmp", cover.toFile()))
        val output = directory.resolve("hidden.bmp")
        val completed = LinkedBlockingQueue<Path>()
        val adapter = connectedAdapter(ConnectedChatClient(), IdleFileTransferServer())
        try {
            adapter.hideStegoMessage(cover, output, "keep this draft") { completed.add(it) }
            assertEquals(output, completed.poll(2, TimeUnit.SECONDS))
            assertEquals("keep this draft", adapter.stegoState.messageDraft)
            assertEquals(cover.toString(), adapter.stegoState.coverPathText)
            assertFalse(adapter.stegoState.busy)
            adapter.extractStegoMessage(output)
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2)
            while (adapter.stegoState.extractedMessage.isEmpty() && System.nanoTime() < deadline) Thread.sleep(10)
            assertEquals("keep this draft", adapter.stegoState.extractedMessage)
        } finally { adapter.shutdown() }
    }

    private fun awaitConnected(adapter: ComposeDesktopHostAdapter) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2)
        while (System.nanoTime() < deadline) {
            if (adapter.statusState.clientConnected && !adapter.statusState.operationInProgress) return
            Thread.sleep(10)
        }
        assertTrue(adapter.statusState.clientConnected && !adapter.statusState.operationInProgress, "Room startup did not complete")
    }

    private fun awaitConnectFailure(adapter: ComposeDesktopHostAdapter): Boolean {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2)
        while (System.nanoTime() < deadline) {
            if (adapter.adapterEvents.any { it.message.startsWith("[connect failed]") }) return true
            Thread.sleep(20L)
        }
        return adapter.adapterEvents.any { it.message.startsWith("[connect failed]") }
    }

    private class BlockingChatClient(
        private val entered: CountDownLatch,
        private val release: CountDownLatch,
    ) : ChatClientService {
        override fun fileIdentity() = com.shterneregen.securelan.crypto.protocol.PeerIdentity()
        override fun connect(request: ChatClientConnectRequest): Boolean {
            entered.countDown()
            release.await(2, TimeUnit.SECONDS)
            return false
        }

        override fun disconnect() = Unit
        override fun sendMessage(text: String?) = com.shterneregen.securelan.chat.service.ChatSendOutcome.WRITTEN
        override fun sendSignal(signal: RtcSignalEnvelope?) = Unit
        override fun isConnected(): Boolean = false
    }

    private class IdleChatServer(private val running: Boolean = false) : ChatServerService {
        override fun start(config: ChatServerConfig) = Unit
        override fun stop() = Unit
        override fun isRunning(): Boolean = running
        override fun connectedUsers(): Int = 0
    }

    private class ConnectedChatClient(private val write: () -> com.shterneregen.securelan.chat.service.ChatSendOutcome = { com.shterneregen.securelan.chat.service.ChatSendOutcome.WRITTEN }) : ChatClientService {
        @Volatile private var connected = false
        private val identity = com.shterneregen.securelan.crypto.protocol.PeerIdentity()
        override fun fileIdentity() = identity

        override fun connect(request: ChatClientConnectRequest): Boolean {
            connected = true
            return true
        }
        override fun disconnect() { connected = false }
        override fun sendMessage(text: String?) = write()
        override fun sendSignal(signal: RtcSignalEnvelope?) = Unit
        override fun isConnected(): Boolean = connected
    }

    private class TrackingFileTransferServer : FileTransferServerService {
        val started = LinkedBlockingQueue<FileTransferServerConfig>()
        @Volatile private var running = false

        override fun start(config: FileTransferServerConfig) {
            running = true
            started.add(config)
        }
        override fun stop() { running = false }
        override fun isRunning(): Boolean = running
    }

    private class IdleFileTransferServer : FileTransferServerService {
        override fun start(config: FileTransferServerConfig) = Unit
        override fun stop() = Unit
        override fun isRunning(): Boolean = false
    }

    private class IdlePeerDiscovery : PeerDiscoveryService {
        override fun start(config: PeerDiscoveryConfig, listener: PeerDiscoveryListener) = Unit
        override fun stop() = Unit
        override fun setAnnounceEnabled(announceEnabled: Boolean) = Unit
        override fun isRunning(): Boolean = false
        override fun snapshot(): List<DiscoveredPeer> = emptyList()
    }

    private class IdleQuickShareService(private val shared: QuickShareSnapshot? = null) : QuickShareService {
        override fun start(config: QuickShareServerConfig) = Unit
        override fun stop() = Unit
        override fun isRunning(): Boolean = false
        override fun port(): Int = -1
        override fun share(request: QuickShareCreateRequest): QuickShareSnapshot = shared ?: error("Not used")
        override fun findShare(id: String): Optional<QuickShareSnapshot> = Optional.empty()
        override fun shares(): List<QuickShareSnapshot> = emptyList()
        override fun stopShare(id: String): Boolean = shared?.id() == id
        override fun landingUrls(): List<String> = emptyList()
    }
}
