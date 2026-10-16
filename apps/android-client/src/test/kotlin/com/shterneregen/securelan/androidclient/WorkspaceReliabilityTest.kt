package com.shterneregen.securelan.androidclient

import android.content.SharedPreferences
import androidx.lifecycle.ViewModelStore
import com.shterneregen.securelan.androidclient.call.*
import com.shterneregen.securelan.androidclient.model.*
import com.shterneregen.securelan.androidclient.network.*
import com.shterneregen.securelan.androidclient.protocol.WireMessage
import com.shterneregen.securelan.chat.protocol.handshake.PeerCapabilities
import com.shterneregen.securelan.common.model.rtc.RtcSignalEnvelope
import com.shterneregen.securelan.crypto.protocol.PeerIdentity
import com.shterneregen.securelan.filetransfer.protocol.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Proxy

@OptIn(ExperimentalCoroutinesApi::class)
class WorkspaceReliabilityTest {
    private class Chat : RoomClient {
        override val fileIdentity = PeerIdentity()
        val sent = mutableListOf<String>()
        val sendGate = CompletableDeferred<Unit>()
        override suspend fun connect(host: String, port: Int, requestedNickname: String, sessionPassword: String, capabilities: PeerCapabilities) = requestedNickname
        override suspend fun sendMessage(text: String) { sent += text; sendGate.await() }
        override suspend fun readMessage(): WireMessage? = awaitCancellation()
        override suspend fun sendSignal(signal: RtcSignalEnvelope) = Unit
        override suspend fun disconnect() = Unit
        override fun close() = Unit
    }
    private class Receiver : IncomingFileReceiver {
        val oldFinally = CompletableDeferred<Unit>()
        var starts = 0
        val diagnostics = mutableListOf<(String, Throwable?) -> Unit>()
        val progress = mutableListOf<(FileTransferMetadata, Long) -> Unit>()
        val started = mutableListOf<(FileTransferMetadata) -> Unit>()
        override fun close() = Unit
        override suspend fun serve(port: Int, sessionPassword: String, onStarted: (FileTransferMetadata) -> Unit,
            onProgress: (FileTransferMetadata, Long) -> Unit, onCompleted: (FileTransferMetadata, SecureFileReceiver.ReceivedFile) -> Unit,
            onError: (String, Throwable?) -> Unit, authentication: FileTransferPeerAuthentication,
            onTransferError: (FileTransferMetadata, String, Throwable?) -> Unit) {
            val attempt = ++starts
            diagnostics += onError; progress += onProgress; started += onStarted
            try { awaitCancellation() }
            finally { if (attempt == 1) withContext(NonCancellable) { oldFinally.await() } }
        }
    }

    @Test fun sendingCapturedDraftKeepsNewRevisionAndSuppressesRepeatedClick() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val chat = Chat()
        val store = ViewModelStore()
        try {
            val model = workspace(store, chat, Receiver())
            model.updateAutoReceiveFiles(false)
            model.updateNickname("alice")
            model.updateSessionPassword("password")
            model.selectPeer(DiscoveredPeer("peer", "host", "127.0.0.1", 5050, 5051))
            model.connectSelectedPeer()
            runCurrent()
            assertTrue(model.uiState.value.connected)
            model.updateInputMessage("first")
            model.sendTextMessage(); model.sendTextMessage()
            runCurrent()
            assertEquals(listOf("first"), chat.sent)
            model.updateInputMessage("second")
            chat.sendGate.complete(Unit)
            runCurrent()
            assertEquals("second", model.uiState.value.inputMessage)
        } finally { store.clear(); runCurrent(); Dispatchers.resetMain() }
    }

    @Test fun oldReceiverCompletionAndCallbacksCannotOverwriteNewRunOrActiveTransfer() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val receiver = Receiver()
        val store = ViewModelStore()
        try {
            val model = workspace(store, Chat(), receiver)
            model.updateSessionPassword("password")
            model.startFileReceiver(); runCurrent()
            model.stopFileReceiver(); runCurrent()
            model.startFileReceiver(); runCurrent()
            assertEquals(2, receiver.starts)
            receiver.oldFinally.complete(Unit); runCurrent()
            model.startFileReceiver(); runCurrent()
            assertEquals("old completion must not clear new job handle", 2, receiver.starts)
            val metadata = FileTransferMetadata("A", "sender", "recipient", "active.bin", 100)
            receiver.started[1](metadata)
            receiver.progress[1](metadata, 50)
            receiver.progress[1](metadata.copy(transferId = "unrelated"), 1)
            receiver.diagnostics[1]("another handshake failed", null)
            assertTrue(model.uiState.value.incomingFileProgress.active)
            assertEquals(50L, model.uiState.value.incomingFileProgress.bytesReceived)
            assertTrue(model.uiState.value.recentTransfers.isEmpty())
            receiver.started[0](metadata.copy(fileName = "old.bin"))
            receiver.progress[0](metadata, 1)
            assertEquals("active.bin", model.uiState.value.incomingFileProgress.fileName)
            assertEquals(50L, model.uiState.value.incomingFileProgress.bytesReceived)
            model.stopFileReceiver(); runCurrent()
            assertFalse(model.uiState.value.fileReceiverRunning)
        } finally { receiver.oldFinally.complete(Unit); store.clear(); runCurrent(); Dispatchers.resetMain() }
    }

    @Test fun discoveryExpiresMissingPeersAndRetainsRenewedAndManualEntries() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val announcements = MutableSharedFlow<DiscoveredPeer>(extraBufferCapacity = 8)
        val store = ViewModelStore()
        try {
            val model = workspace(store, Chat(), Receiver(), { announcements }, { testScheduler.currentTime })
            model.updateNearbyPermission(NearbyPermissionState.GRANTED); runCurrent()
            announcements.emit(DiscoveredPeer("gone", "gone", "127.0.0.2", 5050, 5051))
            announcements.emit(DiscoveredPeer("alive", "alive", "127.0.0.3", 5050, 5051))
            runCurrent(); advanceTimeBy(10_000)
            announcements.emit(DiscoveredPeer("alive", "alive", "127.0.0.3", 5050, 5051))
            runCurrent(); advanceTimeBy(36_000); runCurrent()
            assertEquals(listOf("alive"), model.uiState.value.peers.map { it.peerId })
            model.updateAutoReceiveFiles(false)
            model.connectManualPeer("127.0.0.5", "5050", "5051")
            runCurrent()
            model.stopDiscovery()
            assertEquals(listOf("manual:127.0.0.5:5050"), model.uiState.value.peers.map { it.peerId })
        } finally { store.clear(); runCurrent(); Dispatchers.resetMain() }
    }

    private fun workspace(store: ViewModelStore, chat: Chat, receiver: Receiver,
        discover: () -> Flow<DiscoveredPeer> = { emptyFlow() }, now: () -> Long = { 0 }): MainViewModel {
        val values = mutableMapOf<String, Any?>()
        val editor = Proxy.newProxyInstance(javaClass.classLoader, arrayOf(SharedPreferences.Editor::class.java)) { proxy, method, args ->
            when { method.name.startsWith("put") -> { values[args!![0] as String] = args[1]; proxy }; method.name == "commit" -> true; method.name == "apply" -> null; else -> proxy }
        } as SharedPreferences.Editor
        val preferences = Proxy.newProxyInstance(javaClass.classLoader, arrayOf(SharedPreferences::class.java)) { _, method, args ->
            when (method.name) { "edit" -> editor; "getString", "getInt", "getBoolean" -> values[args!![0]] ?: args[1]; else -> null }
        } as SharedPreferences
        val calls = Proxy.newProxyInstance(javaClass.classLoader, arrayOf(CallSession::class.java)) { _, method, _ ->
            when (method.name) { "getState" -> MutableStateFlow(CallState()); "getUiState" -> MutableStateFlow(CallUiState()); else -> null }
        } as CallSession
        val host = object : HostRoom {
            override val state = MutableStateFlow(AndroidHostServiceState())
            override fun password() = "password"
            override fun start(peerId: String, nickname: String, password: String, chatPort: Int, filePort: Int) = Unit
            override fun stop() = Unit
        }
        return MainViewModel(MainDependencies(preferences, chat, discover, { emptyList() }, host, { _, _, _, _ -> calls },
            createReceiver = { receiver }, monotonicMillis = now)).also { store.put("workspace", it) }
    }
}
