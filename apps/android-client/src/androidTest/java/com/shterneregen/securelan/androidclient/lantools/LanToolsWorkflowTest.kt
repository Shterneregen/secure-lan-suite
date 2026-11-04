package com.shterneregen.securelan.androidclient.lantools

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shterneregen.securelan.androidclient.MainActivity
import com.shterneregen.securelan.androidclient.R
import com.shterneregen.securelan.androidclient.SecureLanDarkColors
import com.shterneregen.securelan.androidclient.lantools.network.*
import com.shterneregen.securelan.androidclient.ui.LocalAppResources
import com.shterneregen.securelan.androidclient.ui.lantools.LanToolsScreen
import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.common.net.interfaces.NetworkBinding
import com.shterneregen.securelan.lantools.diagnostics.*
import com.shterneregen.securelan.lantools.discovery.*
import com.shterneregen.securelan.lantools.model.*
import com.shterneregen.securelan.lantools.wol.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.net.NoRouteToHostException
import java.time.Instant
import java.util.UUID

/** Production composables and theme with deterministic, explicitly test-only network fixtures. */
@RunWith(AndroidJUnit4::class)
class LanToolsWorkflowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun label(id: Int) = compose.activity.getString(id)
    private fun ip(value: String) = requireNotNull(IpLiteral.parse(value))
    private var metadata = MetadataAvailability.AVAILABLE
    private var browse: suspend (suspend (LanDiscoverySnapshot) -> Unit) -> LanResult<LanDiscoverySnapshot> = { LanResult.Success(LanDiscoverySnapshot()) }
    private var browses = 0
    private val device = SavedLanDevice(UUID.randomUUID(), "qnap (test)", ip("192.168.31.138"))

    private fun fixture(saved: Boolean = false, body: (LanToolsViewModel) -> Unit) {
        val link = AndroidLanLink(NetworkBinding("101:192.168.31.123", "101", ip("192.168.31.123"), 24), "wlan0", ip("192.168.31.255"), listOf(ip("192.168.31.1")), listOf("192.168.31.1"))
        val platform = object : AndroidLanPlatform {
            override fun links() = listOf(link)
            override fun route(binding: NetworkBinding): LanSocketBinding? = null
            override fun start(changed: () -> Unit) = Unit
            override fun hasVpn() = false
            override fun close() = Unit
        }
        val store = object : LanDeviceStore {
            var rows = if (saved) listOf(device) else emptyList()
            override fun load() = rows
            override fun save(devices: List<SavedLanDevice>) { rows = devices }
        }
        val diagnostics = DefaultNetworkDiagnosticsService(HostResolver { LanResult.Failure(LanError.RESOLUTION_FAILED) },
            LanNetworkInfoProvider { LanResult.Success(LanNetworkSnapshot(listOf(LanInterfaceSnapshot(link.binding, link.name, true, link.broadcast)))) },
            TcpConnector { _, _, _ -> throw NoRouteToHostException("Deterministic fixture; no physical NAS was contacted") })
        val model = LanToolsViewModel(LanToolsServices(platform, DiagnosticsNetworkInfoProvider {
            LanResult.Success(DiagnosticsNetworkSnapshot(listOf(DiagnosticInterface(LanInterfaceSnapshot(link.binding, link.name, true, link.broadcast), link.gateways, link.dns, metadata)), "Test LinkProperties fixture"))
        }, object : WakeOnLanService { override suspend fun send(request: WakeRequest): LanResult<WakeReceipt> = LanResult.Failure(LanError.UNSUPPORTED) },
            diagnostics, AndroidDeviceBrowser { _, update -> browses++; browse(update) }), store)
        val owner = ViewModelStore().apply { put("test-tools", model) }
        try {
            compose.runOnUiThread {
                compose.activity.setContent {
                    CompositionLocalProvider(LocalAppResources provides compose.activity.resources) {
                        MaterialTheme(colorScheme = SecureLanDarkColors) {
                            Surface(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding(), color = MaterialTheme.colorScheme.background) {
                                LanToolsScreen(model, onBack = {}, onRequestNearby = model::discover)
                            }
                        }
                    }
                }
                model.setActive(true)
            }
            compose.waitUntil(10_000) { model.state.value.loaded && model.state.value.outcome(LanOperation.NETWORK).phase != LanPhase.RUNNING }
            body(model)
        } finally { compose.runOnUiThread { owner.clear() } }
    }

    @Test fun separateDetailsEditorAndHistoricalFailureKeepOriginalTargetAndDraft() = fixture(saved = true) { model ->
        compose.onNodeWithTag("lan-device-list").performScrollToNode(hasTestTag("lan-saved-${device.id}"))
        screenshot("saved-list")
        compose.onNodeWithTag("lan-saved-${device.id}").performClick()
        compose.onNodeWithTag("lan-device-list").assertDoesNotExist()
        compose.onNodeWithTag("lan-device-diagnostics").performScrollTo().performClick()
        compose.onNodeWithTag("lan-tcp-port").assertTextContains("")
        assertEquals("", model.state.value.input.port)
        compose.onNodeWithTag("lan-tcp-port").performTextInput("443")
        Espresso.closeSoftKeyboard()
        compose.onNodeWithTag("lan-check-tcp").performScrollTo().performClick()
        compose.waitUntil(10_000) { model.state.value.outcome(LanOperation.TCP).phase == LanPhase.FAILURE }
        screenshot("tcp-failure")
        compose.onNodeWithTag("lan-tcp-port").performTextReplacement("8443")
        Espresso.closeSoftKeyboard()
        assertEquals(listOf("192.168.31.138", "443"), model.state.value.outcome(LanOperation.TCP).subject?.args)
        compose.onNodeWithTag("lan-back").performClick()
        compose.onNodeWithTag("lan-device-details").assertExists()
        compose.onNodeWithTag("lan-device-list").assertDoesNotExist()
        screenshot("details-failure")
        compose.onNodeWithTag("lan-edit").performScrollTo().performClick()
        Espresso.closeSoftKeyboard()
        compose.onNodeWithTag("lan-broadcast").assertDoesNotExist()
        compose.onNodeWithTag("lan-save").assertIsDisplayed()
        screenshot("editor-collapsed")
        compose.onNodeWithTag("lan-wol-advanced").performScrollTo().performClick()
        compose.onNodeWithTag("lan-broadcast").performScrollTo().assertIsDisplayed()
        screenshot("editor-expanded")
        compose.onNodeWithTag("lan-name").performScrollTo().performTextReplacement("Unfinished NAS")
        Espresso.closeSoftKeyboard()
        compose.onNodeWithTag("lan-back").performClick()
        compose.onNodeWithTag("lan-edit").performScrollTo().performClick()
        compose.onNodeWithTag("lan-name").assertTextContains("Unfinished NAS")
        Espresso.closeSoftKeyboard()
        compose.onNodeWithText(label(R.string.lan_cancel)).performClick()
        assertNull(model.state.value.form)
        compose.onNodeWithTag("lan-back").performClick()
        compose.onNodeWithTag("lan-device-list").performScrollToNode(hasTestTag("lan-saved-${device.id}"))
        compose.onNodeWithTag("lan-saved-${device.id}").assertIsDisplayed()
        compose.onNodeWithTag("lan-search").performScrollTo().performTextInput("does-not-match")
        Espresso.closeSoftKeyboard()
        compose.onNodeWithTag("lan-device-list").performScrollToNode(hasText(label(R.string.lan_empty_filtered)))
        compose.onAllNodesWithText(label(R.string.lan_empty_filtered)).onFirst().assertExists()
    }

    @Test fun overviewDnsValidationAndNumericResultExposePartialMetadataHonestly() = fixture { model ->
        screenshot("empty-list")
        assertEquals(0, browses)
        compose.onNodeWithTag("lan-tab-DIAGNOSTICS").performClick()
        screenshot("diagnostics-overview")
        compose.onNodeWithTag("lan-open-dns").performScrollTo().performClick()
        compose.onNodeWithTag("lan-check-dns").performScrollTo().performClick()
        compose.onNodeWithText(label(R.string.lan_enter_host)).assertExists()
        compose.onNodeWithText(label(R.string.lan_invalid_device)).assertDoesNotExist()
        compose.onNodeWithTag("lan-dns-host").performTextInput("192.168.31.138")
        Espresso.closeSoftKeyboard()
        compose.onNodeWithTag("lan-check-dns").performScrollTo().performClick()
        compose.waitUntil(10_000) { model.state.value.outcome(LanOperation.DNS).phase == LanPhase.SUCCESS }
        screenshot("dns-numeric-result")
        compose.onNodeWithTag("lan-back").performClick()
        compose.runOnIdle { metadata = MetadataAvailability.TIMEOUT; model.refreshNetwork() }
        compose.waitUntil(10_000) { model.state.value.outcome(LanOperation.NETWORK).phase != LanPhase.RUNNING }
        compose.onNodeWithText(label(R.string.lan_network_partial)).assertExists()
        compose.onNodeWithText(label(R.string.lan_metadata_timeout)).assertExists()
        screenshot("network-partial")
        compose.onNodeWithTag("lan-open-gateway").performScrollTo().performClick()
        compose.onNodeWithText(label(R.string.lan_gateway_select_hint)).assertExists()
        compose.onNodeWithTag("lan-gateway-port").assertDoesNotExist()
    }

    @Test fun manualDiscoveryProgressCancelPermissionFailureAndObservedRecordsRemainDistinct() = fixture(saved = true) { model ->
        compose.onNodeWithTag("lan-refresh").performClick()
        assertEquals(R.string.lan_nearby_required, model.state.value.outcome(LanOperation.DISCOVERY).message?.resource)
        browse = { update ->
            update(LanDiscoverySnapshot(listOf(DiscoveredLanDevice("fixture-service", "101:192.168.31.123", "Printer (test)", null,
                listOf(ip("192.168.31.140")), emptyList(), setOf("mDNS/DNS-SD"), Instant.now())), partial = true))
            awaitCancellation()
        }
        compose.runOnIdle { model.permission(true) }
        assertEquals(0, browses)
        compose.onNodeWithTag("lan-refresh").performClick()
        compose.waitUntil(10_000) { model.state.value.found.devices.size == 1 }
        compose.onNodeWithTag("lan-refresh").assertIsNotEnabled()
        compose.onAllNodesWithText(label(R.string.lan_running)).assertCountEquals(1)
        compose.onNodeWithText(label(R.string.lan_partial)).assertExists()
        compose.onNodeWithText(label(R.string.lan_cancel)).performScrollTo().performClick()
        assertEquals(LanPhase.CANCELLED, model.state.value.outcome(LanOperation.DISCOVERY).phase)
        compose.onNodeWithTag("lan-device-list").performScrollToNode(hasTestTag("lan-found-fixture-service"))
        compose.onNodeWithTag("lan-found-fixture-service").performClick()
        compose.onNodeWithText(label(R.string.lan_save_new)).performScrollTo().assertExists()
        compose.onNodeWithText(label(R.string.lan_delete)).assertDoesNotExist()
        assertEquals(1, model.state.value.devices.size)
        screenshot("observed-details")
    }

    private fun screenshot(name: String) {
        if (android.os.Build.VERSION.SDK_INT < 29) return
        compose.waitForIdle()
        val resolver = compose.activity.contentResolver
        val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "lan-tools-uiux-$name.png")
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Download/SecureLanLanTools")
        }) ?: error("Screenshot output unavailable")
        val bitmap = compose.onNodeWithTag("lan-tools").captureToImage().asAndroidBitmap()
        resolver.openOutputStream(uri)!!.use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
