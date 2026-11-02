package com.shterneregen.securelan.androidclient.lantools

import com.shterneregen.securelan.androidclient.lantools.network.*
import com.shterneregen.securelan.androidclient.network.discovery.*
import com.shterneregen.securelan.lantools.discovery.LanDiscoverySnapshot
import com.shterneregen.securelan.lantools.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AndroidLanDeviceDiscoveryTest {
    private class Lease : MulticastLeaseOwner {
        var acquired = 0
        var closed = 0
        override fun acquire() { acquired++ }
        override fun close() { closed++ }
    }
    private class Browser : LanNsdBrowser {
        data class Call(val type: String, val network: String?, val resolved: (NsdFacade.Resolved) -> Unit, val lost: (String) -> Unit, val failed: (NsdFailure) -> Unit)
        val calls = mutableListOf<Call>()
        var closes = 0
        var awaits = 0
        var opened: (Call) -> Unit = {}
        override fun open(type: String, networkKey: String?, resolved: (NsdFacade.Resolved) -> Unit, lost: (String) -> Unit, failed: (NsdFailure) -> Unit): NsdFacade.Browser {
            val call = Call(type, networkKey, resolved, lost, failed)
            calls += call; opened(call)
            return object : NsdFacade.Browser { override fun close() { closes++ }; override suspend fun awaitClosed() { awaits++ } }
        }
    }
    private fun resolved(route: String? = "wifi", addresses: List<String> = listOf("192.168.1.20")) = NsdFacade.Resolved("Printer", 80, addresses, emptyMap(), route)

    @Test fun fixedTypesUseSelectedNetworkAndCleanupLeavesHistoricalSnapshot() = runTest {
        val browser = Browser().apply { opened = { if (it.type == "_http._tcp.") it.resolved(resolved()) } }
        val lease = Lease()
        val updates = mutableListOf<LanDiscoverySnapshot>()
        val discovery = AndroidLanDeviceDiscovery(FakeLanPlatform(), browser, lease, NsdApiCapabilities(true, true), StandardTestDispatcher(testScheduler),
            nanoTime = { testScheduler.currentTime * 1_000_000 }, durationMillis = 500)
        val result = discovery.browse(null) { updates += it } as LanResult.Success
        assertEquals(6, browser.calls.size); assertEquals(setOf("wifi"), browser.calls.map { it.network }.toSet())
        val device = result.value.devices.single()
        assertNull(device.hostname); assertNull(device.deviceType); assertTrue(device.stale)
        assertEquals("192.168.1.20", device.addresses.single().value)
        assertTrue(updates.any { it.devices.any { row -> !row.stale } })
        assertEquals(6, browser.closes); assertEquals(6, browser.awaits); assertEquals(1, lease.acquired); assertEquals(1, lease.closed)
    }
    @Test fun multipleNetworksRequireSelectionAndLegacyRejectsVpn() = runTest {
        val platform = FakeLanPlatform(listOf(link(), link("ether:192.168.2.2", "ether", "192.168.2.2")))
        val browser = Browser(); val lease = Lease()
        val modern = AndroidLanDeviceDiscovery(platform, browser, lease, NsdApiCapabilities(true, true), StandardTestDispatcher(testScheduler), durationMillis = 1)
        assertEquals(LanError.INTERFACE_SELECTION_REQUIRED, (modern.browse(null) {} as LanResult.Failure).code)
        assertTrue(browser.calls.isEmpty())
        assertTrue(modern.browse(platform.entries[1].binding.key) {} is LanResult.Success)
        assertEquals(setOf("ether"), browser.calls.map { it.network }.toSet())
        platform.entries = listOf(link()); platform.vpn = true
        val legacy = AndroidLanDeviceDiscovery(platform, browser, lease, NsdApiCapabilities(false, false), StandardTestDispatcher(testScheduler), durationMillis = 1)
        assertEquals(LanError.UNSUPPORTED, (legacy.browse(null) {} as LanResult.Failure).code)
    }
    @Test fun staleBindingCannotFallBackToAnotherNetwork() = runTest {
        val browser = Browser()
        val discovery = AndroidLanDeviceDiscovery(FakeLanPlatform(), browser, Lease(), NsdApiCapabilities(true, true), StandardTestDispatcher(testScheduler))
        assertEquals(LanError.INTERFACE_UNAVAILABLE, (discovery.browse("old") {} as LanResult.Failure).code)
        assertTrue(browser.calls.isEmpty())
    }
    @Test fun foreignNetworkAndOffLinkAnswersAreIgnored() = runTest {
        val browser = Browser().apply { opened = { call ->
            if (call.type == "_http._tcp.") call.resolved(resolved("foreign"))
            else call.resolved(resolved(addresses = listOf("10.0.0.5", "192.168.1.255", "224.0.0.1", "invalid")))
        } }
        val discovery = AndroidLanDeviceDiscovery(FakeLanPlatform(), browser, Lease(), NsdApiCapabilities(true, true), StandardTestDispatcher(testScheduler), durationMillis = 1)
        val result = discovery.browse(null) {} as LanResult.Success
        assertTrue(result.value.devices.isEmpty()); assertTrue(result.value.partial)
    }
    @Test fun networkLossClearsRowsAndLostEventRemovesService() = runTest {
        val platform = FakeLanPlatform(); val browser = Browser()
        val discovery = AndroidLanDeviceDiscovery(platform, browser, Lease(), NsdApiCapabilities(true, true), StandardTestDispatcher(testScheduler), durationMillis = 1000)
        var latest = LanDiscoverySnapshot()
        val job = async { discovery.browse(null) { latest = it } }
        runCurrent(); browser.calls.first().resolved(resolved()); advanceTimeBy(251); runCurrent(); assertEquals(1, latest.devices.size)
        browser.calls.first().lost("Printer"); advanceTimeBy(251); runCurrent(); assertTrue(latest.devices.isEmpty())
        browser.calls.first().resolved(resolved()); advanceTimeBy(251); runCurrent(); assertEquals(1, latest.devices.size)
        platform.entries = emptyList(); advanceTimeBy(251); runCurrent()
        assertEquals(LanError.NETWORK_CHANGED, (job.await() as LanResult.Failure).code)
        assertTrue(latest.devices.isEmpty())
    }
    @Test fun permissionFailureIsTypedAndCancellationClosesAllHandles() = runTest {
        val browser = Browser(); val lease = Lease()
        val discovery = AndroidLanDeviceDiscovery(FakeLanPlatform(), browser, lease, NsdApiCapabilities(true, true), StandardTestDispatcher(testScheduler), durationMillis = 500)
        var updates = 0
        val job = launch { discovery.browse(null) { updates++ } }
        runCurrent(); job.cancelAndJoin()
        assertEquals(6, browser.closes); assertEquals(1, lease.closed)
        val before = updates; browser.calls.first().resolved(resolved()); assertEquals(before, updates)
        browser.opened = { it.failed(NsdFailure.PERMISSION_DENIED) }
        val beforeDenial = testScheduler.currentTime
        assertEquals(LanError.PERMISSION_DENIED, (discovery.browse(null) {} as LanResult.Failure).code)
        assertTrue(testScheduler.currentTime - beforeDenial < 500)
    }
    @Test fun concurrentBrowsesAreRejectedWithoutAdditionalHandles() = runTest {
        val browser = Browser()
        val discovery = AndroidLanDeviceDiscovery(FakeLanPlatform(), browser, Lease(), NsdApiCapabilities(true, true), StandardTestDispatcher(testScheduler))
        val job = launch { discovery.browse(null) {} }; runCurrent()
        assertEquals(LanError.RESOURCE_LIMIT, (discovery.browse(null) {} as LanResult.Failure).code)
        assertEquals(6, browser.calls.size); job.cancelAndJoin()
    }
    @Test fun missingStopAcknowledgementBlocksNewHandlesUntilNativeCleanupCompletes() = runTest {
        val acknowledged = CompletableDeferred<Unit>()
        var opened = 0
        val browser = LanNsdBrowser { _, _, _, _, _ ->
            opened++
            object : NsdFacade.Browser { override fun close() = Unit; override suspend fun awaitClosed() { acknowledged.await() } }
        }
        val discovery = AndroidLanDeviceDiscovery(FakeLanPlatform(), browser, Lease(), NsdApiCapabilities(true, true), StandardTestDispatcher(testScheduler), durationMillis = 1)
        assertTrue(discovery.browse(null) {} is LanResult.Success)
        assertEquals(6, opened)
        assertEquals(LanError.RESOURCE_LIMIT, (discovery.browse(null) {} as LanResult.Failure).code)
        assertEquals(6, opened)
        acknowledged.complete(Unit)
        assertTrue(discovery.browse(null) {} is LanResult.Success)
        assertEquals(12, opened)
    }

    @Test fun discoveryStopAloneCannotAdmitNewRunWhileServiceCallbacksAreRetiring() = runTest {
        val lifetimes = mutableListOf<NsdBrowserTeardown>()
        val registration = Any()
        val browser = LanNsdBrowser { _, _, _, _, _ ->
            val lifetime = NsdBrowserTeardown().also { it.registered(registration); lifetimes += it }
            object : NsdFacade.Browser {
                override fun close() { lifetime.closeRequested(); lifetime.discoveryStopped() }
                override suspend fun awaitClosed() { lifetime.awaitClosed() }
            }
        }
        val discovery = AndroidLanDeviceDiscovery(FakeLanPlatform(), browser, Lease(), NsdApiCapabilities(true, true),
            StandardTestDispatcher(testScheduler), durationMillis = 1)
        assertTrue(discovery.browse(null) {} is LanResult.Success)
        assertEquals(LanError.RESOURCE_LIMIT, (discovery.browse(null) {} as LanResult.Failure).code)
        assertEquals(6, lifetimes.size)
        lifetimes.forEach { it.unregistered(registration) }
        assertTrue(discovery.browse(null) {} is LanResult.Success)
        assertEquals(12, lifetimes.size)
    }
}
