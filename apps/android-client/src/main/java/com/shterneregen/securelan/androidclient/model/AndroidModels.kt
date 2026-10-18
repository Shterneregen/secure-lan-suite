package com.shterneregen.securelan.androidclient.model

import android.net.Uri
import com.shterneregen.securelan.common.net.NetworkConstants
import java.time.Instant

/** Shared port defaults; Android receive endpoints may use an offset to avoid local conflicts. */
object SecureLanPorts {
    val DEFAULT_CHAT_PORT: Int = NetworkConstants.DEFAULT_CHAT_PORT
    val DEFAULT_FILE_TRANSFER_PORT: Int = NetworkConstants.DEFAULT_FILE_TRANSFER_PORT
    val DEFAULT_DISCOVERY_PORT: Int = NetworkConstants.DEFAULT_DISCOVERY_PORT
}

enum class PeerRole {
    SERVER,
    CHAT_CLIENT,
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class AppLanguage {
    SYSTEM,
    ENGLISH,
    RUSSIAN,
}

enum class NearbyPermissionState {
    NOT_REQUIRED,
    REQUIRED,
    GRANTED,
    DENIED,
}

enum class TransferDirection {
    SENT,
    RECEIVED,
}

enum class TransferResult {
    COMPLETED,
    FAILED,
}

/** Nearby room or joined participant; file endpoints may differ from the room's chat endpoint. */
data class DiscoveredPeer(
    val peerId: String,
    val nickname: String,
    val host: String,
    val chatPort: Int,
    val filePort: Int,
    val lastSeen: Instant = Instant.now(),
    val role: PeerRole = PeerRole.SERVER,
    val fileTargetHost: String = host,
    val fileTargetPort: Int = filePort,
    val routeKey: String? = null,
    val discoveredRowId: String? = null,
    val advertisedFilePort: Int? = filePort,
)

data class ChatLine(
    val sender: String,
    val text: String,
    val outbound: Boolean,
    val timestamp: Instant = Instant.now(),
)

data class AppLogEntry(
    val timestamp: Instant = Instant.now(),
    val level: String = "INFO",
    val message: String,
)

data class SelectedFile(
    val uri: Uri,
    val name: String,
    val size: Long,
)

data class FileSendProgress(
    val fileName: String = "",
    val bytesSent: Long = 0,
    val totalBytes: Long = 0,
    val active: Boolean = false,
    val error: String? = null,
) {
    val percent: Float
        get() = if (totalBytes <= 0) 0f else (bytesSent.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
}

data class IncomingFileProgress(
    val fileName: String = "",
    val bytesReceived: Long = 0,
    val totalBytes: Long = 0,
    val active: Boolean = false,
    val completedPath: String? = null,
    val error: String? = null,
    val transferId: String = "",
) {
    val percent: Float
        get() = if (totalBytes <= 0) 0f else (bytesReceived.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
}

data class TransferRecord(
    val fileName: String,
    val bytes: Long,
    val direction: TransferDirection,
    val result: TransferResult,
    val peerName: String? = null,
    val savedPath: String? = null,
    val contentUri: Uri? = null,
    val timestamp: Instant = Instant.now(),
)

/**
 * Immutable workspace snapshot retained by the ViewModel across configuration changes.
 * [connectionPeer] identifies the room; [selectedPeer] may identify a different file recipient.
 * [sessionPassword] and [importedRoomPassword] are memory-only and must never be logged or persisted. [callPeers] maps
 * voice-capable participant names to video support. Outgoing batches have their own state owner.
 */
data class MainUiState(
    val pendingConnection: com.shterneregen.securelan.chat.connection.RoomConnectionDescriptor? = null,
    val importedRoomPassword: String = "",
    val connectionImportError: com.shterneregen.securelan.chat.connection.ConnectionUriError? = null,
    val roomDiscovery: com.shterneregen.securelan.chat.discovery.DiscoverySnapshot = com.shterneregen.securelan.chat.discovery.DiscoverySnapshot(),
    val hostRoomInstanceId: String? = null,
    val hostedPeerId: String? = null,
    val hostedFilePort: Int? = null,
    val hostMdnsAvailability: com.shterneregen.securelan.chat.discovery.ProviderAvailability = com.shterneregen.securelan.chat.discovery.ProviderAvailability.STOPPED,
    val hostLegacyNsdScopeUnavailable: Boolean = false,
    val hostDiscoverable: Boolean = true,
    val callPeers: Map<String, Boolean> = emptyMap(),
    val nickname: String = "",
    val sessionPassword: String = "",
    val manualHost: String = "",
    val manualChatPort: String = SecureLanPorts.DEFAULT_CHAT_PORT.toString(),
    val manualFilePort: String = SecureLanPorts.DEFAULT_FILE_TRANSFER_PORT.toString(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appLanguage: AppLanguage = AppLanguage.SYSTEM,
    val notificationsEnabled: Boolean = true,
    val autoReceiveFiles: Boolean = true,
    val nearbyPermissionState: NearbyPermissionState = NearbyPermissionState.REQUIRED,
    val networkAvailable: Boolean = true,
    val peers: List<DiscoveredPeer> = emptyList(),
    val selectedPeer: DiscoveredPeer? = null,
    val connectionPeer: DiscoveredPeer? = null,
    val connected: Boolean = false,
    val connecting: Boolean = false,
    val hosting: Boolean = false,
    val hostingStarting: Boolean = false,
    val hostChatPort: String = SecureLanPorts.DEFAULT_CHAT_PORT.toString(),
    val hostFilePort: String = SecureLanPorts.DEFAULT_FILE_TRANSFER_PORT.toString(),
    val localRoomAddresses: List<String> = emptyList(),
    val hostedParticipantCount: Int = 0,
    val inputMessage: String = "",
    val messages: List<ChatLine> = emptyList(),
    val selectedFile: SelectedFile? = null,
    val fileProgress: FileSendProgress = FileSendProgress(),
    val incomingFileProgress: IncomingFileProgress = IncomingFileProgress(),
    val recentTransfers: List<TransferRecord> = emptyList(),
    val fileReceiverRunning: Boolean = false,
    val discoveryRunning: Boolean = false,
    val autoDiscoveryEnabled: Boolean = false,
    val discoveryTimedOut: Boolean = false,
    val status: String = "Ready",
    val error: String? = null,
    val logs: List<AppLogEntry> = listOf(AppLogEntry(message = "Android client started")),
) {
    val fileRecipient: DiscoveredPeer?
        get() = selectedPeer?.takeIf { it.role == PeerRole.CHAT_CLIENT }
            ?: connectionPeer.takeUnless { hosting }

    /** Theme adapter for legacy composables; system mode is resolved by the active Compose shell. */
    val darkThemeEnabled: Boolean
        get() = themeMode == ThemeMode.DARK
}
