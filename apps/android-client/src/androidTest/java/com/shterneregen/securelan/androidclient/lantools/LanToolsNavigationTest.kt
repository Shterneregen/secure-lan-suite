package com.shterneregen.securelan.androidclient.lantools

import android.content.pm.PackageManager
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shterneregen.securelan.androidclient.MainActivity
import com.shterneregen.securelan.androidclient.MainViewModel
import com.shterneregen.securelan.androidclient.R
import com.shterneregen.securelan.androidclient.model.AppLanguage
import com.shterneregen.securelan.androidclient.model.ThemeMode
import com.shterneregen.securelan.androidclient.network.AndroidChatHostService
import com.shterneregen.securelan.chat.event.ChatUserJoinedEvent
import com.shterneregen.securelan.chat.service.ChatClientConnectRequest
import com.shterneregen.securelan.chat.service.ChatEventPublisher
import com.shterneregen.securelan.chat.service.ChatSendOutcome
import com.shterneregen.securelan.chat.service.impl.DefaultChatClientService
import com.shterneregen.securelan.filetransfer.protocol.FileTransferPeerAuthentication
import com.shterneregen.securelan.filetransfer.service.FileTransferClientRequest
import com.shterneregen.securelan.filetransfer.service.FileTransferTimeouts
import com.shterneregen.securelan.filetransfer.service.impl.DefaultFileTransferClientService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

/** Uses a dedicated test device: Orchestrator clears the app's preferences after every test. */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class LanToolsNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun label(id: Int) = compose.activity.getString(id)
    private fun open() {
        // Short lazy viewports may not have composed the utilities card yet.
        if (compose.onAllNodesWithTag("lan-open").fetchSemanticsNodes().isEmpty()) {
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasTestTag("lan-open"))
        }
        compose.onNodeWithTag("lan-open").performScrollTo().performClick()
        compose.onNodeWithTag("lan-tools").assertIsDisplayed()
    }
    private fun model() = ViewModelProvider(compose.activity)[LanToolsViewModel::class.java]

    @Test fun secondaryScreenBackAndRecreationPreserveChatDraftAndToolForm() {
        lateinit var room: MainViewModel
        compose.runOnIdle { room = ViewModelProvider(compose.activity)[MainViewModel::class.java]; room.updateInputMessage("Retained draft") }
        open()
        compose.waitUntil(10_000) { model().state.value.loaded }
        compose.onAllNodesWithText(label(R.string.lan_add)).filter(hasClickAction()).onFirst().performClick()
        compose.onNodeWithTag("lan-name").performTextInput("Retained device")
        Espresso.closeSoftKeyboard()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("lan-tools").assertIsDisplayed()
        compose.onNodeWithTag("lan-name").assertTextContains("Retained device")
        val tools = model()
        compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
        assertFalse(tools.state.value.active)
        compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        compose.onNodeWithTag("lan-name").assertTextContains("Retained device")
        compose.runOnIdle {
            assertSame(room, ViewModelProvider(compose.activity)[MainViewModel::class.java])
            assertEquals("Retained draft", room.uiState.value.inputMessage)
        }
        Espresso.closeSoftKeyboard() // Recreation restores field focus and may reopen the IME.
        Espresso.pressBack() // Suspend the editor, preserving the draft.
        compose.onNodeWithTag("lan-device-list").assertExists()
        Espresso.pressBack() // Leave LAN Tools.
        open()
        compose.onNodeWithTag("lan-resume").performScrollTo().performClick()
        compose.onNodeWithTag("lan-name").assertTextContains("Retained device")
        compose.onNodeWithTag("lan-back").performClick()
        compose.onNodeWithTag("lan-back").performClick()
        compose.runOnIdle { assertEquals("Retained draft", room.uiState.value.inputMessage) }
    }

    @Test fun savedDeviceCrudHasRealPersistenceAndDeleteConfirmation() {
        open(); compose.waitUntil(10_000) { model().state.value.loaded }
        compose.onAllNodesWithText(label(R.string.lan_add)).filter(hasClickAction()).onFirst().performClick()
        compose.onNodeWithTag("lan-name").performTextInput("Test workstation")
        Espresso.closeSoftKeyboard()
        compose.onNodeWithTag("lan-address").performTextInput("192.168.1.20")
        Espresso.closeSoftKeyboard()
        compose.onNodeWithTag("lan-save").performClick()
        compose.waitUntil(10_000) { model().state.value.devices.size == 1 }
        compose.runOnIdle { assertEquals("Test workstation", PreferencesSavedLanDeviceStore(compose.activity).load().single().displayName) }
        compose.onNodeWithTag("lan-device-details").assertIsDisplayed()
        compose.onNodeWithTag("lan-device-list").assertDoesNotExist()
        compose.onNodeWithText(label(R.string.lan_edit)).performScrollTo().assertIsDisplayed()
        screenshot("devices")
        compose.onNodeWithText(label(R.string.lan_delete)).performScrollTo().performClick()
        compose.onNodeWithText(label(R.string.lan_cancel)).performClick()
        compose.runOnIdle { assertEquals(1, model().state.value.devices.size) }
        compose.onNodeWithText(label(R.string.lan_delete)).performScrollTo().performClick()
        compose.onAllNodesWithText(label(R.string.lan_delete)).filter(hasClickAction()).onLast().performClick()
        compose.waitUntil(10_000) { model().state.value.devices.isEmpty() }
        compose.runOnIdle { assertTrue(PreferencesSavedLanDeviceStore(compose.activity).load().isEmpty()) }
    }

    @Test fun openingAndGrantingNoPermissionsNeverStartsBrowse() {
        val permissionBefore = compose.activity.checkSelfPermission(android.Manifest.permission.NEARBY_WIFI_DEVICES)
        open()
        compose.runOnIdle {
            assertEquals(LanPhase.IDLE, model().state.value.outcome(LanOperation.DISCOVERY).phase)
            assertEquals(permissionBefore, compose.activity.checkSelfPermission(android.Manifest.permission.NEARBY_WIFI_DEVICES))
            // Nearby Wi-Fi is a runtime permission only on API 33+. Older devices can browse
            // explicitly even though checkSelfPermission denies this unknown permission name.
            if (android.os.Build.VERSION.SDK_INT >= 33 && permissionBefore != PackageManager.PERMISSION_GRANTED) {
                model().discover()
                assertEquals(LanPhase.FAILURE, model().state.value.outcome(LanOperation.DISCOVERY).phase)
            } else {
                assertTrue(model().state.value.nearbyGranted)
            }
        }
        compose.onNodeWithText(label(R.string.lan_diagnostics)).performClick()
        compose.waitUntil(10_000) { model().state.value.outcome(LanOperation.NETWORK).phase != LanPhase.RUNNING }
        compose.onNodeWithText(label(R.string.lan_advanced)).performScrollTo().performClick()
        compose.onNodeWithText(label(R.string.lan_icmp)).performScrollTo()
        screenshot("diagnostics")
        compose.onNodeWithText(label(R.string.lan_icmp)).assertIsDisplayed()
        compose.onNodeWithTag("lan-back").performClick()
    }

    @Test fun russianDarkSurfaceUsesAppLanguageAndKeepsLongFormScrollable() {
        compose.runOnIdle {
            val room = ViewModelProvider(compose.activity)[MainViewModel::class.java]
            room.updateAppLanguage(AppLanguage.RUSSIAN); room.updateThemeMode(ThemeMode.DARK)
        }
        open(); compose.waitUntil(10_000) { model().state.value.loaded }
        compose.onNodeWithTag("lan-add").performClick()
        compose.onNodeWithTag("lan-name").performTextInput("Домашний компьютер с длинным названием")
        Espresso.closeSoftKeyboard()
        compose.onNodeWithTag("lan-save").assertIsDisplayed()
        screenshot("russian-dark-form")
        compose.onNodeWithTag("lan-save").performClick()
        compose.waitUntil(10_000) { model().state.value.devices.size == 1 }
        compose.onNodeWithTag("lan-back").performClick()
        compose.onNodeWithTag("lan-tab-DIAGNOSTICS").performClick()
        compose.onNodeWithTag("lan-open-tcp").performScrollTo().performClick()
        compose.onNodeWithTag("lan-tcp-port").performScrollTo().assertIsDisplayed()
    }

    /** Real local V2 chat/files and HTTP continue while the secondary surface opens, checks and closes. */
    @Test fun toolChecksPreserveActiveHostingChatAndQuickShare() {
        if (android.os.Build.VERSION.SDK_INT <= 28) {
            // The real Downloads receiver needs the same legacy grants as an explicit file-receive flow.
            val automation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation
            listOf(android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE).forEach { permission ->
                android.os.ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand("pm grant ${compose.activity.packageName} $permission")).use {
                    assertEquals("", it.bufferedReader().readText().trim())
                }
            }
        }
        val (chatPort, filePort) = ServerSocket(0).use { chat -> ServerSocket(0).use { chat.localPort to it.localPort } }
        lateinit var room: MainViewModel
        val keys = ConcurrentHashMap<String, String>()
        val client = DefaultChatClientService(ChatEventPublisher { event ->
            if (event is ChatUserJoinedEvent) event.nickname?.let { keys[it] = event.capabilities.identityKey() }
        })
        val source = java.io.File.createTempFile("lan-tools-payload-", ".bin", compose.activity.cacheDir)
        val payload = ByteArray(256 * 1024) { (it % 251).toByte() }
        source.writeBytes(payload)
        try {
            compose.runOnIdle {
                room = ViewModelProvider(compose.activity)[MainViewModel::class.java]
                room.updateThemeMode(ThemeMode.DARK)
                room.updateAppLanguage(AppLanguage.ENGLISH)
                room.updateSessionPassword("lan-tools-local-test")
                room.setHostDiscoverable(false)
                room.startHosting(chatPort.toString(), filePort.toString())
            }
            compose.waitUntil(15_000) { room.uiState.value.hosting && room.uiState.value.connected && room.uiState.value.fileReceiverRunning }
            assertTrue(runBlocking(Dispatchers.IO) { client.connect(ChatClientConnectRequest("127.0.0.1", chatPort, "LocalToolProbe", "lan-tools-local-test")) })
            // Host summaries intentionally omit loopback participants; the authenticated chat presence does not.
            compose.waitUntil(10_000) { room.uiState.value.peers.any { it.nickname == "LocalToolProbe" } && !keys[room.uiState.value.nickname].isNullOrBlank() }
            compose.runOnIdle { room.updateInputMessage("Keep this draft"); room.createQuickShareText("local share content", 1, null) }
            compose.waitUntil(10_000) { room.quickShares.value.isNotEmpty() }
            val selected = room.uiState.value.selectedPeer
            val transfers = room.transferQueue.value
            val roomId = AndroidChatHostService.state.value.roomInstanceId
            val share = room.quickShares.value.single()
            compose.onNode(hasText(label(R.string.nav_devices)) and hasClickAction()).performClick()
            screenshot("uiux-hosting-dark")
            open()
            compose.runOnIdle {
                model().editInput { it.copy(host = "127.0.0.1", port = chatPort.toString()) }
                model().tcp()
            }
            compose.waitUntil(10_000) { model().state.value.outcome(LanOperation.TCP).phase == LanPhase.SUCCESS }
            assertEquals(ChatSendOutcome.WRITTEN, runBlocking(Dispatchers.IO) { client.sendMessage("message while tools open") })
            compose.waitUntil(10_000) { room.uiState.value.messages.any { it.text == "message while tools open" } }
            runBlocking(Dispatchers.IO) {
                val authentication = FileTransferPeerAuthentication("LocalToolProbe", client.fileIdentity()) { keys[it] }
                DefaultFileTransferClientService({}, timeouts = FileTransferTimeouts(3000, 5000, 5000, 5000)).sendFile(
                    FileTransferClientRequest("127.0.0.1", filePort, "LocalToolProbe", room.uiState.value.nickname,
                        "lan-tools-local-test", source.toPath(), authentication))
            }
            compose.waitUntil(10_000) { room.uiState.value.recentTransfers.any { it.fileName == source.name && it.contentUri != null } }
            val received = room.uiState.value.recentTransfers.first { it.fileName == source.name }
            compose.activity.contentResolver.openInputStream(requireNotNull(received.contentUri))!!.use { assertArrayEquals(payload, it.readBytes()) }
            val response = runBlocking { withContext(Dispatchers.IO) {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress("127.0.0.1", URI(share.url).port), 3000); socket.soTimeout = 3000
                    socket.getOutputStream().write("GET /s/${share.id}/text HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n".toByteArray())
                    socket.getInputStream().bufferedReader().readText()
                }
            } }
            assertTrue(response.startsWith("HTTP/1.1 200 OK")); assertTrue(response.endsWith("local share content"))
            compose.onNodeWithTag("lan-back").performClick()
            assertEquals(ChatSendOutcome.WRITTEN, runBlocking(Dispatchers.IO) { client.sendMessage("message after tools close") })
            compose.waitUntil(10_000) { room.uiState.value.messages.any { it.text == "message after tools close" } }
            compose.runOnIdle {
                assertEquals("Keep this draft", room.uiState.value.inputMessage)
                assertEquals(selected, room.uiState.value.selectedPeer); assertEquals(transfers, room.transferQueue.value)
                assertTrue(room.uiState.value.fileReceiverRunning); assertTrue(client.isConnected())
                assertEquals(roomId, AndroidChatHostService.state.value.roomInstanceId)
            }
        } finally {
            runBlocking(Dispatchers.IO) { client.disconnect() }
            source.delete()
            compose.runOnIdle {
                room.uiState.value.recentTransfers.filter { it.fileName == source.name }.forEach { record ->
                    record.contentUri?.let { compose.activity.contentResolver.delete(it, null, null) }
                }
                room.quickShares.value.forEach { room.stopQuickShare(it.id) }
                room.stopHosting()
            }
            compose.waitUntil(10_000) { !AndroidChatHostService.state.value.running && !AndroidChatHostService.state.value.starting }
        }
    }

    @Test fun referenceLayoutKeepsEditorActionsVisibleAndEmptyDiscoveryTruthful() {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation
                .grantRuntimePermission(compose.activity.packageName, android.Manifest.permission.NEARBY_WIFI_DEVICES)
        }
        compose.runOnIdle {
            val room = ViewModelProvider(compose.activity)[MainViewModel::class.java]
            room.updateAppLanguage(AppLanguage.ENGLISH)
            room.updateThemeMode(ThemeMode.DARK)
            room.updateNearbyPermission(com.shterneregen.securelan.androidclient.model.NearbyPermissionState.GRANTED)
        }
        compose.onNodeWithText(label(R.string.host_room)).assertIsDisplayed()
        screenshot("uiux-devices-dark")
        open(); compose.waitUntil(10_000) { model().state.value.loaded }
        compose.onNodeWithTag("lan-device-list").performScrollToNode(hasText(label(R.string.lan_empty_saved_title)))
        compose.onNodeWithText(label(R.string.lan_empty_saved_title)).assertIsDisplayed()
        compose.onNodeWithTag("lan-device-list").performScrollToNode(hasText(label(R.string.lan_not_searched)))
        compose.onNodeWithText(label(R.string.lan_not_searched)).assertIsDisplayed()
        screenshot("uiux-empty-bottom-dark")
        compose.onNodeWithTag("lan-device-list").performScrollToIndex(0)
        screenshot("uiux-empty-dark")
        compose.onAllNodesWithText(label(R.string.lan_add)).filter(hasClickAction()).onFirst().performClick()
        compose.onNodeWithTag("lan-name").assertIsFocused().performTextInput("Workstation")
        Espresso.closeSoftKeyboard()
        compose.onNodeWithTag("lan-save").assertIsDisplayed()
        screenshot("uiux-editor-dark")
        compose.onNodeWithTag("lan-mac").performScrollTo().performTextInput("invalid")
        Espresso.closeSoftKeyboard()
        compose.onNodeWithTag("lan-save").assertIsDisplayed().performClick()
        compose.onAllNodesWithText(label(R.string.lan_invalid_mac)).onLast().assertIsDisplayed()
        compose.onNodeWithText(label(R.string.lan_cancel)).performClick()
        compose.onNodeWithTag("lan-tab-DIAGNOSTICS").performClick()
        compose.onNodeWithText(label(R.string.lan_overview)).assertIsDisplayed()
        screenshot("uiux-diagnostics-dark")
        compose.onNodeWithTag("lan-open-tcp").performScrollTo().performClick()
        compose.onNodeWithTag("lan-tcp-host").performScrollTo().performClick()
        compose.onNodeWithTag("lan-tcp-host").performKeyInput { pressKey(androidx.compose.ui.input.key.Key.Tab) }
        compose.onNodeWithTag("lan-tcp-port").assertIsFocused()
        Espresso.closeSoftKeyboard()
        compose.onNodeWithTag("lan-back").performClick()
        compose.onNodeWithText(label(R.string.lan_advanced)).performScrollTo().performClick()
        compose.onNodeWithTag("lan-timeout").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("lan-gateway-port").assertDoesNotExist()
        compose.onNodeWithTag("lan-back").performClick()
        compose.runOnIdle { ViewModelProvider(compose.activity)[MainViewModel::class.java].updateThemeMode(ThemeMode.LIGHT) }
        compose.onNodeWithText(label(R.string.lan_overview)).performScrollTo()
        screenshot("uiux-diagnostics-light")
    }

    private fun screenshot(name: String) {
        if (android.os.Build.VERSION.SDK_INT < 29) return
        compose.waitForIdle()
        val resolver = compose.activity.contentResolver
        val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "lan-tools-$name.png")
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Download/SecureLanLanTools")
        }) ?: error("Screenshot output unavailable")
        val toolsVisible = compose.onAllNodesWithTag("lan-tools").fetchSemanticsNodes().isNotEmpty()
        val bitmap = (if (toolsVisible) compose.onNodeWithTag("lan-tools") else compose.onRoot()).captureToImage().asAndroidBitmap()
        resolver.openOutputStream(uri)!!.use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
