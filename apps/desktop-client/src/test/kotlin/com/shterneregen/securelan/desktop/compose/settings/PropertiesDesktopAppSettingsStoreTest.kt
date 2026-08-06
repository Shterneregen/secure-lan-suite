package com.shterneregen.securelan.desktop.compose.settings

import com.shterneregen.securelan.desktop.compose.SecureLanThemeMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class PropertiesDesktopAppSettingsStoreTest {
    @TempDir
    lateinit var temporaryDirectory: Path

    @Test
    fun shouldReturnDefaultsWhenSettingsFileDoesNotExist() {
        val store = PropertiesDesktopAppSettingsStore(temporaryDirectory.resolve("missing/settings.properties"))

        val settings = store.load()

        assertNull(settings.displayName)
        assertEquals(SecureLanThemeMode.DARK, settings.themeMode)
        assertEquals(true, settings.lifecycle.keepRunningAfterWindowClose)
    }

    @Test
    fun shouldRoundTripDisplayNameAndTheme() {
        val path = temporaryDirectory.resolve("nested/settings.properties")
        val store = PropertiesDesktopAppSettingsStore(path)

        assertTrue(store.save(DesktopAppSettings(" Alice ", SecureLanThemeMode.LIGHT)))

        assertEquals(
            DesktopAppSettings("Alice", SecureLanThemeMode.LIGHT),
            store.load(),
        )
    }

    @Test
    fun shouldIgnoreUnknownThemeAndKeepValidDisplayName() {
        val path = temporaryDirectory.resolve("settings.properties")
        Files.writeString(
            path,
            "profile.displayName=Bob\nappearance.themeMode=future-theme\nunknown.key=value\n",
        )

        assertEquals(
            DesktopAppSettings("Bob", SecureLanThemeMode.DARK),
            PropertiesDesktopAppSettingsStore(path).load(),
        )
    }

    @Test
    fun shouldNormalizeBlankDisplayName() {
        assertNull(DesktopAppSettings(displayName = "Alice").withDisplayName("   ").displayName)
    }

    @Test
    fun shouldMigrateLegacyDefaultDownloadsToPortableDirectory() {
        val path = temporaryDirectory.resolve("settings.properties")
        val legacyDownloads = Path.of(System.getProperty("user.home", "."), "Downloads", "SecureLanSuite")
            .toAbsolutePath()
            .normalize()
        Files.writeString(
            path,
            "schema.version=2\nfiles.downloadsDirectory=${legacyDownloads.toString().replace("\\", "\\\\")}\n",
        )

        val settings = PropertiesDesktopAppSettingsStore(path).load()

        assertEquals(DesktopAppPaths.downloadsDirectory(), settings.downloadsPath())
    }

    @Test
    fun shouldRoundTripAllPersistedSettings() {
        val path = temporaryDirectory.resolve("all/settings.properties")
        val store = PropertiesDesktopAppSettingsStore(path)
        val settings = DesktopAppSettings(
            displayName = "Alice",
            themeMode = SecureLanThemeMode.INTERMEDIATE,
            reducedMotion = true,
            lifecycle = DesktopLifecycleSettings(keepRunningAfterWindowClose = false),
            window = DesktopWindowSettings(1440, 900, 120, 80, true),
            downloadsDirectory = temporaryDirectory.resolve("downloads").toString(),
            media = DesktopMediaSettings("mic-1", "camera-1", "speaker-1", 63),
            network = DesktopNetworkSettings(
                discoverable = false,
                serverChatPort = 6123,
                serverFilePort = 6124,
                clientChatPort = 7123,
                clientFilePort = 7124,
                lastConnectionMode = DesktopConnectionMode.JOIN,
                recentRooms = listOf(
                    DesktopRecentRoom("192.168.1.20", 7123, 7124),
                    DesktopRecentRoom("room.local", 8123, 8124),
                ),
            ),
            notifications = DesktopNotificationSettings(
                enabled = false,
                soundsEnabled = false,
                transferNotificationsEnabled = false,
                messageNotificationsEnabled = false,
            ),
            transfers = DesktopTransferSettings(
                IncomingFileConfirmationMode.AUTO_ACCEPT_KNOWN_PEERS,
                notifyOnCompletion = false,
            ),
        )

        assertTrue(store.save(settings))
        assertTrue(Files.readString(path).contains("window.keepRunningAfterClose=false"))

        assertEquals(settings.normalized(), store.load())
    }

    @Test
    fun shouldValidatePortsVolumeWindowAndRecentRoomLimit() {
        val settings = DesktopAppSettings(
            window = DesktopWindowSettings(width = 1, height = 20),
            media = DesktopMediaSettings(volumePercent = 500),
            network = DesktopNetworkSettings(
                serverChatPort = -1,
                recentRooms = (1..20).map { DesktopRecentRoom("host-$it") },
            ),
        ).normalized()

        assertEquals(820, settings.window.width)
        assertEquals(640, settings.window.height)
        assertEquals(100, settings.media.volumePercent)
        assertEquals(DesktopNetworkSettings().serverChatPort, settings.network.serverChatPort)
        assertEquals(DesktopNetworkSettings.MAX_RECENT_ROOMS, settings.network.recentRooms.size)
    }

    @Test
    fun controllerShouldMergeSequentialUpdatesIntoLatestSnapshot() {
        val controller = DesktopAppSettingsController()

        controller.update { it.copy(themeMode = SecureLanThemeMode.LIGHT) }
        controller.update { it.copy(media = it.media.copy(cameraDeviceId = "camera-2")) }

        assertEquals(SecureLanThemeMode.LIGHT, controller.settings.themeMode)
        assertEquals("camera-2", controller.settings.media.cameraDeviceId)
    }
}
