package com.shterneregen.securelan.androidclient

import android.annotation.SuppressLint
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.LocaleList
import android.provider.OpenableColumns
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shterneregen.securelan.androidclient.call.*
import com.shterneregen.securelan.common.model.rtc.RtcSignalCodec
import com.shterneregen.securelan.chat.service.impl.DefaultRandomNicknameService
import com.shterneregen.securelan.androidclient.model.AppLogEntry
import com.shterneregen.securelan.androidclient.model.AppLanguage
import com.shterneregen.securelan.androidclient.model.ChatLine
import com.shterneregen.securelan.androidclient.model.DiscoveredPeer
import com.shterneregen.securelan.androidclient.model.IncomingFileProgress
import com.shterneregen.securelan.androidclient.model.MainUiState
import com.shterneregen.securelan.androidclient.model.NearbyPermissionState
import com.shterneregen.securelan.androidclient.model.PeerRole
import com.shterneregen.securelan.androidclient.model.SecureLanPorts
import com.shterneregen.securelan.androidclient.model.ThemeMode
import com.shterneregen.securelan.androidclient.model.TransferDirection
import com.shterneregen.securelan.androidclient.model.TransferRecord
import com.shterneregen.securelan.androidclient.model.TransferResult
import com.shterneregen.securelan.androidclient.network.AndroidLocalNetworkAddressResolver
import com.shterneregen.securelan.androidclient.network.PeerDiscoveryRepository
import com.shterneregen.securelan.androidclient.network.AndroidChatHostService
import com.shterneregen.securelan.androidclient.network.AndroidHostServiceState
import com.shterneregen.securelan.androidclient.network.SecureChatClient
import com.shterneregen.securelan.androidclient.network.SecureFileReceiver
import com.shterneregen.securelan.androidclient.network.SecureFileSender
import com.shterneregen.securelan.androidclient.network.AndroidQuickShareServer
import com.shterneregen.securelan.androidclient.network.AndroidQuickShareItem
import com.shterneregen.securelan.androidclient.protocol.WireMessageType
import com.shterneregen.securelan.androidclient.transfer.BatchStatus
import com.shterneregen.securelan.androidclient.transfer.TransferCoordinator
import com.shterneregen.securelan.androidclient.transfer.TransferPhase
import com.shterneregen.securelan.androidclient.transfer.TransferQueueState
import com.shterneregen.securelan.androidclient.transfer.TransferRecipient
import com.shterneregen.securelan.androidclient.transfer.TransferSource
import com.shterneregen.securelan.chat.protocol.handshake.PeerCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val randomNicknameService = DefaultRandomNicknameService()
    private val discoveryRepository = PeerDiscoveryRepository()
    private val chatClient = SecureChatClient()
    private val fileSender = SecureFileSender(application.contentResolver, application.cacheDir)
    private val fileReceiver = SecureFileReceiver(application)
    private val localNetworkAddressResolver = AndroidLocalNetworkAddressResolver(application)
    private val quickShareServer = AndroidQuickShareServer(application.contentResolver, application.cacheDir)
    private val _quickShares = MutableStateFlow<List<AndroidQuickShareItem>>(emptyList())
    val quickShares: StateFlow<List<AndroidQuickShareItem>> = _quickShares.asStateFlow()
    @Volatile var quickShareUrls: List<String> = emptyList(); private set

    fun createQuickShareText(text: String, minutes: Long, limit: Int?) {
        viewModelScope.launch(Dispatchers.IO) { runCatching { quickShareServer.createText(text, minutes, limit) }.onSuccess { refreshQuickShares() } }
    }
    fun createQuickShareFile(uri: Uri, name: String, minutes: Long, limit: Int?) {
        viewModelScope.launch(Dispatchers.IO) { runCatching { quickShareServer.createFile(uri, name, minutes, limit) }.onSuccess { refreshQuickShares() } }
    }
    fun stopQuickShare(id: String) { quickShareServer.stopShare(id); refreshQuickShares() }
    fun refreshQuickShares() { _quickShares.value = quickShareServer.items(); quickShareUrls = quickShareServer.urls }

    private var connectionGeneration = 0L
    private val transferCoordinator = TransferCoordinator(
        scope = viewModelScope,
        isAvailable = { recipient -> isTransferRecipientAvailable(recipient) },
        send = { recipient, source, onUpdate ->
            val current = _uiState.value
            fileSender.sendFile(
                host = recipient.host,
                port = recipient.port,
                senderId = current.nickname,
                recipientId = recipient.name,
                sessionPassword = current.sessionPassword,
                selectedFile = source,
                onUpdate = onUpdate,
            )
        },
    )
    val transferQueue: StateFlow<TransferQueueState> = transferCoordinator.state

    private val _uiState = MutableStateFlow(
        MainUiState(
            nickname = preferences.getString(KEY_NICKNAME, null)?.takeIf { it.isNotBlank() }
                ?: randomNicknameService.generate(),
            manualHost = preferences.getString(KEY_MANUAL_HOST, "").orEmpty(),
            manualChatPort = preferences.getInt(KEY_MANUAL_CHAT_PORT, SecureLanPorts.DEFAULT_CHAT_PORT).toString(),
            manualFilePort = preferences.getInt(KEY_MANUAL_FILE_PORT, SecureLanPorts.DEFAULT_FILE_TRANSFER_PORT).toString(),
            themeMode = preferences.getString(KEY_THEME_MODE, null)
                ?.let { saved -> ThemeMode.entries.firstOrNull { it.name == saved } }
                ?: ThemeMode.SYSTEM,
            appLanguage = preferences.getString(KEY_APP_LANGUAGE, null)
                ?.let { saved -> AppLanguage.entries.firstOrNull { it.name == saved } }
                ?: AppLanguage.SYSTEM,
            notificationsEnabled = preferences.getBoolean(KEY_NOTIFICATIONS, true),
            autoReceiveFiles = preferences.getBoolean(KEY_AUTO_RECEIVE, true),
        ),
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val callMedia = WebRtcCallMedia(application, viewModelScope)
    private val incomingCallNotification = IncomingCallNotification(application)
    val callAudio = AudioRouteManager(application) { calls.end() }
    val calls: CallCoordinator = CallCoordinator(
        scope = viewModelScope,
        nickname = { _uiState.value.nickname },
        available = { _uiState.value.connected },
        send = chatClient::sendSignal,
        media = callMedia,
        prepare = { id, video ->
            CallForegroundService.start(application, id, calls.state.value.peer, video, { calls.end() }, { toggleCallMute() })
            callAudio.start(video)
        },
        release = { try { callAudio.stop() } finally { CallForegroundService.stop(application) } },
        now = android.os.SystemClock::elapsedRealtime,
    )

    fun startCall(peer: String, video: Boolean) {
        val supportedVideo = _uiState.value.callPeers[peer] ?: return
        calls.start(peer, video && supportedVideo)
    }
    fun toggleCallMute() { runCatching { callMedia.toggleMute() }.onFailure { calls.end() } }
    fun toggleCallCamera() { runCatching { callMedia.toggleCamera() }.onFailure { calls.end() } }
    fun switchCallCamera() { runCatching { callMedia.switchCamera() }.onFailure { calls.end() } }
    fun toggleCallSpeaker() { runCatching { callAudio.toggleSpeaker() }.onFailure { calls.end() } }

    private var discoveryJob: Job? = null
    private var discoveryTimeoutJob: Job? = null
    private var receiveJob: Job? = null
    private var fileReceiverJob: Job? = null
    private var localAddressRefreshJob: Job? = null
    private var localAddressWatchJob: Job? = null
    private var disconnectRequested: Boolean = false
    private var hostConnectionAttemptedForPeerId: String? = null
    private val hostPeerId: String = preferences.getString(KEY_HOST_PEER_ID, null)
        ?.takeIf { it.isNotBlank() }
        ?: UUID.randomUUID().toString().also { generated ->
            preferences.edit { putString(KEY_HOST_PEER_ID, generated) }
        }

    init {
        viewModelScope.launch { calls.state.collect { incomingCallNotification.update(it) } }
        createNotificationChannel()
        observeHostService()
        observeTransferQueue()
        refreshLocalRoomAddresses()
    }

    fun updateNickname(value: String) {
        val sanitized = value.filterNot(Char::isWhitespace)
        _uiState.update { it.copy(nickname = sanitized) }
        preferences.edit { putString(KEY_NICKNAME, sanitized) }
    }

    fun updateSessionPassword(value: String) = _uiState.update {
        it.copy(sessionPassword = value.filterNot(Char::isWhitespace))
    }

    fun updateThemeMode(value: ThemeMode) {
        _uiState.update { it.copy(themeMode = value) }
        preferences.edit { putString(KEY_THEME_MODE, value.name) }
    }

    fun updateAppLanguage(value: AppLanguage) {
        _uiState.update { it.copy(appLanguage = value) }
        preferences.edit { putString(KEY_APP_LANGUAGE, value.name) }
        createNotificationChannel()
    }

    fun updateNotificationsEnabled(value: Boolean) {
        _uiState.update { it.copy(notificationsEnabled = value) }
        preferences.edit { putBoolean(KEY_NOTIFICATIONS, value) }
    }

    fun updateAutoReceiveFiles(value: Boolean) {
        _uiState.update { it.copy(autoReceiveFiles = value) }
        preferences.edit { putBoolean(KEY_AUTO_RECEIVE, value) }
        if (value && _uiState.value.connected) {
            startFileReceiver()
        } else if (!value) {
            stopFileReceiver()
        }
    }

    fun updateNearbyPermission(state: NearbyPermissionState) {
        _uiState.update { it.copy(nearbyPermissionState = state) }
        if (state == NearbyPermissionState.GRANTED || state == NearbyPermissionState.NOT_REQUIRED) {
            startDiscovery()
        }
    }

    fun updateNetworkAvailable(available: Boolean) {
        if (!available) viewModelScope.launch { calls.connectionLost() }
        _uiState.update { it.copy(networkAvailable = available) }
        refreshLocalRoomAddresses()
        if (!available && discoveryJob != null) {
            stopDiscovery()
        }
    }

    fun updateInputMessage(value: String) = _uiState.update { it.copy(inputMessage = value) }

    fun selectPeer(peer: DiscoveredPeer) {
        val previousRecipient = _uiState.value.fileRecipient
        _uiState.update {
            val connectionPeer = if (peer.role == PeerRole.SERVER) peer else it.connectionPeer
            it.copy(selectedPeer = peer, connectionPeer = connectionPeer, status = "Selected ${peer.nickname}")
        }
        if (transferRecipientKey(previousRecipient) != transferRecipientKey(_uiState.value.fileRecipient)) {
            connectionGeneration++
        }
        addLog("Selected peer ${peer.nickname} at ${peer.host}:${peer.chatPort}")
    }

    fun startDiscovery() {
        if (discoveryJob != null) return
        if (_uiState.value.nearbyPermissionState == NearbyPermissionState.REQUIRED ||
            _uiState.value.nearbyPermissionState == NearbyPermissionState.DENIED
        ) return
        _uiState.update { it.copy(discoveryRunning = true, discoveryTimedOut = false, status = "Listening for SecureLan peers") }
        addLog("Started peer discovery")
        discoveryTimeoutJob?.cancel()
        discoveryTimeoutJob = viewModelScope.launch {
            delay(DISCOVERY_EMPTY_STATE_DELAY_MS.milliseconds)
            if (_uiState.value.peers.none { it.role == PeerRole.SERVER }) {
                _uiState.update { it.copy(discoveryTimedOut = true) }
            }
        }
        discoveryJob = viewModelScope.launch {
            runCatching {
                discoveryRepository.discoverPeers().collect { peer ->
                    if (peer.peerId == hostPeerId) return@collect
                    _uiState.update { state ->
                        val serverPeer = peer.copy(role = PeerRole.SERVER)
                        val peers = upsertPeer(state.peers, serverPeer)
                            .sortedBy { it.nickname.lowercase() }
                        val selectedPeer = state.selectedPeer?.let { selected ->
                            if (samePeer(selected, serverPeer)) serverPeer else selected
                        }
                        val connectionPeer = state.connectionPeer?.let { current ->
                            if (samePeer(current, serverPeer)) serverPeer else current
                        } ?: serverPeer
                        state.copy(
                            peers = peers,
                            selectedPeer = selectedPeer,
                            connectionPeer = connectionPeer,
                            discoveryTimedOut = false,
                            status = "Found ${peers.size} peer(s)",
                        )
                    }
                }
            }.onFailure { error ->
                if (error !is kotlinx.coroutines.CancellationException) {
                    discoveryJob = null
                    discoveryTimeoutJob?.cancel()
                    discoveryTimeoutJob = null
                    _uiState.update {
                        it.copy(discoveryRunning = false, discoveryTimedOut = true, status = "Device search stopped")
                    }
                    addLog(error.message ?: "Device search stopped unexpectedly", level = "ERROR")
                }
            }
        }
    }

    fun connectManualPeer(hostValue: String, chatPortValue: String, filePortValue: String) {
        val host = hostValue.trim().removeSurrounding("[", "]")
        val chatPort = chatPortValue.toIntOrNull()?.takeIf { it in 1..65535 }
        val filePort = filePortValue.toIntOrNull()?.takeIf { it in 1..65535 }
        if (host.isBlank() || host.any { it.isWhitespace() } || "://" in host || '/' in host) {
            setError("Enter a valid IP address or host name")
            return
        }
        if (chatPort == null || filePort == null) {
            setError("Ports must be between 1 and 65535")
            return
        }
        val peer = DiscoveredPeer(
            peerId = "manual:$host:$chatPort",
            nickname = host,
            host = host,
            chatPort = chatPort,
            filePort = filePort,
            role = PeerRole.SERVER,
            fileTargetHost = host,
            fileTargetPort = filePort,
        )
        preferences.edit {
            putString(KEY_MANUAL_HOST, host)
            putInt(KEY_MANUAL_CHAT_PORT, chatPort)
            putInt(KEY_MANUAL_FILE_PORT, filePort)
        }
        _uiState.update { state ->
            state.copy(
                manualHost = host,
                manualChatPort = chatPort.toString(),
                manualFilePort = filePort.toString(),
                peers = upsertPeer(state.peers, peer).sortedBy { it.nickname.lowercase() },
                selectedPeer = peer,
                connectionPeer = peer,
                error = null,
                status = "Connecting to $host",
            )
        }
        addLog("Added manual desktop $host:$chatPort")
        connectSelectedPeer()
    }

    fun restartDiscovery() {
        refreshLocalRoomAddresses()
        val previousJob = discoveryJob
        if (previousJob == null) {
            startDiscovery()
            return
        }
        stopDiscovery()
        viewModelScope.launch {
            previousJob.join()
            startDiscovery()
        }
    }

    fun refreshLocalRoomAddresses() {
        localAddressRefreshJob?.cancel()
        localAddressRefreshJob = viewModelScope.launch {
            val addresses = withContext(Dispatchers.IO) { localNetworkAddressResolver.resolve() }
            _uiState.update { state ->
                if (state.localRoomAddresses == addresses) state else state.copy(localRoomAddresses = addresses)
            }
        }
    }

    fun startFileReceiver() {
        if (fileReceiverJob != null) return
        val state = _uiState.value
        val password = state.sessionPassword
        if (password.isBlank()) {
            setError("Session password must not be blank to receive files")
            return
        }
        val listenPort = localFileReceiverPort(state)
        _uiState.update { it.copy(fileReceiverRunning = true, status = "File receiver listening on port $listenPort") }
        addLog("File receiver listening on port $listenPort")
        fileReceiverJob = viewModelScope.launch {
            runCatching {
                fileReceiver.serve(
                    port = listenPort,
                    sessionPassword = password,
                    onStarted = { metadata ->
                        _uiState.update {
                            it.copy(
                                incomingFileProgress = IncomingFileProgress(
                                    fileName = metadata.fileName,
                                    totalBytes = metadata.fileSize,
                                    active = true,
                                ),
                                status = "Receiving ${metadata.fileName}",
                            )
                        }
                        addLog("Receiving file ${metadata.fileName} (${metadata.fileSize} bytes) from ${metadata.senderId}")
                    },
                    onProgress = { metadata, received ->
                        _uiState.update {
                            it.copy(
                                incomingFileProgress = it.incomingFileProgress.copy(
                                    fileName = metadata.fileName,
                                    bytesReceived = received,
                                    totalBytes = metadata.fileSize,
                                    active = true,
                                ),
                            )
                        }
                    },
                    onCompleted = { metadata, receivedFile ->
                        _uiState.update {
                            it.copy(
                                incomingFileProgress = it.incomingFileProgress.copy(
                                    fileName = metadata.fileName,
                                    bytesReceived = metadata.fileSize,
                                    totalBytes = metadata.fileSize,
                                    active = false,
                                    completedPath = receivedFile.savedPath,
                                    error = null,
                                ),
                                recentTransfers = listOf(
                                    TransferRecord(
                                        fileName = metadata.fileName,
                                        bytes = metadata.fileSize,
                                        direction = TransferDirection.RECEIVED,
                                        result = TransferResult.COMPLETED,
                                        peerName = metadata.senderId,
                                        savedPath = receivedFile.savedPath,
                                        contentUri = receivedFile.contentUri,
                                    ),
                                ) + it.recentTransfers.take(MAX_RECENT_TRANSFERS - 1),
                                status = "Received ${metadata.fileName}",
                            )
                        }
                        addLog("Received file ${metadata.fileName}; saved to ${receivedFile.savedPath}")
                        postFileReceivedNotification(metadata.fileName, receivedFile.savedPath)
                    },
                    onError = { message, _ ->
                        _uiState.update {
                            it.copy(
                                incomingFileProgress = it.incomingFileProgress.copy(active = false, error = message),
                                recentTransfers = listOf(
                                    TransferRecord(
                                        fileName = it.incomingFileProgress.fileName.ifBlank { "Incoming file" },
                                        bytes = it.incomingFileProgress.totalBytes,
                                        direction = TransferDirection.RECEIVED,
                                        result = TransferResult.FAILED,
                                    ),
                                ) + it.recentTransfers.take(MAX_RECENT_TRANSFERS - 1),
                                status = message,
                            )
                        }
                        addLog(message, level = "ERROR")
                    },
                )
            }.onFailure { error ->
                fileReceiverJob = null
                if (error !is kotlinx.coroutines.CancellationException) {
                    _uiState.update {
                        it.copy(fileReceiverRunning = false, error = error.message, status = "File receiver stopped")
                    }
                    addLog(error.message ?: "File receiver stopped unexpectedly", level = "ERROR")
                }
            }
        }
    }

    fun stopFileReceiver() {
        fileReceiverJob?.cancel()
        fileReceiverJob = null
        _uiState.update { it.copy(fileReceiverRunning = false) }
        addLog("File receiver stopped")
    }

    fun stopDiscovery() {
        discoveryJob?.cancel()
        discoveryJob = null
        discoveryTimeoutJob?.cancel()
        discoveryTimeoutJob = null
        _uiState.update { it.copy(discoveryRunning = false, discoveryTimedOut = false, status = "Discovery stopped") }
        addLog("Peer discovery stopped")
    }

    fun connectSelectedPeer() {
        val state = _uiState.value
        val peer = state.selectedPeer?.takeIf { it.role == PeerRole.SERVER }
            ?: state.connectionPeer
            ?: state.peers.firstOrNull { it.role == PeerRole.SERVER }
            ?: return setError("Select a server peer first")
        if (state.nickname.isBlank()) return setError("Nickname must not be blank")
        connectToPeer(peer, state.sessionPassword, localFileReceiverPortFor(peer))
    }

    fun startHosting(chatPortValue: String, filePortValue: String) {
        val state = _uiState.value
        val chatPort = chatPortValue.toIntOrNull()?.takeIf { it in 1..65535 }
        val filePort = filePortValue.toIntOrNull()?.takeIf { it in 1..65535 }
        if (state.nickname.isBlank()) return setError("Nickname must not be blank")
        if (state.sessionPassword.isBlank()) return setError("Room password must not be blank")
        if (chatPort == null || filePort == null) return setError("Ports must be between 1 and 65535")
        if (chatPort == filePort) return setError("Chat and file ports must be different")
        if (state.connected || state.connecting) return setError("Disconnect from the current room before hosting")
        if (state.hosting || state.hostingStarting) return

        hostConnectionAttemptedForPeerId = null
        _uiState.update {
            it.copy(
                hostingStarting = true,
                hostChatPort = chatPort.toString(),
                hostFilePort = filePort.toString(),
                error = null,
                status = "Starting hosted room",
            )
        }
        addLog("Starting Android-hosted room on chat port $chatPort and file port $filePort")
        runCatching {
            AndroidChatHostService.start(
                context = getApplication(),
                peerId = hostPeerId,
                nickname = state.nickname.trim(),
                password = state.sessionPassword,
                chatPort = chatPort,
                filePort = filePort,
            )
        }.onFailure { error ->
            _uiState.update { it.copy(hostingStarting = false) }
            setError(error.message ?: "Unable to start hosted room")
        }
    }

    fun stopHosting() {
        calls.connectionLost()
        _uiState.update { it.copy(callPeers = emptyMap()) }
        if (!_uiState.value.hosting && !_uiState.value.hostingStarting) return
        hostConnectionAttemptedForPeerId = null
        disconnectRequested = true
        receiveJob?.cancel()
        receiveJob = null
        fileReceiverJob?.cancel()
        fileReceiverJob = null
        connectionGeneration++
        fileSender.close()
        _uiState.update { it.copy(hostingStarting = false, status = "Stopping hosted room") }
        viewModelScope.launch {
            chatClient.disconnect()
            AndroidChatHostService.stop(getApplication())
            _uiState.update {
                it.copy(
                    connected = false,
                    connecting = false,
                    fileReceiverRunning = false,
                    hostedParticipantCount = 0,
                    status = "Hosted room stopped",
                )
            }
            addLog("Android-hosted room stopped")
        }
    }

    private fun connectToPeer(peer: DiscoveredPeer, password: String, localFilePort: Int) {
        val state = _uiState.value
        if (state.connecting) return
        if (state.connected) return setError("Disconnect from the current room before connecting")
        _uiState.update { it.copy(connecting = true, callPeers = emptyMap(), error = null, status = "Connecting to ${peer.nickname}") }
        disconnectRequested = false
        addLog("Connecting to ${peer.nickname} at ${peer.host}:${peer.chatPort}")
        viewModelScope.launch {
            runCatching {
                chatClient.connect(
                    peer.host,
                    peer.chatPort,
                    state.nickname.trim(),
                    password,
                    androidCapabilities(localFilePort),
                )
            }.onSuccess { acceptedNickname ->
                _uiState.update {
                    it.copy(
                        nickname = acceptedNickname,
                        selectedPeer = peer,
                        connectionPeer = peer,
                        connected = true,
                        connecting = false,
                        status = "Connected as $acceptedNickname",
                        error = null,
                    )
                }
                preferences.edit { putString(KEY_NICKNAME, acceptedNickname) }
                connectionGeneration++
                addLog("Connected as $acceptedNickname")
                startReceiving()
                if (_uiState.value.autoReceiveFiles) startFileReceiver()
            }.onFailure { error ->
                _uiState.update { it.copy(connecting = false, connected = false, error = error.message, status = "Connection failed") }
                addLog(error.message ?: "Connection failed", level = "ERROR")
            }
        }
    }

    fun disconnect() {
        calls.connectionLost()
        _uiState.update { it.copy(callPeers = emptyMap()) }
        if (_uiState.value.hosting || _uiState.value.hostingStarting) {
            stopHosting()
            return
        }
        disconnectRequested = true
        receiveJob?.cancel()
        receiveJob = null
        fileReceiverJob?.cancel()
        fileReceiverJob = null
        connectionGeneration++
        fileSender.close()
        viewModelScope.launch {
            chatClient.disconnect()
            _uiState.update { it.copy(connected = false, fileReceiverRunning = false, error = null, status = "Disconnected") }
            addLog("Disconnected")
        }
    }

    fun sendTextMessage() {
        val text = _uiState.value.inputMessage.trim()
        if (text.isBlank()) return
        viewModelScope.launch {
            runCatching { chatClient.sendMessage(text) }
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            inputMessage = "",
                            messages = state.messages + ChatLine(state.nickname, text, outbound = true),
                            status = "Message sent",
                        )
                    }
                    addLog("Sent message (${text.length} chars)")
                }
                .onFailure { setError(it.message ?: "Unable to send message") }
        }
    }

    fun beginFileSelection(): Boolean {
        val recipient = currentTransferRecipient()
        if (recipient == null) {
            setError("Select a recipient first")
            return false
        }
        val accepted = transferCoordinator.beginSelection(recipient)
        if (!accepted) {
            setError("Finish the current file batch first")
        }
        return accepted
    }

    fun addSelectedFiles(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val token = transferCoordinator.selectionToken() ?: return
        viewModelScope.launch {
            val sources = withContext(Dispatchers.IO) { uris.map { resolveTransferSource(it) } }
            transferCoordinator.addSelection(token, sources)
        }
    }

    fun clearFiles() = transferCoordinator.clear()

    fun removeFile(id: String) = transferCoordinator.remove(id)

    fun startFileBatch() = transferCoordinator.start()

    fun retryFailedFiles() = transferCoordinator.retry()

    fun skipFailedFiles() = transferCoordinator.skipAndContinue()

    fun stopAfterCurrentFile() = transferCoordinator.stopAfterCurrent()

    private fun startReceiving() {
        receiveJob?.cancel()
        receiveJob = viewModelScope.launch {
            while (_uiState.value.connected) {
                runCatching { chatClient.readMessage() }
                    .onSuccess { message ->
                        if (message == null) {
                            calls.connectionLost()
                            _uiState.update { it.copy(callPeers = emptyMap()) }
                            val status = if (disconnectRequested) "Disconnected" else "Connection closed"
                            fileReceiverJob?.cancel()
                            fileReceiverJob = null
                            _uiState.update { it.copy(connected = false, fileReceiverRunning = false, error = null, status = status) }
                            addLog(status)
                            return@launch
                        }
                        when (message.type) {
                            WireMessageType.SIGNAL -> {
                                // Invalid signaling must not tear down the text chat receive loop.
                                runCatching { RtcSignalCodec.deserialize(message.payload) }
                                    .onSuccess { calls.receive(message.sender, it) }
                                    .onFailure { addLog("Invalid call signal", "WARN") }
                            }
                            WireMessageType.CHAT -> handleIncomingChatMessage(message.sender, message.payload)
                            WireMessageType.USER_JOINED -> handleUserJoined(message.sender, message.payload)
                            WireMessageType.USER_LEFT -> handleUserLeft(message.sender)
                            WireMessageType.SYSTEM,
                            -> _uiState.update { state ->
                                state.copy(status = message.payload.ifBlank { message.type.name })
                            }
                            else -> Unit
                        }
                    }
                    .onFailure { error ->
                        calls.connectionLost()
                        _uiState.update { it.copy(callPeers = emptyMap()) }
                        fileReceiverJob?.cancel()
                        fileReceiverJob = null
                        if (disconnectRequested || error.isExpectedDisconnect()) {
                            _uiState.update { it.copy(connected = false, fileReceiverRunning = false, error = null, status = "Disconnected") }
                            addLog("Disconnected")
                        } else {
                            _uiState.update { it.copy(connected = false, fileReceiverRunning = false, error = error.message, status = "Receive failed") }
                            addLog(error.message ?: "Receive failed", level = "ERROR")
                        }
                        return@launch
                    }
            }
        }
    }

    private fun handleIncomingChatMessage(sender: String, text: String) {
        if (sender.equals(_uiState.value.nickname, ignoreCase = true)) {
            return
        }
        _uiState.update { state ->
            state.copy(messages = state.messages + ChatLine(sender, text, outbound = false))
        }
    }

    private fun handleUserJoined(nickname: String, encodedCapabilities: String) {
        val peerNickname = nickname.trim()
        if (peerNickname.isBlank() || peerNickname.equals(_uiState.value.nickname, ignoreCase = true)) {
            return
        }
        _uiState.update { state ->
            val serverPeer = state.selectedPeer
            val existingServerPeer = state.peers.firstOrNull { it.role == PeerRole.SERVER && it.nickname.equals(peerNickname, ignoreCase = true) }
            val hostedParticipant = AndroidChatHostService.state.value.participants
                .firstOrNull { it.nickname.equals(peerNickname, ignoreCase = true) }
            val capabilities = PeerCapabilities.decode(encodedCapabilities)
            val peer = DiscoveredPeer(
                peerId = existingServerPeer?.peerId ?: "chat:$peerNickname",
                nickname = peerNickname,
                host = hostedParticipant?.host ?: existingServerPeer?.host ?: serverPeer?.host ?: "chat-room",
                chatPort = existingServerPeer?.chatPort ?: serverPeer?.chatPort ?: SecureLanPorts.DEFAULT_CHAT_PORT,
                filePort = existingServerPeer?.filePort ?: serverPeer?.filePort ?: SecureLanPorts.DEFAULT_FILE_TRANSFER_PORT,
                role = existingServerPeer?.role ?: PeerRole.CHAT_CLIENT,
                fileTargetHost = hostedParticipant?.host ?: existingServerPeer?.fileTargetHost ?: serverPeer?.host ?: "chat-room",
                fileTargetPort = hostedParticipant?.filePort
                    ?: capabilities.fileReceivePort().takeIf { it in 1..65535 }
                    ?: existingServerPeer?.fileTargetPort
                    ?: clientFilePortForServer(serverPeer),
            )
            val peers = upsertPeer(state.peers, peer)
                .sortedBy { it.nickname.lowercase() }
            val callPeers = state.callPeers.toMutableMap().apply {
                remove(peerNickname)
                if (capabilities.supportsVoice()) {
                    put(peerNickname, capabilities.supportsVideo())
                }
            }
            state.copy(peers = peers, callPeers = callPeers, status = "$peerNickname joined the chat")
        }
        addLog("Peer joined chat: $peerNickname")
    }

    private fun handleUserLeft(nickname: String) {
        calls.peerLeft(nickname)
        _uiState.update { it.copy(callPeers = it.callPeers.filterKeys { name -> !name.equals(nickname, true) }) }
        val peerNickname = nickname.trim()
        if (peerNickname.isBlank()) return
        val recipient = _uiState.value.fileRecipient
        _uiState.update { state ->
            state.copy(
                peers = state.peers.filterNot { it.role == PeerRole.CHAT_CLIENT && it.nickname.equals(peerNickname, ignoreCase = true) },
                status = "$peerNickname left the chat",
            )
        }
        if (recipient?.nickname.equals(peerNickname, ignoreCase = true)) connectionGeneration++
        addLog("Peer left chat: $peerNickname")
    }

    private fun upsertPeer(peers: List<DiscoveredPeer>, peer: DiscoveredPeer): List<DiscoveredPeer> {
        val filtered = peers.filterNot { existing ->
            samePeer(existing, peer)
        }
        return filtered + peer
    }

    private fun samePeer(left: DiscoveredPeer, right: DiscoveredPeer): Boolean {
        if (left.peerId.isNotBlank() && right.peerId.isNotBlank() && left.peerId == right.peerId) {
            return true
        }
        return left.host == right.host && left.chatPort == right.chatPort && left.role == right.role
    }

    private fun clientFilePortForServer(serverPeer: DiscoveredPeer?): Int {
        val serverFilePort = serverPeer?.filePort ?: SecureLanPorts.DEFAULT_FILE_TRANSFER_PORT
        val candidate = serverFilePort + CLIENT_FILE_PORT_OFFSET
        return if (candidate > 65535) {
            SecureLanPorts.DEFAULT_FILE_TRANSFER_PORT + CLIENT_FILE_PORT_OFFSET
        } else {
            candidate
        }
    }

    private fun androidCapabilities(fileReceivePort: Int): PeerCapabilities = PeerCapabilities(
        PeerCapabilities.PLATFORM_ANDROID, APP_VERSION, true, fileReceivePort,
        supportsVoice = true,
        supportsVideo = getApplication<Application>().packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_CAMERA_ANY),
        deviceName = Build.MODEL ?: "Android device",
    )

    private fun resolveTransferSource(uri: Uri): TransferSource {
        val resolver = getApplication<Application>().contentResolver
        var name = uri.lastPathSegment?.takeIf { it.isNotBlank() } ?: "selected-file"
        var size: Long? = null
        val resolved = runCatching {
            val cursor: Cursor? = resolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null,
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        it.getString(nameIndex)?.takeIf { value -> value.isNotBlank() }?.let { value -> name = value }
                    }
                    val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex >= 0 && !it.isNull(sizeIndex)) {
                        val raw = it.getLong(sizeIndex)
                        if (raw >= 0) size = raw
                    }
                }
            }
            true
        }.getOrDefault(false)
        return TransferSource(reference = uri.toString(), name = name, size = size, unavailable = !resolved)
    }

    private fun currentTransferRecipient(): TransferRecipient? {
        val state = _uiState.value
        if (!state.connected && !state.hosting) return null
        val peer = state.fileRecipient ?: return null
        return TransferRecipient(
            id = peer.peerId,
            name = peer.nickname,
            host = peer.fileTargetHost,
            port = peer.fileTargetPort,
            generation = connectionGeneration,
        )
    }

    private fun isTransferRecipientAvailable(recipient: TransferRecipient): Boolean {
        val current = currentTransferRecipient() ?: return false
        return current.id == recipient.id &&
            current.name == recipient.name &&
            current.host == recipient.host &&
            current.port == recipient.port &&
            current.generation == recipient.generation
    }

    private fun transferRecipientKey(peer: DiscoveredPeer?): String? =
        peer?.let { "${it.peerId}|${it.fileTargetHost}|${it.fileTargetPort}" }

    private fun observeTransferQueue() {
        viewModelScope.launch {
            transferCoordinator.state.collect { queue ->
                if (queue.status == BatchStatus.FINISHED) {
                    recordFinalTransfers(queue)
                    publishBatchResult(queue)
                    // Finished batches leave no persistent panel; per-file outcomes stay in history.
                    transferCoordinator.clear()
                }
            }
        }
    }

    private fun publishBatchResult(queue: TransferQueueState) {
        if (queue.items.isEmpty()) return
        val summary = "Sent ${queue.confirmed} of ${queue.items.size}: ${queue.failed} failed or unconfirmed, ${queue.skipped} skipped"
        _uiState.update { it.copy(status = summary) }
        addLog(summary)
    }

    private fun recordFinalTransfers(queue: TransferQueueState) {
        val terminal = queue.items.filter { it.phase in TRANSFER_TERMINAL_PHASES }
        if (terminal.isEmpty()) return
        val records = terminal.map { item ->
            TransferRecord(
                fileName = item.source.name,
                bytes = item.attempts.lastOrNull()?.total ?: item.source.size ?: 0L,
                direction = TransferDirection.SENT,
                result = if (item.phase == TransferPhase.COMPLETED) TransferResult.COMPLETED else TransferResult.FAILED,
                peerName = queue.recipient?.name,
            )
        }
        _uiState.update {
            it.copy(recentTransfers = (records.asReversed() + it.recentTransfers).take(MAX_RECENT_TRANSFERS))
        }
    }

    private fun setError(message: String) {
        _uiState.update { it.copy(error = message, status = message) }
        addLog(message, level = "ERROR")
    }

    private fun addLog(message: String, level: String = "INFO") {
        _uiState.update { state ->
            val updatedLogs = (state.logs + AppLogEntry(level = level, message = message)).takeLast(MAX_LOG_ENTRIES)
            state.copy(logs = updatedLogs)
        }
    }

    private fun localFileReceiverPort(state: MainUiState): Int {
        return localFileReceiverPortFor(state.connectionPeer ?: state.selectedPeer)
    }

    private fun localFileReceiverPortFor(peer: DiscoveredPeer?): Int {
        if (_uiState.value.hosting) {
            return _uiState.value.hostFilePort.toIntOrNull()?.takeIf { it in 1..65535 }
                ?: SecureLanPorts.DEFAULT_FILE_TRANSFER_PORT
        }
        val remoteFilePort = peer?.filePort ?: SecureLanPorts.DEFAULT_FILE_TRANSFER_PORT
        val candidate = remoteFilePort + CLIENT_FILE_PORT_OFFSET
        return if (candidate > 65535) {
            SecureLanPorts.DEFAULT_FILE_TRANSFER_PORT + CLIENT_FILE_PORT_OFFSET
        } else {
            candidate
        }
    }

    private fun createNotificationChannel() {
        val application = getApplication<Application>()
        val manager = application.getSystemService(NotificationManager::class.java)
        val localizedContext = localizedApplicationContext()
        manager.createNotificationChannel(
            NotificationChannel(
                FILE_NOTIFICATION_CHANNEL,
                localizedContext.getString(R.string.notification_channel_files),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
    }

    private fun observeHostService() {
        viewModelScope.launch {
            AndroidChatHostService.state.collect { hostState -> handleHostServiceState(hostState) }
        }
    }

    private fun handleHostServiceState(hostState: AndroidHostServiceState) {
        val previous = _uiState.value
        if (hostState.running) {
            val localPeer = DiscoveredPeer(
                peerId = hostState.peerId,
                nickname = hostState.nickname,
                host = LOOPBACK_HOST,
                chatPort = hostState.chatPort,
                filePort = hostState.filePort,
                role = PeerRole.SERVER,
                fileTargetHost = LOOPBACK_HOST,
                fileTargetPort = hostState.filePort,
            )
            val hostedPeers = hostState.participants.map { participant ->
                DiscoveredPeer(
                    peerId = "host-client:${participant.nickname.lowercase()}",
                    nickname = participant.nickname,
                    host = participant.host,
                    chatPort = hostState.chatPort,
                    filePort = participant.filePort,
                    role = PeerRole.CHAT_CLIENT,
                    fileTargetHost = participant.host,
                    fileTargetPort = participant.filePort,
                )
            }
            _uiState.update { state ->
                val selectedHostedPeer = state.selectedPeer?.takeIf { it.role == PeerRole.CHAT_CLIENT }?.let { selected ->
                    hostedPeers.firstOrNull { it.nickname.equals(selected.nickname, ignoreCase = true) }
                }
                state.copy(
                    sessionPassword = AndroidChatHostService.currentSessionPassword(),
                    hosting = true,
                    hostingStarting = false,
                    hostChatPort = hostState.chatPort.toString(),
                    hostFilePort = hostState.filePort.toString(),
                    hostedParticipantCount = hostedPeers.size,
                    connectionPeer = localPeer,
                    selectedPeer = selectedHostedPeer ?: localPeer,
                    peers = state.peers.filter { it.role == PeerRole.SERVER && it.peerId != hostState.peerId } + hostedPeers,
                    error = null,
                    status = if (state.connected) "Hosting room" else "Hosted room is ready",
                )
            }
            startLocalAddressWatch()
            if (!previous.connected && !previous.connecting && hostConnectionAttemptedForPeerId != hostState.peerId) {
                hostConnectionAttemptedForPeerId = hostState.peerId
                connectToPeer(localPeer, AndroidChatHostService.currentSessionPassword(), hostState.filePort)
            }
            return
        }

        if (hostState.starting) {
            _uiState.update { it.copy(hostingStarting = true, error = null, status = "Starting hosted room") }
            return
        }

        if (hostState.error != null) {
            hostConnectionAttemptedForPeerId = null
            stopLocalAddressWatch()
            _uiState.update {
                it.copy(
                    hosting = false,
                    hostingStarting = false,
                    connected = false,
                    connecting = false,
                    localRoomAddresses = emptyList(),
                    error = hostState.error,
                    status = "Unable to host room",
                )
            }
            addLog(hostState.error, level = "ERROR")
            return
        }

        if (previous.hosting || previous.hostingStarting) {
            calls.connectionLost()
            _uiState.update { it.copy(callPeers = emptyMap()) }
            hostConnectionAttemptedForPeerId = null
            stopLocalAddressWatch()
            receiveJob?.cancel()
            receiveJob = null
            fileReceiverJob?.cancel()
            fileReceiverJob = null
            disconnectRequested = true
            connectionGeneration++
            fileSender.close()
            viewModelScope.launch { chatClient.disconnect() }
            _uiState.update {
                it.copy(
                    hosting = false,
                    hostingStarting = false,
                    connected = false,
                    connecting = false,
                    fileReceiverRunning = false,
                    localRoomAddresses = emptyList(),
                    hostedParticipantCount = 0,
                    peers = it.peers.filterNot { peer -> peer.role == PeerRole.CHAT_CLIENT },
                    selectedPeer = null,
                    connectionPeer = null,
                    status = "Hosted room stopped",
                )
            }
        }
    }

    private fun startLocalAddressWatch() {
        if (localAddressWatchJob?.isActive == true) return
        refreshLocalRoomAddresses()
        localAddressWatchJob = viewModelScope.launch {
            while (isActive && _uiState.value.hosting) {
                delay(LOCAL_ADDRESS_REFRESH_INTERVAL_MS.milliseconds)
                if (_uiState.value.hosting) refreshLocalRoomAddresses()
            }
        }
    }

    private fun stopLocalAddressWatch() {
        localAddressWatchJob?.cancel()
        localAddressWatchJob = null
    }

    private fun postFileReceivedNotification(fileName: String, savedPath: String) {
        if (!_uiState.value.notificationsEnabled) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            getApplication<Application>().checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return
        val application = getApplication<Application>()
        val localizedContext = localizedApplicationContext()
        val intent = Intent(application, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            application,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = android.app.Notification.Builder(application, FILE_NOTIFICATION_CHANNEL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(localizedContext.getString(R.string.notification_file_received))
            .setContentText(localizedContext.getString(R.string.notification_file_saved, fileName, savedPath))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        application.getSystemService(NotificationManager::class.java)
            .notify(FILE_NOTIFICATION_ID, notification)
    }

    @SuppressLint("AppBundleLocaleChanges") // Language splitting is disabled in android.bundle.language.
    private fun localizedApplicationContext(): Context {
        val application = getApplication<Application>()
        val locale = when (_uiState.value.appLanguage) {
            AppLanguage.SYSTEM -> return application
            AppLanguage.ENGLISH -> Locale.ENGLISH
            AppLanguage.RUSSIAN -> Locale("ru")
        }
        val configuration = Configuration(application.resources.configuration).apply {
            setLocales(LocaleList(locale))
        }
        return application.createConfigurationContext(configuration)
    }

    override fun onCleared() {
        quickShareServer.close()
        calls.close()
        incomingCallNotification.clear()
        discoveryJob?.cancel()
        discoveryTimeoutJob?.cancel()
        receiveJob?.cancel()
        fileReceiverJob?.cancel()
        localAddressRefreshJob?.cancel()
        localAddressWatchJob?.cancel()
        fileSender.close()
        viewModelScope.launch { chatClient.disconnect() }
        super.onCleared()
    }

    private companion object {
        private const val APP_VERSION = "0.5.0"
        private const val CLIENT_FILE_PORT_OFFSET = 1000
        private const val MAX_LOG_ENTRIES = 300
        private const val MAX_RECENT_TRANSFERS = 20
        private const val DISCOVERY_EMPTY_STATE_DELAY_MS = 8_000L
        private const val LOCAL_ADDRESS_REFRESH_INTERVAL_MS = 10_000L
        private val TRANSFER_TERMINAL_PHASES = setOf(
            TransferPhase.COMPLETED,
            TransferPhase.FAILED,
            TransferPhase.OUTCOME_UNKNOWN,
            TransferPhase.SKIPPED,
        )
        private const val PREFERENCES_NAME = "secure_lan_preferences"
        private const val KEY_NICKNAME = "nickname"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_APP_LANGUAGE = "app_language"
        private const val KEY_NOTIFICATIONS = "notifications"
        private const val KEY_AUTO_RECEIVE = "auto_receive"
        private const val KEY_MANUAL_HOST = "manual_host"
        private const val KEY_MANUAL_CHAT_PORT = "manual_chat_port"
        private const val KEY_MANUAL_FILE_PORT = "manual_file_port"
        private const val KEY_HOST_PEER_ID = "host_peer_id"
        private const val LOOPBACK_HOST = "127.0.0.1"
        private const val FILE_NOTIFICATION_CHANNEL = "secure_lan_files"
        private const val FILE_NOTIFICATION_ID = 5051
    }
}

private fun Throwable.isExpectedDisconnect(): Boolean {
    val message = message?.lowercase() ?: return false
    return "socket closed" in message || "connection reset" in message || "connection abort" in message
}
