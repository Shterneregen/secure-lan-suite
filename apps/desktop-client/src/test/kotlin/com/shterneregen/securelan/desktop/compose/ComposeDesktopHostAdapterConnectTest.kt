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
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareCreateRequest
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareServerConfig
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareService
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareSnapshot
import com.shterneregen.securelan.filetransfer.service.FileTransferServerConfig
import com.shterneregen.securelan.filetransfer.service.FileTransferServerService
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

class ComposeDesktopHostAdapterConnectTest {
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
