package com.shterneregen.securelan.desktop.lantools

import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.common.net.interfaces.NetworkBinding
import com.shterneregen.securelan.desktop.compose.state.lantools.*
import com.shterneregen.securelan.lantools.model.*
import com.shterneregen.securelan.lantools.wol.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.Instant
import java.util.UUID

internal class MemoryLanStore(var devices: List<SavedLanDevice> = emptyList()) : SavedLanDeviceStore {
    var failedWrite = false
    var failedRead = false
    var warning: String? = null
    override fun load(): SavedDevicesLoad { if (failedRead) throw IOException(); return SavedDevicesLoad(devices, warning) }
    override fun save(devices: List<SavedLanDevice>) { if (failedWrite) throw IOException(); this.devices = devices }
}

@OptIn(ExperimentalCoroutinesApi::class)
class DesktopLanToolsControllerTest {
    private val device = SavedLanDevice(UUID.randomUUID(), "Workstation", wake = WakeConfiguration(MacAddress.parse("02:ab:cd:ef:12:34")!!))
    private val network = LanInterfaceSnapshot(NetworkBinding("eth0:192.168.1.42", "eth0", IpLiteral.parse("192.168.1.42")!!, 24),
        "Ethernet", true, IpLiteral.parse("192.168.1.255"))
    private val networks = LanNetworkInfoProvider { LanResult.Success(LanNetworkSnapshot(listOf(network))) }
    private val receipt get() = WakeReceipt(WakeRoute(network, network.broadcast!!, 9), 102, Instant.EPOCH)
    private fun successWake() = object : WakeOnLanService { override suspend fun send(request: WakeRequest) = LanResult.Success(receipt) }

    @Test fun crudIsPublishedOnlyAfterPersistenceAndSurvivesControllerRestart() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val store = MemoryLanStore()
        val controller = DesktopLanToolsController(store, networks, successWake(), dispatcher, dispatcher)
        controller.open(); runCurrent()
        controller.beginAdd(); controller.editDraft(LanDeviceDraft(name = "Desktop", mac = "02:ab:cd:ef:12:34"))
        controller.saveEditor()
        assertTrue(controller.state.value.devices.isEmpty())
        runCurrent()
        val saved = controller.state.value.devices.single()
        controller.beginEdit(saved.id); controller.editDraft(controller.state.value.editor!!.copy(name = "Renamed"))
        controller.saveEditor(); runCurrent()
        assertEquals("Renamed", store.devices.single().displayName)
        controller.close()
        val restored = DesktopLanToolsController(store, networks, successWake(), dispatcher, dispatcher)
        restored.open(); runCurrent()
        assertEquals(store.devices, restored.state.value.devices)
        restored.requestDelete(saved.id); restored.cancelDelete()
        assertEquals(1, store.devices.size)
        restored.requestDelete(saved.id); restored.confirmDelete(); runCurrent()
        assertTrue(store.devices.isEmpty())
        restored.close()
    }

    @Test fun failedSaveRetainsListAndDraftForRetryAndInputValidationPrecedesWrite() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val store = MemoryLanStore(listOf(device))
        val controller = DesktopLanToolsController(store, networks, successWake(), dispatcher, dispatcher)
        controller.open(); runCurrent()
        controller.beginEdit(device.id)
        controller.editDraft(controller.state.value.editor!!.copy(mac = "broken")); controller.saveEditor()
        assertNotNull(controller.state.value.editorError)
        controller.editDraft(controller.state.value.editor!!.copy(name = "Changed", mac = "02:ab:cd:ef:12:34"))
        store.failedWrite = true
        controller.saveEditor(); runCurrent()
        assertEquals(listOf(device), controller.state.value.devices)
        assertEquals("Changed", controller.state.value.editor!!.name)
        assertNotNull(controller.state.value.storageError)
        store.failedWrite = false
        controller.saveEditor(); runCurrent()
        assertEquals("Changed", controller.state.value.devices.single().displayName)
        controller.close()
    }

    @Test fun readFailureAndCorruptionNeverEnableWritesAndReloadRecovers() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val store = MemoryLanStore().also { it.failedRead = true }
        val controller = DesktopLanToolsController(store, networks, successWake(), dispatcher, dispatcher)
        controller.open(); runCurrent()
        assertFalse(controller.state.value.canModify)
        store.failedRead = false; store.warning = "Corrupt record"
        controller.reload(); runCurrent()
        assertFalse(controller.state.value.canModify)
        store.warning = null
        controller.reload(); runCurrent()
        assertTrue(controller.state.value.canModify)
        controller.close()
    }

    @Test fun closingCancelsSendPreservesDraftSelectionAndRejectsLateResults() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        var cleaned = false
        val slow = object : WakeOnLanService {
            override suspend fun send(request: WakeRequest): LanResult<WakeReceipt> {
                try { withContext(NonCancellable) { delay(100) } } finally { cleaned = true }
                return LanResult.Success(receipt)
            }
        }
        val controller = DesktopLanToolsController(MemoryLanStore(listOf(device)), networks, slow, dispatcher, dispatcher)
        controller.open(); runCurrent(); controller.selectDevice(device.id)
        controller.beginAdd(); controller.editDraft(LanDeviceDraft(name = "Unfinished"))
        controller.wakeDevice(device.id); runCurrent()
        controller.dismiss(); advanceUntilIdle()
        assertTrue(cleaned)
        assertEquals(device.id, controller.state.value.selectedId)
        assertEquals("Unfinished", controller.state.value.editor!!.name)
        assertFalse(controller.state.value.lastResults.getValue(device.id).contains("Packet sent"))
        controller.open(); runCurrent()
        assertEquals("Unfinished", controller.state.value.editor!!.name)
        controller.close()
    }

    @Test fun sendsWithoutRoomUsesChosenInterfaceAndReportsPacketOnly() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        var captured: WakeRequest? = null
        val wake = object : WakeOnLanService {
            override suspend fun send(request: WakeRequest): LanResult<WakeReceipt> { captured = request; return LanResult.Success(receipt) }
        }
        val controller = DesktopLanToolsController(MemoryLanStore(listOf(device)), networks, wake, dispatcher, dispatcher)
        controller.open(); runCurrent(); controller.selectDevice(device.id); controller.selectBinding(network.binding.key)
        controller.wakeDevice(device.id); runCurrent()
        assertEquals(network.binding.key, captured!!.bindingKey)
        assertTrue(controller.state.value.lastResults.getValue(device.id).contains("wake-up is not confirmed"))
        controller.close()
    }

    @Test fun networkRefreshHasDeadlineAndDoesNotPublishAfterDismissal() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val unavailable = LanNetworkInfoProvider { awaitCancellation() }
        val controller = DesktopLanToolsController(MemoryLanStore(), unavailable, successWake(), dispatcher, dispatcher)
        controller.open(); advanceUntilIdle()
        assertNotNull(controller.state.value.networkError)
        assertFalse(controller.state.value.networkLoading)
        controller.refreshInterfaces(); runCurrent(); controller.dismiss(); runCurrent()
        assertFalse(controller.state.value.networkLoading)
        controller.close()
    }
}
