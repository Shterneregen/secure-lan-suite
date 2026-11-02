package com.shterneregen.securelan.desktop.lantools.discovery

import com.shterneregen.securelan.chat.discovery.*
import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.common.net.interfaces.NetworkBinding
import com.shterneregen.securelan.common.net.interfaces.NetworkKind
import com.shterneregen.securelan.desktop.network.discovery.DesktopMdnsDiscoveryProvider
import com.shterneregen.securelan.desktop.network.discovery.JmDnsFacade
import com.shterneregen.securelan.lantools.discovery.LanDiscoverySnapshot
import com.shterneregen.securelan.lantools.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class DesktopLanDeviceDiscoveryTest {
    private val source = NetworkBinding("eth0:192.168.1.2", "eth0", IpLiteral.parse("192.168.1.2")!!, 24)
    private fun resolved(name: String = "Printer", host: String = "printer.local.", address: String = "192.168.1.20") =
        DnsSdEvent.Resolved(DnsSdService("_ipp._tcp.local.", name, host, listOf(address), 631))
    private class Browser(private val initial: List<DnsSdEvent> = emptyList()) : LanDnsSdBrowser {
        lateinit var emit: (DnsSdEvent) -> Unit
        var closed = 0
        override fun start(emit: (DnsSdEvent) -> Unit) { this.emit = emit; initial.forEach(emit) }
        override fun close() { closed++ }
    }

    @Test fun manualBrowseHandlesMultipleNetworksAndClosesEveryHandle() = runTest {
        val second = source.copy(key = "eth1:192.168.1.2", interfaceKey = "eth1")
        val browsers = mutableListOf<Browser>()
        val service = DesktopLanDeviceDiscovery({ listOf(source, second) }, { Browser(listOf(resolved())).also(browsers::add) },
            StandardTestDispatcher(testScheduler), Clock.fixed(Instant.EPOCH, ZoneOffset.UTC), { testScheduler.currentTime * 1_000_000 }, 1_000)
        val updates = mutableListOf<LanDiscoverySnapshot>()
        val result = service.browse { updates += it } as LanResult.Success
        assertEquals(2, result.value.devices.size)
        assertTrue(result.value.devices.all { it.stale })
        assertTrue(updates.any { it.devices.size == 2 && it.devices.none { row -> row.stale } })
        assertEquals(listOf(1, 1), browsers.map { it.closed })
    }

    @Test fun lostServicesAndNetworkChangesInvalidateRowsAndLateOldHandleCallbacks() = runTest {
        var bindings = listOf(source)
        val browsers = mutableListOf<Browser>()
        val service = DesktopLanDeviceDiscovery({ bindings }, { Browser(listOf(resolved())).also(browsers::add) },
            StandardTestDispatcher(testScheduler), nanoTime = { testScheduler.currentTime * 1_000_000 }, durationMillis = 5_000)
        var current = LanDiscoverySnapshot()
        val run = async { service.browse { current = it } }
        runCurrent()
        val firstId = current.devices.single().id
        browsers[0].emit(DnsSdEvent.Removed("_ipp._tcp.local.", "Printer"))
        advanceTimeBy(250); runCurrent()
        assertTrue(current.devices.isEmpty())
        browsers[0].emit(resolved())
        advanceTimeBy(250); runCurrent()
        browsers[0].emit(resolved(address = "203.0.113.4"))
        advanceTimeBy(250); runCurrent()
        assertTrue(current.devices.isEmpty(), "An updated service outside this subnet must retire its previous endpoint")
        browsers[0].emit(resolved())
        advanceTimeBy(250); runCurrent()
        bindings = listOf(source.copy(prefixLength = 25))
        advanceTimeBy(1_000); runCurrent()
        assertEquals(1, browsers[0].closed)
        assertNotEquals(firstId, current.devices.single().id)
        browsers[0].emit(resolved("Late stale callback"))
        advanceTimeBy(250); runCurrent()
        assertEquals("Printer", current.devices.single().displayName)
        bindings = emptyList()
        advanceTimeBy(1_750); runCurrent()
        assertTrue(current.devices.isEmpty())
        run.cancelAndJoin()
        assertEquals(listOf(1, 1), browsers.map { it.closed })
    }

    @Test fun floodsAndInterfaceLimitsProducePartialResultsInsteadOfUnboundedRowsOrHandles() = runTest {
        val all = (0..6).map { source.copy(key = "eth$it:192.168.1.2", interfaceKey = "eth$it") }
        var opened = 0
        val service = DesktopLanDeviceDiscovery({ all }, {
            opened++
            Browser((0..599).map { resolved("Device $it", "host$it.local.") })
        }, StandardTestDispatcher(testScheduler), nanoTime = { testScheduler.currentTime * 1_000_000 }, durationMillis = 500)
        val result = service.browse {} as LanResult.Success
        assertEquals(4, opened)
        assertTrue(result.value.partial)
        assertTrue(result.value.devices.size <= 256)
    }

    @Test fun doesNotOpenVpnOrNonMulticastInterfacesAndReportsUnavailable() = runTest {
        val service = DesktopLanDeviceDiscovery({ listOf(source.copy(kind = NetworkKind.VPN), source.copy(multicast = false)) },
            { error("No handle should be created") }, StandardTestDispatcher(testScheduler), durationMillis = 1)
        assertEquals(LanError.NETWORK_UNAVAILABLE, (service.browse {} as LanResult.Failure).code)
    }

    @Test fun rejectsExternalBroadcastAndMalformedRecordsAndReportsSourceFailure() = runTest {
        val browser = Browser(listOf(resolved(address = "203.0.113.4"), resolved(address = "192.168.1.255"),
            resolved(name = "a\nb"), resolved(host = "external.example"), DnsSdEvent.Limited))
        val service = DesktopLanDeviceDiscovery({ listOf(source) }, { browser }, StandardTestDispatcher(testScheduler), durationMillis = 1)
        val result = service.browse {} as LanResult.Success
        assertTrue(result.value.devices.isEmpty())
        assertTrue(result.value.partial)
        assertEquals(1, browser.closed)
        val denied = DesktopLanDeviceDiscovery({ throw SecurityException() }, dispatcher = StandardTestDispatcher(testScheduler))
        assertEquals(LanError.PERMISSION_DENIED, (denied.browse {} as LanResult.Failure).code)
    }

    @Test fun cancellationKeepsCapacityOccupiedUntilCleanupActuallyCompletes() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val closing = CountDownLatch(1)
        val release = CountDownLatch(1)
        var closes = 0
        val service = DesktopLanDeviceDiscovery({ listOf(source) }, {
            object : LanDnsSdBrowser {
                override fun start(emit: (DnsSdEvent) -> Unit) { started.complete(Unit) }
                override fun close() { closing.countDown(); check(release.await(5, TimeUnit.SECONDS)); closes++ }
            }
        }, durationMillis = 1_000)
        val first = launch { service.browse {} }
        try {
            withTimeout(3_000) { started.await() }
            first.cancel()
            assertTrue(withContext(Dispatchers.IO) { closing.await(3, TimeUnit.SECONDS) })
            assertEquals(LanError.RESOURCE_LIMIT, (service.browse {} as LanResult.Failure).code)
        } finally { release.countDown(); first.cancelAndJoin() }
        assertEquals(1, closes)
    }

    @Test fun aFailedSourceKeepsOtherResultsPartialAndClosesItsPartiallyStartedBrowser() = runTest {
        val good = Browser(listOf(resolved()))
        var failedCloses = 0
        val bad = object : LanDnsSdBrowser {
            override fun start(emit: (DnsSdEvent) -> Unit) { throw IllegalStateException("synthetic start failure") }
            override fun close() { failedCloses++ }
        }
        val second = source.copy(key = "eth1:192.168.1.2", interfaceKey = "eth1")
        val service = DesktopLanDeviceDiscovery({ listOf(source, second) }, { if (it == source) good else bad },
            StandardTestDispatcher(testScheduler), nanoTime = { testScheduler.currentTime * 1_000_000 }, durationMillis = 500)
        val result = service.browse {} as LanResult.Success
        assertEquals(1, result.value.devices.size)
        assertTrue(result.value.partial)
        assertEquals(1, failedCloses)
        assertEquals(1, good.closed)
    }

    @Test fun explorerCancellationDoesNotStopOrMutateTheSeparateRoomDiscoveryProvider() = runTest {
        val roomStarted = CountDownLatch(1)
        val roomClosed = CountDownLatch(1)
        val room = DesktopMdnsDiscoveryProvider({ listOf(source) }, {
            object : JmDnsFacade {
                override fun browse(resolved: (JmDnsFacade.Resolved) -> Unit, removed: (String) -> Unit) { roomStarted.countDown() }
                override fun advertise(value: HostedRoomAdvertisement?) = Unit
                override fun close() { roomClosed.countDown() }
            }
        })
        room.start(DiscoveryStartRequest(PeerDiscoveryConfig.listenOnly("own-peer", "Own"))) {}
        try {
            assertTrue(roomStarted.await(3, TimeUnit.SECONDS))
            val browser = Browser(listOf(resolved()))
            val service = DesktopLanDeviceDiscovery({ listOf(source) }, { browser }, StandardTestDispatcher(testScheduler), durationMillis = 500)
            val run = launch { service.browse {} }; runCurrent(); run.cancelAndJoin()
            assertEquals(1, browser.closed)
            assertEquals(1, roomClosed.count)
        } finally { room.stop(); assertTrue(roomClosed.await(3, TimeUnit.SECONDS)) }
    }
}
