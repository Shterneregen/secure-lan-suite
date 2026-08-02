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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap

data class HostedParticipant(
    val nickname: String,
    val host: String,
    val filePort: Int,
)

data class AndroidHostServiceState(
    val starting: Boolean = false,
    val running: Boolean = false,
    val peerId: String = "",
    val nickname: String = "",
    val chatPort: Int = SecureLanPorts.DEFAULT_CHAT_PORT,
    val filePort: Int = SecureLanPorts.DEFAULT_FILE_TRANSFER_PORT,
    val participants: List<HostedParticipant> = emptyList(),
    val error: String? = null,
)

/** Owns the desktop-compatible chat server while Android is in host mode. */
class AndroidChatHostService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val participants = ConcurrentHashMap<String, HostedParticipant>()
    private var server: DefaultChatServerService? = null
    private var announceJob: Job? = null
    private var activeRequest: HostRequest? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopHosting()
            ACTION_START -> startHosting(HostRequest.from(intent))
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        closeHostResources()
        serviceScope.cancel()
        if (_state.value.running || _state.value.starting) _state.value = AndroidHostServiceState()
        synchronized(stateLock) { sessionPassword = "" }
        super.onDestroy()
    }

    private fun startHosting(request: HostRequest) {
        if (server?.isRunning() == true && activeRequest == request) return
        closeHostResources()
        participants.clear()
        activeRequest = request
        synchronized(stateLock) { sessionPassword = request.sessionPassword }
        _state.value = request.toState(starting = true)
        createNotificationChannel()
        showForegroundNotification(request, 0)

        runCatching {
            DefaultChatServerService(::handleServerEvent).also { created ->
                created.start(ChatServerConfig(request.chatPort, request.sessionPassword))
                server = created
            }
            _state.value = request.toState(running = true)
            startAnnouncements(request)
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
        announceJob?.cancel()
        announceJob = null
        server?.stop()
        server = null
    }

    private fun startAnnouncements(request: HostRequest) {
        announceJob?.cancel()
        announceJob = serviceScope.launch {
            val payload = DiscoveryCodec.encode(request.peerId, request.nickname, request.chatPort, request.filePort)
            val resolver = BroadcastAddressResolver()
            while (isActive && server?.isRunning() == true) {
                runCatching {
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
        _state.value = request.toState(running = server?.isRunning() == true, participants = snapshot)
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
    ) {
        fun toState(
            starting: Boolean = false,
            running: Boolean = false,
            participants: List<HostedParticipant> = emptyList(),
            error: String? = null,
        ) = AndroidHostServiceState(starting, running, peerId, nickname, chatPort, filePort, participants, error)

        companion object {
            fun from(intent: Intent): HostRequest = HostRequest(
                peerId = intent.getStringExtra(EXTRA_PEER_ID).orEmpty(),
                nickname = intent.getStringExtra(EXTRA_NICKNAME).orEmpty(),
                sessionPassword = intent.getStringExtra(EXTRA_PASSWORD).orEmpty(),
                chatPort = intent.getIntExtra(EXTRA_CHAT_PORT, SecureLanPorts.DEFAULT_CHAT_PORT),
                filePort = intent.getIntExtra(EXTRA_FILE_PORT, SecureLanPorts.DEFAULT_FILE_TRANSFER_PORT),
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

        fun start(context: android.content.Context, peerId: String, nickname: String, password: String, chatPort: Int, filePort: Int) {
            val intent = Intent(context, AndroidChatHostService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_PEER_ID, peerId)
                .putExtra(EXTRA_NICKNAME, nickname)
                .putExtra(EXTRA_PASSWORD, password)
                .putExtra(EXTRA_CHAT_PORT, chatPort)
                .putExtra(EXTRA_FILE_PORT, filePort)
            context.startForegroundService(intent)
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
