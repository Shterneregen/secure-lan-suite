package com.shterneregen.securelan.desktop.compose

import com.shterneregen.securelan.chat.discovery.DiscoveredPeer
import com.shterneregen.securelan.chat.discovery.PeerDiscoveryConfig
import com.shterneregen.securelan.chat.discovery.PeerDiscoveryListener
import com.shterneregen.securelan.chat.discovery.PeerDiscoveryService
import com.shterneregen.securelan.chat.service.ChatClientConnectRequest
import com.shterneregen.securelan.chat.service.ChatClientService
import com.shterneregen.securelan.chat.service.ChatServerConfig
import com.shterneregen.securelan.chat.service.ChatServerService
import com.shterneregen.securelan.chat.service.RandomNicknameService
import com.shterneregen.securelan.common.model.rtc.RtcSignalEnvelope
import com.shterneregen.securelan.common.model.FileTransferProgress
import com.shterneregen.securelan.common.model.TransferStatus
import com.shterneregen.securelan.desktop.compose.state.transfer.ComposeFileTransferState
import com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListState
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareCreateRequest
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareServerConfig
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareService
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareSnapshot
import com.shterneregen.securelan.filetransfer.event.FileTransferCompletedEvent
import com.shterneregen.securelan.filetransfer.event.FileTransferStartedEvent
import com.shterneregen.securelan.filetransfer.event.FileTransferProgressEvent
import com.shterneregen.securelan.filetransfer.event.FileTransferFailedEvent
import com.shterneregen.securelan.filetransfer.service.FileTransferServerConfig
import com.shterneregen.securelan.filetransfer.service.FileTransferServerService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.util.Optional
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

class ComposeDesktopHostAdapterConnectTest {
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
            assertEquals("[transfer] Received report.pdf", adapter.chatMessages[0].displayText)
            assertEquals(downloadedFile, adapter.chatMessages[0].actionPath)
            assertEquals("[transfer] Sent archive.zip", adapter.chatMessages[1].displayText)
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
        override fun connect(request: ChatClientConnectRequest): Boolean {
            entered.countDown()
            release.await(2, TimeUnit.SECONDS)
            return false
        }

        override fun disconnect() = Unit
        override fun sendMessage(text: String?) = Unit
        override fun sendSignal(signal: RtcSignalEnvelope?) = Unit
        override fun isConnected(): Boolean = false
    }

    private class IdleChatServer : ChatServerService {
        override fun start(config: ChatServerConfig) = Unit
        override fun stop() = Unit
        override fun isRunning(): Boolean = false
        override fun connectedUsers(): Int = 0
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

    private class IdleQuickShareService : QuickShareService {
        override fun start(config: QuickShareServerConfig) = Unit
        override fun stop() = Unit
        override fun isRunning(): Boolean = false
        override fun port(): Int = -1
        override fun share(request: QuickShareCreateRequest): QuickShareSnapshot = error("Not used")
        override fun findShare(id: String): Optional<QuickShareSnapshot> = Optional.empty()
        override fun shares(): List<QuickShareSnapshot> = emptyList()
        override fun stopShare(id: String): Boolean = false
        override fun landingUrls(): List<String> = emptyList()
    }
}
