package com.shterneregen.securelan.desktop.compose.settings

import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.*

/** Dependency-free, forward-compatible settings store backed by a properties file. */
class PropertiesDesktopAppSettingsStore(settingsPath: Path) : DesktopAppSettingsStore {
    private val settingsPath: Path = settingsPath.toAbsolutePath().normalize()

    @Synchronized
    override fun load(): DesktopAppSettings {
        if (!Files.isRegularFile(settingsPath)) return DesktopAppSettings()
        return try {
            val properties = Properties().also { loaded -> Files.newInputStream(settingsPath).use(loaded::load) }
            val defaults = DesktopAppSettings()
            val schemaVersion = properties.int(SCHEMA_VERSION_KEY, 0)
            val downloadsDirectory = properties.string(DOWNLOADS_DIRECTORY_KEY)?.let { storedPath ->
                if (schemaVersion < PORTABLE_DOWNLOADS_SCHEMA_VERSION &&
                    DesktopAppPaths.isLegacyDefaultDownloadsDirectory(storedPath)
                ) {
                    defaults.downloadsDirectory
                } else {
                    storedPath
                }
            } ?: defaults.downloadsDirectory
            DesktopAppSettings(
                displayName = properties.string(DISPLAY_NAME_KEY),
                themeMode = properties.enum(THEME_MODE_KEY, defaults.themeMode),
                reducedMotion = properties.boolean(REDUCED_MOTION_KEY, defaults.reducedMotion),
                window = DesktopWindowSettings(
                    width = properties.int(WINDOW_WIDTH_KEY, defaults.window.width),
                    height = properties.int(WINDOW_HEIGHT_KEY, defaults.window.height),
                    x = properties.nullableInt(WINDOW_X_KEY),
                    y = properties.nullableInt(WINDOW_Y_KEY),
                    maximized = properties.boolean(WINDOW_MAXIMIZED_KEY, defaults.window.maximized),
                ),
                downloadsDirectory = downloadsDirectory,
                media = DesktopMediaSettings(
                    microphoneDeviceId = properties.string(MICROPHONE_DEVICE_KEY).orEmpty(),
                    cameraDeviceId = properties.string(CAMERA_DEVICE_KEY).orEmpty(),
                    outputDeviceId = properties.string(OUTPUT_DEVICE_KEY).orEmpty(),
                    volumePercent = properties.int(VOLUME_KEY, defaults.media.volumePercent),
                ),
                network = DesktopNetworkSettings(
                    discoverable = properties.boolean(DISCOVERABLE_KEY, defaults.network.discoverable),
                    serverChatPort = properties.int(SERVER_CHAT_PORT_KEY, defaults.network.serverChatPort),
                    serverFilePort = properties.int(SERVER_FILE_PORT_KEY, defaults.network.serverFilePort),
                    clientChatPort = properties.int(CLIENT_CHAT_PORT_KEY, defaults.network.clientChatPort),
                    clientFilePort = properties.int(CLIENT_FILE_PORT_KEY, defaults.network.clientFilePort),
                    lastConnectionMode = properties.enum(LAST_CONNECTION_MODE_KEY, defaults.network.lastConnectionMode),
                    recentRooms = properties.recentRooms(),
                ),
                notifications = DesktopNotificationSettings(
                    enabled = properties.boolean(NOTIFICATIONS_ENABLED_KEY, defaults.notifications.enabled),
                    soundsEnabled = properties.boolean(NOTIFICATION_SOUNDS_KEY, defaults.notifications.soundsEnabled),
                    transferNotificationsEnabled = properties.boolean(
                        TRANSFER_NOTIFICATIONS_KEY,
                        defaults.notifications.transferNotificationsEnabled,
                    ),
                ),
                transfers = DesktopTransferSettings(
                    incomingFileConfirmation = properties.enum(
                        INCOMING_CONFIRMATION_KEY,
                        defaults.transfers.incomingFileConfirmation,
                    ),
                    notifyOnCompletion = properties.boolean(
                        NOTIFY_TRANSFER_COMPLETION_KEY,
                        defaults.transfers.notifyOnCompletion,
                    ),
                ),
            ).normalized()
        } catch (_: IOException) {
            DesktopAppSettings()
        } catch (_: IllegalArgumentException) {
            DesktopAppSettings()
        } catch (_: SecurityException) {
            DesktopAppSettings()
        }
    }

    @Synchronized
    override fun save(settings: DesktopAppSettings): Boolean {
        val normalized = settings.normalized()
        val parent = settingsPath.parent ?: return false
        val temporaryPath = parent.resolve("${settingsPath.fileName}.tmp")
        return try {
            Files.createDirectories(parent)
            val properties = Properties().apply {
                set(SCHEMA_VERSION_KEY, CURRENT_SCHEMA_VERSION)
                normalized.displayName?.let { set(DISPLAY_NAME_KEY, it) }
                set(THEME_MODE_KEY, normalized.themeMode)
                set(REDUCED_MOTION_KEY, normalized.reducedMotion)
                set(WINDOW_WIDTH_KEY, normalized.window.width)
                set(WINDOW_HEIGHT_KEY, normalized.window.height)
                normalized.window.x?.let { set(WINDOW_X_KEY, it) }
                normalized.window.y?.let { set(WINDOW_Y_KEY, it) }
                set(WINDOW_MAXIMIZED_KEY, normalized.window.maximized)
                set(DOWNLOADS_DIRECTORY_KEY, normalized.downloadsDirectory)
                set(MICROPHONE_DEVICE_KEY, normalized.media.microphoneDeviceId)
                set(CAMERA_DEVICE_KEY, normalized.media.cameraDeviceId)
                set(OUTPUT_DEVICE_KEY, normalized.media.outputDeviceId)
                set(VOLUME_KEY, normalized.media.volumePercent)
                set(DISCOVERABLE_KEY, normalized.network.discoverable)
                set(SERVER_CHAT_PORT_KEY, normalized.network.serverChatPort)
                set(SERVER_FILE_PORT_KEY, normalized.network.serverFilePort)
                set(CLIENT_CHAT_PORT_KEY, normalized.network.clientChatPort)
                set(CLIENT_FILE_PORT_KEY, normalized.network.clientFilePort)
                set(LAST_CONNECTION_MODE_KEY, normalized.network.lastConnectionMode)
                set(RECENT_ROOMS_COUNT_KEY, normalized.network.recentRooms.size)
                normalized.network.recentRooms.forEachIndexed { index, room ->
                    set("network.recent.$index.host", room.host)
                    set("network.recent.$index.chatPort", room.chatPort)
                    set("network.recent.$index.filePort", room.filePort)
                }
                set(NOTIFICATIONS_ENABLED_KEY, normalized.notifications.enabled)
                set(NOTIFICATION_SOUNDS_KEY, normalized.notifications.soundsEnabled)
                set(TRANSFER_NOTIFICATIONS_KEY, normalized.notifications.transferNotificationsEnabled)
                set(INCOMING_CONFIRMATION_KEY, normalized.transfers.incomingFileConfirmation)
                set(NOTIFY_TRANSFER_COMPLETION_KEY, normalized.transfers.notifyOnCompletion)
            }
            Files.newOutputStream(temporaryPath).use { output ->
                properties.store(output, "SecureLanSuite desktop-client settings")
            }
            replaceAtomically(temporaryPath, settingsPath)
            true
        } catch (_: IOException) {
            false
        } catch (_: SecurityException) {
            false
        } finally {
            runCatching { Files.deleteIfExists(temporaryPath) }
        }
    }

    private fun replaceAtomically(source: Path, target: Path) {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    companion object {
        const val SETTINGS_PATH_PROPERTY: String = "securelan.settings.path"

        fun default(): PropertiesDesktopAppSettingsStore {
            val configuredPath = System.getProperty(SETTINGS_PATH_PROPERTY)?.trim()?.takeIf(String::isNotEmpty)
            val path = configuredPath?.let(Path::of)
                ?: DesktopAppPaths.settingsPath()
            return PropertiesDesktopAppSettingsStore(path)
        }

        private const val CURRENT_SCHEMA_VERSION = 3
        private const val PORTABLE_DOWNLOADS_SCHEMA_VERSION = 3
        private const val SCHEMA_VERSION_KEY = "schema.version"
        private const val DISPLAY_NAME_KEY = "profile.displayName"
        private const val THEME_MODE_KEY = "appearance.themeMode"
        private const val REDUCED_MOTION_KEY = "appearance.reducedMotion"
        private const val WINDOW_WIDTH_KEY = "window.width"
        private const val WINDOW_HEIGHT_KEY = "window.height"
        private const val WINDOW_X_KEY = "window.x"
        private const val WINDOW_Y_KEY = "window.y"
        private const val WINDOW_MAXIMIZED_KEY = "window.maximized"
        private const val DOWNLOADS_DIRECTORY_KEY = "files.downloadsDirectory"
        private const val MICROPHONE_DEVICE_KEY = "media.microphoneDeviceId"
        private const val CAMERA_DEVICE_KEY = "media.cameraDeviceId"
        private const val OUTPUT_DEVICE_KEY = "media.outputDeviceId"
        private const val VOLUME_KEY = "media.volumePercent"
        private const val DISCOVERABLE_KEY = "network.discoverable"
        private const val SERVER_CHAT_PORT_KEY = "network.serverChatPort"
        private const val SERVER_FILE_PORT_KEY = "network.serverFilePort"
        private const val CLIENT_CHAT_PORT_KEY = "network.clientChatPort"
        private const val CLIENT_FILE_PORT_KEY = "network.clientFilePort"
        private const val LAST_CONNECTION_MODE_KEY = "network.lastConnectionMode"
        private const val RECENT_ROOMS_COUNT_KEY = "network.recent.count"
        private const val NOTIFICATIONS_ENABLED_KEY = "notifications.enabled"
        private const val NOTIFICATION_SOUNDS_KEY = "notifications.soundsEnabled"
        private const val TRANSFER_NOTIFICATIONS_KEY = "notifications.transfersEnabled"
        private const val INCOMING_CONFIRMATION_KEY = "transfers.incomingConfirmation"
        private const val NOTIFY_TRANSFER_COMPLETION_KEY = "transfers.notifyOnCompletion"

        private fun Properties.string(key: String): String? = getProperty(key)?.trim()?.takeIf(String::isNotEmpty)
        private fun Properties.int(key: String, fallback: Int): Int = string(key)?.toIntOrNull() ?: fallback
        private fun Properties.nullableInt(key: String): Int? = string(key)?.toIntOrNull()
        private fun Properties.boolean(key: String, fallback: Boolean): Boolean = when (string(key)?.lowercase()) {
            "true" -> true
            "false" -> false
            else -> fallback
        }

        private inline fun <reified T : Enum<T>> Properties.enum(key: String, fallback: T): T =
            string(key)?.let { value -> enumValues<T>().firstOrNull { it.name.equals(value, ignoreCase = true) } }
                ?: fallback

        private fun Properties.recentRooms(): List<DesktopRecentRoom> =
            (0 until int(RECENT_ROOMS_COUNT_KEY, 0).coerceIn(0, DesktopNetworkSettings.MAX_RECENT_ROOMS))
                .mapNotNull { index ->
                    val host = string("network.recent.$index.host") ?: return@mapNotNull null
                    DesktopRecentRoom(
                        host = host,
                        chatPort = int("network.recent.$index.chatPort", -1),
                        filePort = int("network.recent.$index.filePort", -1),
                    ).normalizedOrNull()
                }

        private fun Properties.set(key: String, value: Any) {
            setProperty(key, if (value is Enum<*>) value.name else value.toString())
        }
    }
}
