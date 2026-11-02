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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.common.net.interfaces.NetworkBinding
import com.shterneregen.securelan.desktop.compose.SecureLanTheme
import com.shterneregen.securelan.desktop.compose.SecureLanThemeMode
import com.shterneregen.securelan.desktop.compose.settings.DesktopAppSettingsController
import com.shterneregen.securelan.desktop.compose.ui.shell.AppToolButtons
import com.shterneregen.securelan.desktop.compose.ui.shell.LiveComposeShellContent
import com.shterneregen.securelan.desktop.lantools.DesktopDiagnosticsController
import com.shterneregen.securelan.desktop.lantools.DesktopExplorerController
import com.shterneregen.securelan.desktop.lantools.DesktopLanToolsController
import com.shterneregen.securelan.desktop.lantools.LanToolsNativeWindowProbe
import com.shterneregen.securelan.desktop.lantools.MemoryLanStore
import com.shterneregen.securelan.lantools.diagnostics.*
import com.shterneregen.securelan.lantools.discovery.*
import com.shterneregen.securelan.lantools.model.*
import com.shterneregen.securelan.lantools.wol.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
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
    private var blockDns = false
    private var metadata = MetadataAvailability.AVAILABLE
    private var discoveryCalls = 0
    private var blockDiscovery = false
    private var discovered = LanDiscoverySnapshot()
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
        }, Dispatchers.Unconfined, Dispatchers.Unconfined,
        diagnostics = DesktopDiagnosticsController(
            DefaultNetworkDiagnosticsService(HostResolver {
                if (blockDns) awaitCancellation() else LanResult.Success(listOf(network.binding.address))
            }, LanNetworkInfoProvider { LanResult.Success(LanNetworkSnapshot(listOf(network))) }, TcpConnector { _, _, _ -> }),
            DiagnosticsNetworkInfoProvider { LanResult.Success(DiagnosticsNetworkSnapshot(listOf(DiagnosticInterface(network,
                listOf(IpLiteral.parse("192.168.1.1")!!), listOf("192.168.1.1"), metadata)), "controlled fixture")) }, Dispatchers.Unconfined),
        explorer = DesktopExplorerController(LanDeviceDiscovery { update ->
            discoveryCalls++
            update(discovered)
            if (blockDiscovery) awaitCancellation()
            LanResult.Success(discovered)
        }, Dispatchers.Unconfined, Dispatchers.Unconfined))

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
        compose.onNodeWithText("Automatic / saved preference").performScrollTo().performClick()
        compose.onNodeWithText("Ethernet · 192.168.1.42 · Recommended").performClick()
        compose.waitUntil(2_000) { compose.onAllNodesWithText("Ethernet · 192.168.1.42 · Recommended").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithTag("wake-details").performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, sends); assertEquals(1, store.devices.size); assertEquals(network.binding.key, sentBinding) }
        compose.onAllNodesWithText("Device wake-up is not confirmed.", substring = true)[0].assertExists()
        compose.onNodeWithText("Edit device").performScrollTo().performClick()
        compose.onNodeWithTag("lan-name").performTextReplacement("Renamed")
        compose.onNodeWithText("Save device").performScrollTo().performClick()
        compose.onNodeWithText("Delete device").performScrollTo().performClick()
        compose.onNodeWithText("Delete device").performKeyInput { pressKey(Key.Escape) }
        compose.onNodeWithText("Confirm delete").assertDoesNotExist()
        compose.runOnIdle { assertTrue(controller.state.value.visible) }
        compose.runOnIdle { assertEquals("Renamed", store.devices.single().displayName) }
        compose.onNodeWithText("Delete device").performScrollTo().performClick()
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
        compose.onNodeWithText("Network information").assertExists()
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

    @Test fun diagnosticsValidatesChecksTcpDnsAndGatewayWithHonestResults() {
        content(740, 520, SecureLanThemeMode.LIGHT)
        compose.onNodeWithText("Diagnostics").performClick()
        compose.onNodeWithText("IPv4: 192.168.1.42/24", substring = true).assertExists()
        compose.onNodeWithTag("diag-tcp-host").performScrollTo().performTextInput("host.local")
        compose.onNodeWithTag("diag-tcp-host").performKeyInput { pressKey(Key.Tab) }
        compose.onNodeWithTag("diag-tcp-port").assertIsFocused()
        compose.onNodeWithText("Check TCP", substring = false).performScrollTo().performClick()
        compose.onNodeWithText("Enter a TCP port", substring = true).assertExists()
        compose.onNodeWithTag("diag-tcp-port").performScrollTo().performTextInput("1234")
        compose.onNodeWithText("Check TCP", substring = false).performScrollTo().performClick()
        compose.onNodeWithText("Only this TCP port accepted", substring = true).assertExists()
        compose.onNodeWithText("Only this TCP port accepted", substring = true).performScrollTo()
        screenshot("diagnostics-tcp-narrow-light.png")
        compose.onNodeWithTag("diag-dns-host").performScrollTo().performTextInput("host.local")
        compose.onNodeWithText("Look up DNS").performScrollTo().performClick()
        compose.onNodeWithText("Lookup time:", substring = true).assertExists()
        compose.onNodeWithTag("diag-source").performScrollTo().performClick()
        compose.onNodeWithText("eth0 · 192.168.1.42", substring = false).performClick()
        compose.waitUntil(2_000) { compose.onAllNodesWithText("eth0 · 192.168.1.42", substring = false).fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithText("Choose gateway").performScrollTo().performClick()
        compose.onNodeWithText("192.168.1.1", substring = false).performClick()
        compose.onNodeWithTag("diag-gateway-port").performScrollTo().performTextInput("1234")
        compose.onNodeWithText("Check gateway TCP", substring = false).performScrollTo().performClick()
        compose.onNodeWithText("Gateway TCP 192.168.1.1:1234", substring = true).assertExists()
        compose.onNodeWithText("Gateway TCP 192.168.1.1:1234", substring = true).performScrollTo()
        screenshot("diagnostics-gateway-narrow-light.png")
        compose.onNodeWithText("Network information").performScrollTo()
        screenshot("diagnostics-narrow-light.png")
        compose.onNodeWithText("Devices", substring = false).performClick()
        compose.onNodeWithText("Diagnostics", substring = false).performClick()
        compose.onNodeWithTag("diag-tcp-host").performScrollTo().assertTextContains("host.local")
    }

    @Test fun diagnosticsCancelAndUnsupportedGatewayRemainTruthful() {
        blockDns = true
        metadata = MetadataAvailability.UNSUPPORTED
        content()
        compose.onNodeWithText("Diagnostics").performClick()
        compose.onNodeWithText("Gateway / DNS: Unsupported", substring = true).assertExists()
        compose.onNodeWithTag("diag-dns-host").performScrollTo().performTextInput("host.local")
        compose.onNodeWithText("Look up DNS").performScrollTo().performClick()
        compose.onNodeWithText("Cancel dns").performScrollTo().performClick()
        compose.onNodeWithText("Lookup cancelled.", substring = true).assertExists()
        compose.onNodeWithText("Check gateway TCP", substring = false).assertDoesNotExist()
        compose.onNodeWithText("ICMP ping: unavailable", substring = true).performScrollTo().assertIsDisplayed()
        screenshot("diagnostics-cancel-wide-dark.png")
        compose.onNodeWithText("Network information").performScrollTo()
        screenshot("diagnostics-wide-dark.png")
    }

    @Test fun discoveryShowsRealSourceFactsAndCopiesOnlyReviewedSettingsWithoutMergingSavedRows() {
        val ip = IpLiteral.parse("192.168.1.77")!!
        val row = DiscoveredLanDevice("printer-row", network.binding.key, "Advertised printer", "printer.local", listOf(ip),
            listOf(LanAdvertisedService("Printer", "_ipp._tcp.local.", 631, listOf(ip))), setOf("mDNS/DNS-SD"), Instant.EPOCH)
        discovered = LanDiscoverySnapshot(listOf(row))
        store.devices = listOf(SavedLanDevice(UUID.randomUUID(), "Saved user label", ip))
        content()
        compose.runOnIdle { assertEquals(0, discoveryCalls) }
        compose.onNodeWithText("Refresh devices").performClick()
        compose.onNodeWithTag("found-row-printer-row").assertExists()
        compose.runOnIdle { assertEquals(1, store.devices.size); assertEquals(1, discoveryCalls) }
        compose.onNodeWithText("Show: all").performClick()
        compose.onNodeWithText("Discovered", substring = false).performClick()
        compose.onNodeWithTag("lan-search").performTextInput("IPP")
        compose.onNodeWithText("Details", substring = false).performScrollTo().performClick()
        compose.onNodeWithText("Type: Unknown").assertExists()
        compose.onNodeWithText("Source: mDNS/DNS-SD").assertExists()
        screenshot("explorer-wide-dark.png")
        compose.onNodeWithText("Diagnostics 192.168.1.77:631").performScrollTo().performClick()
        compose.onNodeWithTag("diag-tcp-host").performScrollTo().assertTextContains("192.168.1.77")
        compose.onNodeWithTag("diag-tcp-port").performScrollTo().assertTextContains("631")
        compose.onNodeWithText("Check TCP", substring = false).performScrollTo().performClick()
        compose.onNodeWithText("Devices", substring = false).performClick()
        compose.onAllNodesWithText("Last TCP check", substring = true)[0].assertExists()
        compose.onNodeWithText("Save as new device").performScrollTo().performClick()
        compose.onNodeWithTag("lan-name").assertTextContains("Advertised printer")
        compose.onNodeWithTag("lan-mac").assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
        compose.onNodeWithText("Save device").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(2, store.devices.size)
            assertEquals("printer.local", store.devices.last().hostnameHint)
            assertNull(store.devices.last().wake)
            assertEquals("Saved user label", store.devices.first().displayName)
        }
    }

    @Test fun cancellingDiscoveryKeepsHistoricalRowsAndSearchSurvivesCloseInNarrowLightLayout() {
        val ip = IpLiteral.parse("192.168.1.77")!!
        discovered = LanDiscoverySnapshot(listOf(DiscoveredLanDevice("row", network.binding.key, "Workstation", null, listOf(ip),
            listOf(LanAdvertisedService("Workstation", "_ssh._tcp.local.", 22, listOf(ip))), setOf("mDNS/DNS-SD"), Instant.EPOCH)), partial = true)
        blockDiscovery = true
        content(740, 520, SecureLanThemeMode.LIGHT)
        compose.onNodeWithText("Refresh devices").performClick()
        compose.onNodeWithText("Cancel discovery").assertIsDisplayed().performClick()
        compose.onNodeWithText("Partial results:", substring = true).assertExists()
        compose.onNodeWithText("Discovered · Historical snapshot").performScrollTo().assertIsDisplayed()
        screenshot("explorer-narrow-light.png")
        compose.onNodeWithTag("lan-search").performTextInput("no-match")
        compose.onNodeWithText("No matching devices.").assertExists()
        compose.runOnIdle { controller.dismiss(); controller.open() }
        compose.onNodeWithTag("lan-search").assertTextContains("no-match")
        compose.runOnIdle { assertEquals(1, discoveryCalls) }
    }

    @Test fun minimumWindowKeepsDiscoveryControlsAndDeviceActionsReachableByScrolling() {
        val ip = IpLiteral.parse("192.168.1.77")!!
        store.devices = listOf(SavedLanDevice(UUID.randomUUID(), "Saved"))
        discovered = LanDiscoverySnapshot(listOf(DiscoveredLanDevice("small", network.binding.key, "Workstation", null, listOf(ip),
            listOf(LanAdvertisedService("Workstation", "_ssh._tcp.local.", 22, listOf(ip))), setOf("mDNS/DNS-SD"), Instant.EPOCH)))
        content(400, 320, SecureLanThemeMode.LIGHT)
        compose.onNodeWithText("Refresh devices").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithTag("lan-device-list").performScrollToNode(hasTestTag("found-row-small"))
        compose.onNode(hasText("Details") and hasAnyAncestor(hasTestTag("found-row-small"))).performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText("Save as new device").performScrollTo().assertIsDisplayed()
        screenshot("explorer-minimum-light.png")
        compose.onNodeWithTag("lan-device-list").performScrollToIndex(0)
        compose.onNodeWithTag("lan-search").performScrollTo().assertIsDisplayed()
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
