package com.shterneregen.securelan.lantools

import com.shterneregen.securelan.common.net.address.MacAddress
import com.shterneregen.securelan.lantools.model.*
import com.shterneregen.securelan.lantools.wol.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.IOException
import java.net.BindException
import java.net.NoRouteToHostException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class WakeOnLanServiceTest {
    @Test
    fun validatesBeforeReadingNetworksOrSending() = runTest {
        val never = LanNetworkInfoProvider { error("Invalid input reached network provider") }
        val service = DefaultWakeOnLanService(never, DatagramSender { _, _ -> error("sent") })
        val req = request()
        val cases = listOf(
            req.copy(configuration = WakeConfiguration(MacAddress.parse("ff:ff:ff:ff:ff:ff")!!)),
            req.copy(configuration = WakeConfiguration(MacAddress.parse("00:00:00:00:00:00")!!)),
            req.copy(configuration = WakeConfiguration(mac, port = 0)),
            req.copy(configuration = WakeConfiguration(mac, port = 65536)),
            req.copy(configuration = WakeConfiguration(mac, broadcast = ip("::1"))),
            req.copy(configuration = WakeConfiguration(mac, preferredInterfaceKey = " ")),
            req.copy(bindingKey = ""), req.copy(timeoutMillis = 0), req.copy(timeoutMillis = 30001),
        )
        cases.forEach { assertEquals(LanError.INVALID_INPUT, (service.send(it) as LanResult.Failure).code) }
    }

    @Test
    fun sendsOneExactDatagramAndReportsOnlyOsAcceptance() = runTest {
        val now = Instant.parse("2026-11-02T12:00:00Z")
        var sends = 0
        val service = DefaultWakeOnLanService(networks(network()), DatagramSender { route, payload ->
            sends++
            assertEquals(network(), route.network)
            assertEquals(ip("192.168.10.255"), route.target)
            assertArrayEquals(MagicPacket.create(mac), payload)
        }, Clock.fixed(now, ZoneOffset.UTC))
        val receipt = (service.send(request()) as LanResult.Success).value
        assertEquals(1, sends)
        assertEquals(102, receipt.bytesSent)
        assertEquals(now, receipt.sentAt)
    }

    @Test
    fun mapsFailuresWithoutLeakingExceptionMessages() = runTest {
        val cases = listOf(
            IOException("sensitive OS details") to LanError.IO,
            SecurityException("sensitive OS details") to LanError.PERMISSION_DENIED,
            BindException("sensitive OS details") to LanError.NETWORK_CHANGED,
            NoRouteToHostException("sensitive OS details") to LanError.NETWORK_UNAVAILABLE,
            UnsupportedOperationException("unsupported socket option") to LanError.UNSUPPORTED,
        )
        cases.forEach { (error, expected) ->
            val service = DefaultWakeOnLanService(networks(network()), DatagramSender { _, _ -> throw error })
            assertEquals(LanResult.Failure(expected), service.send(request()))
        }
    }

    @Test
    fun preservesTypedProviderFailuresAndDoesNotSend() = runTest {
        val denied = LanResult.Failure(LanError.PERMISSION_DENIED)
        val service = DefaultWakeOnLanService(LanNetworkInfoProvider { denied }, DatagramSender { _, _ -> error("sent") })
        assertEquals(denied, service.send(request()))
        val invalidTarget = DefaultWakeOnLanService(networks(network()), DatagramSender { _, _ -> error("sent") })
        assertEquals(LanResult.Failure(LanError.INVALID_INPUT, "broadcast"),
            invalidTarget.send(request().copy(configuration = WakeConfiguration(mac, broadcast = ip("10.0.0.255")))))
    }

    @Test
    fun detectsInterfaceDisappearanceOrChangedTopologyBeforeOpeningSender() = runTest {
        val initial = network()
        for (fresh in listOf(emptyList(), listOf(network("wifi")), listOf(initial.copy(binding = initial.binding.copy(up = false))),
            listOf(initial.copy(binding = initial.binding.copy(prefixLength = 25))))) {
            var reads = 0
            val provider = LanNetworkInfoProvider {
                LanResult.Success(LanNetworkSnapshot(if (reads++ == 0) listOf(initial) else fresh))
            }
            val service = DefaultWakeOnLanService(provider, DatagramSender { _, _ -> error("sent after topology change") })
            assertEquals(LanResult.Failure(LanError.NETWORK_CHANGED), service.send(request()))
        }
    }

    @Test
    fun boundsWholeOperationIncludingSnapshotAndReleasesCapacity() = runTest {
        var slow = true
        val provider = LanNetworkInfoProvider {
            if (slow) delay(10_000)
            LanResult.Success(LanNetworkSnapshot(listOf(network())))
        }
        val service = DefaultWakeOnLanService(provider, DatagramSender { _, _ -> })
        assertEquals(LanResult.Failure(LanError.TIMEOUT), service.send(request().copy(timeoutMillis = 100)))
        assertEquals(100, testScheduler.currentTime)
        slow = false
        assertTrue(service.send(request()) is LanResult.Success)
    }

    @Test
    fun timesOutSuspendedSenderAndRunsCleanup() = runTest {
        var cleaned = false
        val service = DefaultWakeOnLanService(networks(network()), DatagramSender { _, _ ->
            try { awaitCancellation() } finally { cleaned = true }
        })
        assertEquals(LanResult.Failure(LanError.TIMEOUT), service.send(request().copy(timeoutMillis = 50)))
        assertTrue(cleaned)
    }

    @Test
    fun propagatesCallerCancellationAndOuterDeadline() = runTest {
        var entered = false
        var cleaned = false
        val service = DefaultWakeOnLanService(networks(network()), DatagramSender { _, _ ->
            entered = true
            try { awaitCancellation() } finally { cleaned = true }
        })
        val operation = async { service.send(request()) }
        runCurrent()
        assertTrue(entered)
        operation.cancelAndJoin()
        assertTrue(operation.isCancelled)
        assertTrue(cleaned)
        var propagated = false
        try { withTimeout(10) { service.send(request()) } } catch (_: TimeoutCancellationException) { propagated = true }
        assertTrue(propagated)
    }

    @Test
    fun cancelledCallDoesNotReadSnapshotOrOpenTransport() = runTest {
        val service = DefaultWakeOnLanService(LanNetworkInfoProvider { error("snapshot") }, DatagramSender { _, _ -> error("send") })
        val operation = launch { currentCoroutineContext().cancel(); service.send(request()) }
        operation.join()
        assertTrue(operation.isCancelled)
    }

    @Test
    fun cancellationWinsOverConcurrentIoFailure() = runTest {
        val service = DefaultWakeOnLanService(networks(network()), DatagramSender { _, _ ->
            currentCoroutineContext().cancel()
            throw IOException("channel closed by cancellation")
        })
        val operation = async { service.send(request()) }
        operation.join()
        assertTrue(operation.isCancelled)
    }

    @Test
    fun limitsConcurrentCallsAndRecoversAfterCancellation() = runTest {
        var block = true
        val service = DefaultWakeOnLanService(networks(network()), DatagramSender { _, _ -> if (block) awaitCancellation() })
        val jobs = List(4) { async { service.send(request()) } }
        runCurrent()
        assertEquals(LanResult.Failure(LanError.RESOURCE_LIMIT), service.send(request()))
        jobs.forEach { it.cancelAndJoin() }
        block = false
        assertTrue(service.send(request()) is LanResult.Success)
    }
}
