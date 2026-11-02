package com.shterneregen.securelan.desktop.lantools

import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.desktop.compose.state.lantools.*
import com.shterneregen.securelan.lantools.discovery.*
import com.shterneregen.securelan.lantools.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class DesktopExplorerControllerTest {
    private val address = IpLiteral.parse("192.168.1.20")!!
    private val row = DiscoveredLanDevice("found", "eth0:192.168.1.2", "Printer", "printer.local", listOf(address),
        listOf(LanAdvertisedService("Printer", "_ipp._tcp.local.", 631, listOf(address))), setOf("mDNS/DNS-SD"), Instant.EPOCH)

    @Test fun openingDoesNotBrowseAndClosingRetainsHistoricalRowsButRejectsLateUpdates() = runTest {
        var calls = 0
        val discovery = LanDeviceDiscovery { update ->
            calls++
            update(LanDiscoverySnapshot(listOf(row)))
            withContext(NonCancellable) { delay(100) }
            update(LanDiscoverySnapshot())
            LanResult.Success(LanDiscoverySnapshot())
        }
        val dispatcher = StandardTestDispatcher(testScheduler)
        val controller = DesktopExplorerController(discovery, dispatcher, dispatcher)
        controller.open(); runCurrent()
        assertEquals(0, calls)
        controller.query("printer"); controller.filter(LanDeviceFilter.DISCOVERED); controller.select(row.id)
        controller.refresh(); runCurrent()
        controller.dismiss(); advanceUntilIdle()
        assertEquals(DiagnosticPhase.CANCELLED, controller.state.value.phase)
        assertTrue(controller.state.value.snapshot.devices.single().stale)
        controller.open()
        assertEquals("printer", controller.state.value.query)
        assertEquals(row.id, controller.state.value.selectedId)
        controller.close()
    }

    @Test fun explicitRefreshReconcilesMissingRowsAndDoesNotMergeSavedIdentity() = runTest {
        var current = LanDiscoverySnapshot(listOf(row))
        val dispatcher = StandardTestDispatcher(testScheduler)
        val controller = DesktopExplorerController(LanDeviceDiscovery { update -> update(current); LanResult.Success(current) }, dispatcher, dispatcher)
        val saved = SavedLanDevice(UUID.randomUUID(), "User label", address)
        controller.open(); controller.refresh(); runCurrent()
        assertEquals(1, controller.state.value.filteredDevices.size)
        assertTrue(controller.state.value.includes(saved))
        controller.query("IPP")
        assertEquals(1, controller.state.value.filteredDevices.size)
        assertFalse(controller.state.value.includes(saved))
        controller.query(""); controller.filter(LanDeviceFilter.SAVED)
        assertTrue(controller.state.value.filteredDevices.isEmpty())
        assertTrue(controller.state.value.includes(saved))
        current = LanDiscoverySnapshot()
        controller.refresh(); runCurrent()
        assertTrue(controller.state.value.snapshot.devices.isEmpty())
        assertEquals("User label", saved.displayName)
        controller.close()
    }

    @Test fun failedBrowseKeepsOldRowsHistoricalAndSupportsRetry() = runTest {
        var fail = false
        val dispatcher = StandardTestDispatcher(testScheduler)
        val controller = DesktopExplorerController(LanDeviceDiscovery {
            if (fail) LanResult.Failure(LanError.RESOURCE_LIMIT) else LanResult.Success(LanDiscoverySnapshot(listOf(row), partial = true))
        }, dispatcher, dispatcher)
        controller.open(); controller.refresh(); runCurrent()
        fail = true
        controller.refresh(); runCurrent()
        assertEquals(DiagnosticPhase.FAILURE, controller.state.value.phase)
        assertTrue(controller.state.value.snapshot.devices.single().stale)
        assertTrue(controller.state.value.message.contains("still closing"))
        fail = false
        controller.refresh(); runCurrent()
        assertEquals(DiagnosticPhase.SUCCESS, controller.state.value.phase)
        controller.close()
    }
}
