package com.shterneregen.securelan.desktop.compose.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.Density
import com.shterneregen.securelan.chat.discovery.PeerDiscoveryService
import com.shterneregen.securelan.chat.event.ChatMessageReceivedEvent
import com.shterneregen.securelan.chat.event.ChatUserJoinedEvent
import com.shterneregen.securelan.chat.protocol.handshake.PeerCapabilities
import com.shterneregen.securelan.chat.service.*
import com.shterneregen.securelan.desktop.compose.*
import com.shterneregen.securelan.desktop.compose.settings.*
import com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListState
import com.shterneregen.securelan.desktop.compose.ui.settings.*
import com.shterneregen.securelan.desktop.compose.ui.shell.ComposeShellContent
import com.shterneregen.securelan.filetransfer.quickshare.QuickShareService
import com.shterneregen.securelan.filetransfer.service.FileTransferServerService
import org.jetbrains.skia.Image
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Proxy
import java.nio.file.Files
import java.nio.file.Path

/** Production composables with local service doubles; never opens network or media devices. */
@OptIn(ExperimentalTestApi::class)
class TargetUiInteractionTest {
    @Test fun entryKeepsDraftWhenReturningToChoices() = runDesktopComposeUiTest(width = 1600, height = 864) {
        Fixture().use { f ->
            shell(f)
            onNodeWithText("Set up room").assertIsDisplayed()
            capture("main-disconnected")
            onNodeWithText("Set up room").performClick()
            onAllNodes(hasSetTextAction())[0].performTextReplacement("Draft name")
            onAllNodes(hasSetTextAction())[1].performTextReplacement("synthetic-secret")
            onNodeWithText("Back to connection options").performClick()
            onNodeWithText("Set up room").performClick()
            onAllNodes(hasSetTextAction())[0].assertTextContains("Draftname")
            onAllNodes(hasSetTextAction())[1].assertTextContains("synthetic-secret")
            onNodeWithText("Join", substring = false).performClick()
            onNodeWithTag("connection-Room password").assertTextContains("synthetic-secret")
        }
    }

    @Test fun hostDetailsPreserveComposerAndVisibilityUsesLiveHandler() = runDesktopComposeUiTest(width = 1600, height = 864) {
        Fixture(hosting = true).use { f ->
            shell(f)
            capture("main-hosting")
            onAllNodes(hasSetTextAction())[0].performTextInput("Unsent room message")
            onNodeWithText("Room details", substring = false).performClick()
            onNodeWithText("Stop hosting").assertIsDisplayed()
            onNodeWithText("Hide room details").performClick()
            onAllNodes(hasSetTextAction())[0].assertTextContains("Unsent room message")
            runOnIdle { f.addPeers() }
            capture("main-connected-host")
            onNodeWithText("Invite", substring = false).assertExists()
            assertFalse(f.calls.any { it.endsWith(".stop") || it.endsWith(".disconnect") })
        }
    }

    @Test fun joinedRoomHasNoInviteAndKeepsComposerAtMinimumWidth() = runDesktopComposeUiTest(width = 820, height = 640) {
        Fixture(joined = true).use { f ->
            f.addPeers()
            shell(f)
            onNodeWithText("Invite", substring = false).assertDoesNotExist()
            onNodeWithText("Send", substring = false).assertIsDisplayed()
            onNodeWithText("Room details", substring = false).performClick()
            onNodeWithText("Leave room").assertIsDisplayed()
            capture("main-joined-820")
            onNodeWithText("Hide room details").performClick()
            onNodeWithContentDescription("Open Actions drawer").performClick()
            onNodeWithContentDescription("Close Actions drawer").performClick()
            onNodeWithText("Send", substring = false).assertIsDisplayed()
        }
    }

    @Test fun allSettingsPagesRenderAndVisibilityAppliesImmediately() = runDesktopComposeUiTest(width = 1600, height = 864) {
        Fixture(hosting = true).use { f ->
            setContent { SecureLanTheme { Surface(Modifier.fillMaxSize().testTag("capture"), color = MaterialTheme.colors.background) {
                SettingsWindowContent(SettingsPage.entries) { page -> LiveSettingsPageContent(f.adapter, ComposePeerListState(), f.settings, page) }
            } } }
            for (page in SettingsPage.entries) {
                onAllNodesWithText(page.title)[0].performClick()
                if (page == SettingsPage.AUDIO_VIDEO) onNodeWithText("Test camera").assertIsDisplayed()
                capture("settings-" + page.name.lowercase())
            }
            onNodeWithText("Current room discoverable").performClick()
            runOnIdle {
                assertTrue(f.calls.contains("PeerDiscoveryService.setAnnounceEnabled"))
                assertFalse(f.settings.settings.network.discoverable)
                assertTrue(f.adapter.statusState.localServerRunning)
            }
            onAllNodesWithText("General")[0].performClick()
            onAllNodesWithText("General")[0].performKeyInput { pressKey(Key.DirectionRight) }
            onNodeWithText("Refresh devices").assertIsDisplayed()
        }
    }

    @Test fun narrowSettingsKeepControlsReachableInEveryTheme() = runDesktopComposeUiTest(width = 820, height = 640) {
        Fixture().use { f ->
            var theme by mutableStateOf(SecureLanThemeMode.DARK)
            setContent { SecureLanTheme(themeMode = theme) { Surface(Modifier.fillMaxSize().testTag("capture"), color = MaterialTheme.colors.background) {
                SettingsWindowContent(SettingsPage.entries) { page -> LiveSettingsPageContent(f.adapter, ComposePeerListState(), f.settings, page) }
            } } }
            for (mode in SecureLanThemeMode.entries) {
                runOnIdle { theme = mode }
                onAllNodesWithText("Audio & video")[0].performClick()
                onNodeWithText("Refresh devices").assertIsDisplayed()
                onNodeWithText("Test camera").performScrollTo().assertIsDisplayed()
                capture("settings-820-" + mode.name.lowercase())
                onAllNodesWithText("Network")[0].performClick()
                onNodeWithText("Clear recent rooms").performScrollTo().assertIsDisplayed()
            }
        }
    }

    @Test fun conversationAndActivityRemainReachableAcrossWorkspaceWidths() {
        for (width in listOf(1024, 1280, 1360, 1440, 1600)) {
            runDesktopComposeUiTest(width = width, height = 720) {
                Fixture(joined = true).use { f ->
                    f.addPeers()
                    shell(f)
                    onNodeWithText("Send", substring = false).assertIsDisplayed()
                    if (width - 24 < 1280) {
                        onNodeWithContentDescription("Open Actions drawer").performClick()
                        onNodeWithContentDescription("Close Actions drawer").performClick()
                    } else onNodeWithText("Activity", substring = false).assertIsDisplayed()
                    capture("main-joined-$width")
                }
            }
        }
    }

    @Test fun scaledSettingsKeepTabsAndRecoveryControlsReachable() = runDesktopComposeUiTest(width = 1280, height = 900) {
        Fixture().use { f ->
            setContent { CompositionLocalProvider(LocalDensity provides Density(1.25f, 1.2f)) { SecureLanTheme {
                Surface(Modifier.fillMaxSize().testTag("capture"), color = MaterialTheme.colors.background) {
                    SettingsWindowContent(SettingsPage.entries) { page -> LiveSettingsPageContent(f.adapter, ComposePeerListState(), f.settings, page) }
                }
            } } }
            onAllNodesWithText("Network")[0].performClick()
            onNodeWithText("Clear recent rooms").performScrollTo().assertIsDisplayed()
            capture("settings-scaled-network")
            onAllNodesWithText("Audio & video")[0].performClick()
            onNodeWithText("Test camera").performScrollTo().assertIsDisplayed()
        }
    }

    @Test fun hostRoomDetailsKeepExitFullyVisibleWithoutScrolling() {
        for (width in listOf(820, 1600)) {
            runDesktopComposeUiTest(width = width, height = 640) {
                Fixture(hosting = true).use { f ->
                    shell(f)
                    onNodeWithText("Room details", substring = false).performClick()
                    capture("room-details-host-$width")
                    val exit = onNodeWithText("Stop hosting").getUnclippedBoundsInRoot()
                    val details = onNodeWithTag("room-details").getUnclippedBoundsInRoot()
                    assertTrue("Stop hosting must fit completely inside Room details without scrolling", exit.bottom <= details.bottom)
                    val card = onNodeWithTag("room-details-content").getUnclippedBoundsInRoot()
                    assertTrue("Room details card must not be cut off", card.bottom <= details.bottom)
                    onNodeWithText("No messages yet").assertIsDisplayed()
                    onNodeWithText("Send", substring = false).assertIsDisplayed()
                }
            }
        }
    }

    @Test fun hostHeaderActionsWrapWithoutHidingCallsOrRoomDetails() = runDesktopComposeUiTest(width = 820, height = 640) {
        Fixture(hosting = true).use { f ->
            f.addPeers()
            shell(f)
            listOf("Invite", "Voice call", "Video call", "Context", "Room details").forEach {
                onNodeWithText(it, substring = false).assertIsDisplayed()
            }
            capture("room-header-host-820")
            onNodeWithText("Room details", substring = false).performClick()
            onNodeWithText("Stop hosting").assertIsDisplayed()
            onNodeWithText("Send", substring = false).assertIsDisplayed()
        }
    }

    @Test fun connectionFormsKeepActionsReachableAndValidatePorts() {
        for (width in listOf(820, 1360, 1600)) runDesktopComposeUiTest(width = width, height = if (width == 820) 640 else 864) {
            Fixture().use { f ->
                shell(f)
                onNodeWithText("Set up room").performClick()
                onNodeWithText("Stop hosting").assertDoesNotExist()
                onNodeWithText("Start secure room").performScrollTo().assertIsDisplayed()
                onNodeWithText("Show room on local network").performScrollTo().assertIsDisplayed()
                onNodeWithText("Advanced settings").performScrollTo().performClick()
                onNodeWithTag("connection-File port").performScrollTo().performTextReplacement("5050")
                onNodeWithText("Choose a different port from the chat port.").assertIsDisplayed()
                onNodeWithText("Start secure room").assertIsNotEnabled()
                onNodeWithTag("connection-File port").performTextReplacement("70000")
                onNodeWithText("Enter a number from 1 to 65535.").assertIsDisplayed()
                capture("connection-host-error-$width")
                onNodeWithTag("connection-File port").performTextReplacement("5051")
                onNodeWithText("Start secure room").performScrollTo().assertIsEnabled()
                capture("connection-host-$width")
                assertFalse(f.calls.contains("ChatServerService.start"))
            }
        }
    }

    @Test fun selectedRoomCanBeEditedWithoutLosingCredentialsOrJoining() = runDesktopComposeUiTest(width = 1600, height = 864) {
        Fixture().use { f ->
            shell(f)
            onNodeWithText("Review connection").performScrollTo().performClick()
            onNodeWithText("Selected room: 192.168.31.123").assertIsDisplayed()
            onNodeWithTag("connection-Room address").assertDoesNotExist()
            onNodeWithText("Disconnect").assertDoesNotExist()
            onNodeWithTag("connection-Your name").performTextReplacement("Atlas")
            onNodeWithTag("connection-Room password").performTextReplacement("synthetic-secret")
            onNodeWithText("Advanced settings").performClick()
            capture("connection-join-selected")
            onNodeWithText("Change room").performClick()
            onNodeWithTag("connection-Room address").assertIsFocused().performTextReplacement("192.168.31.124")
            onNodeWithText("Selected room: 192.168.31.124").assertExists()
            onNodeWithTag("connection-Your name").assertTextContains("Atlas")
            onNodeWithTag("connection-Room password").assertTextContains("synthetic-secret")
            capture("connection-join-manual")
            onNodeWithText("Host", substring = false).performClick()
            onNodeWithText("Join", substring = false).performClick()
            onNodeWithTag("connection-Room address").assertTextContains("192.168.31.124")
            assertFalse(f.calls.contains("ChatClientService.connect"))
        }
    }

    @Test fun narrowConnectionFormSupportsThemesAndScaledText() = runDesktopComposeUiTest(width = 1024, height = 800) {
        Fixture().use { f ->
            var theme by mutableStateOf(SecureLanThemeMode.DARK)
            setContent { CompositionLocalProvider(LocalDensity provides Density(1.25f, 1.2f)) { SecureLanTheme(themeMode = theme) {
                Surface(Modifier.fillMaxSize().testTag("capture"), color = MaterialTheme.colors.background) {
                    ComposeShellContent(f.adapter, theme, f.settings, {})
                }
            } } }
            onNodeWithText("Set up room").performClick()
            for (mode in SecureLanThemeMode.entries) {
                runOnIdle { theme = mode }
                onNodeWithText("Show room on local network").performScrollTo().assertIsDisplayed()
                onNodeWithText("Start secure room").performScrollTo().assertIsDisplayed()
                capture("connection-scaled-" + mode.name.lowercase())
            }
            onNodeWithText("Join", substring = false).performScrollTo().performClick()
            onNodeWithTag("connection-Room address").performScrollTo().assertIsDisplayed()
            onNodeWithText("Join Room", substring = false).performScrollTo().assertIsDisplayed()
            capture("connection-join-scaled")
        }
    }

    private fun DesktopComposeUiTest.shell(f: Fixture) {
        setContent { SecureLanTheme { Surface(Modifier.fillMaxSize().testTag("capture"), color = MaterialTheme.colors.background) {
            ComposeShellContent(f.adapter, SecureLanThemeMode.DARK, f.settings, {})
        } } }
    }

    private fun DesktopComposeUiTest.capture(name: String) {
        val bitmap = onNodeWithTag("capture").captureToImage().asSkiaBitmap()
        val directory = Path.of("build/reports/target-ui")
        Files.createDirectories(directory)
        Image.makeFromBitmap(bitmap).use { image -> image.encodeToData()!!.use { Files.write(directory.resolve("$name.png"), it.bytes) } }
    }

    private class Fixture(hosting: Boolean = false, joined: Boolean = false) : AutoCloseable {
        val calls = mutableListOf<String>()
        val settings = DesktopAppSettingsController(DesktopAppSettings(displayName = "Atlas", reducedMotion = true,
            network = DesktopNetworkSettings(recentRooms = listOf(DesktopRecentRoom("192.168.31.123")))))
        val adapter = ComposeDesktopHostAdapter(
            proxy(ChatServerService::class.java, mapOf("isRunning" to hosting)),
            proxy(ChatClientService::class.java, mapOf("isConnected" to (hosting || joined))),
            proxy(FileTransferServerService::class.java, mapOf("isRunning" to (hosting || joined))),
            proxy(QuickShareService::class.java, mapOf("shares" to emptyList<Any>(), "landingUrls" to emptyList<String>())),
            proxy(PeerDiscoveryService::class.java, mapOf("snapshot" to emptyList<Any>())),
            proxy(RandomNicknameService::class.java, mapOf("generate" to "Atlas")),
            settingsController = settings, uiStateDispatcher = { it() },
        )
        fun addPeers() {
            adapter.chatEventPublisher.publish(ChatUserJoinedEvent("Yvonne", "192.168.31.10:5050", PeerCapabilities.desktop("test", 5051)))
            adapter.chatEventPublisher.publish(ChatUserJoinedEvent("Morgan", "192.168.31.11:5050", PeerCapabilities.desktop("test", 5051)))
            adapter.chatEventPublisher.publish(ChatMessageReceivedEvent("Yvonne", "Connected successfully. Can you send the image?"))
            adapter.chatEventPublisher.publish(ChatMessageReceivedEvent("Atlas", "Sure, sending it now."))
        }
        private fun <T> proxy(type: Class<T>, values: Map<String, Any?>): T = type.cast(
            Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { _, method, _ ->
                if (method.name in listOf("start", "stop", "connect", "disconnect", "setAnnounceEnabled")) calls += "${type.simpleName}.${method.name}"
                when {
                    values.containsKey(method.name) -> values[method.name]
                    method.returnType == java.lang.Boolean.TYPE -> false
                    method.returnType == Integer.TYPE -> 0
                    method.name == "toString" -> type.simpleName
                    else -> null
                }
            },
        )
        override fun close() = adapter.shutdown()
    }
}
