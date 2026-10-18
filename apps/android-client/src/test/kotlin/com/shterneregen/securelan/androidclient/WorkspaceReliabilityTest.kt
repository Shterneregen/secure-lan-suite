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
        var connects = 0
        var lastPassword: String? = null
        val sent = mutableListOf<String>()
        val sendGate = CompletableDeferred<Unit>()
        override suspend fun connect(host: String, port: Int, requestedNickname: String, sessionPassword: String, capabilities: PeerCapabilities): String { connects++; lastPassword = sessionPassword; return requestedNickname }
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
            model.updateNearbyPermission(NearbyPermissionState.GRANTED)
            model.restartDiscovery(); runCurrent()
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

    @Test fun uriImportDoesNotConnectOrReusePasswordAndCancelPreservesWorkspace() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        val chat = Chat()
        try {
            val model = workspace(store, chat, Receiver())
            model.updateSessionPassword("previous-room-password")
            model.updateInputMessage("draft")
            model.importConnectionUri("securelan://join?v=1&proto=2&host=192.168.1.42&port=5050")
            runCurrent()
            assertNotNull(model.uiState.value.pendingConnection)
            assertEquals("", model.uiState.value.importedRoomPassword)
            assertEquals(0, chat.connects)
            model.importConnectionUri("https://example.com")
            assertNotNull(model.uiState.value.connectionImportError)
            assertNotNull(model.uiState.value.pendingConnection)
            model.dismissConnectionImport()
            assertEquals("previous-room-password", model.uiState.value.sessionPassword)
            assertEquals("draft", model.uiState.value.inputMessage)
            assertEquals(0, chat.connects)
        } finally { store.clear(); runCurrent(); Dispatchers.resetMain() }
    }

    @Test fun unifiedSearchUsesProviderLeaseAndExplicitStopSurvivesLifecycle() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        val snapshots = MutableSharedFlow<com.shterneregen.securelan.chat.discovery.DiscoverySnapshot>(extraBufferCapacity = 8)
        try {
            val model = workspace(store, Chat(), Receiver(), rooms = { snapshots })
            model.updateNearbyPermission(NearbyPermissionState.GRANTED)
            model.restartDiscovery(); runCurrent()
            val endpoint = com.shterneregen.securelan.chat.discovery.DiscoveryEndpoint(com.shterneregen.securelan.common.net.address.IpLiteral.parse("192.168.1.42")!!, 5050)
            val room = com.shterneregen.securelan.chat.discovery.DiscoveredRoom("one", "remote", null, "Room", listOf(endpoint), null,
                setOf(com.shterneregen.securelan.chat.discovery.DiscoveryProviderId.MDNS))
            snapshots.emit(com.shterneregen.securelan.chat.discovery.DiscoverySnapshot(listOf(room))); runCurrent()
            advanceTimeBy(46_000); runCurrent()
            assertEquals(1, model.uiState.value.peers.size)
            assertNull(model.uiState.value.peers.single().advertisedFilePort)
            model.stopDiscovery(); runCurrent()
            assertTrue(model.uiState.value.peers.isEmpty())
            model.updateSearchForeground(false); model.updateSearchForeground(true); runCurrent()
            assertFalse(model.uiState.value.discoveryRunning)
            model.restartDiscovery(); runCurrent()
            assertTrue(model.uiState.value.discoveryRunning)
            advanceTimeBy(60_001); runCurrent()
            assertFalse(model.uiState.value.discoveryRunning)
        } finally { store.clear(); runCurrent(); Dispatchers.resetMain() }
    }

    @Test fun emptyDiscoverySnapshotKeepsSearchingUntilTheDefaultWindowEnds() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        val snapshots = MutableSharedFlow<com.shterneregen.securelan.chat.discovery.DiscoverySnapshot>(extraBufferCapacity = 8)
        try {
            val model = workspace(store, Chat(), Receiver(), rooms = { snapshots })
            assertFalse(model.uiState.value.autoDiscoveryEnabled)
            model.updateNearbyPermission(NearbyPermissionState.GRANTED); runCurrent()
            assertFalse(model.uiState.value.discoveryRunning)
            model.restartDiscovery(); runCurrent()
            snapshots.emit(com.shterneregen.securelan.chat.discovery.DiscoverySnapshot()); runCurrent()
            advanceTimeBy(8_001); runCurrent()
            assertTrue(model.uiState.value.discoveryRunning)
            assertFalse(model.uiState.value.discoveryTimedOut)
            advanceTimeBy(52_000); runCurrent()
            assertFalse(model.uiState.value.discoveryRunning)
            assertTrue(model.uiState.value.discoveryTimedOut)
        } finally { store.clear(); runCurrent(); Dispatchers.resetMain() }
    }

    @Test fun automaticSearchRespectsSurfaceForegroundNetworkAndExplicitStop() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            val model = workspace(store, Chat(), Receiver(), rooms = { flow { awaitCancellation() } })
            model.updateNearbyPermission(NearbyPermissionState.GRANTED)
            model.updateAutoDiscovery(true); runCurrent()
            advanceTimeBy(120_001); runCurrent()
            assertTrue(model.uiState.value.discoveryRunning)
            model.updateSearchSurface(false); runCurrent()
            assertFalse(model.uiState.value.discoveryRunning)
            model.updateSearchSurface(true); runCurrent()
            assertTrue(model.uiState.value.discoveryRunning)
            model.updateSearchForeground(false); runCurrent()
            assertFalse(model.uiState.value.discoveryRunning)
            model.updateSearchForeground(true); runCurrent()
            assertTrue(model.uiState.value.discoveryRunning)
            model.updateNetworkAvailable(false); runCurrent()
            model.restartDiscovery(); runCurrent()
            assertFalse(model.uiState.value.discoveryRunning)
            model.updateNetworkAvailable(true); runCurrent()
            assertTrue(model.uiState.value.discoveryRunning)
            model.stopDiscovery(); runCurrent()
            model.updateSearchForeground(false); model.updateSearchForeground(true)
            model.updateNetworkAvailable(false); model.updateNetworkAvailable(true); runCurrent()
            advanceTimeBy(120_001); runCurrent()
            assertFalse(model.uiState.value.discoveryRunning)
            assertFalse(model.uiState.value.autoDiscoveryEnabled)
            model.restartDiscovery(); runCurrent()
            assertTrue(model.uiState.value.discoveryRunning)
            model.updateAutoDiscovery(true)
            model.updateAutoDiscovery(false)
            assertFalse(model.uiState.value.discoveryRunning)
            advanceTimeBy(60_001); runCurrent()
            assertFalse(model.uiState.value.discoveryRunning)
        } finally { store.clear(); runCurrent(); Dispatchers.resetMain() }
    }

    @Test fun manualSearchDoesNotEnableAutomaticSearchOrResumeAfterLifecycleCallbacks() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        var searches = 0
        try {
            val model = workspace(store, Chat(), Receiver(), rooms = { flow { searches++; awaitCancellation() } })
            model.updateNearbyPermission(NearbyPermissionState.GRANTED)
            model.updateSearchSurface(false); model.updateSearchSurface(true)
            model.updateSearchForeground(false); model.updateSearchForeground(true)
            model.updateNetworkAvailable(false); model.updateNetworkAvailable(true)
            runCurrent()
            assertEquals(0, searches)
            assertFalse(model.uiState.value.discoveryRunning)
            model.restartDiscovery(); runCurrent()
            assertTrue(model.uiState.value.discoveryRunning)
            assertFalse(model.uiState.value.autoDiscoveryEnabled)
            assertEquals(1, searches)
            model.updateSearchSurface(false); model.updateSearchSurface(true)
            model.updateSearchForeground(false); model.updateSearchForeground(true)
            model.updateNetworkAvailable(false); model.updateNetworkAvailable(true)
            model.updateNearbyPermission(NearbyPermissionState.DENIED)
            model.updateNearbyPermission(NearbyPermissionState.GRANTED)
            runCurrent()
            assertFalse(model.uiState.value.discoveryRunning)
            assertEquals(1, searches)
            model.restartDiscovery(); runCurrent()
            assertTrue(model.uiState.value.discoveryRunning)
            advanceTimeBy(60_001); runCurrent()
            assertFalse(model.uiState.value.discoveryRunning)
            assertFalse(model.uiState.value.autoDiscoveryEnabled)
            assertEquals(2, searches)
        } finally { store.clear(); runCurrent(); Dispatchers.resetMain() }
    }

    @Test fun disablingAutomaticSearchCancelsAQueuedRestartWhileTheProviderIsClosing() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        val closed = CompletableDeferred<Unit>()
        var searches = 0
        try {
            val model = workspace(store, Chat(), Receiver(), rooms = { flow {
                searches++
                try { awaitCancellation() } finally { withContext(NonCancellable) { closed.await() } }
            } })
            model.updateNearbyPermission(NearbyPermissionState.GRANTED)
            model.updateAutoDiscovery(true); runCurrent()
            assertEquals(1, searches)
            model.updateAutoDiscovery(false)
            assertFalse(model.uiState.value.discoveryRunning)
            model.updateAutoDiscovery(true); runCurrent()
            assertEquals(1, searches)
            model.updateAutoDiscovery(false)
            closed.complete(Unit); runCurrent()
            assertFalse(model.uiState.value.discoveryRunning)
            assertEquals(1, searches)
            model.updateAutoDiscovery(true); runCurrent()
            assertTrue(model.uiState.value.discoveryRunning)
            assertEquals(2, searches)
        } finally { closed.complete(Unit); store.clear(); runCurrent(); Dispatchers.resetMain() }
    }

    @Test fun enablingAutomaticSearchRestartsAPausedSearchWithoutChangingWorkspace() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            val model = workspace(store, Chat(), Receiver(), rooms = { flow { awaitCancellation() } })
            model.updateNearbyPermission(NearbyPermissionState.GRANTED); runCurrent()
            model.updateSessionPassword("room-password")
            model.updateInputMessage("unsent message")
            model.stopDiscovery(); runCurrent()
            model.updateAutoDiscovery(true); runCurrent()
            assertTrue(model.uiState.value.discoveryRunning)
            assertEquals("room-password", model.uiState.value.sessionPassword)
            assertEquals("unsent message", model.uiState.value.inputMessage)
        } finally { store.clear(); runCurrent(); Dispatchers.resetMain() }
    }

    @Test fun importedPasswordIsPrefilledFiltersWhitespaceAndSurvivesAddressSelectionUntilExplicitJoin() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        val chat = Chat()
        try {
            val model = workspace(store, chat, Receiver())
            model.updateAutoReceiveFiles(false)
            model.updateSessionPassword("previous-room-password")
            model.importConnectionUri("securelan://join?v=1&proto=2&host=192.168.1.42&port=5050&alt=192.168.1.43&password=invite%2B%26%2525")
            runCurrent()
            assertEquals("invite+&%25", model.uiState.value.importedRoomPassword)
            assertEquals("previous-room-password", model.uiState.value.sessionPassword)
            assertEquals(0, chat.connects)
            model.updateImportedRoomPassword(" new\tpass\n\u00a0word ")
            assertEquals("newpassword", model.uiState.value.importedRoomPassword)
            model.chooseImportedAddress(com.shterneregen.securelan.common.net.address.IpLiteral.parse("192.168.1.43")!!)
            assertEquals("newpassword", model.uiState.value.importedRoomPassword)
            model.confirmImportedConnection()
            runCurrent()
            assertEquals(1, chat.connects)
            assertEquals("newpassword", chat.lastPassword)
            assertNull(model.uiState.value.pendingConnection)
            assertEquals("", model.uiState.value.importedRoomPassword)
        } finally { store.clear(); runCurrent(); Dispatchers.resetMain() }
    }

    @Test fun newPasswordlessImportAndCancelClearOnlyImportedCredential() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            val model = workspace(store, Chat(), Receiver())
            model.updateSessionPassword("previous-room-password")
            val link = "securelan://join?v=1&proto=2&host=192.168.1.42&port=5050"
            model.importConnectionUri("$link&password=invited")
            model.importConnectionUri(link)
            assertEquals("", model.uiState.value.importedRoomPassword)
            model.updateImportedRoomPassword("replacement")
            model.dismissConnectionImport()
            assertEquals("", model.uiState.value.importedRoomPassword)
            assertNull(model.uiState.value.pendingConnection)
            assertEquals("previous-room-password", model.uiState.value.sessionPassword)
        } finally { store.clear(); runCurrent(); Dispatchers.resetMain() }
    }

    private fun workspace(store: ViewModelStore, chat: Chat, receiver: Receiver,
        discover: () -> Flow<DiscoveredPeer> = { emptyFlow() }, now: () -> Long = { 0 },
        rooms: ((String) -> Flow<com.shterneregen.securelan.chat.discovery.DiscoverySnapshot>)? = null): MainViewModel {
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
            createReceiver = { receiver }, monotonicMillis = now, discoverRooms = rooms,
            ioDispatcher = StandardTestDispatcher())).also { store.put("workspace", it) }
    }
}
