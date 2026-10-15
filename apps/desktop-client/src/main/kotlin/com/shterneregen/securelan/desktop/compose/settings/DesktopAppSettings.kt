package com.shterneregen.securelan.desktop.compose.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.shterneregen.securelan.common.net.NetworkConstants
import com.shterneregen.securelan.desktop.compose.SecureLanThemeMode
import java.nio.file.Path

/** Restorable window geometry in Compose dp; missing coordinates use the platform's placement. */
data class DesktopWindowSettings(
    val width: Int = 1360,
    val height: Int = 860,
    val x: Int? = null,
    val y: Int? = null,
    val maximized: Boolean = false,
) {
    /** Bounds persisted geometry so malformed settings cannot create an unusable window. */
    fun normalized(): DesktopWindowSettings = copy(
        width = width.coerceIn(820, 10_000),
        height = height.coerceIn(640, 10_000),
        x = x?.coerceIn(-50_000, 50_000),
        y = y?.coerceIn(-50_000, 50_000),
    )
}

/** Preferred device IDs and output volume; an empty ID selects the system default device. */
data class DesktopMediaSettings(
    val microphoneDeviceId: String = "",
    val cameraDeviceId: String = "",
    val outputDeviceId: String = "",
    val volumePercent: Int = 100,
) {
    fun normalized(): DesktopMediaSettings = copy(
        microphoneDeviceId = microphoneDeviceId.trim(),
        cameraDeviceId = cameraDeviceId.trim(),
        outputDeviceId = outputDeviceId.trim(),
        volumePercent = volumePercent.coerceIn(0, 100),
    )
}

/** The entry workflow to restore the next time the connection surface opens. */
enum class DesktopConnectionMode { HOST, JOIN }

/** A recent room endpoint without its password or other session secrets. */
data class DesktopRecentRoom(
    val host: String,
    val chatPort: Int = NetworkConstants.DEFAULT_CHAT_PORT,
    val filePort: Int = NetworkConstants.DEFAULT_FILE_TRANSFER_PORT,
) {
    /** Returns a trimmed endpoint, or `null` when the address or either port is invalid. */
    fun normalizedOrNull(): DesktopRecentRoom? = copy(host = host.trim()).takeIf {
        it.host.isNotEmpty() && it.chatPort in 1..65_535 && it.filePort in 1..65_535
    }
}

/** Connection defaults and a bounded, most-recent-first history of room endpoints. */
data class DesktopNetworkSettings(
    val discoverable: Boolean = true,
    val serverChatPort: Int = NetworkConstants.DEFAULT_CHAT_PORT,
    val serverFilePort: Int = NetworkConstants.DEFAULT_FILE_TRANSFER_PORT,
    val clientChatPort: Int = NetworkConstants.DEFAULT_CHAT_PORT,
    val clientFilePort: Int = NetworkConstants.DEFAULT_FILE_TRANSFER_PORT,
    val lastConnectionMode: DesktopConnectionMode = DesktopConnectionMode.HOST,
    val recentRooms: List<DesktopRecentRoom> = emptyList(),
) {
    /** Repairs invalid ports and removes invalid, duplicate, or excess recent endpoints. */
    fun normalized(): DesktopNetworkSettings = copy(
        serverChatPort = serverChatPort.validPortOr(NetworkConstants.DEFAULT_CHAT_PORT),
        serverFilePort = serverFilePort.validPortOr(NetworkConstants.DEFAULT_FILE_TRANSFER_PORT),
        clientChatPort = clientChatPort.validPortOr(NetworkConstants.DEFAULT_CHAT_PORT),
        clientFilePort = clientFilePort.validPortOr(NetworkConstants.DEFAULT_FILE_TRANSFER_PORT),
        recentRooms = recentRooms.mapNotNull(DesktopRecentRoom::normalizedOrNull)
            .distinctBy { "${it.host.lowercase()}:${it.chatPort}:${it.filePort}" }
            .take(MAX_RECENT_ROOMS),
    )

    /** Moves a valid endpoint to the front of the history without storing its credentials. */
    fun withRecentRoom(room: DesktopRecentRoom): DesktopNetworkSettings {
        val normalizedRoom = room.normalizedOrNull() ?: return this
        return copy(
            recentRooms = (listOf(normalizedRoom) + recentRooms.filterNot {
                it.host.equals(normalizedRoom.host, ignoreCase = true) &&
                    it.chatPort == normalizedRoom.chatPort && it.filePort == normalizedRoom.filePort
            }).take(MAX_RECENT_ROOMS),
        )
    }

    companion object {
        const val MAX_RECENT_ROOMS: Int = 10
    }
}

/** Independent notification preferences, gated by the master [enabled] flag. */
data class DesktopNotificationSettings(
    val enabled: Boolean = true,
    val soundsEnabled: Boolean = true,
    val transferNotificationsEnabled: Boolean = true,
    val messageNotificationsEnabled: Boolean = true,
)

/** Window-close preference; hiding still requires an available system tray. */
data class DesktopLifecycleSettings(
    val keepRunningAfterWindowClose: Boolean = true,
)

/** Incoming file policy; automatic acceptance still requires a connected, known peer. */
enum class IncomingFileConfirmationMode {
    ASK,
    AUTO_ACCEPT_KNOWN_PEERS,
}

/** Receive-approval and completion-feedback preferences for encrypted file transfers. */
data class DesktopTransferSettings(
    val incomingFileConfirmation: IncomingFileConfirmationMode = IncomingFileConfirmationMode.ASK,
    val notifyOnCompletion: Boolean = true,
)

/** User preferences that are safe to restore between desktop-client sessions. */
data class DesktopAppSettings(
    val displayName: String? = null,
    val themeMode: SecureLanThemeMode = SecureLanThemeMode.DARK,
    val reducedMotion: Boolean = false,
    val lifecycle: DesktopLifecycleSettings = DesktopLifecycleSettings(),
    val window: DesktopWindowSettings = DesktopWindowSettings(),
    val downloadsDirectory: String = defaultDownloadsDirectory(),
    val media: DesktopMediaSettings = DesktopMediaSettings(),
    val network: DesktopNetworkSettings = DesktopNetworkSettings(),
    val notifications: DesktopNotificationSettings = DesktopNotificationSettings(),
    val transfers: DesktopTransferSettings = DesktopTransferSettings(),
) {
    /** Returns safe restore values while preserving all unrelated preferences. */
    fun normalized(): DesktopAppSettings = copy(
        displayName = displayName?.trim()?.takeIf(String::isNotEmpty),
        window = window.normalized(),
        downloadsDirectory = downloadsDirectory.trim().takeIf(String::isNotEmpty) ?: defaultDownloadsDirectory(),
        media = media.normalized(),
        network = network.normalized(),
    )

    fun withDisplayName(value: String?): DesktopAppSettings = copy(displayName = value).normalized()

    /** Resolves the configured output directory, falling back to portable downloads for an invalid path. */
    fun downloadsPath(): Path = runCatching {
        Path.of(downloadsDirectory).toAbsolutePath().normalize()
    }.getOrElse { Path.of(defaultDownloadsDirectory()) }
}

/** Persistence boundary for non-secret desktop preferences. */
interface DesktopAppSettingsStore {
    /** Returns restorable preferences, using defaults when stored data cannot be read. */
    fun load(): DesktopAppSettings

    /** Returns whether normalized preferences were successfully persisted. */
    fun save(settings: DesktopAppSettings): Boolean
}

/** Single source of truth shared by the window, Compose UI, and host adapter. */
class DesktopAppSettingsController(
    initialSettings: DesktopAppSettings = DesktopAppSettings(),
    private val store: DesktopAppSettingsStore? = null,
    private val dispatch: (() -> Unit) -> Unit = { action ->
        if (javax.swing.SwingUtilities.isEventDispatchThread()) action() else javax.swing.SwingUtilities.invokeLater(action)
    },
) : AutoCloseable {
    private val writer = java.util.concurrent.Executors.newSingleThreadExecutor { work ->
        Thread(work, "desktop-settings-writer")
    }
    private var revision = 0L
    private var closed = false
    var persistenceError: String? by mutableStateOf(null)
        private set
    var saving: Boolean by mutableStateOf(false)
        private set
    var settings: DesktopAppSettings by mutableStateOf(initialSettings.normalized())
        private set

    /**
     * Normalizes and applies a preference change, then persists it when values actually differ.
     * A failed save leaves the updated in-memory preferences active for the current session.
     */
    @Synchronized
    fun update(transform: (DesktopAppSettings) -> DesktopAppSettings): DesktopAppSettings {
        val updated = transform(settings).normalized()
        if (updated != settings) {
            settings = updated
            persist(updated)
        }
        return settings
    }
    /** Retries the latest in-memory preferences; successful persistence clears the recovery state. */
    @Synchronized
    fun retryPersistence() { persist(settings) }

    private fun persist(snapshot: DesktopAppSettings) {
        val target = store ?: return
        if (closed) return
        val captured = ++revision
        saving = true
        writer.execute {
            val saved = runCatching { target.save(snapshot) }.getOrDefault(false)
            dispatch {
                if (captured == revision) {
                    saving = false
                    persistenceError = if (saved) null else "Settings apply for this session but could not be saved. Check storage access and retry."
                }
            }
        }
    }

    /** Drains queued writes in order without blocking the UI thread. */
    @Synchronized
    override fun close() { closed = true; writer.shutdown() }

}

private fun Int.validPortOr(fallback: Int): Int = takeIf { it in 1..65_535 } ?: fallback

private fun defaultDownloadsDirectory(): String =
    DesktopAppPaths.downloadsDirectory().toString()
