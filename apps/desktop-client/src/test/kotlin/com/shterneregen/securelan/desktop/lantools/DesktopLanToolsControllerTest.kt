package com.shterneregen.securelan.desktop.lantools

import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.common.net.address.MacAddress
import com.shterneregen.securelan.common.net.interfaces.NetworkBinding
import com.shterneregen.securelan.desktop.compose.state.lantools.*
import com.shterneregen.securelan.lantools.model.*
import com.shterneregen.securelan.lantools.wol.*
import com.shterneregen.securelan.neighbor.NeighborMacResolver
import com.shterneregen.securelan.neighbor.NeighborMacResult
import com.shterneregen.securelan.neighbor.NeighborQuery
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

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
                try { withContext(NonCancellable) { delay(100.milliseconds) } } finally { cleaned = true }
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
        assertFalse(controller.state.value.lastResults.getValue(device.id).contains("Wake packet sent"))
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

    @Test fun macLookupIsAnExplicitOnLinkSuggestionThatIsNotSaved() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val mac = MacAddress.parse("02:11:22:33:44:55")!!
        var query: NeighborQuery? = null
        val resolver = NeighborMacResolver { requested -> query = requested; NeighborMacResult.Found(mac) }
        val controller = DesktopLanToolsController(MemoryLanStore(), networks, successWake(), dispatcher, dispatcher,
            neighbor = resolver)
        controller.open(); runCurrent()
        controller.beginAdd(); controller.editDraft(LanDeviceDraft(name = "Desktop", address = "192.168.1.20"))
        assertTrue(controller.state.value.canLookupMac)
        controller.resolveMac()
        assertTrue(controller.state.value.macLookup is MacLookupStatus.Running)
        runCurrent()
        assertEquals(mac.value, controller.state.value.editor!!.mac)
        assertEquals(MacLookupStatus.Found(mac.value), controller.state.value.macLookup)
        val requested = requireNotNull(query)
        assertEquals("192.168.1.20", requested.target.value)
        assertEquals("eth0", requested.binding.interfaceKey)
        assertTrue(controller.state.value.devices.isEmpty())
        controller.close()
    }

    @Test fun ambiguousMacLookupExplainsHowToChooseTheInterface() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val second = network.copy(
            binding = network.binding.copy(key = "wlan0:192.168.1.50", interfaceKey = "wlan0",
                address = IpLiteral.parse("192.168.1.50")!!),
            displayName = "Wi-Fi",
        )
        val ambiguous = LanNetworkInfoProvider { LanResult.Success(LanNetworkSnapshot(listOf(network, second))) }
        var calls = 0
        var requested: NeighborQuery? = null
        val resolver = NeighborMacResolver { query -> calls++; requested = query; NeighborMacResult.NotFound }
        val controller = DesktopLanToolsController(MemoryLanStore(), ambiguous, successWake(), dispatcher, dispatcher,
            neighbor = resolver)
        controller.open(); runCurrent()
        controller.beginAdd(); controller.editDraft(LanDeviceDraft(name = "Desktop", address = "192.168.1.20"))
        controller.resolveMac(); runCurrent()
        assertEquals(0, calls)
        val message = (controller.state.value.macLookup as MacLookupStatus.Unavailable).message
        assertTrue(message.contains("matches more than one network interface"))
        assertTrue(message.contains("Network interface below"))
        controller.editDraft(controller.state.value.editor!!.copy(preferredInterface = "wlan0"))
        controller.resolveMac(); runCurrent()
        assertEquals(1, calls)
        assertEquals("wlan0", requested?.binding?.interfaceKey)
        controller.close()
    }

    @Test fun macLookupRejectsOffLinkTargetsAndReportsMissingCache() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        var calls = 0
        val resolver = NeighborMacResolver { calls++; NeighborMacResult.NotFound }
        val controller = DesktopLanToolsController(MemoryLanStore(), networks, successWake(), dispatcher, dispatcher,
            neighbor = resolver)
        controller.open(); runCurrent()
        controller.beginAdd(); controller.editDraft(LanDeviceDraft(name = "Desktop", address = "10.0.0.5"))
        controller.resolveMac(); runCurrent()
        assertEquals(0, calls)
        assertTrue(controller.state.value.macLookup is MacLookupStatus.Unavailable)
        controller.editDraft(controller.state.value.editor!!.copy(address = "192.168.1.20"))
        controller.resolveMac(); runCurrent()
        assertEquals(1, calls)
        assertEquals("", controller.state.value.editor!!.mac)
        assertTrue((controller.state.value.macLookup as MacLookupStatus.Unavailable).message.contains("neighbor cache"))
        controller.close()
    }

    @Test fun macLookupWaitsForNetworkSnapshotAndFailsClosed() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        var pending: CompletableDeferred<LanResult<LanNetworkSnapshot>>? = null
        var result: LanResult<LanNetworkSnapshot> = LanResult.Success(LanNetworkSnapshot(listOf(network)))
        var calls = 0
        val controller = DesktopLanToolsController(MemoryLanStore(),
            LanNetworkInfoProvider { pending?.await() ?: result }, successWake(), dispatcher, dispatcher,
            neighbor = NeighborMacResolver { calls++; NeighborMacResult.NotFound })
        controller.open(); runCurrent()
        controller.beginAdd(); controller.editDraft(LanDeviceDraft(name = "Desktop", address = "192.168.1.20"))
        assertTrue(controller.state.value.canLookupMac)
        pending = CompletableDeferred()
        controller.refreshInterfaces(); runCurrent()
        assertTrue(controller.state.value.networkLoading)
        assertFalse(controller.state.value.canLookupMac)
        controller.resolveMac(); runCurrent()
        assertEquals(0, calls)
        pending.complete(LanResult.Success(LanNetworkSnapshot(listOf(network)))); pending = null
        advanceUntilIdle()
        assertTrue(controller.state.value.canLookupMac)
        result = LanResult.Failure(LanError.NETWORK_UNAVAILABLE)
        controller.refreshInterfaces(); runCurrent()
        assertNotNull(controller.state.value.networkError)
        assertFalse(controller.state.value.canLookupMac)
        controller.resolveMac(); runCurrent()
        assertEquals(0, calls)
        controller.close()
    }

    @Test fun changingTheAddressDiscardsALateLookupResult() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val mac = MacAddress.parse("02:11:22:33:44:55")!!
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val returned = CountDownLatch(1)
        var calls = 0
        val resolver = NeighborMacResolver {
            calls++
            started.countDown()
            check(release.await(5, TimeUnit.SECONDS)) { "lookup was not released" }
            returned.countDown()
            NeighborMacResult.Found(mac)
        }
        val controller = DesktopLanToolsController(MemoryLanStore(), networks, successWake(), dispatcher,
            Dispatchers.IO, neighbor = resolver)
        controller.open()
        val readyBy = System.nanoTime() + TimeUnit.SECONDS.toNanos(2)
        while ((!controller.state.value.loaded || controller.state.value.networkLoading) && System.nanoTime() < readyBy) {
            runCurrent()
            Thread.sleep(5)
        }
        assertTrue(controller.state.value.loaded && !controller.state.value.networkLoading, "controller did not load")
        controller.beginAdd(); controller.editDraft(LanDeviceDraft(name = "Desktop", address = "192.168.1.20"))
        controller.resolveMac(); runCurrent()
        assertTrue(started.await(2, TimeUnit.SECONDS), "lookup did not start")
        controller.editDraft(controller.state.value.editor!!.copy(address = "192.168.1.21"))
        release.countDown()
        assertTrue(returned.await(2, TimeUnit.SECONDS), "lookup did not finish")
        runCurrent()
        assertEquals(1, calls)
        assertEquals(MacLookupStatus.Idle, controller.state.value.macLookup)
        assertEquals("", controller.state.value.editor!!.mac)
        controller.close()
    }

    @Test fun lostExplicitInterfaceCannotFallBackToAnotherNetworkAfterRefresh() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        var current = network
        val changing = LanNetworkInfoProvider { LanResult.Success(LanNetworkSnapshot(listOf(current))) }
        val sent = mutableListOf<String>()
        val controller = DesktopLanToolsController(MemoryLanStore(listOf(device)), changing,
            DefaultWakeOnLanService(changing, { route, _ -> sent += route.network.binding.key }), dispatcher, dispatcher)
        try {
            controller.open(); runCurrent(); controller.selectDevice(device.id); controller.selectBinding(network.binding.key)
            current = network.copy(binding = network.binding.copy(key = "other:192.168.1.42", interfaceKey = "other"))
            controller.refreshInterfaces(); runCurrent()
            assertEquals(network.binding.key, controller.state.value.bindingKey)
            controller.wakeDevice(device.id); runCurrent()
            assertTrue(sent.isEmpty(), "Interface loss must not authorize a send through another network")
            assertTrue(controller.state.value.lastResults.getValue(device.id).contains("unavailable"))
            controller.selectBinding(current.binding.key); controller.wakeDevice(device.id); runCurrent()
            assertEquals(listOf(current.binding.key), sent)
        } finally { controller.close() }
    }
}
