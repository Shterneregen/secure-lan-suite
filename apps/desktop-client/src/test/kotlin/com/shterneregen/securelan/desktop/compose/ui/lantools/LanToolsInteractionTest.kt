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
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.common.net.address.MacAddress
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
import com.shterneregen.securelan.neighbor.NeighborMacResult
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
    private var diagnosticInterfaces: List<DiagnosticInterface>? = null
    private var metadata = MetadataAvailability.AVAILABLE
    private var discoveryCalls = 0
    private var blockDiscovery = false
    private var discovered = LanDiscoverySnapshot()
    private var interfaces: List<LanInterfaceSnapshot>? = null
    private val network = LanInterfaceSnapshot(NetworkBinding("eth0:192.168.1.42", "eth0", IpLiteral.parse("192.168.1.42")!!, 24),
        "Ethernet", true, IpLiteral.parse("192.168.1.255"))
    private val controller = DesktopLanToolsController(store,
        { LanResult.Success(LanNetworkSnapshot(interfaces ?: listOf(network), "eth0")) },
        object : WakeOnLanService {
            override suspend fun send(request: WakeRequest): LanResult<WakeReceipt> {
                sends++
                sentBinding = request.bindingKey
                return LanResult.Success(WakeReceipt(WakeRoute(network, network.broadcast!!, 9), 102, Instant.EPOCH))
            }
        }, Dispatchers.Unconfined, Dispatchers.Unconfined,
        diagnostics = DesktopDiagnosticsController(
            DefaultNetworkDiagnosticsService({
                if (blockDns) awaitCancellation() else LanResult.Success(listOf(network.binding.address))
            }, { LanResult.Success(LanNetworkSnapshot(listOf(network))) }, { _, _, _ -> }),
            { LanResult.Success(DiagnosticsNetworkSnapshot(diagnosticInterfaces ?: listOf(DiagnosticInterface(network,
                listOf(IpLiteral.parse("192.168.1.1")!!), listOf("192.168.1.1"), metadata)), "controlled fixture")) }, Dispatchers.Unconfined),
        explorer = DesktopExplorerController({ update ->
            discoveryCalls++
            update(discovered)
            if (blockDiscovery) awaitCancellation()
            LanResult.Success(discovered)
        }, Dispatchers.Unconfined, Dispatchers.Unconfined),
        neighbor = { NeighborMacResult.Found(MacAddress.parse("02:11:22:33:44:55")!!) })

    @After fun close() { compose.runOnIdle { controller.close() } }

    private fun content(width: Int = 968, height: Int = 640, theme: SecureLanThemeMode = SecureLanThemeMode.DARK) {
        controller.open()
        compose.setContent { SecureLanTheme(themeMode = theme) {
            Surface(color = MaterialTheme.colors.background) {
            LanToolsContent(controller, controller::dismiss, Modifier.requiredSize(width.dp, height.dp).testTag("lan-content"))
            }
        } }
    }

    @Test fun macLookupIconFillsAnEmptyMacWithoutSaving() {
        content()
        compose.onAllNodesWithText("Add device")[0].performClick()
        compose.onNodeWithTag("lan-name").performTextInput("Desktop")
        compose.onNodeWithTag("lan-address").performTextInput("192.168.1.20")
        compose.onNodeWithTag("lan-mac-lookup").performScrollTo().assertIsDisplayed().assertIsEnabled()
            .assertContentDescriptionEquals("Look up MAC address from IP").performClick()
        compose.waitUntil(2_000) {
            compose.onNodeWithTag("lan-mac").fetchSemanticsNode().config
                .getOrNull(SemanticsProperties.EditableText)?.text == "02:11:22:33:44:55"
        }
        compose.onNodeWithText("MAC from the local neighbor cache", substring = true).assertExists()
        compose.runOnIdle { assertEquals(0, store.devices.size) }
    }

    @Test fun addValidateWakeEditAndConfirmDeleteWithoutAnyRoom() {
        content()
        compose.onNodeWithText("No devices yet").assertExists()
        compose.onAllNodesWithText("Add device")[0].performClick()
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
        compose.onNodeWithText("No devices yet").assertExists()
    }

    @Test fun vanishedInterfaceIsShownAsUnavailableUntilUserChoosesAnotherRoute() {
        val device = SavedLanDevice(UUID.randomUUID(), "Saved PC", wake = WakeConfiguration(MacAddress.parse("02:11:22:33:44:55")!!))
        store.devices = listOf(device)
        content()
        compose.runOnIdle { controller.selectDevice(device.id); controller.selectBinding(network.binding.key); interfaces = emptyList(); controller.refreshInterfaces() }
        compose.onNodeWithText("Selected interface unavailable — choose an interface").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Selected interface unavailable — choose an interface").performClick()
        compose.onNodeWithText("Automatic / saved preference").performClick()
        compose.runOnIdle { assertNull(controller.state.value.bindingKey) }
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
        compose.onNodeWithText("Network overview").assertExists()
        compose.onNodeWithText("Devices", substring = false).performClick()
        compose.onAllNodesWithText("Add device")[0].performClick()
        compose.onNodeWithTag("lan-name").performTextInput("Unfinished")
        compose.onNodeWithTag("lan-name").performKeyInput { pressKey(Key.Escape) }
        compose.waitUntil(5_000) { !controller.state.value.visible }
        compose.runOnIdle { assertFalse(controller.state.value.visible) }
        compose.onNodeWithText("Tools").performClick()
        compose.onNodeWithText("LAN Tools", substring = false).performClick()
        compose.onNodeWithTag("lan-name").assertTextContains("Unfinished").assertIsFocused()
    }

    @Test fun narrowLightLayoutRemainsScrollableAndCapturesScreenshot() {
        val device = SavedLanDevice(UUID.randomUUID(), "Saved workstation with a long readable name", IpLiteral.parse("192.168.1.42"),
            wake = WakeConfiguration(MacAddress.parse("02:ab:cd:ef:12:34")!!))
        store.devices = listOf(device)
        content(740, 520, SecureLanThemeMode.LIGHT)
        compose.onNodeWithTag("saved-row-${device.id}").performScrollTo().performClick()
        compose.onNodeWithTag("wake-details").performScrollTo().assertIsDisplayed()
        screenshot("narrow-light.png")
        compose.onNodeWithText("Edit device").performScrollTo().performClick()
        compose.onNodeWithText("Save device").performScrollTo().assertIsDisplayed()
    }

    @Test fun wideDarkLayoutHasDetailsAndNoWakeForDevicesWithoutMac() {
        val device = SavedLanDevice(UUID.randomUUID(), "Printer")
        store.devices = listOf(device)
        content()
        compose.onNodeWithTag("saved-row-${device.id}").performClick()
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
            waitForLanToolsWindow(showing = true)
            compose.runOnIdle { controller.dismiss() }
            waitForLanToolsWindow(showing = false)
            compose.onNode(hasSetTextAction()).assertTextContains("Keep this chat draft")
            compose.onNode(hasText("Other room") and selected).assertExists()
        } finally { compose.runOnIdle { host.shutdown(); settings.close() } }
    }

    private fun waitForLanToolsWindow(showing: Boolean) {
        compose.waitForIdle()
        // Native window creation runs on AWT, outside the test scene's frame clock. Allow a
        // cold CI runner to initialize it, and inspect AWT state on its owning event thread.
        compose.waitUntil(15_000) {
            compose.runOnUiThread {
                java.awt.Window.getWindows().filterIsInstance<java.awt.Dialog>()
                    .any { it.isShowing && it.title.endsWith("LAN Tools") } == showing
            }
        }
    }

    @Test fun diagnosticsValidatesChecksTcpDnsAndGatewayWithHonestResults() {
        content(740, 520, SecureLanThemeMode.LIGHT)
        compose.onNodeWithText("Diagnostics").performClick()
        compose.onNodeWithText("192.168.1.42/24", substring = false).assertExists()
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
        compose.onNode(hasText("192.168.1.1", substring = false) and hasClickAction()).performClick()
        compose.onNodeWithTag("diag-gateway-port").performScrollTo().performTextInput("1234")
        compose.onNodeWithText("Check gateway TCP", substring = false).performScrollTo().performClick()
        compose.onNodeWithText("Gateway TCP 192.168.1.1:1234", substring = true).assertExists()
        compose.onNodeWithText("Gateway TCP 192.168.1.1:1234", substring = true).performScrollTo()
        screenshot("diagnostics-gateway-narrow-light.png")
        compose.onNodeWithText("Network overview").performScrollTo()
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
        compose.onNodeWithText("Advanced settings").performScrollTo().performClick()
        compose.onNodeWithText("ICMP ping: unavailable", substring = true).performScrollTo().assertIsDisplayed()
        screenshot("diagnostics-cancel-wide-dark.png")
        compose.onNodeWithText("Network overview").performScrollTo()
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
        compose.onAllNodesWithText("Refresh devices")[0].performClick()
        compose.onNodeWithTag("found-row-printer-row").assertExists()
        compose.runOnIdle { assertEquals(1, store.devices.size); assertEquals(1, discoveryCalls) }
        compose.onNodeWithText("Show: All").performClick()
        compose.onNodeWithText("Discovered", substring = false).performClick()
        compose.onNodeWithTag("lan-search").performTextInput("IPP")
        compose.onNodeWithTag("found-row-printer-row").performScrollTo().performClick()
        compose.onNodeWithText("Unknown").assertExists()
        compose.onNodeWithText("mDNS/DNS-SD").assertExists()
        screenshot("explorer-wide-dark.png")
        compose.onNodeWithTag("check-service-printer-row-192.168.1.77-631").performScrollTo().performClick()
        compose.onNodeWithTag("diag-tcp-host").performScrollTo().assertTextContains("192.168.1.77")
        compose.onNodeWithTag("diag-tcp-port").performScrollTo().assertTextContains("631")
        compose.onNodeWithText("Check TCP", substring = false).performScrollTo().performClick()
        compose.onNodeWithText("Devices", substring = false).performClick()
        compose.onAllNodesWithText("Last TCP check", substring = true)[0].assertExists()
        compose.onNodeWithText("Save as new device").assertIsDisplayed().performClick()
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
        compose.onAllNodesWithText("Refresh devices")[0].performClick()
        compose.onNodeWithText("Cancel discovery").assertIsDisplayed().performClick()
        compose.onNodeWithText("Partial results; some sources did not finish.").assertExists()
        compose.onNodeWithText("Historical", substring = true).performScrollTo().assertIsDisplayed()
        screenshot("explorer-narrow-light.png")
        compose.onNodeWithTag("lan-search").performTextInput("no-match")
        compose.onNodeWithText("No matching devices").assertExists()
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
        compose.onAllNodesWithText("Refresh devices")[0].performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithTag("lan-device-list").performScrollToNode(hasTestTag("found-row-small"))
        compose.onNodeWithTag("found-row-small").performClick()
        compose.onNodeWithText("Save as new device").performScrollTo().assertIsDisplayed()
        screenshot("explorer-minimum-light.png")
        compose.onNodeWithTag("lan-device-list").performScrollToIndex(0)
        compose.onNodeWithTag("lan-search").performScrollTo().assertIsDisplayed()
    }

    @Test fun defaultEmptyAndWideDiagnosticsExposeUsefulActionsAndPartialMetadata() {
        metadata = MetadataAvailability.TIMEOUT
        content(1000, 760, SecureLanThemeMode.LIGHT)
        compose.onNodeWithText("No devices yet").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, discoveryCalls) }
        screenshot("redesign-empty-default-light.png")
        compose.onAllNodesWithText("Refresh devices")[1].performClick()
        compose.onNodeWithText("No services found").assertIsDisplayed()
        compose.onNodeWithText("Diagnostics").performClick()
        compose.onNodeWithTag("diag-tcp-host").assertIsDisplayed()
        compose.onNodeWithTag("diag-dns-host").assertIsDisplayed()
        compose.onNodeWithText("Partial: loaded interface data.", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Gateway / DNS: OS query timed out", substring = true).assertIsDisplayed()
        screenshot("redesign-diagnostics-default-light.png")
    }

    @Test fun wideEmptyAndRunningResultsDoNotDuplicatePhaseLabels() = runDesktopComposeUiTest(width = 1360, height = 860) {
        controller.open()
        setContent { SecureLanTheme(themeMode = SecureLanThemeMode.DARK) {
            Surface(color = MaterialTheme.colors.background) {
                LanToolsContent(controller, controller::dismiss, Modifier.fillMaxSize().testTag("wide-content"))
            }
        } }
        fun capture(name: String) {
            val bitmap = onNodeWithTag("wide-content").captureToImage().asSkiaBitmap()
            assertEquals(1360, bitmap.width)
            assertEquals(860, bitmap.height)
            val directory = Path.of("build/reports/lan-tools-ui")
            Files.createDirectories(directory)
            Image.makeFromBitmap(bitmap).use { image -> image.encodeToData()!!.use { Files.write(directory.resolve(name), it.bytes) } }
        }
        onNodeWithText("Close").assertIsDisplayed()
        onNodeWithTag("lan-search").assertIsDisplayed()
        capture("redesign-empty-wide-dark.png")
        blockDns = true
        onNodeWithText("Diagnostics").performClick()
        onNodeWithTag("diag-dns-host").performTextInput("pending.local")
        onNodeWithText("Look up DNS").performClick()
        onNodeWithText("Running: Running", substring = true).assertDoesNotExist()
        onNodeWithText("DNS pending.local:", substring = true).assertIsDisplayed()
        capture("redesign-diagnostics-running-wide-dark.png")
        onNodeWithText("Cancel dns").performClick()
        onNodeWithText("Devices", substring = false).performClick()
        onAllNodesWithText("Add device")[0].performClick()
        onNodeWithTag("lan-name").assertIsFocused()
        capture("reference-editor-wide-dark.png")
    }

    @Test fun minimumDiagnosticsReachesFormsAndAdvancedTimeout() {
        content(400, 320, SecureLanThemeMode.LIGHT)
        compose.onNodeWithText("Diagnostics").performClick()
        compose.onNodeWithTag("diag-tcp-host").performScrollTo().performTextInput("host.local")
        compose.onNodeWithTag("diag-tcp-port").performTextInput("80")
        compose.onNodeWithText("Check TCP", substring = false).performScrollTo().performClick()
        compose.onNodeWithText("Only this TCP port accepted", substring = true).assertExists()
        screenshot("redesign-diagnostics-minimum-light.png")
        compose.onNodeWithText("Advanced settings").performScrollTo().performClick()
        compose.onNodeWithTag("diag-timeout").performScrollTo().performTextReplacement("1000")
        compose.runOnIdle { assertEquals("1000", controller.diagnostics!!.state.value.inputs.timeoutMillis) }
        compose.onNodeWithTag("diag-dns-host").performScrollTo().assertIsDisplayed()
    }

    @Test fun overviewPrioritizesLanAndKeepsOtherInterfacesInspectableWithoutChangingRouting() {
        val other = network.copy(binding = network.binding.copy(key = "virtual", interfaceKey = "virtual0", up = false), displayName = "Virtual adapter")
        diagnosticInterfaces = listOf(DiagnosticInterface(other), DiagnosticInterface(network))
        content(1000, 760)
        compose.onNodeWithText("Diagnostics").performClick()
        compose.onNodeWithText("Ethernet (eth0)").assertIsDisplayed()
        compose.onNodeWithText("Virtual adapter (virtual0)").assertDoesNotExist()
        compose.onNodeWithText("Other interfaces (1)").performClick()
        compose.onNodeWithText("Virtual adapter (virtual0)").assertIsDisplayed()
        compose.runOnIdle { assertNull(controller.diagnostics!!.state.value.inputs.bindingKey) }
        screenshot("redesign-other-interfaces-dark.png")
    }

    @Test fun editorPickerKeepsManualInputAndOptionalWakeConfiguration() {
        content(1000, 760, SecureLanThemeMode.LIGHT)
        compose.onAllNodesWithText("Add device")[0].performClick()
        compose.onNodeWithTag("lan-name").assertIsFocused().performTextInput("Saved printer")
        compose.onNodeWithTag("lan-preferred-picker").performScrollTo().performClick()
        compose.onNodeWithText("Ethernet (eth0)").performClick()
        compose.onNodeWithTag("lan-preferred-interface").assertTextContains("eth0").performTextReplacement("custom0")
        compose.onNodeWithTag("lan-preferred-interface").assertTextContains("custom0")
        screenshot("reference-editor-light.png")
        compose.onNodeWithText("Save device").performScrollTo().performClick()
        compose.onNodeWithText("Enter a MAC address before configuring Wake-on-LAN.").assertExists()
        compose.onNodeWithTag("lan-preferred-picker").performScrollTo().performClick()
        compose.onNodeWithText("No saved interface").performClick()
        compose.onNodeWithText("Save device").performScrollTo().performClick()
        compose.runOnIdle { assertNull(store.devices.single().wake); assertEquals("Saved printer", store.devices.single().displayName) }
    }

    @Test fun minimumEditorScrollsToEveryFieldAndBackCancelsUnsavedDraft() {
        content(400, 320, SecureLanThemeMode.LIGHT)
        compose.onAllNodesWithText("Add device")[0].performScrollTo().performClick()
        compose.onNodeWithTag("lan-name").performScrollTo().performTextInput("Unsaved")
        for (tag in listOf("lan-address", "lan-mac", "lan-broadcast", "lan-port", "lan-preferred-interface")) {
            compose.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
        }
        screenshot("reference-editor-minimum-light.png")
        compose.onNodeWithText("Back to devices").performScrollTo().performClick()
        compose.runOnIdle { assertNull(controller.state.value.editor); assertTrue(store.devices.isEmpty()) }
        compose.onNodeWithText("No devices yet").assertExists()
    }

    @Test fun referenceEditorKeepsFooterVisibleWithoutScrollingAtReferenceSize() = runDesktopComposeUiTest(width = 1336, height = 720) {
        controller.open()
        controller.beginAdd()
        setContent { SecureLanTheme(themeMode = SecureLanThemeMode.DARK) {
            Surface(color = MaterialTheme.colors.background) {
                LanToolsContent(controller, controller::dismiss, Modifier.fillMaxSize().testTag("reference-content"))
            }
        } }
        onNodeWithTag("lan-name").assertIsFocused()
        onNodeWithTag("lan-mac").assertIsDisplayed()
        onNodeWithTag("lan-preferred-interface").assertIsDisplayed()
        onNodeWithText("Save device").assertIsDisplayed()
        onNodeWithText("Cancel editing").assertIsDisplayed()
        onNodeWithText("Back to devices").assertIsDisplayed()
        val bitmap = onNodeWithTag("reference-content").captureToImage().asSkiaBitmap()
        assertEquals(1336, bitmap.width)
        assertEquals(720, bitmap.height)
        val directory = Path.of("build/reports/lan-tools-ui")
        Files.createDirectories(directory)
        Image.makeFromBitmap(bitmap).use { image -> image.encodeToData()!!.use { Files.write(directory.resolve("fidelity-editor-reference-dark.png"), it.bytes) } }
    }

    @Test fun referenceCompactWindowKeepsDiagnosticsAndEditorActionsReachable() = runDesktopComposeUiTest(width = 800, height = 820) {
        val primary = network.copy(displayName = "Intel(R) Wi-Fi 6E AX211 160MHz")
        val other = network.copy(displayName = "Virtual adapter", binding = network.binding.copy(key = "virtual", interfaceKey = "virtual0", up = false))
        diagnosticInterfaces = listOf(
            DiagnosticInterface(primary, listOf(IpLiteral.parse("192.168.1.1")!!), listOf("192.168.1.1"), MetadataAvailability.AVAILABLE),
            DiagnosticInterface(other, metadata = MetadataAvailability.AVAILABLE),
        )
        controller.open()
        var theme by mutableStateOf(SecureLanThemeMode.DARK)
        setContent { SecureLanTheme(themeMode = theme) {
            Surface(color = MaterialTheme.colors.background) {
                LanToolsContent(controller, controller::dismiss, Modifier.fillMaxSize().testTag("reference-compact"))
            }
        } }
        fun capture(name: String) {
            val bitmap = onNodeWithTag("reference-compact").captureToImage().asSkiaBitmap()
            assertEquals(800, bitmap.width)
            assertEquals(820, bitmap.height)
            val directory = Path.of("build/reports/lan-tools-ui")
            Files.createDirectories(directory)
            Image.makeFromBitmap(bitmap).use { image -> image.encodeToData()!!.use { Files.write(directory.resolve(name), it.bytes) } }
        }
        onNodeWithTag("lan-search").assertIsDisplayed()
        capture("fidelity-empty-compact-dark.png")
        onNodeWithText("Diagnostics").performClick()
        onNodeWithTag("diag-tcp-host").assertIsDisplayed()
        onNodeWithTag("diag-dns-host").assertIsDisplayed()
        onNodeWithText("Advanced settings").assertIsDisplayed().performClick()
        onNodeWithTag("diag-timeout").assertIsDisplayed()
        val timeoutBounds = onNodeWithTag("diag-timeout-field").fetchSemanticsNode().boundsInRoot
        val surfaceBounds = onNodeWithTag("reference-compact").fetchSemanticsNode().boundsInRoot
        assertTrue("Timeout must be fully visible without scrolling", timeoutBounds.bottom <= surfaceBounds.bottom - 16f)
        capture("fidelity-diagnostics-compact-dark.png")
        theme = SecureLanThemeMode.INTERMEDIATE
        capture("fidelity-diagnostics-compact-intermediate.png")
        theme = SecureLanThemeMode.LIGHT
        capture("fidelity-diagnostics-compact-light.png")
        theme = SecureLanThemeMode.DARK
        onNodeWithText("Devices", substring = false).performClick()
        onAllNodesWithText("Add device")[0].performClick()
        onNodeWithTag("lan-name").assertIsFocused()
        onNodeWithText("Save device").performScrollTo().assertIsDisplayed()
        capture("fidelity-editor-compact-dark.png")
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
