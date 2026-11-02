package com.shterneregen.securelan.lantools.diagnostics

import com.shterneregen.securelan.lantools.ip
import com.shterneregen.securelan.lantools.network
import com.shterneregen.securelan.lantools.networks
import com.shterneregen.securelan.lantools.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.net.ConnectException
import java.net.NoRouteToHostException

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkDiagnosticsServiceTest {
    private val neverResolve = HostResolver { error("Unexpected DNS") }

    @ParameterizedTest
    @ValueSource(strings = ["", "https://host", "host:80", "bad name", "1.2.3", "256.1.1.1", "01.2.3.4", "-host.local", "host..local", "[::1]", "fe80::1%2", "host/path", "host\n.local"])
    fun rejectsMalformedHostBeforeAnyNetworkOperation(raw: String) = runTest {
        val service = DefaultNetworkDiagnosticsService(neverResolve, LanNetworkInfoProvider { error("snapshot") }, TcpConnector { _, _, _ -> error("connect") })
        assertEquals(LanError.INVALID_INPUT, (service.tcp(TcpCheckRequest(raw, 80)) as LanResult.Failure).code)
        assertEquals(LanError.INVALID_INPUT, (service.lookup(DnsLookupRequest(raw)) as LanResult.Failure).code)
    }

    @Test fun validatesScalarInputsAndRejectsNonUnicastTcpTargets() = runTest {
        val service = DefaultNetworkDiagnosticsService(neverResolve, networks(), TcpConnector { _, _, _ -> error("connect") })
        for (port in listOf(0, -1, 65536)) assertEquals(LanResult.Failure(LanError.INVALID_INPUT, "port"), service.tcp(TcpCheckRequest("127.0.0.1", port)))
        for (timeout in listOf(0L, 30001L)) assertEquals(LanResult.Failure(LanError.INVALID_INPUT, "timeout"), service.lookup(DnsLookupRequest("localhost", timeoutMillis = timeout)))
        for (host in listOf("0.0.0.0", "224.0.0.1", "255.255.255.255", "::")) assertEquals(LanResult.Failure(LanError.INVALID_INPUT, "host"), service.tcp(TcpCheckRequest(host, 80)))
        assertEquals("xn--bcher-kva.local", DiagnosticHost.parse("bücher.local"))
    }

    @Test fun measuresConnectSeparatelyAndSelectsOnlyFirstMatchingAddress() = runTest {
        var time = 10L
        var connections = 0
        val resolver = HostResolver { assertEquals("host.local", it); time += 1000; LanResult.Success(listOf(ip("10.0.0.2"), ip("10.0.0.3"))) }
        val service = DefaultNetworkDiagnosticsService(resolver, networks(), TcpConnector { address, port, source ->
            assertEquals(ip("10.0.0.2"), address); assertEquals(1234, port); assertNull(source); connections++; time += 50
        }, { time })
        val receipt = (service.tcp(TcpCheckRequest("HOST.local", 1234)) as LanResult.Success).value
        assertEquals(50, receipt.connectNanos)
        assertEquals(1, connections)
        val lookup = (service.lookup(DnsLookupRequest("host.local")) as LanResult.Success).value
        assertEquals(1000, lookup.lookupNanos)
        assertEquals(2, lookup.addresses.size)
    }

    @Test fun filtersDnsAnswersWithoutSendingTcpOrMisclassifyingNoRecords() = runTest {
        val resolver = HostResolver { LanResult.Success(listOf(ip("10.0.0.2"), ip("::1"), ip("10.0.0.2"))) }
        val service = DefaultNetworkDiagnosticsService(resolver, networks(), TcpConnector { _, _, _ -> error("connect") })
        val lookup = (service.lookup(DnsLookupRequest("host.local", AddressFamily.IPV6)) as LanResult.Success).value
        assertEquals(listOf(ip("::1")), lookup.addresses)
        val empty = DefaultNetworkDiagnosticsService(HostResolver { LanResult.Success(emptyList()) }, networks())
        assertEquals(LanResult.Failure(LanError.RESOLUTION_FAILED), empty.lookup(DnsLookupRequest("host.local")))
        assertEquals(listOf(ip("127.0.0.1")), (service.lookup(DnsLookupRequest("127.0.0.1")) as LanResult.Success).value.addresses)
    }

    @Test fun validatesSourceAndDetectsTopologyChangesBeforeConnect() = runTest {
        val source = network()
        val neverConnect = TcpConnector { _, _, _ -> error("connect") }
        val req = TcpCheckRequest("127.0.0.1", 80, source.binding.key)
        assertEquals(LanResult.Failure(LanError.INTERFACE_UNAVAILABLE), DefaultNetworkDiagnosticsService(neverResolve, networks(), neverConnect).tcp(req))
        val down = source.copy(binding = source.binding.copy(up = false))
        assertEquals(LanResult.Failure(LanError.INTERFACE_UNSUITABLE), DefaultNetworkDiagnosticsService(neverResolve, networks(down), neverConnect).tcp(req))
        var snapshots = 0
        val changed = LanNetworkInfoProvider { LanResult.Success(LanNetworkSnapshot(if (snapshots++ == 0) listOf(source) else emptyList())) }
        assertEquals(LanResult.Failure(LanError.NETWORK_CHANGED), DefaultNetworkDiagnosticsService(neverResolve, changed, neverConnect).tcp(req))
        val broadcast = req.copy(host = source.broadcast!!.value)
        assertEquals(LanResult.Failure(LanError.INVALID_INPUT, "host"), DefaultNetworkDiagnosticsService(neverResolve, networks(source), neverConnect).tcp(broadcast))
    }

    @Test fun mapsRefusalRoutesAndPermissionsWithoutRawExceptions() = runTest {
        for ((error, expected) in listOf(ConnectException("private") to LanError.CONNECTION_FAILED,
            NoRouteToHostException("private") to LanError.NETWORK_UNAVAILABLE, SecurityException("private") to LanError.PERMISSION_DENIED)) {
            val service = DefaultNetworkDiagnosticsService(neverResolve, networks(), TcpConnector { _, _, _ -> throw error })
            assertEquals(LanResult.Failure(expected), service.tcp(TcpCheckRequest("127.0.0.1", 1234)))
        }
    }

    @Test fun timeoutIncludesDnsAndCancellationReleasesCapacity() = runTest {
        val resolver = HostResolver { awaitCancellation() }
        val service = DefaultNetworkDiagnosticsService(resolver, networks(), TcpConnector { _, _, _ -> error("connect") })
        assertEquals(LanResult.Failure(LanError.TIMEOUT), service.tcp(TcpCheckRequest("host.local", 80, timeoutMillis = 100)))
        assertEquals(100, testScheduler.currentTime)
        val jobs = List(4) { async { service.lookup(DnsLookupRequest("host.local")) } }
        runCurrent()
        assertEquals(LanResult.Failure(LanError.RESOURCE_LIMIT), service.lookup(DnsLookupRequest("host.local")))
        jobs.forEach { it.cancelAndJoin() }
        assertTrue(service.lookup(DnsLookupRequest("127.0.0.1")) is LanResult.Success)
        var propagated = false
        try { withTimeout(10) { service.lookup(DnsLookupRequest("host.local")) } } catch (_: TimeoutCancellationException) { propagated = true }
        assertTrue(propagated)
    }

    @Test fun cancellationWinsOverConcurrentIoErrorAndRunsCleanup() = runTest {
        var closed = false
        val service = DefaultNetworkDiagnosticsService(neverResolve, networks(), TcpConnector { _, _, _ ->
            try { currentCoroutineContext().cancel(); throw ConnectException() } finally { closed = true }
        })
        val job = async { service.tcp(TcpCheckRequest("127.0.0.1", 80)) }
        job.join()
        assertTrue(job.isCancelled)
        assertTrue(closed)
    }
}
