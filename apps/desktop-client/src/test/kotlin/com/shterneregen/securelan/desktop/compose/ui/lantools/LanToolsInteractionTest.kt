package com.shterneregen.securelan.desktop.compose.ui.lantools

import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.common.net.interfaces.NetworkBinding
import com.shterneregen.securelan.desktop.compose.SecureLanTheme
import com.shterneregen.securelan.desktop.compose.SecureLanThemeMode
import com.shterneregen.securelan.desktop.compose.settings.DesktopAppSettingsController
import com.shterneregen.securelan.desktop.compose.ui.shell.AppToolButtons
import com.shterneregen.securelan.desktop.compose.ui.shell.LiveComposeShellContent
import com.shterneregen.securelan.desktop.lantools.DesktopLanToolsController
import com.shterneregen.securelan.desktop.lantools.LanToolsNativeWindowProbe
import com.shterneregen.securelan.desktop.lantools.MemoryLanStore
import com.shterneregen.securelan.lantools.model.*
import com.shterneregen.securelan.lantools.wol.*
import kotlinx.coroutines.Dispatchers
import org.jetbrains.skia.Image
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.UUID

/** Production content with controlled endpoints. Native AWT window ownership is checked separately. */
@OptIn(ExperimentalTestApi::class)
class LanToolsInteractionTest {
    @get:Rule val compose = createComposeRule()
    private val store = MemoryLanStore()
    private var sends = 0
    private var sentBinding: String? = null
    private val network = LanInterfaceSnapshot(NetworkBinding("eth0:192.168.1.42", "eth0", IpLiteral.parse("192.168.1.42")!!, 24),
        "Ethernet", true, IpLiteral.parse("192.168.1.255"))
    private val controller = DesktopLanToolsController(store,
        LanNetworkInfoProvider { LanResult.Success(LanNetworkSnapshot(listOf(network), "eth0")) },
        object : WakeOnLanService {
            override suspend fun send(request: WakeRequest): LanResult<WakeReceipt> {
                sends++
                sentBinding = request.bindingKey
                return LanResult.Success(WakeReceipt(WakeRoute(network, network.broadcast!!, 9), 102, Instant.EPOCH))
            }
        }, Dispatchers.Unconfined, Dispatchers.Unconfined)

    @After fun close() { compose.runOnIdle { controller.close() } }

    private fun content(width: Int = 968, height: Int = 640, theme: SecureLanThemeMode = SecureLanThemeMode.DARK) {
        controller.open()
        compose.setContent { SecureLanTheme(themeMode = theme) {
            Surface(color = MaterialTheme.colors.background) {
            LanToolsContent(controller, controller::dismiss, Modifier.requiredSize(width.dp, height.dp).testTag("lan-content"))
            }
        } }
    }

    @Test fun addValidateWakeEditAndConfirmDeleteWithoutAnyRoom() {
        content()
        compose.onNodeWithText("No saved devices yet.").assertExists()
        compose.onNodeWithText("Add device").performClick()
        compose.onNodeWithTag("lan-name").assertIsFocused().performTextInput("Desktop")
        compose.onNodeWithTag("lan-name").performKeyInput { pressKey(Key.Tab) }
        compose.onNodeWithTag("lan-address").assertIsFocused()
        compose.onNodeWithTag("lan-mac").performTextInput("invalid")
        compose.onNodeWithText("Save device").performScrollTo().performClick()
        compose.onNodeWithText("Enter a valid nonzero unicast MAC address.").assertExists()
        compose.onNodeWithTag("lan-mac").performScrollTo().performTextReplacement("02:ab:cd:ef:12:34")
        compose.onNodeWithText("Save device").performScrollTo().performClick()
        compose.onNodeWithText("Automatic / saved preference").performClick()
        compose.onNodeWithText("Ethernet · 192.168.1.42 · Recommended").performClick()
        compose.waitUntil(2_000) { compose.onAllNodesWithText("Ethernet · 192.168.1.42 · Recommended").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithTag("wake-details").performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, sends); assertEquals(1, store.devices.size); assertEquals(network.binding.key, sentBinding) }
        compose.onAllNodesWithText("Device wake-up is not confirmed.", substring = true)[0].assertExists()
        compose.onNodeWithText("Edit device").performClick()
        compose.onNodeWithTag("lan-name").performTextReplacement("Renamed")
        compose.onNodeWithText("Save device").performScrollTo().performClick()
        compose.onNodeWithText("Delete device").performClick()
        compose.onNodeWithText("Delete device").performKeyInput { pressKey(Key.Escape) }
        compose.onNodeWithText("Confirm delete").assertDoesNotExist()
        compose.runOnIdle { assertTrue(controller.state.value.visible) }
        compose.runOnIdle { assertEquals("Renamed", store.devices.single().displayName) }
        compose.onNodeWithText("Delete device").performClick()
        compose.onNodeWithText("Confirm delete").performClick()
        compose.onNodeWithText("No saved devices yet.").assertExists()
    }

    @Test fun toolsMenuOpensUtilityAndEscapePreservesFormAndTab() {
        var steganography = 0
        compose.setContent { SecureLanTheme {
            val state by controller.state.collectAsState()
            Column {
                Row { AppToolButtons({}, { steganography++ }, controller::open) }
                if (state.visible) LanToolsContent(controller, controller::dismiss)
            }
        } }
        compose.onNodeWithText("Tools").performClick()
        compose.onNodeWithText("Steganography").performClick()
        compose.runOnIdle { assertEquals(1, steganography) }
        compose.onNodeWithText("Tools").performClick()
        compose.onNodeWithText("LAN Tools", substring = false).performClick()
        compose.onNodeWithText("Diagnostics").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Network diagnostics are not available yet.").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Network diagnostics are not available yet.").assertExists()
        compose.onNodeWithText("Devices", substring = false).performClick()
        compose.onNodeWithText("Add device").performClick()
        compose.onNodeWithTag("lan-name").performTextInput("Unfinished")
        compose.onNodeWithTag("lan-name").performKeyInput { pressKey(Key.Escape) }
        compose.waitUntil(5_000) { !controller.state.value.visible }
        compose.runOnIdle { assertFalse(controller.state.value.visible) }
        compose.onNodeWithText("Tools").performClick()
        compose.onNodeWithText("LAN Tools", substring = false).performClick()
        compose.onNodeWithTag("lan-name").assertTextContains("Unfinished").assertIsFocused()
    }

    @Test fun narrowLightLayoutRemainsScrollableAndCapturesScreenshot() {
        store.devices = listOf(SavedLanDevice(UUID.randomUUID(), "Saved workstation with a long readable name", IpLiteral.parse("192.168.1.42"),
            wake = WakeConfiguration(MacAddress.parse("02:ab:cd:ef:12:34")!!)))
        content(740, 520, SecureLanThemeMode.LIGHT)
        compose.onNodeWithText("Details").performScrollTo().performClick()
        compose.onNodeWithTag("wake-details").performScrollTo().assertIsDisplayed()
        screenshot("narrow-light.png")
        compose.onNodeWithText("Edit device").performScrollTo().performClick()
        compose.onNodeWithText("Save device").performScrollTo().assertIsDisplayed()
    }

    @Test fun wideDarkLayoutHasDetailsAndNoWakeForDevicesWithoutMac() {
        val device = SavedLanDevice(UUID.randomUUID(), "Printer")
        store.devices = listOf(device)
        content()
        compose.onNodeWithText("Details").performClick()
        compose.onNodeWithTag("wake-row-${device.id}").assertIsNotEnabled()
        compose.onNodeWithText("Edit this device and add a MAC address to enable Wake-on-LAN.").assertExists()
        screenshot("wide-dark.png")
    }

    @Test fun realShellKeepsSelectedPeerAndChatDraftWhenUtilityOpensAndCloses() {
        val host = LanToolsNativeWindowProbe.workspaceHost()
        val settings = DesktopAppSettingsController()
        try {
            compose.setContent { SecureLanTheme {
                Surface(color = MaterialTheme.colors.background) {
                    LiveComposeShellContent(host, SecureLanThemeMode.DARK, settings, {}, controller)
                }
            } }
            compose.onNode(hasSetTextAction()).performTextInput("Keep this chat draft")
            val selected = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Selected")
            compose.onNode(hasText("Other room") and hasClickAction()).performClick()
            compose.onNode(hasText("Other room") and selected).assertExists()
            compose.runOnIdle { controller.open() }
            compose.waitUntil(5_000) { java.awt.Window.getWindows().filterIsInstance<java.awt.Dialog>().any { it.isShowing && it.title.endsWith("LAN Tools") } }
            compose.runOnIdle { controller.dismiss() }
            compose.waitUntil(5_000) { java.awt.Window.getWindows().filterIsInstance<java.awt.Dialog>().none { it.isShowing && it.title.endsWith("LAN Tools") } }
            compose.onNode(hasSetTextAction()).assertTextContains("Keep this chat draft")
            compose.onNode(hasText("Other room") and selected).assertExists()
        } finally { compose.runOnIdle { host.shutdown(); settings.close() } }
    }

    private fun screenshot(name: String) {
        val bitmap = compose.onNodeWithTag("lan-content").captureToImage().asSkiaBitmap()
        val directory = Path.of("build/reports/lan-tools-ui")
        Files.createDirectories(directory)
        Image.makeFromBitmap(bitmap).use { image ->
            image.encodeToData()!!.use { data -> Files.write(directory.resolve(name), data.bytes) }
        }
    }
}
