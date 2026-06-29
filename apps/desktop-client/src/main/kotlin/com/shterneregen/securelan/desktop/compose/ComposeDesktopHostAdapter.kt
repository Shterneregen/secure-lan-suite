package com.shterneregen.securelan.desktop.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.shterneregen.securelan.chat.discovery.DiscoveredPeer
import com.shterneregen.securelan.chat.discovery.PeerDiscoveryConfig
import com.shterneregen.securelan.chat.discovery.PeerDiscoveryListener
import com.shterneregen.securelan.chat.discovery.PeerDiscoveryService
import com.shterneregen.securelan.chat.event.ChatConnectedEvent
import com.shterneregen.securelan.chat.event.ChatDisconnectedEvent
import com.shterneregen.securelan.chat.event.ChatErrorEvent
import com.shterneregen.securelan.chat.event.ChatMessageReceivedEvent
import com.shterneregen.securelan.chat.event.ChatMessageSentEvent
import com.shterneregen.securelan.chat.event.ChatSignalReceivedEvent
import com.shterneregen.securelan.chat.event.ChatUserJoinedEvent
import com.shterneregen.securelan.chat.event.ChatUserLeftEvent
import com.shterneregen.securelan.chat.protocol.handshake.PeerCapabilities
import com.shterneregen.securelan.chat.service.ChatClientConnectRequest
import com.shterneregen.securelan.chat.service.ChatClientService
import com.shterneregen.securelan.chat.service.ChatEventPublisher
import com.shterneregen.securelan.chat.service.ChatServerConfig
import com.shterneregen.securelan.chat.service.ChatServerService
import com.shterneregen.securelan.chat.service.RandomNicknameService
import com.shterneregen.securelan.common.model.rtc.RtcSessionMode
import com.shterneregen.securelan.common.model.rtc.RtcSessionState
import com.shterneregen.securelan.common.net.NetworkConstants
import com.shterneregen.securelan.desktop.ui.DesktopMainViewHelpers
import com.shterneregen.securelan.desktop.ui.DesktopQuickShareFormatters
import com.shterneregen.securelan.desktop.ui.DesktopRealtimeFormatters
import com.shterneregen.securelan.desktop.ui.DesktopTransferFormatters
import com.shterneregen.securelan.desktop.ui.MediaDeviceChoice
import com.shterneregen.securelan.desktop.ui.PeerPresence
import com.shterneregen.securelan.desktop.ui.QuickShareEntry
import com.shterneregen.securelan.desktop.ui.TransferEntry
import com.shterneregen.securelan.filetransfer.event.FileTransferCompletedEvent
import com.shterneregen.securelan.filetransfer.event.FileTransferEvent
import com.shterneregen.securelan.filetransfer.event.FileTransferFailedEvent
import com.shterneregen.securelan.filetransfer.event.FileTransferProgressEvent
import com.shterneregen.securelan.filetransfer.event.FileTransferStartedEvent
import com.shterneregen.securelan.filetransfer.protocol.FileTransferMetadata
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareCreateRequest
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareEvent
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareServerConfig
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareService
import com.shterneregen.securelan.filetransfer.service.FileTransferClientRequest
import com.shterneregen.securelan.filetransfer.service.FileTransferClientService
import com.shterneregen.securelan.filetransfer.service.FileTransferServerConfig
import com.shterneregen.securelan.filetransfer.service.FileTransferServerService
import com.shterneregen.securelan.stego.StegoServices
import com.shterneregen.securelan.stego.model.BmpCapacity
import com.shterneregen.securelan.stego.service.SteganographyService
import com.shterneregen.securelan.webrtc.event.RtcAudioLevelEvent
import com.shterneregen.securelan.webrtc.event.RtcEvent
import com.shterneregen.securelan.webrtc.event.RtcRuntimeWarningEvent
import com.shterneregen.securelan.webrtc.event.RtcStateChangedEvent
import com.shterneregen.securelan.webrtc.event.RtcVideoFrameEvent
import com.shterneregen.securelan.webrtc.runtime.RtcRuntimeStatus
import com.shterneregen.securelan.webrtc.service.RtcMediaDeviceService
import com.shterneregen.securelan.webrtc.service.RtcSessionRequest
import com.shterneregen.securelan.webrtc.service.RtcSessionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.net.InetSocketAddress
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.SwingUtilities

/**
 * Live desktop host adapter for status/connection, peer-list, and chat workspace wiring.
 *
 * Wraps [ChatServerService], [ChatClientService], [PeerDiscoveryService], and optional [ChatEventPublisher]
 * to manage room hosting, manual connection, discovery visibility, peer snapshots, and shared-room chat.
 * Publishes [ComposeConnectionEvent] events that match the [ComposeAdapterEventRouting] contract
 * from [ComposeShellMetadata].
 *
 * JavaFX remains the production path; this adapter is used only by the Compose shell.
 */
class ComposeDesktopHostAdapter(
    private val chatServerService: ChatServerService,
    private val chatClientService: ChatClientService,
    private val fileTransferServerService: FileTransferServerService,
    private val quickShareService: QuickShareService,
    private val discoveryService: PeerDiscoveryService,
    private val randomNicknameService: RandomNicknameService,
    private val fileTransferClientService: FileTransferClientService? = null,
    private val steganographyService: SteganographyService = StegoServices.createDefault().steganographyService(),
    private val rtcSessionService: RtcSessionService? = null,
    private val rtcMediaDeviceService: RtcMediaDeviceService? = null,
    private val downloadsPath: Path = Path.of("downloads").toAbsolutePath().normalize(),
    private val uiStateDispatcher: (((() -> Unit)) -> Unit)? = null,
) : AutoCloseable {
    /** Live status/connection state, updated after each action. */
    var statusState: ComposeStatusConnectionState by mutableStateOf(
        ComposeStatusConnectionState(nickname = randomNicknameService.generate()),
    )
        private set

    /** Last room password used by hosting/manual connect; reused by encrypted file send UI. */
    var currentRoomPassword: String by mutableStateOf(ComposeShellMetadata.DEFAULT_STATUS_ADAPTER_STATE.roomPasswordPlaceholder)
        private set

    /** Mirrors the transfer checkbox used by the listener acceptance callback. */
    var autoAcceptIncomingFiles: Boolean by mutableStateOf(false)
        private set

    /** Adapter events since the last clear, in chronological order. */
    var adapterEvents: List<ComposeConnectionEvent> by mutableStateOf(emptyList())
        private set

    /** Snapshot of discovered peers from the discovery service. */
    var discoveredPeers: List<DiscoveredPeer> by mutableStateOf(emptyList())
        private set

    /** Manual peers entered from the Compose peer-list validation shell. */
    var manualPeers: List<DiscoveredPeer> by mutableStateOf(emptyList())
        private set

    /** Chat-room peers observed through USER_JOINED, chat, signal, and USER_LEFT events. */
    var chatPeers: List<PeerPresence> by mutableStateOf(emptyList())
        private set

    /** Combined peer list rendered by the Compose shell. */
    val visiblePeers: List<DiscoveredPeer>
        get() = (discoveredPeers + manualPeers)
            .filterNot(::isLocalPeer)
            .distinctBy { it.peerId }
            .sortedWith(Comparator.comparing(DiscoveredPeer::nickname, String.CASE_INSENSITIVE_ORDER))

    /** Combined JavaFX-parity peer list: discovered/manual LAN targets plus connected chat participants. */
    val visiblePeerItems: List<PeerPresence>
        get() {
            val merged = LinkedHashMap<String, PeerPresence>()
            chatPeers.filterNot(::isLocalPeer).forEach { peer ->
                merged[peerKey(peer)] = peer
            }
            visiblePeers.forEach { peer ->
                val existing = chatPeers.firstOrNull { DesktopMainViewHelpers.samePeer(it, peer.nickname, peer.peerId) }
                val presence = existing ?: PeerPresence(peer.nickname, true, peer.peerId, peer.host, peer.chatPort, peer.filePort, peer.lastSeen)
                merged[peerKey(presence)] = presence
            }
            return merged.values
                .sortedWith(
                    Comparator.comparing(PeerPresence::online).reversed()
                        .thenComparing(PeerPresence::nickname, String.CASE_INSENSITIVE_ORDER),
                )
        }

    /** Runtime peer-list diagnostics for discovery and targeting validation. */
    var peerListDiagnostics: List<String> by mutableStateOf(listOf("Peer discovery has not started."))
        private set

    /** Chat transcript lines for workspace wiring. */
    var chatTranscript: List<String> by mutableStateOf(emptyList())
        private set

    /** Chat transcript messages with UI-only receive timestamps; wire-format text stays unchanged. */
    var chatMessages: List<ComposeChatMessage> by mutableStateOf(emptyList())
        private set

    /** File transfer rows for encrypted-transfer workspace wiring. */
    var transferEntries: List<TransferEntry> by mutableStateOf(emptyList())
        private set

    /** Incoming receive prompts captured before acceptance decisions. */
    var incomingTransferPrompts: List<ComposeIncomingTransferPrompt> by mutableStateOf(emptyList())
        private set

    private val pendingIncomingTransferDecisions = ConcurrentHashMap<String, CompletableFuture<Boolean>>()

    /** Dedicated coroutine scope for blocking desktop file-transfer client work. */
    private val fileTransferIoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** File-transfer diagnostics for the Compose shell. */
    var transferDiagnostics: List<String> by mutableStateOf(listOf("File transfer listener has not started."))
        private set

    /** Quick-share rows for browser-link workspace wiring. */
    var quickShareEntries: List<QuickShareEntry> by mutableStateOf(emptyList())
        private set

    /** Quick-share status copy shown by Compose. */
    var quickShareStatus: String by mutableStateOf("Quick share idle")
        private set

    /** Quick-share landing URL copy shown by Compose. */
    var quickShareLanding: String by mutableStateOf("Start quick share to get LAN browser links.")
        private set

    /** Quick-share diagnostics for trusted-LAN browser-link actions. */
    var quickShareDiagnostics: List<String> by mutableStateOf(listOf("Quick share is stopped."))
        private set

    /** Steganography UI state for Compose wiring. */
    var stegoState: ComposeSteganographyState by mutableStateOf(ComposeSteganographyState())
        private set

    /** Media/voice UI state for Compose wiring. */
    var mediaVoiceState: ComposeMediaVoiceState by mutableStateOf(
        ComposeMediaVoiceState(statusState = statusState, peerListState = ComposePeerListState(peers = emptyList())),
    )
        private set

    /** Experimental camera/video UI state for Compose wiring. */
    var experimentalVideoState: ComposeExperimentalVideoState by mutableStateOf(
        ComposeExperimentalVideoState(statusState = statusState, peerListState = ComposePeerListState(peers = emptyList())),
    )
        private set

    /** Realtime diagnostics for voice/video/device actions. */
    var realtimeDiagnostics: List<String> by mutableStateOf(listOf("RTC runtime not checked."))
        private set

    /** Manual/runtime evidence records captured for regression review. */
    private var runtimeEvidenceRecords: List<ComposeRuntimeEvidenceRecord> by mutableStateOf(emptyList())

    /** Build/package evidence records captured for release review. */
    private var packagingEvidenceRecords: List<ComposePackagingEvidenceRecord> by mutableStateOf(emptyList())

    /** Full-regression readiness state for diagnostics. */
    var regressionReadinessState: ComposeRegressionReadinessState by mutableStateOf(buildRegressionReadinessState())
        private set

    /** Packaging readiness state for promotion gates. */
    var packagingReadinessState: ComposePackagingReadinessState by mutableStateOf(ComposeShellMetadata.DEFAULT_PACKAGING_STATE)
        private set

    /** Whether the chat client is connected (for send readiness). */
    val chatConnected: Boolean get() = chatClientService.isConnected()

    /** Whether quick share is running. */
    val quickShareRunning: Boolean get() = quickShareService.isRunning()

    /** Quick-share landing URLs currently exposed by the local browser-link server. */
    val quickShareLandingUrls: List<String> get() = quickShareService.landingUrls()

    /** Live diagnostics state aggregated from all Compose adapter channels. */
    val diagnosticsState: ComposeDiagnosticsState
        get() = buildDiagnosticsState()

    /** Local LAN addresses shown by the Compose profile card, matching the JavaFX startup info line. */
    var localNetworkInfo: String by mutableStateOf(DesktopMainViewHelpers.localNetworkInfoMessage(emptyList()))
        private set

    /** Send an RTC signaling payload through the connected chat transport. */
    fun chatClientServiceSendSignal(signal: com.shterneregen.securelan.common.model.rtc.RtcSignalEnvelope?) {
        chatClientService.sendSignal(signal)
    }

    private val shuttingDown = AtomicBoolean(false)
    private val localPeerId = UUID.randomUUID().toString()
    private var currentConfig: PeerDiscoveryConfig? = null
    private val transferEntryMap = LinkedHashMap<String, TransferEntry>()
    private var cameraPreviewSession: RtcMediaDeviceService.CameraPreviewSession? = null

    val chatEventPublisher: ChatEventPublisher = ChatEventPublisher { event ->
        when (event) {
            is ChatConnectedEvent -> {
                appendChatTranscript("[connected] ${event.nickname ?: "unknown"} -> ${formatEndpoint(event.remoteAddress)}")
                clearChatPeers()
            }
            is ChatDisconnectedEvent -> {
                appendChatTranscript("[disconnected] ${event.nickname ?: "unknown"} - ${event.reason ?: "unknown"}")
                clearChatPeers()
                publish(ComposeConnectionEventKind.INFO, "Chat disconnected: ${event.reason ?: "unknown"}")
            }
            is ChatMessageReceivedEvent -> {
                val sender = event.senderNickname ?: "unknown"
                val text = normalizeChatText(event.text, sender)
                if (!isSystemSender(sender)) {
                    upsertChatPeer(sender, online = true)
                }
                appendChatTranscript(formatChatMessage(sender, text))
            }
            is ChatMessageSentEvent -> {
                // JavaFX shows the message through the normal chat receive flow; keep Compose parity and avoid duplicates.
            }
            is ChatUserJoinedEvent -> {
                val peer = upsertJoinedPeer(event)
                if (peer != null && peer.online()) {
                    appendChatTranscript("[join] ${normalizeChatText(event.nickname, "join")}")
                }
            }
            is ChatUserLeftEvent -> {
                if (markChatPeerOffline(event.nickname)) {
                    appendChatTranscript("[left] ${event.nickname}")
                }
            }
            is ChatSignalReceivedEvent -> {
                upsertChatPeer(event.signal.fromPeer(), online = true)
                publishPeerListDiagnostic("RTC signal preserved through chat-core: ${event.signal.type()} from ${event.signal.fromPeer()} to ${event.signal.toPeer()}.")
            }
            is ChatErrorEvent -> {
                val detail = "[error] ${event.message ?: "Chat error"}${event.cause?.message?.let { " -> $it" } ?: ""}"
                appendChatTranscript(detail)
                publish(ComposeConnectionEventKind.ERROR, event.message ?: "Chat error")
            }
        }
    }

    val fileTransferEventPublisher = com.shterneregen.securelan.filetransfer.service.FileTransferEventPublisher { event ->
        dispatchUiStateUpdate { handleFileTransferEvent(event) }
    }

    val quickShareEventPublisher = com.shterneregen.securelan.filetransfer.quickshare.QuickShareEventPublisher { event ->
        handleQuickShareEvent(event)
    }

    val rtcEventPublisher = com.shterneregen.securelan.webrtc.service.RtcEventPublisher { event ->
        handleRtcEvent(event)
    }

    private val discoveryListener = object : PeerDiscoveryListener {
        override fun onPeerDiscovered(peer: DiscoveredPeer) {
            discoveredPeers = discoveryService.snapshot()
            publishPeerListDiagnostic(DesktopMainViewHelpers.discoveryPeerFoundDiagnostics(peer) + "; snapshot=${discoveredPeers.size}.")
            refreshMigrationReadinessState()
        }

        override fun onPeerExpired(peer: DiscoveredPeer) {
            discoveredPeers = discoveryService.snapshot()
            publishPeerListDiagnostic(DesktopMainViewHelpers.discoveryPeerExpiredDiagnostics(peer) + "; snapshot=${discoveredPeers.size}.")
            refreshMigrationReadinessState()
        }

        override fun onDiscoveryError(message: String, cause: Throwable) {
            publishPeerListDiagnostic(DesktopMainViewHelpers.discoveryErrorDiagnostics(message, cause))
            if (!discoveryService.isRunning()) {
                publish(ComposeConnectionEventKind.ERROR, DesktopMainViewHelpers.discoveryChatMessage(message))
            }
            refreshState()
        }
    }

    init {
        publishLocalNetworkInfo()
        startPeerDiscoveryListener()
        refreshState()
    }

    // ---- public actions ----

    /**
     * Start hosting a room on the given ports, then optionally start discovery announcements.
     * Fires [ComposeAdapterEventKind.HOST_STARTED] on success.
     */
    fun openRoom(
        nickname: String,
        password: String,
        chatPort: Int,
        filePort: Int,
        discoverable: Boolean,
    ) {
        if (shuttingDown.get()) return
        val trimmedNick = nickname.trim()
        if (trimmedNick.isEmpty()) {
            publish(ComposeConnectionEventKind.WARNING, "Nickname is required to open a room.")
            return
        }
        if (chatServerService.isRunning()) {
            publish(ComposeConnectionEventKind.WARNING, "Room is already running; stop it first.")
            return
        }
        publishEventKind(ComposeAdapterEventKind.CLEANUP_STARTED, "Preparing to open room.")

        try {
            currentRoomPassword = password
            statusState = statusState.copy(
                nickname = trimmedNick,
                serverChatPortText = chatPort.toString(),
                serverFilePortText = filePort.toString(),
                discoverable = discoverable,
            )
            chatServerService.start(ChatServerConfig(chatPort, password))
            startFileTransferListener(filePort, password)
            publish(ComposeConnectionEventKind.SUCCESS, "Room opened on chat port $chatPort, file port $filePort.")
            publishEventKind(ComposeAdapterEventKind.HOST_STARTED, "Host started: $trimmedNick")

            connectLocalHostedChat(chatPort, trimmedNick, password)

            val discoveryConfig = PeerDiscoveryConfig.defaults(localPeerId, trimmedNick, chatPort, filePort, discoverable)
            currentConfig = discoveryConfig
            discoveryService.start(discoveryConfig, discoveryListener)
            discoveredPeers = discoveryService.snapshot()
            publishPeerListDiagnostic(
                "Discovery started on port ${discoveryConfig.discoveryPort}; announce=${discoveryConfig.announceEnabled}; snapshot=${discoveredPeers.size}.",
            )

            if (discoverable) {
                publish(ComposeConnectionEventKind.SUCCESS, "Discovery announcements active.")
                publishEventKind(ComposeAdapterEventKind.DISCOVERY_VISIBILITY_CHANGED, "Discoverable: true")
            } else {
                publish(ComposeConnectionEventKind.INFO, "Discovery listen-only; room is hidden.")
                publishEventKind(ComposeAdapterEventKind.DISCOVERY_VISIBILITY_CHANGED, "Discoverable: false")
            }
        } catch (e: Exception) {
            publish(ComposeConnectionEventKind.ERROR, "Failed to open room: ${e.message}")
            publishEventKind(ComposeAdapterEventKind.RUNTIME_ERROR, e.message ?: "Unknown room hosting error")
        }

        refreshState()
    }

    /** Stop the room, discovery, and client if connected. */
    fun stopHosting() {
        if (shuttingDown.get()) return
        if (!chatServerService.isRunning()) {
            publish(ComposeConnectionEventKind.WARNING, "No room is currently running.")
            return
        }
        publishEventKind(ComposeAdapterEventKind.CLEANUP_STARTED, "Stopping hosting.")

        try {
            if (chatClientService.isConnected()) {
                chatClientService.disconnect()
                publish(ComposeConnectionEventKind.INFO, "Client disconnected before stopping room.")
            }
            if (fileTransferServerService.isRunning()) {
                fileTransferServerService.stop()
                publish(ComposeConnectionEventKind.INFO, "File transfer listener stopped.")
            }
            discoveryService.stop()
            currentConfig = null
            discoveredPeers = emptyList()
            clearChatPeers()
            publishPeerListDiagnostic("Discovery stopped; peer snapshot cleared.")
            chatServerService.stop()
            publish(ComposeConnectionEventKind.SUCCESS, "Room stopped.")
            publishEventKind(ComposeAdapterEventKind.HOST_STOPPED, "Host stopped.")
        } catch (e: Exception) {
            publish(ComposeConnectionEventKind.ERROR, "Error stopping room: ${e.message}")
            publishEventKind(ComposeAdapterEventKind.RUNTIME_ERROR, e.message ?: "Unknown stop hosting error")
        }

        refreshState()
    }

    /** Connect to a remote room manually. */
    fun connect(
        host: String,
        nickname: String,
        password: String,
        chatPort: Int,
        filePort: Int,
    ) {
        if (shuttingDown.get()) return
        val trimmedHost = host.trim()
        val trimmedNick = nickname.trim()
        if (trimmedHost.isEmpty() || trimmedNick.isEmpty()) {
            publish(ComposeConnectionEventKind.WARNING, "Host address and nickname are required to connect.")
            return
        }
        if (chatClientService.isConnected()) {
            publish(ComposeConnectionEventKind.WARNING, "Already connected; disconnect first.")
            return
        }
        publishEventKind(ComposeAdapterEventKind.CONNECT_STARTED, "Connecting to $trimmedHost")

        try {
            currentRoomPassword = password
            statusState = statusState.copy(
                nickname = trimmedNick,
                manualHost = trimmedHost,
                clientChatPortText = chatPort.toString(),
                clientFilePortText = filePort.toString(),
            )
            val request = ChatClientConnectRequest(trimmedHost, chatPort, trimmedNick, password, desktopCapabilities(filePort))
            val connected = chatClientService.connect(request)
            if (connected) {
                startFileTransferListener(filePort, password)
                startPeerDiscoveryListenOnly(trimmedNick)
                publish(ComposeConnectionEventKind.SUCCESS, "Connected to $trimmedHost as $trimmedNick.")
                publishEventKind(ComposeAdapterEventKind.CONNECTED, "Connected: $trimmedNick")
            } else {
                publish(ComposeConnectionEventKind.ERROR, "Connection to $trimmedHost failed.")
                publishEventKind(ComposeAdapterEventKind.CONNECT_FAILED, "Connection failed: $trimmedHost")
            }
        } catch (e: Exception) {
            publish(ComposeConnectionEventKind.ERROR, "Connection error: ${e.message}")
            publishEventKind(ComposeAdapterEventKind.CONNECT_FAILED, e.message ?: "Unknown connection failure")
        }

        refreshState()
    }

    /** Disconnect the chat client. */
    fun disconnect() {
        if (shuttingDown.get()) return
        if (!chatClientService.isConnected()) {
            publish(ComposeConnectionEventKind.WARNING, "Not currently connected.")
            return
        }

        try {
            chatClientService.disconnect()
            clearChatPeers()
            if (!chatServerService.isRunning()) {
                startPeerDiscoveryListenOnly(statusState.nickname)
            }
            publish(ComposeConnectionEventKind.SUCCESS, "Disconnected.")
            publishEventKind(ComposeAdapterEventKind.DISCONNECTED, "Disconnected.")
        } catch (e: Exception) {
            publish(ComposeConnectionEventKind.ERROR, "Disconnect error: ${e.message}")
            publishEventKind(ComposeAdapterEventKind.RUNTIME_ERROR, e.message ?: "Unknown disconnect error")
        }

        refreshState()
    }

    /** Toggle discovery announcement mode. Only meaningful while a room is hosted. */
    fun setDiscoverable(enabled: Boolean) {
        if (shuttingDown.get()) return
        if (!discoveryService.isRunning()) {
            publish(ComposeConnectionEventKind.WARNING, "Discovery is not running; open a room first.")
            return
        }
        try {
            discoveryService.setAnnounceEnabled(enabled)
            discoveredPeers = discoveryService.snapshot()
            val label = if (enabled) "Discoverable" else "Hidden"
            publish(ComposeConnectionEventKind.SUCCESS, "Room set to $label mode.")
            publishEventKind(ComposeAdapterEventKind.DISCOVERY_VISIBILITY_CHANGED, label)
            publishPeerListDiagnostic("Discovery visibility changed to $label; snapshot=${discoveredPeers.size}.")
        } catch (e: Exception) {
            publish(ComposeConnectionEventKind.ERROR, "Failed to change discovery mode: ${e.message}")
            publishPeerListDiagnostic("Failed to change discovery visibility: ${e.message ?: "unknown error"}.")
        }
        refreshState()
    }

    /** Add a manual peer target when UDP discovery is unavailable during validation. */
    fun addManualPeer(
        nickname: String,
        host: String,
        chatPort: Int,
        filePort: Int,
    ) {
        if (shuttingDown.get()) return
        val trimmedNick = nickname.trim()
        val trimmedHost = host.trim()
        if (trimmedNick.isEmpty() || trimmedHost.isEmpty()) {
            publishPeerListDiagnostic("Manual peer rejected: nickname and host are required.")
            return
        }
        val peerId = "manual-${trimmedNick.lowercase()}-${trimmedHost.lowercase()}-$chatPort-$filePort"
            .replace(Regex("[^a-z0-9.-]+"), "-")
        val peer = DiscoveredPeer(peerId, trimmedNick, trimmedHost, chatPort, filePort, Instant.now())
        manualPeers = (manualPeers.filterNot { it.peerId == peer.peerId } + peer)
            .sortedWith(Comparator.comparing(DiscoveredPeer::nickname, String.CASE_INSENSITIVE_ORDER))
        publishPeerListDiagnostic("Manual peer added: ${peer.nickname}@${peer.host}; visible=${visiblePeers.size}.")
    }

    /** Resolve a selected Compose peer row to its advertised LAN file endpoint. */
    fun discoveredPeerFor(nickname: String): DiscoveredPeer? {
        val discovered = (discoveredPeers + manualPeers).firstOrNull { it.nickname.equals(nickname, ignoreCase = true) }
        if (discovered != null) {
            return discovered
        }
        return visiblePeerItems.firstOrNull { it.nickname().equals(nickname, ignoreCase = true) && DesktopMainViewHelpers.selectedPeerFileCapable(it) }
            ?.let { peer ->
                DiscoveredPeer(
                    peer.peerId() ?: "peer-${peer.nickname().lowercase()}",
                    peer.nickname(),
                    peer.host().orEmpty(),
                    peer.chatPort(),
                    peer.filePort(),
                    peer.lastSeen() ?: Instant.now(),
                )
            }
    }

    /** Resolve a selected Compose peer row to the address fields used by the Join room form. */
    fun joinTargetFor(nickname: String): ComposeConnectionJoinTarget? = discoveredPeerFor(nickname)
        ?.takeIf { it.host.isNotBlank() && it.chatPort > 0 && it.filePort > 0 }
        ?.let(ComposeConnectionJoinTarget::fromDiscoveredPeer)

    /** Clear manually added Compose peer targets. */
    fun clearManualPeers() {
        manualPeers = emptyList()
        publishPeerListDiagnostic("Manual peer targets cleared; visible=${visiblePeers.size}.")
    }

    /** Generate a default random nickname. */
    fun generateNickname(): String = randomNicknameService.generate()

    /** Restart JavaFX-parity UDP discovery listener using the current hosting state. */
    fun refreshPeerDiscovery() {
        if (shuttingDown.get()) return
        startPeerDiscoveryListener()
        refreshState()
    }

    /** Send a chat message through the connected client. */
    fun sendMessage(text: String) {
        if (shuttingDown.get()) return
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            publish(ComposeConnectionEventKind.WARNING, "Message is empty; nothing sent.")
            return
        }
        if (!chatClientService.isConnected()) {
            publish(ComposeConnectionEventKind.WARNING, "Connect to a room before sending a chat message.")
            return
        }

        try {
            chatClientService.sendMessage(trimmed)
        } catch (e: Exception) {
            val message = "Chat send failed: ${e.message ?: "unknown error"}"
            appendChatTranscript("[error] $message")
            publish(ComposeConnectionEventKind.ERROR, message)
        }
    }

    /** Record an incoming receive prompt and return the deterministic Compose acceptance decision. */
    fun acceptIncomingFileTransfer(
        metadata: FileTransferMetadata,
        remoteAddress: String,
        autoAccept: Boolean = false,
    ): Boolean {
        if (shuttingDown.get()) return false

        var immediateDecision: Boolean? = null
        var pendingDecision: CompletableFuture<Boolean>? = null
        var pendingPromptId: String? = null
        runUiStateUpdateAndWait {
            if (!chatClientService.isConnected()) {
                val prompt = ComposeIncomingTransferPrompt.from(metadata, remoteAddress, ComposeIncomingTransferPromptStatus.REJECTED)
                incomingTransferPrompts = upsertIncomingTransferPrompt(prompt)
                appendChatTranscript(DesktopTransferFormatters.fileRejectedDisconnectedMessage(metadata.fileName, metadata.senderId))
                publishTransferDiagnostic("Incoming file rejected because chat is not connected: ${metadata.fileName}.")
                immediateDecision = false
                return@runUiStateUpdateAndWait
            }
            val knownPeer = findPeerForIncomingFile(metadata, remoteAddress) != null
            if (!knownPeer) {
                val prompt = ComposeIncomingTransferPrompt.from(metadata, remoteAddress, ComposeIncomingTransferPromptStatus.REJECTED)
                incomingTransferPrompts = upsertIncomingTransferPrompt(prompt)
                appendChatTranscript(DesktopTransferFormatters.fileRejectedUnknownPeerMessage(metadata.fileName, metadata.senderId))
                publishTransferDiagnostic("Incoming file rejected from unknown/offline peer ${metadata.senderId}.")
                immediateDecision = false
                return@runUiStateUpdateAndWait
            }
            if (autoAccept || autoAcceptIncomingFiles) {
                val prompt = ComposeIncomingTransferPrompt.from(metadata, remoteAddress, ComposeIncomingTransferPromptStatus.AUTO_ACCEPTED)
                incomingTransferPrompts = upsertIncomingTransferPrompt(prompt)
                appendChatTranscript(DesktopTransferFormatters.fileAutoAcceptedMessage(metadata.fileName, metadata.senderId))
                publishTransferDiagnostic("Incoming file auto-accepted: ${metadata.fileName} from ${metadata.senderId}.")
                immediateDecision = true
                return@runUiStateUpdateAndWait
            }

            val prompt = ComposeIncomingTransferPrompt.from(metadata, remoteAddress, ComposeIncomingTransferPromptStatus.WAITING)
            incomingTransferPrompts = upsertIncomingTransferPrompt(prompt)
            publishTransferDiagnostic("Incoming file prompt waiting for user decision: ${prompt.header}.")
            val decision = CompletableFuture<Boolean>()
            pendingIncomingTransferDecisions[prompt.id] = decision
            pendingPromptId = prompt.id
            pendingDecision = decision
        }
        immediateDecision?.let { return it }
        val decision = pendingDecision ?: return false
        if (SwingUtilities.isEventDispatchThread()) {
            publishTransferDiagnostic("Incoming file prompt rejected because waiting on the UI thread would freeze the Compose event loop.")
            pendingPromptId?.let { pendingIncomingTransferDecisions.remove(it) }
            return false
        }
        return try {
            decision.get()
        } catch (e: Exception) {
            dispatchUiStateUpdate { publishTransferDiagnostic(DesktopTransferFormatters.fileConfirmationFailedDiagnostics(e.message)) }
            false
        } finally {
            pendingPromptId?.let { pendingIncomingTransferDecisions.remove(it) }
        }
    }

    /** Record an explicit Compose receive-prompt decision. */
    fun recordIncomingFileDecision(promptId: String, accepted: Boolean) {
        val prompt = incomingTransferPrompts.firstOrNull { it.id == promptId } ?: return
        pendingIncomingTransferDecisions[promptId]?.complete(accepted)
        incomingTransferPrompts = incomingTransferPrompts.map {
            if (it.id == promptId) {
                it.withStatus(
                    if (accepted) {
                        ComposeIncomingTransferPromptStatus.ACCEPTED
                    } else {
                        ComposeIncomingTransferPromptStatus.REJECTED
                    },
                )
            } else {
                it
            }
        }
        appendChatTranscript(DesktopTransferFormatters.fileConfirmationResultMessage(accepted, prompt.fileName, prompt.senderId))
        publishTransferDiagnostic("Incoming file ${if (accepted) "accepted" else "rejected"}: ${prompt.fileName} from ${prompt.senderId}.")
    }

    private fun upsertIncomingTransferPrompt(prompt: ComposeIncomingTransferPrompt): List<ComposeIncomingTransferPrompt> =
        (incomingTransferPrompts.filterNot { it.id == prompt.id } + prompt).takeLast(20)

    private fun findPeerForIncomingFile(metadata: FileTransferMetadata, remoteAddress: String): PeerPresence? {
        val remoteHost = remoteHostOnly(remoteAddress)
        return visiblePeerItems.firstOrNull { peer ->
            peer.online() &&
                (
                    peer.nickname().equals(metadata.senderId, ignoreCase = true) ||
                        peer.peerId() == metadata.senderId ||
                        (!peer.host().isNullOrBlank() && peer.host() == remoteHost)
                    )
        }
    }

    private fun remoteHostOnly(remoteAddress: String): String? =
        runCatching {
            val socketAddress = InetSocketAddress.createUnresolved(remoteAddress.substringBeforeLast(':'), 0)
            socketAddress.hostString
                .removePrefix("/")
                .takeIf { it.isNotBlank() && it != remoteAddress }
        }.getOrNull()
            ?: remoteAddress
                .removePrefix("/")
                .substringBeforeLast(':')
                .takeIf { it.isNotBlank() && it != remoteAddress }

    /** Mirror the JavaFX auto-accept checkbox into the live file-transfer listener callback. */
    fun updateAutoAcceptIncomingFiles(enabled: Boolean) {
        if (autoAcceptIncomingFiles == enabled) {
            return
        }
        autoAcceptIncomingFiles = enabled
    }

    /** Record manual/runtime validation evidence gathered outside deterministic unit tests. */
    fun updateRuntimeValidationEvidence(
        chatRuntimeValidated: Boolean = regressionReadinessState.chatRuntimeValidated,
        fileTransferRuntimeValidated: Boolean = regressionReadinessState.fileTransferRuntimeValidated,
        quickShareRuntimeValidated: Boolean = regressionReadinessState.quickShareRuntimeValidated,
        steganographyRuntimeValidated: Boolean = regressionReadinessState.steganographyRuntimeValidated,
        voiceRuntimeValidated: Boolean = regressionReadinessState.voiceRuntimeValidated,
        videoRuntimeValidated: Boolean = regressionReadinessState.videoRuntimeValidated,
        fullRuntimeRegressionValidated: Boolean = regressionReadinessState.fullRuntimeRegressionValidated,
    ) {
        regressionReadinessState = buildRegressionReadinessState(
            chatRuntimeValidated = chatRuntimeValidated,
            fileTransferRuntimeValidated = fileTransferRuntimeValidated,
            quickShareRuntimeValidated = quickShareRuntimeValidated,
            steganographyRuntimeValidated = steganographyRuntimeValidated,
            voiceRuntimeValidated = voiceRuntimeValidated,
            videoRuntimeValidated = videoRuntimeValidated,
            fullRuntimeRegressionValidated = fullRuntimeRegressionValidated,
        )
    }

    /** Mark one runtime-evidence requirement from the Compose regression checklist. */
    fun recordRuntimeEvidence(kind: ComposeRuntimeEvidenceKind, recorded: Boolean = true) {
        updateRuntimeValidationEvidence(
            chatRuntimeValidated = if (kind == ComposeRuntimeEvidenceKind.CHAT_INTEROP) recorded else regressionReadinessState.chatRuntimeValidated,
            fileTransferRuntimeValidated = if (kind == ComposeRuntimeEvidenceKind.FILE_TRANSFER) recorded else regressionReadinessState.fileTransferRuntimeValidated,
            quickShareRuntimeValidated = if (kind == ComposeRuntimeEvidenceKind.QUICK_SHARE) recorded else regressionReadinessState.quickShareRuntimeValidated,
            steganographyRuntimeValidated = if (kind == ComposeRuntimeEvidenceKind.STEGANOGRAPHY) recorded else regressionReadinessState.steganographyRuntimeValidated,
            voiceRuntimeValidated = if (kind == ComposeRuntimeEvidenceKind.VOICE) recorded else regressionReadinessState.voiceRuntimeValidated,
            videoRuntimeValidated = if (kind == ComposeRuntimeEvidenceKind.EXPERIMENTAL_VIDEO) recorded else regressionReadinessState.videoRuntimeValidated,
            fullRuntimeRegressionValidated = if (kind == ComposeRuntimeEvidenceKind.FULL_REGRESSION) recorded else regressionReadinessState.fullRuntimeRegressionValidated,
        )
    }

    /** Capture a copyable runtime evidence record while keeping acceptance explicit. */
    fun recordRuntimeEvidenceRecord(
        kind: ComposeRuntimeEvidenceKind,
        note: String,
        status: ComposeRuntimeEvidenceChecklistStatus = ComposeRuntimeEvidenceChecklistStatus.RECORDED,
        recordedAt: Instant = Instant.now(),
    ) {
        runtimeEvidenceRecords = (runtimeEvidenceRecords.filterNot { it.kind == kind } +
            ComposeRuntimeEvidenceRecord(kind, status, note.ifBlank { "Runtime evidence recorded without details." }, recordedAt))
            .sortedBy { it.kind.ordinal }
        recordRuntimeEvidence(kind, status == ComposeRuntimeEvidenceChecklistStatus.ACCEPTED)
    }

    /** Record build/package validation evidence without changing launcher or jpackage configuration. */
    fun updatePackagingValidationEvidence(
        desktopTestsPassed: Boolean = packagingReadinessState.desktopTestsPassed,
        desktopBuildPassed: Boolean = packagingReadinessState.desktopBuildPassed,
        composeRuntimeSmokePassed: Boolean = packagingReadinessState.composeRuntimeSmokePassed,
        portableZipValidated: Boolean = packagingReadinessState.portableZipValidated,
        composePortableZipValidated: Boolean = packagingReadinessState.composePortableZipValidated,
        windowsExeValidated: Boolean = packagingReadinessState.windowsExeValidated,
        composePromotionApproved: Boolean = packagingReadinessState.composePromotionApproved,
        fullRuntimeRegressionValidated: Boolean = packagingReadinessState.fullRuntimeRegressionValidated,
    ) {
        packagingReadinessState = packagingReadinessState.copy(
            desktopTestsPassed = desktopTestsPassed,
            desktopBuildPassed = desktopBuildPassed,
            composeRuntimeSmokePassed = composeRuntimeSmokePassed,
            portableZipValidated = portableZipValidated,
            composePortableZipValidated = composePortableZipValidated,
            windowsExeValidated = windowsExeValidated,
            composePromotionApproved = composePromotionApproved,
            fullRuntimeRegressionValidated = fullRuntimeRegressionValidated,
            evidenceRecords = packagingEvidenceRecords,
        )
    }

    /** Capture one build/package evidence record without changing launcher or jpackage configuration. */
    fun recordPackagingEvidenceRecord(
        kind: ComposePackagingEvidenceKind,
        validated: Boolean,
        note: String,
        recordedAt: Instant = Instant.now(),
    ) {
        packagingEvidenceRecords = (packagingEvidenceRecords.filterNot { it.kind == kind } +
            ComposePackagingEvidenceRecord(kind, validated, note.ifBlank { "Packaging evidence recorded without details." }, recordedAt))
            .sortedBy { it.kind.ordinal }
        updatePackagingValidationEvidence(
            desktopTestsPassed = if (kind == ComposePackagingEvidenceKind.DESKTOP_TESTS) validated else packagingReadinessState.desktopTestsPassed,
            desktopBuildPassed = if (kind == ComposePackagingEvidenceKind.DESKTOP_BUILD) validated else packagingReadinessState.desktopBuildPassed,
            composeRuntimeSmokePassed = if (kind == ComposePackagingEvidenceKind.COMPOSE_RUNTIME_SMOKE) validated else packagingReadinessState.composeRuntimeSmokePassed,
            portableZipValidated = if (kind == ComposePackagingEvidenceKind.PORTABLE_ZIP) validated else packagingReadinessState.portableZipValidated,
            composePortableZipValidated = if (kind == ComposePackagingEvidenceKind.COMPOSE_PORTABLE_ZIP) validated else packagingReadinessState.composePortableZipValidated,
            windowsExeValidated = if (kind == ComposePackagingEvidenceKind.WINDOWS_EXE) validated else packagingReadinessState.windowsExeValidated,
            composePromotionApproved = if (kind == ComposePackagingEvidenceKind.PROMOTION_APPROVAL) validated else packagingReadinessState.composePromotionApproved,
            fullRuntimeRegressionValidated = if (kind == ComposePackagingEvidenceKind.FULL_RUNTIME_REGRESSION) validated else packagingReadinessState.fullRuntimeRegressionValidated,
        )
    }

    /** Mark one packaging artifact check without mutating launcher or jpackage configuration. */
    fun recordPackagingArtifactEvidence(kind: ComposePackagingArtifactKind, validated: Boolean = true) {
        updatePackagingValidationEvidence(
            composeRuntimeSmokePassed = if (kind == ComposePackagingArtifactKind.COMPOSE_ENTRYPOINT) validated else packagingReadinessState.composeRuntimeSmokePassed,
            portableZipValidated = if (kind == ComposePackagingArtifactKind.PORTABLE_ZIP) validated else packagingReadinessState.portableZipValidated,
            composePortableZipValidated = if (kind == ComposePackagingArtifactKind.COMPOSE_PORTABLE_ZIP) validated else packagingReadinessState.composePortableZipValidated,
            windowsExeValidated = if (kind == ComposePackagingArtifactKind.WINDOWS_EXE) validated else packagingReadinessState.windowsExeValidated,
        )
    }

    /** Mirror accepted full runtime-regression evidence into the packaging promotion gate. */
    fun recordFullRegressionPackagingEvidence(validated: Boolean = true) {
        recordRuntimeEvidence(ComposeRuntimeEvidenceKind.FULL_REGRESSION, validated)
        updatePackagingValidationEvidence(fullRuntimeRegressionValidated = validated)
    }

    fun sendFileToPeer(filePath: Path, senderId: String, recipient: DiscoveredPeer, sessionPassword: String): CompletableFuture<String?> {
        if (shuttingDown.get()) return CompletableFuture.completedFuture(null)
        val completion = CompletableFuture<String?>()
        val client = fileTransferClientService
        if (client == null) {
            publishTransferDiagnostic("Outgoing file send unavailable: file-transfer client service is not configured.")
            completion.complete(null)
            return completion
        }
        if (!chatClientService.isConnected()) {
            publishTransferDiagnostic("Outgoing file send blocked: connect to chat before sending files.")
            completion.complete(null)
            return completion
        }

        publishTransferDiagnostic("Outgoing file send queued for ${recipient.nickname}; file checks and transfer will run on the IO dispatcher.")
        fileTransferIoScope.launch {
            val normalizedFile = filePath.toAbsolutePath().normalize()
            if (!Files.isRegularFile(normalizedFile)) {
                dispatchUiStateUpdate { publishTransferDiagnostic("Outgoing file send blocked: file does not exist: $normalizedFile.") }
                completion.complete(null)
                return@launch
            }
            try {
                val transferId = client.sendFile(
                    FileTransferClientRequest(
                        recipient.host,
                        recipient.filePort,
                        senderId.trim(),
                        recipient.nickname,
                        sessionPassword,
                        normalizedFile,
                    ),
                )
                dispatchUiStateUpdate { publishTransferDiagnostic("Outgoing file send finished: ${normalizedFile.fileName} to ${recipient.nickname}.") }
                completion.complete(transferId)
            } catch (e: Exception) {
                val message = "Outgoing file send failed: ${DesktopMainViewHelpers.fileTransferErrorMessage(e)}"
                dispatchUiStateUpdate {
                    publishTransferDiagnostic(message)
                    publish(ComposeConnectionEventKind.ERROR, message)
                }
                completion.complete(null)
            }
        }
        return completion
    }

    fun startQuickShare(port: Int) {
        if (shuttingDown.get()) return
        try {
            quickShareService.start(QuickShareServerConfig(port))
            refreshQuickShareState()
            appendChatTranscript(DesktopQuickShareFormatters.formatServerStartedMessage(quickShareService.port()))
            publishQuickShareDiagnostic(DesktopQuickShareFormatters.formatLandingUrlsDiagnostics(quickShareService.landingUrls()))
        } catch (e: Exception) {
            val message = "Quick-share start failed: ${e.message ?: "unknown error"}"
            publish(ComposeConnectionEventKind.ERROR, message)
            publishQuickShareDiagnostic(message)
        }
    }

    fun stopQuickShare() {
        if (shuttingDown.get()) return
        quickShareService.stop()
        refreshQuickShareState()
        appendChatTranscript(DesktopQuickShareFormatters.formatServerStoppedMessage())
        publishQuickShareDiagnostic("Quick-share server stopped from Compose.")
    }

    fun createTextQuickShare(text: String, expirationMinutes: Long, accessLimit: Int) {
        if (shuttingDown.get()) return
        val normalizedText = text.trim()
        if (normalizedText.isBlank()) {
            publishQuickShareDiagnostic("Text quick-share rejected: enter text to share first.")
            return
        }
        if (expirationMinutes < 1 || accessLimit < 1) {
            publishQuickShareDiagnostic("Text quick-share rejected: expiration and access limit must be at least 1.")
            return
        }
        try {
            if (!quickShareService.isRunning()) {
                quickShareService.start(QuickShareServerConfig())
            }
            val snapshot = quickShareService.share(
                QuickShareCreateRequest.text(
                    normalizedText,
                    DesktopQuickShareFormatters.formatTextDisplayName(normalizedText),
                    Duration.ofMinutes(expirationMinutes),
                    accessLimit,
                ),
            )
            refreshQuickShareState()
            appendChatTranscript(DesktopQuickShareFormatters.formatTextLinkCopiedMessage(snapshot.primaryUrl()))
            publishQuickShareDiagnostic("Text quick-share created: ${snapshot.displayName()}.")
        } catch (e: Exception) {
            val message = "Text quick-share failed: ${e.message ?: "unknown error"}"
            publish(ComposeConnectionEventKind.ERROR, message)
            publishQuickShareDiagnostic(message)
        }
    }

    fun stopQuickShareEntry(id: String) {
        if (shuttingDown.get()) return
        if (quickShareService.stopShare(id)) {
            refreshQuickShareState()
            publishQuickShareDiagnostic("Quick-share stopped: $id.")
        }
    }

    fun createFileQuickShare(filePath: Path, expirationMinutes: Long, accessLimit: Int) {
        if (shuttingDown.get()) return
        val normalizedFile = filePath.toAbsolutePath().normalize()
        if (expirationMinutes < 1 || accessLimit < 1) {
            publishQuickShareDiagnostic("File quick-share rejected: expiration and access limit must be at least 1.")
            return
        }
        try {
            if (!quickShareService.isRunning()) {
                quickShareService.start(QuickShareServerConfig())
            }
            val snapshot = quickShareService.share(
                QuickShareCreateRequest.file(
                    normalizedFile,
                    normalizedFile.fileName.toString(),
                    Duration.ofMinutes(expirationMinutes),
                    accessLimit,
                ),
            )
            refreshQuickShareState()
            appendChatTranscript("[quick-share] file link copied: ${snapshot.primaryUrl()}")
            publishQuickShareDiagnostic("File quick-share created: ${snapshot.displayName()}.")
        } catch (e: Exception) {
            val message = "File quick-share failed: ${e.message ?: "unknown error"}"
            publish(ComposeConnectionEventKind.ERROR, message)
            publishQuickShareDiagnostic(message)
        }
    }

    fun inspectStegoCover(coverPath: Path): BmpCapacity? {
        if (shuttingDown.get()) return null
        return try {
            val normalized = coverPath.toAbsolutePath().normalize()
            val capacity = steganographyService.inspect(DesktopMainViewHelpers.readImageAsBmpBytes(normalized))
            stegoState = stegoState.copy(
                coverPathText = normalized.toString(),
                outputPathText = DesktopMainViewHelpers.suggestedStegoOutputPath(normalized).toString(),
                capacity = capacity,
                statusText = "Cover BMP ready: ${normalized.fileName}",
            )
            appendChatTranscript("[stego] inspected cover BMP: ${normalized.fileName}")
            capacity
        } catch (e: Exception) {
            val message = "Steganography inspect failed: ${DesktopMainViewHelpers.fileTransferErrorMessage(e)}"
            stegoState = stegoState.copy(statusText = message)
            publish(ComposeConnectionEventKind.ERROR, message)
            null
        }
    }

    fun hideStegoMessage(coverPath: Path, outputPath: Path, message: String, password: String? = null): Path? {
        if (shuttingDown.get()) return null
        val normalizedMessage = message.trim()
        if (normalizedMessage.isBlank()) {
            val status = "Steganography hide rejected: message is empty."
            stegoState = stegoState.copy(statusText = status)
            return null
        }
        val passwordChars = password?.toCharArray()
        return try {
            val cover = coverPath.toAbsolutePath().normalize()
            val output = DesktopMainViewHelpers.ensureBmpExtension(outputPath.toAbsolutePath().normalize())
            val bmpBytes = DesktopMainViewHelpers.readImageAsBmpBytes(cover)
            val hiddenBytes = if (passwordChars != null && passwordChars.isNotEmpty()) {
                steganographyService.hideEncryptedText(bmpBytes, normalizedMessage, passwordChars)
            } else {
                steganographyService.hideText(bmpBytes, normalizedMessage)
            }
            Files.write(output, hiddenBytes)
            val capacity = steganographyService.inspect(hiddenBytes)
            stegoState = stegoState.copy(
                coverPathText = cover.toString(),
                inputPathText = output.toString(),
                outputPathText = output.toString(),
                messageDraft = normalizedMessage,
                passwordDraft = password.orEmpty(),
                encryptPayload = !password.isNullOrEmpty(),
                capacity = capacity,
                statusText = "Hidden message saved to ${output.fileName}",
            )
            appendChatTranscript("[stego] hidden message saved: $output")
            output
        } catch (e: Exception) {
            val status = "Steganography hide failed: ${DesktopMainViewHelpers.fileTransferErrorMessage(e)}"
            stegoState = stegoState.copy(statusText = status)
            publish(ComposeConnectionEventKind.ERROR, status)
            null
        } finally {
            passwordChars?.fill('\u0000')
        }
    }

    fun extractStegoMessage(inputPath: Path, password: String? = null): String? {
        if (shuttingDown.get()) return null
        val passwordChars = password?.toCharArray()
        return try {
            val input = inputPath.toAbsolutePath().normalize()
            val bmpBytes = DesktopMainViewHelpers.readImageAsBmpBytes(input)
            val extracted = if (passwordChars != null && passwordChars.isNotEmpty()) {
                steganographyService.extractEncryptedText(bmpBytes, passwordChars)
            } else {
                steganographyService.extractText(bmpBytes)
            }
            stegoState = stegoState.copy(
                inputPathText = input.toString(),
                passwordDraft = password.orEmpty(),
                encryptedExtract = !password.isNullOrEmpty(),
                extractedMessage = extracted,
                statusText = "Extracted message from ${input.fileName}",
            )
            appendChatTranscript("[stego] extracted message from ${input.fileName}")
            extracted
        } catch (e: Exception) {
            val status = "Steganography extract failed: ${DesktopMainViewHelpers.fileTransferErrorMessage(e)}"
            stegoState = stegoState.copy(statusText = status)
            publish(ComposeConnectionEventKind.ERROR, status)
            null
        } finally {
            passwordChars?.fill('\u0000')
        }
    }

    fun refreshMediaDevices() {
        if (shuttingDown.get()) return
        val mediaService = rtcMediaDeviceService
        if (mediaService == null) {
            publishRealtimeDiagnostic("RTC media device service is not configured.")
            refreshRealtimeState()
            return
        }
        val microphones = buildMediaChoices(mediaService.audioCaptureDevices(), "System default microphone")
        val outputDevices = buildMediaChoices(mediaService.audioRenderDevices(), "System default speaker")
        val cameras = buildMediaChoices(mediaService.videoCaptureDevices(), "System default camera")
        mediaVoiceState = mediaVoiceState.copy(
            microphones = microphones,
            outputDevices = outputDevices,
            microphoneTestStatus = "Microphones refreshed: ${microphones.size}",
            speakerTestStatus = "Speakers refreshed: ${outputDevices.size}",
        )
        experimentalVideoState = experimentalVideoState.copy(cameras = cameras, cameraTestStatus = "Cameras refreshed: ${cameras.size}")
        publishRealtimeDiagnostic("Media devices refreshed: ${microphones.size} microphones, ${outputDevices.size} speakers, ${cameras.size} cameras.")
        refreshRealtimeState()
    }

    fun testMicrophone(deviceId: String? = mediaVoiceState.selectedMicrophone.deviceId): String {
        val mediaService = rtcMediaDeviceService
        val result = if (mediaService == null) {
            "Microphone test unavailable: RTC media device service is not configured."
        } else {
            mediaService.testAudioCaptureDevice(deviceId)
        }
        mediaVoiceState = mediaVoiceState.copy(selectedMicrophoneId = deviceId.orEmpty(), microphoneTestStatus = result)
        publishRealtimeDiagnostic(result)
        return result
    }

    fun selectMicrophone(deviceId: String?) {
        mediaVoiceState = mediaVoiceState.copy(selectedMicrophoneId = deviceId.orEmpty())
        publishRealtimeDiagnostic("Microphone selected: ${mediaVoiceState.selectedMicrophone}.")
    }

    fun testSpeaker(deviceId: String? = mediaVoiceState.selectedOutputDevice.deviceId): String {
        val mediaService = rtcMediaDeviceService
        val result = if (mediaService == null) {
            "Speaker test unavailable: RTC media device service is not configured."
        } else {
            mediaService.testAudioRenderDevice(deviceId)
        }
        mediaVoiceState = mediaVoiceState.copy(selectedOutputDeviceId = deviceId.orEmpty(), speakerTestStatus = result)
        publishRealtimeDiagnostic(result)
        return result
    }

    fun selectSpeaker(deviceId: String?) {
        mediaVoiceState = mediaVoiceState.copy(selectedOutputDeviceId = deviceId.orEmpty())
        publishRealtimeDiagnostic("Speaker output selected: ${mediaVoiceState.selectedOutputDevice}.")
    }

    fun testCamera(deviceId: String? = experimentalVideoState.selectedCamera.deviceId): String {
        val mediaService = rtcMediaDeviceService
        val result = if (mediaService == null) {
            "Camera test unavailable: RTC media device service is not configured."
        } else {
            mediaService.testVideoCaptureDevice(deviceId)
        }
        experimentalVideoState = experimentalVideoState.copy(selectedCameraId = deviceId.orEmpty(), cameraTestStatus = result)
        publishRealtimeDiagnostic(result)
        return result
    }

    fun selectCamera(deviceId: String?) {
        experimentalVideoState = experimentalVideoState.copy(selectedCameraId = deviceId.orEmpty())
        publishRealtimeDiagnostic("Camera selected: ${experimentalVideoState.selectedCamera}.")
    }

    fun startRealtimeSession(
        localPeer: String,
        remotePeer: String,
        mode: RtcSessionMode,
        dataChannelLabel: String = "securelan-data",
        audioDeviceId: String? = mediaVoiceState.selectedMicrophone.deviceId,
        videoDeviceId: String? = experimentalVideoState.selectedCamera.deviceId,
    ) {
        if (shuttingDown.get()) return
        val service = rtcSessionService
        if (service == null) {
            publishRealtimeDiagnostic("RTC session service is not configured.")
            return
        }
        if (!chatClientService.isConnected()) {
            publishRealtimeDiagnostic("RTC session blocked: connect to chat before starting realtime.")
            return
        }
        try {
            val snapshot = service.startSession(
                RtcSessionRequest(
                    localPeer.trim(),
                    remotePeer.trim(),
                    mode,
                    dataChannelLabel,
                    audioDeviceId.orEmpty(),
                    videoDeviceId.orEmpty(),
                ),
            )
            publishRealtimeDiagnostic(
                DesktopRealtimeFormatters.rtcStateDiagnostics(
                    mode,
                    snapshot.state ?: RtcSessionState.IDLE,
                    snapshot.remotePeer,
                    snapshot.message,
                ),
            )
        } catch (e: Exception) {
            val message = "RTC session failed: ${e.message ?: "unknown error"}"
            publishRealtimeDiagnostic(message)
            publish(ComposeConnectionEventKind.ERROR, message)
        }
        refreshRealtimeState()
    }

    fun closeRealtimeSession() {
        if (shuttingDown.get()) return
        rtcSessionService?.closeCurrentSession()
        publishRealtimeDiagnostic("RTC session close requested from Compose.")
        refreshRealtimeState()
    }

    fun startCameraPreview(deviceId: String? = experimentalVideoState.selectedCamera.deviceId) {
        if (shuttingDown.get()) return
        val mediaService = rtcMediaDeviceService
        if (mediaService == null) {
            val message = "Camera preview unavailable: RTC media device service is not configured."
            experimentalVideoState = experimentalVideoState.copy(
                previewRunning = false,
                latestPreviewFrame = null,
                cameraTestStatus = message,
            )
            publishRealtimeDiagnostic(message)
            return
        }
        closeCameraPreview()
        experimentalVideoState = experimentalVideoState.copy(
            selectedCameraId = deviceId.orEmpty(),
            previewRunning = true,
            latestPreviewFrame = null,
            cameraTestStatus = "Starting camera preview…",
        )
        val session = try {
            mediaService.startVideoPreview(deviceId) { event ->
                experimentalVideoState = experimentalVideoState.copy(latestPreviewFrame = event, previewRunning = true)
                publishRealtimeDiagnostic(DesktopRealtimeFormatters.cameraPreviewLiveStatus(event.width(), event.height()))
            }
        } catch (error: Throwable) {
            val message = "Camera preview failed: ${error::class.java.simpleName}: ${error.message.orEmpty()}"
            experimentalVideoState = experimentalVideoState.copy(
                previewRunning = false,
                latestPreviewFrame = null,
                cameraTestStatus = message,
            )
            publishRealtimeDiagnostic(message)
            return
        }
        val status = session.statusMessage()
        if (status.startsWith("Camera preview failed", ignoreCase = true)) {
            runCatching { session.close() }
            experimentalVideoState = experimentalVideoState.copy(
                previewRunning = false,
                latestPreviewFrame = null,
                cameraTestStatus = status,
            )
            publishRealtimeDiagnostic(status)
            return
        }
        cameraPreviewSession = session
        experimentalVideoState = experimentalVideoState.copy(
            selectedCameraId = deviceId.orEmpty(),
            previewRunning = true,
            cameraTestStatus = status,
        )
        publishRealtimeDiagnostic(status)
    }

    fun closeCameraPreview() {
        val session = cameraPreviewSession
        cameraPreviewSession = null
        try {
            session?.close()
        } catch (_: Exception) { /* best-effort */ }
        experimentalVideoState = experimentalVideoState.copy(
            previewRunning = false,
            latestPreviewFrame = null,
            cameraTestStatus = "Camera preview stopped.",
        )
    }

    // ---- lifecycle ----

    /** Shut down the adapter and all underlying services. */
    fun shutdown() {
        if (!shuttingDown.compareAndSet(false, true)) return
        publishEventKind(ComposeAdapterEventKind.CLEANUP_STARTED, "Adapter shutdown initiated.")
        pendingIncomingTransferDecisions.values.forEach { it.complete(false) }
        pendingIncomingTransferDecisions.clear()
        fileTransferIoScope.cancel()

        try {
            if (chatClientService.isConnected()) {
                chatClientService.disconnect()
            }
        } catch (_: Exception) { /* best-effort */ }

        try {
            if (fileTransferServerService.isRunning()) {
                fileTransferServerService.stop()
            }
        } catch (_: Exception) { /* best-effort */ }

        try {
            quickShareService.stop()
            refreshQuickShareState()
        } catch (_: Exception) { /* best-effort */ }

        try {
            closeCameraPreview()
        } catch (_: Exception) { /* best-effort */ }

        try {
            rtcSessionService?.close()
        } catch (_: Exception) { /* best-effort */ }

        try {
            rtcMediaDeviceService?.close()
        } catch (_: Exception) { /* best-effort */ }

        try {
            discoveryService.stop()
            currentConfig = null
            discoveredPeers = emptyList()
            publishPeerListDiagnostic("Discovery stopped during adapter shutdown; peer snapshot cleared.")
        } catch (_: Exception) { /* best-effort */ }

        try {
            if (chatServerService.isRunning()) {
                chatServerService.stop()
            }
        } catch (_: Exception) { /* best-effort */ }

        publishEventKind(ComposeAdapterEventKind.CLEANUP_COMPLETED, "Adapter shutdown complete.")
    }

    override fun close() {
        shutdown()
    }

    // ---- internal helpers ----

    private fun refreshState() {
        val serverRunning = chatServerService.isRunning()
        val clientConnected = chatClientService.isConnected()
        val discoveryActive = discoveryService.isRunning()
        val config = currentConfig

        statusState = statusState.copy(
            localServerRunning = serverRunning,
            clientConnected = clientConnected,
            serverStatus = if (serverRunning) "Room running" else "Server stopped",
            connectionStatus = if (clientConnected) "Connected" else "Connection idle",
            discoveryStatus = when {
                discoveryActive && config?.announceEnabled == true -> "Discovery active"
                discoveryActive -> "Discovery listen-only"
                else -> "Discovery not started"
            },
            discoverable = config?.announceEnabled ?: statusState.discoverable,
        )
        refreshRealtimeState()
        refreshMigrationReadinessState()
    }

    private fun publish(kind: ComposeConnectionEventKind, message: String) {
        adapterEvents = adapterEvents + ComposeConnectionEvent(kind, message)
    }

    private fun dispatchUiStateUpdate(action: () -> Unit) {
        if (shuttingDown.get()) return
        val customDispatcher = uiStateDispatcher
        if (customDispatcher != null) {
            customDispatcher(action)
            return
        }
        if (SwingUtilities.isEventDispatchThread()) {
            action()
        } else {
            SwingUtilities.invokeLater {
                if (!shuttingDown.get()) {
                    action()
                }
            }
        }
    }

    private fun runUiStateUpdateAndWait(action: () -> Unit) {
        val customDispatcher = uiStateDispatcher
        if (customDispatcher != null) {
            customDispatcher(action)
            return
        }
        if (SwingUtilities.isEventDispatchThread()) {
            action()
        } else {
            SwingUtilities.invokeAndWait {
                if (!shuttingDown.get()) {
                    action()
                }
            }
        }
    }

    private fun publishPeerListDiagnostic(message: String) {
        peerListDiagnostics = (peerListDiagnostics + message).takeLast(8)
    }

    private fun publishTransferDiagnostic(message: String) {
        val normalized = message.trim()
        if (normalized.isEmpty()) {
            return
        }
        val progressPrefix = "Transfer progress: "
        transferDiagnostics = if (normalized.startsWith(progressPrefix)) {
            val target = normalized.substringAfter(progressPrefix).substringBeforeLast(' ').trim()
            val retained = transferDiagnostics.filterNot { it.startsWith(progressPrefix) && it.substringAfter(progressPrefix).substringBeforeLast(' ').trim() == target }
            (retained + normalized).takeLast(8)
        } else {
            (transferDiagnostics + normalized).takeLast(8)
        }
    }

    private fun publishQuickShareDiagnostic(message: String) {
        quickShareDiagnostics = (quickShareDiagnostics + message).takeLast(8)
    }

    private fun publishRealtimeDiagnostic(message: String) {
        realtimeDiagnostics = (realtimeDiagnostics + message).takeLast(10)
    }

    private fun appendChatTranscript(line: String) {
        val normalized = normalizeTranscriptLine(line)
        if (normalized.isBlank()) {
            return
        }
        val timestamp = Instant.now()
        chatTranscript = (chatTranscript + normalized).takeLast(200)
        chatMessages = (chatMessages + ComposeChatMessage.fromTranscriptLine(normalized, timestamp)).takeLast(200)
        refreshMigrationReadinessState()
    }

    private fun formatChatMessage(sender: String, text: String): String {
        val normalizedSender = sender.trim().ifEmpty { "unknown" }
        val normalizedText = normalizeChatText(text, normalizedSender)
        return if (isSystemSender(normalizedSender)) {
            "[system] $normalizedText"
        } else {
            "$normalizedSender: $normalizedText"
        }
    }

    private fun normalizeTranscriptLine(line: String): String = line.trim()
        .replace(Regex("^system:\\s*\\[system]\\s*", RegexOption.IGNORE_CASE), "[system] ")
        .replace(Regex("^\\[system]\\s*system:\\s*", RegexOption.IGNORE_CASE), "[system] ")
        .replace(Regex("^\\[join]\\s*\\[join]\\s*", RegexOption.IGNORE_CASE), "[join] ")
        .replace(Regex("^\\[left]\\s*\\[left]\\s*", RegexOption.IGNORE_CASE), "[left] ")
        .replace(Regex("^\\[connected]\\s*([^>]+)->\\s*/([^\\s]+)"), "[connected] $1-> $2")

    private fun normalizeChatText(text: String?, sender: String): String {
        var normalized = text?.trim().orEmpty()
        val normalizedSender = Regex.escape(sender.trim())
        if (normalizedSender.isNotEmpty()) {
            normalized = normalized.replace(Regex("^(?:$normalizedSender):\\s*", RegexOption.IGNORE_CASE), "")
        }
        normalized = normalized
            .replace(Regex("^system:\\s*\\[system]\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^\\[system]\\s*system:\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^\\[system]\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^\\[join]\\s*", RegexOption.IGNORE_CASE), "")
        return normalized
    }

    private fun formatEndpoint(value: Any?): String {
        val raw = value?.toString()?.trim().orEmpty()
        if (raw.isBlank()) {
            return "unknown"
        }
        return raw.removePrefix("/")
    }

    private fun publishLocalNetworkInfo() {
        localNetworkInfo = try {
            DesktopMainViewHelpers.localNetworkInfoMessage(DesktopMainViewHelpers.resolveLocalLanIps())
        } catch (e: Exception) {
            DesktopMainViewHelpers.localNetworkInfoErrorMessage(e.message)
        }
        appendChatTranscript(localNetworkInfo)
        publishPeerListDiagnostic(localNetworkInfo)
    }

    private fun startFileTransferListener(filePort: Int, password: String) {
        if (fileTransferServerService.isRunning()) {
            return
        }
        fileTransferServerService.start(
            FileTransferServerConfig(
                filePort,
                downloadsPath,
                password,
                this::acceptIncomingFileTransfer,
            ),
        )
        publish(ComposeConnectionEventKind.SUCCESS, "File transfer listener started on port $filePort.")
        publishTransferDiagnostic("File transfer listener started on port $filePort.")
    }

    private fun handleFileTransferEvent(event: FileTransferEvent) {
        when (event) {
            is FileTransferStartedEvent -> {
                val entry = TransferEntry(
                    event.transferId ?: "transfer-${transferEntryMap.size + 1}",
                    event.fileName ?: "unknown file",
                    event.outgoing,
                    if (event.outgoing) "Sending" else "Receiving",
                    0,
                    event.totalBytes,
                )
                transferEntryMap[entry.transferId] = entry
                appendChatTranscript((if (event.outgoing) "[file-send] " else "[file-recv] ") + "started: ${entry.fileName}")
                publishTransferDiagnostic("Transfer started: ${entry.fileName}.")
            }

            is FileTransferProgressEvent -> {
                val transferId = event.transferId ?: return
                val existing = transferEntryMap[transferId] ?: return
                val progress = event.progress ?: return
                if (existing.active()) {
                    existing.status = if (event.outgoing) "Sending" else "Receiving"
                    existing.updateProgress(progress.transferredBytes, progress.percent(), progress.totalBytes)
                    publishTransferDiagnostic("Transfer progress: ${existing.fileName} ${existing.percent}%.")
                }
            }

            is FileTransferCompletedEvent -> {
                val transferId = event.transferId ?: "transfer-${transferEntryMap.size + 1}"
                val entry = transferEntryMap.getOrPut(transferId) {
                    TransferEntry(transferId, event.fileName ?: "unknown file", event.outgoing, "Completed", 100, event.totalBytes)
                }
                entry.status = "Completed"
                entry.percent = 100
                entry.totalBytes = event.totalBytes
                entry.stopSpeedTracking()
                appendChatTranscript((if (event.outgoing) "[file-send] " else "[file-recv] ") + "completed: ${event.path}")
                publishTransferDiagnostic("Transfer completed: ${entry.fileName}.")
            }

            is FileTransferFailedEvent -> {
                val transferId = event.transferId ?: "transfer-${transferEntryMap.size + 1}"
                val entry = transferEntryMap.getOrPut(transferId) {
                    TransferEntry(transferId, event.fileName ?: "unknown file", event.outgoing, "Failed", 0, 0)
                }
                entry.status = "Failed"
                entry.stopSpeedTracking()
                val message = event.message?.takeIf { it.isNotBlank() } ?: "unknown error"
                appendChatTranscript((if (event.outgoing) "[file-send] " else "[file-recv] ") + "failed: $message")
                publishTransferDiagnostic("Transfer failed: ${entry.fileName}: $message.")
            }
        }
        transferEntries = transferEntryMap.values.toList()
    }

    private fun handleQuickShareEvent(event: QuickShareEvent) {
        if (event.message().isNotBlank()) {
            publishQuickShareDiagnostic(
                DesktopQuickShareFormatters.formatEventDiagnostics(event.message(), event.remoteAddress()),
            )
        }
        refreshQuickShareState()
    }

    private fun refreshQuickShareState() {
        quickShareEntries = quickShareService.shares().map(::QuickShareEntry)
        if (quickShareService.isRunning()) {
            val landingUrls = quickShareService.landingUrls()
            quickShareStatus = DesktopQuickShareFormatters.formatServerStatus(quickShareService.port())
            quickShareLanding = DesktopQuickShareFormatters.formatLandingValue(landingUrls)
        } else {
            quickShareStatus = "Quick share idle"
            quickShareLanding = "Start quick share to get LAN browser links."
        }
    }

    private fun refreshRealtimeState() {
        val peerState = ComposePeerListState(peers = visiblePeerItems.map { ComposePeerListItem.fromPeer(it, chatClientService.isConnected()) }, selectedPeerIndex = if (visiblePeerItems.isEmpty()) -1 else 0)
        val runtimeStatus = rtcSessionService?.runtimeStatus() ?: RtcRuntimeStatus.unavailable("RTC session service is not configured in Compose.")
        val currentSession = rtcSessionService?.currentSession()?.orElse(null)
        mediaVoiceState = mediaVoiceState.copy(statusState = statusState, peerListState = peerState, runtimeStatus = runtimeStatus, currentSession = currentSession)
        experimentalVideoState = experimentalVideoState.copy(statusState = statusState, peerListState = peerState, runtimeStatus = runtimeStatus, currentSession = currentSession)
    }

    private fun buildDiagnosticsState(): ComposeDiagnosticsState = ComposeDiagnosticsState(
        statusState = statusState,
        peerListState = ComposePeerListState(
            peers = visiblePeerItems.map { ComposePeerListItem.fromPeer(it, chatClientService.isConnected()) },
            selectedPeerIndex = if (visiblePeerItems.isEmpty()) -1 else 0,
        ),
        chatDiagnostics = chatTranscript,
        fileTransferDiagnostics = transferDiagnostics,
        quickShareDiagnostics = quickShareDiagnostics,
        realtimeDiagnostics = realtimeDiagnostics,
    )

    private fun buildRegressionReadinessState(
        chatRuntimeValidated: Boolean = runCatching { regressionReadinessState.chatRuntimeValidated }.getOrDefault(false),
        fileTransferRuntimeValidated: Boolean = runCatching { regressionReadinessState.fileTransferRuntimeValidated }.getOrDefault(false),
        quickShareRuntimeValidated: Boolean = runCatching { regressionReadinessState.quickShareRuntimeValidated }.getOrDefault(false),
        steganographyRuntimeValidated: Boolean = runCatching { regressionReadinessState.steganographyRuntimeValidated }.getOrDefault(false),
        voiceRuntimeValidated: Boolean = runCatching { regressionReadinessState.voiceRuntimeValidated }.getOrDefault(false),
        videoRuntimeValidated: Boolean = runCatching { regressionReadinessState.videoRuntimeValidated }.getOrDefault(false),
        fullRuntimeRegressionValidated: Boolean = runCatching { regressionReadinessState.fullRuntimeRegressionValidated }.getOrDefault(false),
    ): ComposeRegressionReadinessState {
        val peerState = ComposePeerListState(
            peers = visiblePeerItems.map { ComposePeerListItem.fromPeer(it, chatClientService.isConnected()) },
            selectedPeerIndex = if (visiblePeerItems.isEmpty()) -1 else 0,
        )
        return ComposeRegressionReadinessState(
            statusState = statusState,
            peerListState = peerState,
            chatState = ComposeChatWorkspaceState(
                statusState = statusState,
                peerListState = peerState,
                messages = chatMessages.ifEmpty { chatTranscript.map { ComposeChatMessage.fromTranscriptLine(it) } },
            ),
            fileTransferState = ComposeFileTransferState(
                statusState = statusState,
                peerListState = peerState,
                entries = transferEntries,
                incomingPrompts = incomingTransferPrompts,
            ),
            quickShareState = ComposeQuickShareState(
                running = quickShareRunning,
                entries = quickShareEntries,
                landingUrls = quickShareService.landingUrls(),
            ),
            steganographyState = stegoState,
            mediaVoiceState = mediaVoiceState,
            experimentalVideoState = experimentalVideoState,
            diagnosticsState = buildDiagnosticsState(),
            chatRuntimeValidated = chatRuntimeValidated,
            fileTransferRuntimeValidated = fileTransferRuntimeValidated,
            quickShareRuntimeValidated = quickShareRuntimeValidated,
            steganographyRuntimeValidated = steganographyRuntimeValidated,
            voiceRuntimeValidated = voiceRuntimeValidated,
            videoRuntimeValidated = videoRuntimeValidated,
            fullRuntimeRegressionValidated = fullRuntimeRegressionValidated,
            runtimeEvidenceRecords = runtimeEvidenceRecords,
        )
    }

    private fun refreshMigrationReadinessState() {
        regressionReadinessState = buildRegressionReadinessState()
    }

    private fun handleRtcEvent(event: RtcEvent) {
        when (event) {
            is RtcStateChangedEvent -> {
                publishRealtimeDiagnostic(
                    DesktopRealtimeFormatters.rtcStateDiagnostics(
                        event.mode ?: RtcSessionMode.DATA,
                        event.state ?: RtcSessionState.IDLE,
                        event.remotePeer,
                        event.message,
                    ),
                )
            }
            is RtcRuntimeWarningEvent -> {
                publishRealtimeDiagnostic(DesktopRealtimeFormatters.rtcWarningDiagnostics(event.message.orEmpty()))
            }
            is RtcAudioLevelEvent -> {
                if (event.local) {
                    mediaVoiceState = mediaVoiceState.copy(localAudioLevel = event.level)
                } else {
                    mediaVoiceState = mediaVoiceState.copy(remoteAudioLevel = event.level)
                }
            }
            is RtcVideoFrameEvent -> {
                experimentalVideoState = experimentalVideoState.copy(latestPreviewFrame = event)
            }
            else -> {
                publishRealtimeDiagnostic("RTC event: ${event.javaClass.simpleName}.")
            }
        }
        refreshRealtimeState()
    }

    private fun buildMediaChoices(devices: List<com.shterneregen.securelan.webrtc.service.RtcMediaDevice>, defaultLabel: String): List<MediaDeviceChoice> =
        buildList {
            add(MediaDeviceChoice.systemDefault(defaultLabel))
            devices.mapTo(this, MediaDeviceChoice::of)
        }

    private fun connectLocalHostedChat(chatPort: Int, nickname: String, password: String) {
        if (chatClientService.isConnected()) {
            return
        }
        val connected = chatClientService.connect(
            ChatClientConnectRequest(
                "127.0.0.1",
                chatPort,
                nickname,
                password,
                desktopCapabilities(statusState.serverFilePort ?: NetworkConstants.DEFAULT_FILE_TRANSFER_PORT),
            ),
        )
        if (connected) {
            publish(ComposeConnectionEventKind.SUCCESS, "Joined local hosted room as $nickname.")
            publishEventKind(ComposeAdapterEventKind.CONNECTED, "Connected locally: $nickname")
        } else {
            publish(ComposeConnectionEventKind.ERROR, "Local hosting connection failed.")
            publishEventKind(ComposeAdapterEventKind.CONNECT_FAILED, "Local hosting connection failed.")
        }
    }

    private fun startPeerDiscoveryListener() {
        if (chatServerService.isRunning()) {
            val chatPort = statusState.serverChatPort ?: NetworkConstants.DEFAULT_CHAT_PORT
            val filePort = statusState.serverFilePort ?: NetworkConstants.DEFAULT_FILE_TRANSFER_PORT
            startPeerDiscovery(PeerDiscoveryConfig.defaults(localPeerId, statusState.nickname.trim(), chatPort, filePort, statusState.discoverable), hosting = true)
        } else {
            startPeerDiscovery(PeerDiscoveryConfig.listenOnly(localPeerId, statusState.nickname.trim()), hosting = false)
        }
    }

    private fun startPeerDiscoveryListenOnly(nickname: String) {
        val trimmedNick = nickname.trim().ifBlank { statusState.nickname.trim() }
        if (trimmedNick.isBlank()) return
        startPeerDiscovery(PeerDiscoveryConfig.listenOnly(localPeerId, trimmedNick), hosting = false)
    }

    private fun startPeerDiscovery(discoveryConfig: PeerDiscoveryConfig, hosting: Boolean) {
        if (discoveryConfig.nickname.isBlank()) return
        currentConfig = discoveryConfig
        discoveryService.start(discoveryConfig, discoveryListener)
        discoveredPeers = discoveryService.snapshot()
        if (discoveryService.isRunning()) {
            val message = if (hosting) {
                DesktopMainViewHelpers.discoveryStartedMessage(discoveryConfig)
            } else {
                DesktopMainViewHelpers.discoveryListeningMessage(discoveryConfig.discoveryPort)
            }
            publish(ComposeConnectionEventKind.INFO, message)
            publishPeerListDiagnostic("$message; snapshot=${discoveredPeers.size}.")
        }
    }

    private fun upsertJoinedPeer(event: ChatUserJoinedEvent): PeerPresence? {
        val nickname = event.nickname ?: return null
        if (chatServerService.isRunning() && !event.remoteAddress.isNullOrBlank()) {
            val host = DesktopMainViewHelpers.hostFromRemoteAddress(event.remoteAddress)
            if (host.isNotBlank()) {
                return upsertChatPeer(
                    nickname = nickname,
                    online = true,
                    peerId = null,
                    host = host,
                    chatPort = statusState.serverChatPort ?: NetworkConstants.DEFAULT_CHAT_PORT,
                    filePort = filePortFromCapabilities(event.capabilities, inferredClientFilePort()),
                    lastSeen = Instant.now(),
                    capabilities = event.capabilities,
                )
            }
        }
        return upsertChatPeer(nickname, online = true, filePort = filePortFromCapabilities(event.capabilities, 0), capabilities = event.capabilities)
    }

    private fun desktopCapabilities(filePort: Int): PeerCapabilities = PeerCapabilities.desktop(APP_VERSION, filePort)

    private fun filePortFromCapabilities(capabilities: PeerCapabilities, fallback: Int): Int =
        if (capabilities.supportsFileReceive() && capabilities.fileReceivePort() > 0) capabilities.fileReceivePort() else fallback

    private fun upsertChatPeer(
        nickname: String?,
        online: Boolean,
        peerId: String? = null,
        host: String? = null,
        chatPort: Int = 0,
        filePort: Int = 0,
        lastSeen: Instant? = null,
        capabilities: PeerCapabilities = PeerCapabilities.unknown(),
    ): PeerPresence? {
        if (nickname.isNullOrBlank() || isSystemSender(nickname) || isLocalNickname(nickname)) {
            return null
        }
        val existing = chatPeers.firstOrNull { DesktopMainViewHelpers.samePeer(it, nickname, peerId) }
        if (existing != null) {
            existing.apply(online, peerId, host, chatPort, filePort, lastSeen, capabilities)
            refreshChatPeers()
            return existing
        }
        val created = PeerPresence(nickname, online, peerId, host, chatPort, filePort, lastSeen, capabilities)
        chatPeers = (chatPeers + created).sortedWith(
            Comparator.comparing(PeerPresence::online).reversed()
                .thenComparing(PeerPresence::nickname, String.CASE_INSENSITIVE_ORDER),
        )
        refreshMigrationReadinessState()
        return created
    }

    private fun markChatPeerOffline(nickname: String?): Boolean {
        if (nickname.isNullOrBlank() || isSystemSender(nickname) || isLocalNickname(nickname)) {
            return false
        }
        val peer = chatPeers.firstOrNull { it.nickname().equals(nickname, ignoreCase = true) } ?: return false
        val changed = peer.markOffline()
        if (changed) {
            refreshChatPeers()
        }
        return changed
    }

    private fun clearChatPeers() {
        if (chatPeers.isNotEmpty()) {
            chatPeers = emptyList()
            refreshMigrationReadinessState()
        }
    }

    private fun refreshChatPeers() {
        chatPeers = chatPeers.sortedWith(
            Comparator.comparing(PeerPresence::online).reversed()
                .thenComparing(PeerPresence::nickname, String.CASE_INSENSITIVE_ORDER),
        )
        refreshMigrationReadinessState()
    }

    private fun inferredClientFilePort(): Int = DesktopMainViewHelpers.resolveInferredClientFilePort(
        statusState.serverFilePortText.trim(),
        NetworkConstants.DEFAULT_FILE_TRANSFER_PORT,
        COMPOSE_CLIENT_FILE_PORT_OFFSET,
    )

    private fun isLocalPeer(peer: DiscoveredPeer): Boolean = peer.peerId == localPeerId || isLocalNickname(peer.nickname)

    private fun isLocalPeer(peer: PeerPresence): Boolean = peer.peerId() == localPeerId || isLocalNickname(peer.nickname())

    private fun isLocalNickname(nickname: String?): Boolean = !nickname.isNullOrBlank() && nickname.equals(statusState.nickname.trim(), ignoreCase = true)

    private fun isSystemSender(nickname: String?): Boolean = nickname != null && nickname.equals("system", ignoreCase = true)

    private fun peerKey(peer: PeerPresence): String = peer.peerId()?.takeIf { it.isNotBlank() } ?: "nick:${peer.nickname().lowercase()}"

    private fun publishEventKind(kind: ComposeAdapterEventKind, detail: String) {
        val prefix = kind.name.lowercase().replace('_', ' ')
        publish(
            ComposeConnectionEventKind.INFO,
            "[$prefix] $detail",
        )
    }

    private companion object {
        private const val APP_VERSION = "0.5.0"
        const val COMPOSE_CLIENT_FILE_PORT_OFFSET: Int = 1000
    }
}
