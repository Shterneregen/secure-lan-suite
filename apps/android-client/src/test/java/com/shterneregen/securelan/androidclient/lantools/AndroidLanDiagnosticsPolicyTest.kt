package com.shterneregen.securelan.androidclient.lantools

import com.shterneregen.securelan.androidclient.lantools.network.AndroidLanNetworkInfoProvider
import com.shterneregen.securelan.lantools.diagnostics.*
import com.shterneregen.securelan.lantools.model.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import java.net.NoRouteToHostException

/** Reproduces the reported addresses with injected transport, never probing a user's NAS. */
@OptIn(ExperimentalCoroutinesApi::class)
class AndroidLanDiagnosticsPolicyTest {
    private val wifi = link("101:192.168.31.123", "101", "192.168.31.123").copy(name = "wlan0", gateways = listOf(ip("192.168.31.1")), dns = listOf("192.168.31.1"))
    private val target = "192.168.31.138"

    @Test fun wifiFactsDoNotRequireBroadcastForAnExplicitTcpSource() = runTest {
        val platform = FakeLanPlatform(listOf(wifi.copy(broadcast = null)))
        val provider = AndroidLanNetworkInfoProvider(platform, StandardTestDispatcher(testScheduler))
        var calls = 0
        val service = DefaultNetworkDiagnosticsService(HostResolver { error("numeric target") }, provider, TcpConnector { address, port, source ->
            assertEquals(ip(target), address); assertEquals(443, port); assertEquals(wifi.binding.address, source); calls++
        })
        assertTrue(service.tcp(TcpCheckRequest(target, 443, wifi.binding.key)) is LanResult.Success)
        assertEquals(1, calls)
    }

    @Test fun osRoutingKeepsSourceNullEvenWithVpnAndSeveralLanRecords() = runTest {
        val platform = FakeLanPlatform(listOf(wifi, link("102:10.0.0.2", "102", "10.0.0.2"))).apply { vpn = true }
        val provider = AndroidLanNetworkInfoProvider(platform, StandardTestDispatcher(testScheduler))
        var calls = 0
        val service = DefaultNetworkDiagnosticsService(HostResolver { error("numeric") }, provider, TcpConnector { _, _, source -> assertNull(source); calls++ })
        assertTrue(service.tcp(TcpCheckRequest(target, 443)) is LanResult.Success)
        assertEquals(1, calls)
    }

    @Test fun osNoRouteIsNotInventedFromMissingWifiMetadataOrPortRefusal() = runTest {
        val platform = FakeLanPlatform(listOf(wifi))
        val provider = AndroidLanNetworkInfoProvider(platform, StandardTestDispatcher(testScheduler))
        val service = DefaultNetworkDiagnosticsService(HostResolver { error("numeric") }, provider, TcpConnector { _, port, source ->
            assertEquals(1, port); assertNull(source); throw NoRouteToHostException("OS transport evidence")
        })
        assertEquals(LanResult.Failure(LanError.NETWORK_UNAVAILABLE), service.tcp(TcpCheckRequest(target, 1)))
    }

    @Test fun reconnectAndMissingOrDuplicateBindingNeverFallBackToAnotherNetwork() = runTest {
        val platform = FakeLanPlatform(listOf(wifi))
        val provider = AndroidLanNetworkInfoProvider(platform, StandardTestDispatcher(testScheduler))
        val service = DefaultNetworkDiagnosticsService(HostResolver { error("numeric") }, provider, TcpConnector { _, _, _ -> error("must not connect") })
        for (entries in listOf(emptyList(), listOf(wifi.copy(binding = wifi.binding.copy(key = "new", interfaceKey = "new"))), listOf(wifi, wifi))) {
            platform.entries = entries
            assertEquals(LanResult.Failure(LanError.INTERFACE_UNAVAILABLE), service.tcp(TcpCheckRequest(target, 443, wifi.binding.key)))
        }
    }

    @Test fun lostNetworkBetweenSnapshotsAndPermissionDenialRemainTypedFailures() = runTest {
        val platform = FakeLanPlatform(listOf(wifi))
        val provider = AndroidLanNetworkInfoProvider(platform, StandardTestDispatcher(testScheduler))
        var reads = 0
        val changing = LanNetworkInfoProvider {
            if (++reads == 2) platform.entries = emptyList()
            provider.snapshot()
        }
        val connector = TcpConnector { _, _, _ -> error("must not connect") }
        val request = TcpCheckRequest(target, 443, wifi.binding.key)
        assertEquals(LanResult.Failure(LanError.NETWORK_CHANGED), DefaultNetworkDiagnosticsService(HostResolver { error("numeric") }, changing, connector).tcp(request))
        val denied = LanNetworkInfoProvider { LanResult.Failure(LanError.PERMISSION_DENIED) }
        assertEquals(LanResult.Failure(LanError.PERMISSION_DENIED), DefaultNetworkDiagnosticsService(HostResolver { error("numeric") }, denied, connector).tcp(request))
    }
}
