package com.shterneregen.securelan.desktop.lantools

import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.common.net.interfaces.NetworkBinding
import com.shterneregen.securelan.desktop.compose.state.lantools.*
import com.shterneregen.securelan.lantools.diagnostics.*
import com.shterneregen.securelan.lantools.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DesktopDiagnosticsControllerTest {
    private val ip = IpLiteral.parse("192.168.1.42")!!
    private val gateway = IpLiteral.parse("192.168.1.1")!!
    private val source = LanInterfaceSnapshot(NetworkBinding("eth0:$ip", "eth0", ip, 24), "Ethernet", true, null)
    private val snapshot = DiagnosticsNetworkSnapshot(listOf(DiagnosticInterface(source, listOf(gateway), listOf(gateway.value), MetadataAvailability.AVAILABLE)), "fixture")
    private val provider = DiagnosticsNetworkInfoProvider { LanResult.Success(snapshot) }
    private val success = object : NetworkDiagnosticsService {
        override suspend fun tcp(request: TcpCheckRequest) = LanResult.Success(TcpCheckReceipt(IpLiteral.parse(request.host) ?: ip, request.port, ip, 1200000))
        override suspend fun lookup(request: DnsLookupRequest) = LanResult.Success(DnsLookupReceipt(request.host, listOf(ip), 2300000))
    }

    @Test fun validatesFormsAndExplainsSuccessWithoutInferringPowerOrIcmp() = runTest {
        val controller = DesktopDiagnosticsController(success, provider, StandardTestDispatcher(testScheduler))
        controller.open(); runCurrent()
        controller.checkTcp()
        assertEquals(DiagnosticPhase.FAILURE, controller.state.value.outcome(DiagnosticKind.TCP).phase)
        controller.edit { it.copy(tcpHost = "192.168.1.42", tcpPort = "1234", dnsHost = "host.local") }
        controller.checkTcp(); controller.lookupDns(); runCurrent()
        assertTrue(controller.state.value.outcome(DiagnosticKind.TCP).message.contains("not ICMP"))
        assertTrue(controller.state.value.outcome(DiagnosticKind.DNS).message.contains("not a connectivity check"))
        controller.close()
    }

    @Test fun gatewayRequiresOsMetadataAndAnExplicitPortAndRechecksTopology() = runTest {
        var current = snapshot
        var calls = 0
        val service = object : NetworkDiagnosticsService by success {
            override suspend fun tcp(request: TcpCheckRequest): LanResult<TcpCheckReceipt> { calls++; return success.tcp(request) }
        }
        val controller = DesktopDiagnosticsController(service, DiagnosticsNetworkInfoProvider { LanResult.Success(current) }, StandardTestDispatcher(testScheduler))
        controller.open(); runCurrent(); controller.checkGateway()
        assertEquals(0, calls)
        controller.selectInterface(source.binding.key)
        controller.edit { it.copy(gateway = gateway.value, gatewayPort = "1234") }
        current = snapshot.copy(interfaces = emptyList())
        controller.checkGateway(); runCurrent()
        assertEquals(0, calls)
        current = snapshot
        controller.checkGateway(); runCurrent()
        assertEquals(1, calls)
        assertTrue(controller.state.value.outcome(DiagnosticKind.GATEWAY).message.contains("Gateway TCP"))
        controller.close()
    }

    @Test fun cancelAndDismissRetainFormsAndRejectLateLookupResults() = runTest {
        val service = object : NetworkDiagnosticsService by success {
            override suspend fun lookup(request: DnsLookupRequest): LanResult<DnsLookupReceipt> {
                withContext(NonCancellable) { delay(100) }
                return success.lookup(request)
            }
        }
        val controller = DesktopDiagnosticsController(service, provider, StandardTestDispatcher(testScheduler))
        controller.open(); runCurrent(); controller.edit { it.copy(dnsHost = "host.local") }
        controller.lookupDns(); runCurrent()
        assertEquals(DiagnosticPhase.RUNNING, controller.state.value.outcome(DiagnosticKind.DNS).phase)
        controller.dismiss(); advanceUntilIdle()
        assertEquals(DiagnosticPhase.CANCELLED, controller.state.value.outcome(DiagnosticKind.DNS).phase)
        controller.open(); runCurrent()
        assertEquals("host.local", controller.state.value.inputs.dnsHost)
        controller.close()
    }

    @Test fun aCancelledLocalRefreshCannotReplaceTheNextSessionsSnapshot() = runTest {
        var calls = 0
        val fresh = snapshot.copy(provenance = "new session")
        val provider = DiagnosticsNetworkInfoProvider {
            if (++calls == 1) {
                // Model a native adapter that returns after its original session was cancelled.
                withContext(NonCancellable) { delay(100) }
                LanResult.Success(snapshot)
            } else LanResult.Success(fresh)
        }
        val controller = DesktopDiagnosticsController(success, provider, StandardTestDispatcher(testScheduler))
        controller.open(); runCurrent(); controller.dismiss(); controller.open(); runCurrent()
        assertEquals(fresh, controller.state.value.network)
        advanceUntilIdle()
        assertEquals(fresh, controller.state.value.network)
        assertEquals(DiagnosticPhase.SUCCESS, controller.state.value.outcome(DiagnosticKind.NETWORK).phase)
        controller.close()
    }

    @Test fun deviceHistoryCapturesTheSubmittedEndpointAndDoesNotFollowSubsequentEdits() = runTest {
        val service = object : NetworkDiagnosticsService by success {
            override suspend fun tcp(request: TcpCheckRequest): LanResult<TcpCheckReceipt> {
                delay(100)
                return success.tcp(request)
            }
        }
        val controller = DesktopDiagnosticsController(service, provider, StandardTestDispatcher(testScheduler))
        controller.open(); runCurrent()
        controller.prepareTcp("192.168.1.42", 631, source.binding.key, "found:printer")
        assertTrue(controller.state.value.deviceChecks.isEmpty())
        controller.checkTcp(); runCurrent()
        controller.prepareTcp("192.168.1.50", 80, null, "found:other")
        advanceUntilIdle()
        val history = controller.state.value.deviceChecks.getValue("found:printer")
        assertEquals("192.168.1.42", history.host)
        assertEquals(631, history.port)
        assertFalse(controller.state.value.deviceChecks.containsKey("found:other"))
        controller.edit { it.copy(tcpHost = "192.168.1.60") }
        assertNull(controller.state.value.inputs.deviceKey)
        controller.close()
    }

    @Test fun aSavedDeviceCanChooseAPortAndItsHistoryRemainsBounded() = runTest {
        val controller = DesktopDiagnosticsController(success, provider, StandardTestDispatcher(testScheduler))
        controller.open(); runCurrent()
        repeat(260) { index ->
            controller.prepareTcp("192.168.1.42", null, null, "saved:$index")
            controller.edit { it.copy(tcpPort = "1234") }
            controller.checkTcp(); runCurrent()
        }
        assertEquals(256, controller.state.value.deviceChecks.size)
        assertFalse(controller.state.value.deviceChecks.containsKey("saved:0"))
        assertEquals(1234, controller.state.value.deviceChecks.getValue("saved:259").port)
        controller.close()
    }

    @Test fun failureAndTimeoutStatesAreRecoverableAndDoNotBlockOtherChecks() = runTest {
        var slow = true
        val service = object : NetworkDiagnosticsService by success {
            override suspend fun tcp(request: TcpCheckRequest): LanResult<TcpCheckReceipt> =
                if (slow) { awaitCancellation() } else LanResult.Failure(LanError.CONNECTION_FAILED)
        }
        val controller = DesktopDiagnosticsController(service, provider, StandardTestDispatcher(testScheduler))
        controller.open(); runCurrent(); controller.edit { it.copy(tcpHost = "host.local", tcpPort = "1234", timeoutMillis = "50", dnsHost = "host.local") }
        controller.checkTcp(); controller.lookupDns(); advanceUntilIdle()
        assertEquals(DiagnosticPhase.FAILURE, controller.state.value.outcome(DiagnosticKind.TCP).phase)
        assertEquals(DiagnosticPhase.SUCCESS, controller.state.value.outcome(DiagnosticKind.DNS).phase)
        slow = false
        controller.checkTcp(); runCurrent()
        assertTrue(controller.state.value.outcome(DiagnosticKind.TCP).message.contains("may still be running"))
        controller.close()
    }
}
