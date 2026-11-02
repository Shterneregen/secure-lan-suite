package com.shterneregen.securelan.androidclient.lantools

import android.content.pm.PackageManager
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
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Uses a dedicated test device: Orchestrator clears the app's preferences after every test. */
@RunWith(AndroidJUnit4::class)
class LanToolsNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun label(id: Int) = compose.activity.getString(id)
    private fun open() { compose.onNodeWithTag("lan-open").performScrollTo().performClick(); compose.onNodeWithTag("lan-tools").assertIsDisplayed() }
    private fun model() = ViewModelProvider(compose.activity)[LanToolsViewModel::class.java]

    @Test fun secondaryScreenBackAndRecreationPreserveChatDraftAndToolForm() {
        lateinit var room: MainViewModel
        compose.runOnIdle { room = ViewModelProvider(compose.activity)[MainViewModel::class.java]; room.updateInputMessage("Retained draft") }
        open()
        compose.waitUntil(10_000) { model().state.value.loaded }
        compose.onNodeWithText(label(R.string.lan_add)).performClick()
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
        Espresso.pressBack()
        compose.onNodeWithTag("lan-open").assertExists()
        open()
        compose.onNodeWithTag("lan-name").assertTextContains("Retained device")
        compose.onNodeWithTag("lan-back").performClick()
        compose.runOnIdle { assertEquals("Retained draft", room.uiState.value.inputMessage) }
    }

    @Test fun savedDeviceCrudHasRealPersistenceAndDeleteConfirmation() {
        open(); compose.waitUntil(10_000) { model().state.value.loaded }
        compose.onNodeWithText(label(R.string.lan_add)).performClick()
        compose.onNodeWithTag("lan-name").performTextInput("Test workstation")
        Espresso.closeSoftKeyboard()
        compose.onNodeWithTag("lan-address").performTextInput("192.168.1.20")
        Espresso.closeSoftKeyboard()
        compose.onNodeWithTag("lan-save").performScrollTo().performClick()
        compose.waitUntil(10_000) { model().state.value.devices.size == 1 }
        compose.runOnIdle { assertEquals("Test workstation", PreferencesSavedLanDeviceStore(compose.activity).load().single().displayName) }
        compose.onNodeWithText(label(R.string.lan_details)).performScrollTo().performClick()
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
            if (permissionBefore != PackageManager.PERMISSION_GRANTED) {
                model().discover()
                assertEquals(LanPhase.FAILURE, model().state.value.outcome(LanOperation.DISCOVERY).phase)
            }
        }
        compose.onNodeWithText(label(R.string.lan_diagnostics)).performClick()
        compose.waitUntil(10_000) { model().state.value.outcome(LanOperation.NETWORK).phase != LanPhase.RUNNING }
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
        compose.onNodeWithText("Добавить устройство").performClick()
        compose.onNodeWithTag("lan-name").performTextInput("Домашний компьютер с длинным названием")
        Espresso.closeSoftKeyboard()
        compose.onNodeWithTag("lan-save").performScrollTo().assertIsDisplayed()
        screenshot("russian-dark-form")
        compose.onNodeWithTag("lan-save").performClick()
        compose.waitUntil(10_000) { model().state.value.devices.size == 1 }
        compose.onNodeWithTag("lan-tab-DIAGNOSTICS").performClick()
        compose.onNodeWithTag("lan-tcp-port").performScrollTo().assertIsDisplayed()
    }

    private fun screenshot(name: String) {
        if (android.os.Build.VERSION.SDK_INT < 29) return
        val resolver = compose.activity.contentResolver
        val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "lan-tools-$name.png")
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Download/SecureLanLanTools")
        }) ?: error("Screenshot output unavailable")
        val bitmap = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        resolver.openOutputStream(uri)!!.use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
