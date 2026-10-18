package com.shterneregen.securelan.androidclient.network

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.shterneregen.securelan.androidclient.MainActivity
import com.shterneregen.securelan.androidclient.R
import com.shterneregen.securelan.androidclient.model.SecureLanPorts
import com.shterneregen.securelan.androidclient.protocol.DiscoveryCodec
import com.shterneregen.securelan.chat.event.ChatCoreEvent
import com.shterneregen.securelan.chat.event.ChatErrorEvent
import com.shterneregen.securelan.chat.event.ChatUserJoinedEvent
import com.shterneregen.securelan.chat.event.ChatUserLeftEvent
import com.shterneregen.securelan.chat.service.ChatServerConfig
import com.shterneregen.securelan.chat.service.impl.DefaultChatServerService
import com.shterneregen.securelan.common.net.udp.BroadcastAddressResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap

/** Authenticated remote participant with the file-receive endpoint advertised during its handshake. */
data class HostedParticipant(
    val nickname: String,
    val host: String,
    val filePort: Int,
)

/** Process-local host status without room secrets; observed independently of Activity lifecycle. */
data class AndroidHostServiceState(
    val starting: Boolean = false,
    val running: Boolean = false,
    val peerId: String = "",
    val nickname: String = "",
    val chatPort: Int = SecureLanPorts.DEFAULT_CHAT_PORT,
    val filePort: Int = SecureLanPorts.DEFAULT_FILE_TRANSFER_PORT,
    val participants: List<HostedParticipant> = emptyList(),
    val error: String? = null,
    val roomInstanceId: String = "",
    val discoverable: Boolean = true,
    val activeFilePort: Int? = null,
    val mdnsAvailability: com.shterneregen.securelan.chat.discovery.ProviderAvailability = com.shterneregen.securelan.chat.discovery.ProviderAvailability.STOPPED,
    val legacyNsdScopeUnavailable: Boolean = false,
)

/**
 * Owns the desktop-compatible chat server and LAN announcements in a foreground service.
 * Activity recreation does not stop hosting. The process-local state contains no password;
 * explicit stop releases the server and announcements, and process death does not restart hosting.
 */
class AndroidChatHostService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val participants = ConcurrentHashMap<String, HostedParticipant>()
    private var server: DefaultChatServerService? = null
    private var announceJob: Job? = null
    private var advertisementRetryJob: Job? = null
    private var advertisementRetryAttempt = 0
    private var activeRequest: HostRequest? = null
    private val advertiser by lazy { com.shterneregen.securelan.androidclient.network.discovery.AndroidRoomAdvertiser(this, ::handleAdvertisingStatus) }
    @Volatile private var hostGeneration = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopHosting()
            ACTION_START -> startHosting(HostRequest.from(intent))
            ACTION_VISIBILITY -> {
                val request = activeRequest
                if (request != null && server?.isRunning() == true) {
                    activeRequest = request.copy(discoverable = intent.getBooleanExtra("discoverable", true))
                    _state.value = _state.value.copy(discoverable = activeRequest!!.discoverable)
                    updateAdvertisement()
                }
            }
            ACTION_FILE_READY -> {
                if (intent.getStringExtra("room_instance") == _state.value.roomInstanceId && _state.value.running) {
                    val port = intent.getIntExtra("ready_port", 0).takeIf { it in 1..65535 && it == _state.value.filePort }
                    _state.value = _state.value.copy(activeFilePort = port)
                    updateAdvertisement()
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        closeHostResources()
        advertiser.close()
        serviceScope.cancel()
        if (_state.value.running || _state.value.starting) _state.value = AndroidHostServiceState()
        synchronized(stateLock) { sessionPassword = "" }
        super.onDestroy()
    }

    private fun startHosting(request: HostRequest) {
        if (server?.isRunning() == true && activeRequest?.copy(roomInstanceId = request.roomInstanceId) == request) return
        closeHostResources()
        participants.clear()
        activeRequest = request
        synchronized(stateLock) { sessionPassword = request.sessionPassword }
        _state.value = request.toState(starting = true)
        createNotificationChannel()
        showForegroundNotification(request, 0)

        runCatching {
            val generation = hostGeneration
            DefaultChatServerService { event ->
                serviceScope.launch(Dispatchers.Main.immediate) {
                    if (generation == hostGeneration) handleServerEvent(event)
                }
            }.also { created ->
                created.start(ChatServerConfig(request.chatPort, request.sessionPassword))
                server = created
            }
            _state.value = request.toState(running = true)
            startAnnouncements(request)
            updateAdvertisement()
            showForegroundNotification(request, 0)
        }.onFailure { error ->
            closeHostResources()
            val message = error.cause?.message ?: error.message ?: "Unable to start Android chat server"
            _state.value = request.toState(error = message)
            synchronized(stateLock) { sessionPassword = "" }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun stopHosting() {
        closeHostResources()
        participants.clear()
        activeRequest = null
        synchronized(stateLock) { sessionPassword = "" }
        _state.value = AndroidHostServiceState()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun closeHostResources() {
        hostGeneration++
        advertisementRetryJob?.cancel()
        advertisementRetryJob = null
        advertisementRetryAttempt = 0
        announceJob?.cancel()
        announceJob = null
        advertiser.update(null)
        server?.stop()
        server = null
    }

    private fun startAnnouncements(request: HostRequest) {
        announceJob?.cancel()
        announceJob = serviceScope.launch {
            val payload = DiscoveryCodec.encode(request.peerId, request.nickname, request.chatPort, request.filePort)
            val resolver = BroadcastAddressResolver()
            while (isActive && server?.isRunning() == true) {
                if (_state.value.discoverable) runCatching {
                    DatagramSocket().use { socket ->
                        socket.broadcast = true
                        resolver.resolve().forEach { address ->
                            socket.send(DatagramPacket(payload, payload.size, address, SecureLanPorts.DEFAULT_DISCOVERY_PORT))
                        }
                    }
                }
                delay(ANNOUNCE_INTERVAL_MS)
            }
        }
    }

    private fun updateAdvertisement() {
        val current = _state.value
        if (!current.running) { advertiser.update(null); return }
        runCatching { advertiser.update(com.shterneregen.securelan.chat.discovery.HostedRoomAdvertisement(current.peerId, current.roomInstanceId,
            current.nickname.takeIf(com.shterneregen.securelan.chat.discovery.DiscoveryValidation::displayName), current.chatPort,
            current.activeFilePort, current.discoverable)) }.onFailure {
                _state.update { it.copy(mdnsAvailability = com.shterneregen.securelan.chat.discovery.ProviderAvailability.UNAVAILABLE) }
            }
    }

    private fun handleAdvertisingStatus(status: com.shterneregen.securelan.androidclient.network.discovery.NsdAdvertisingStatus) {
        serviceScope.launch(Dispatchers.Main.immediate) {
            val current = _state.value
            if (!current.running) return@launch
            _state.update { it.copy(mdnsAvailability = status.availability, legacyNsdScopeUnavailable = status.legacyScopeUnavailable) }
            if (status.availability == com.shterneregen.securelan.chat.discovery.ProviderAvailability.RUNNING) advertisementRetryAttempt = 0
            if (status.availability != com.shterneregen.securelan.chat.discovery.ProviderAvailability.FAILED || !current.discoverable || advertisementRetryJob != null) return@launch
            val generation = hostGeneration
            val seconds = listOf(1, 2, 5, 15, 30)[advertisementRetryAttempt.coerceAtMost(4)]
            advertisementRetryAttempt++
            advertisementRetryJob = serviceScope.launch(Dispatchers.Main.immediate) {
                delay(seconds * 1000L + kotlin.random.Random.nextLong(200))
                advertisementRetryJob = null
                if (generation == hostGeneration && _state.value.running && _state.value.discoverable) updateAdvertisement()
            }
        }
    }

    private fun handleServerEvent(event: ChatCoreEvent) {
        when (event) {
            is ChatUserJoinedEvent -> {
                val nickname = event.nickname?.trim().orEmpty()
                val host = remoteHost(event.remoteAddress)
                if (nickname.isNotBlank() && host.isNotBlank() && !isLoopback(host)) {
                    participants[nickname.lowercase()] = HostedParticipant(
                        nickname = nickname,
                        host = host,
                        filePort = event.capabilities.fileReceivePort().takeIf { it in 1..65535 }
                            ?: activeRequest?.filePort
                            ?: SecureLanPorts.DEFAULT_FILE_TRANSFER_PORT,
                    )
                    publishParticipants()
                }
            }
            is ChatUserLeftEvent -> {
                event.nickname?.trim()?.lowercase()?.let(participants::remove)
                publishParticipants()
            }
            is ChatErrorEvent -> Unit // Per-client socket errors do not stop the hosted room.
            else -> Unit
        }
    }

    private fun publishParticipants() {
        val request = activeRequest ?: return
        val snapshot = participants.values.sortedBy { it.nickname.lowercase() }
        _state.value = request.toState(running = server?.isRunning() == true, participants = snapshot).copy(activeFilePort = _state.value.activeFilePort, mdnsAvailability = _state.value.mdnsAvailability, legacyNsdScopeUnavailable = _state.value.legacyNsdScopeUnavailable)
        showForegroundNotification(request, snapshot.size)
    }

    private fun createNotificationChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL,
                getString(R.string.notification_channel_hosting),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }

    private fun showForegroundNotification(request: HostRequest, participantCount: Int) {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, AndroidChatHostService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(this, NOTIFICATION_CHANNEL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.host_notification_title, request.nickname))
            .setContentText(resources.getQuantityString(R.plurals.host_notification_text, participantCount, request.chatPort, participantCount))
            .setContentIntent(openIntent)
            .setOngoing(true)
            .addAction(Notification.Action.Builder(null, getString(R.string.stop_hosting), stopIntent).build())
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private data class HostRequest(
        val peerId: String,
        val nickname: String,
        val sessionPassword: String,
        val chatPort: Int,
        val filePort: Int,
        val roomInstanceId: String = java.util.UUID.randomUUID().toString(),
        val discoverable: Boolean = true,
    ) {
        fun toState(
            starting: Boolean = false,
            running: Boolean = false,
            participants: List<HostedParticipant> = emptyList(),
            error: String? = null,
        ) = AndroidHostServiceState(starting, running, peerId, nickname, chatPort, filePort, participants, error, roomInstanceId, discoverable)

        companion object {
            fun from(intent: Intent): HostRequest = HostRequest(
                peerId = intent.getStringExtra(EXTRA_PEER_ID).orEmpty(),
                nickname = intent.getStringExtra(EXTRA_NICKNAME).orEmpty(),
                sessionPassword = intent.getStringExtra(EXTRA_PASSWORD).orEmpty(),
                chatPort = intent.getIntExtra(EXTRA_CHAT_PORT, SecureLanPorts.DEFAULT_CHAT_PORT),
                filePort = intent.getIntExtra(EXTRA_FILE_PORT, SecureLanPorts.DEFAULT_FILE_TRANSFER_PORT),
                discoverable = intent.getBooleanExtra("discoverable", true),
            ).also {
                require(it.peerId.isNotBlank()) { "Host peer id must not be blank" }
                require(it.nickname.isNotBlank()) { "Host nickname must not be blank" }
                require(it.sessionPassword.isNotBlank()) { "Host password must not be blank" }
                require(it.chatPort in 1..65535) { "Chat port must be between 1 and 65535" }
                require(it.filePort in 1..65535) { "File port must be between 1 and 65535" }
                require(it.chatPort != it.filePort) { "Chat and file ports must be different" }
            }
        }
    }

    companion object {
        private const val ACTION_VISIBILITY = "com.shterneregen.securelan.androidclient.action.HOST_VISIBILITY"
        private const val ACTION_FILE_READY = "com.shterneregen.securelan.androidclient.action.HOST_FILE_READY"
        private const val ACTION_START = "com.shterneregen.securelan.androidclient.action.START_HOST"
        private const val ACTION_STOP = "com.shterneregen.securelan.androidclient.action.STOP_HOST"
        private const val EXTRA_PEER_ID = "peer_id"
        private const val EXTRA_NICKNAME = "nickname"
        private const val EXTRA_PASSWORD = "password"
        private const val EXTRA_CHAT_PORT = "chat_port"
        private const val EXTRA_FILE_PORT = "file_port"
        private const val NOTIFICATION_CHANNEL = "secure_lan_hosting"
        private const val NOTIFICATION_ID = 5050
        private const val ANNOUNCE_INTERVAL_MS = 10_000L
        private val stateLock = Any()
        private val _state = MutableStateFlow(AndroidHostServiceState())
        val state: StateFlow<AndroidHostServiceState> = _state.asStateFlow()
        private var sessionPassword: String = ""

        fun start(context: android.content.Context, peerId: String, nickname: String, password: String, chatPort: Int, filePort: Int, discoverable: Boolean = true) {
            val intent = Intent(context, AndroidChatHostService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_PEER_ID, peerId)
                .putExtra(EXTRA_NICKNAME, nickname)
                .putExtra(EXTRA_PASSWORD, password)
                .putExtra(EXTRA_CHAT_PORT, chatPort)
                .putExtra(EXTRA_FILE_PORT, filePort)
                .putExtra("discoverable", discoverable)
            context.startForegroundService(intent)
        }

        fun setDiscoverable(context: android.content.Context, value: Boolean) {
            context.startService(Intent(context, AndroidChatHostService::class.java).setAction(ACTION_VISIBILITY).putExtra("discoverable", value))
        }
        fun updateFileReadiness(context: android.content.Context, roomInstanceId: String, port: Int?) {
            context.startService(Intent(context, AndroidChatHostService::class.java).setAction(ACTION_FILE_READY)
                .putExtra("room_instance", roomInstanceId).putExtra("ready_port", port ?: 0))
        }
        fun stop(context: android.content.Context) {
            context.startService(Intent(context, AndroidChatHostService::class.java).setAction(ACTION_STOP))
        }

        fun currentSessionPassword(): String = synchronized(stateLock) { sessionPassword }

        internal fun remoteHost(remoteAddress: String?): String {
            val value = remoteAddress?.trim().orEmpty().removePrefix("/")
            if (value.startsWith("[")) return value.substringAfter('[').substringBefore(']')
            return value.substringBeforeLast(':', value).trim()
        }

        private fun isLoopback(host: String): Boolean = runCatching { InetAddress.getByName(host).isLoopbackAddress }.getOrDefault(false)
    }
}
