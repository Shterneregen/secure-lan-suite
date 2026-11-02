package com.shterneregen.securelan.androidclient.lantools

import androidx.lifecycle.ViewModelStore
import com.shterneregen.securelan.androidclient.R
import com.shterneregen.securelan.androidclient.lantools.network.AndroidDeviceBrowser
import com.shterneregen.securelan.lantools.diagnostics.*
import com.shterneregen.securelan.lantools.discovery.DiscoveredLanDevice
import com.shterneregen.securelan.lantools.discovery.LanDiscoverySnapshot
import com.shterneregen.securelan.lantools.model.*
import com.shterneregen.securelan.lantools.wol.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class LanToolsViewModelTest {
    private class Fixture(val test: TestScope) {
        val platform = FakeLanPlatform()
        val store = FakeLanStore()
        var browses = 0
        var wakes = 0
        var released = false
        var tcpCalls = mutableListOf<TcpCheckRequest>()
        var tcp: suspend (TcpCheckRequest) -> LanResult<TcpCheckReceipt> = { LanResult.Success(TcpCheckReceipt(ip(it.host), it.port, null, 1_000_000)) }
        var browse: suspend (suspend (LanDiscoverySnapshot) -> Unit) -> LanResult<LanDiscoverySnapshot> = { awaitCancellation() }
        val model = LanToolsViewModel(LanToolsServices(platform, DiagnosticsNetworkInfoProvider { LanResult.Success(platform.snapshot()) },
            object : WakeOnLanService { override suspend fun send(request: WakeRequest): LanResult<WakeReceipt> { wakes++; return LanResult.Failure(LanError.INTERFACE_UNAVAILABLE) } },
            object : NetworkDiagnosticsService {
                override suspend fun tcp(request: TcpCheckRequest): LanResult<TcpCheckReceipt> { tcpCalls += request; return this@Fixture.tcp(request) }
                override suspend fun lookup(request: DnsLookupRequest): LanResult<DnsLookupReceipt> = LanResult.Failure(LanError.RESOLUTION_FAILED)
            }, AndroidDeviceBrowser { _, update -> browses++; browse(update) }, { released = true }), store,
            StandardTestDispatcher(test.testScheduler), StandardTestDispatcher(test.testScheduler))
        val owner = ViewModelStore().apply { put("lan", model) }
        fun close() = owner.clear()
        fun open() { model.setActive(true); test.runCurrent() }
    }
    @Test fun entryDoesNotBrowseOrWakeAndReentryPreservesFormAndInput() = runTest {
        val f = Fixture(this)
        try {
            f.open(); f.model.add(); f.model.changeForm(f.model.state.value.form!!.copy(name = "PC"))
            f.model.editInput { it.copy(host = "192.168.1.20", port = "443") }
            f.model.setActive(false); f.open()
            assertEquals(0, f.browses); assertEquals(0, f.wakes)
            assertEquals("PC", f.model.state.value.form!!.name)
            assertEquals("443", f.model.state.value.input.port)
            assertEquals(2, f.platform.starts); assertEquals(1, f.platform.closes)
        } finally { f.close() }
        assertTrue(f.released)
    }
    @Test fun permissionGrantDoesNotStartDiscoveryAndRevocationCancelsIt() = runTest {
        val f = Fixture(this)
        try {
            f.open(); f.model.discover(); runCurrent()
            assertEquals(R.string.lan_nearby_required, f.model.state.value.outcome(LanOperation.DISCOVERY).message!!.resource)
            f.model.permission(true); runCurrent(); assertEquals(0, f.browses)
            f.model.discover(); runCurrent(); assertEquals(1, f.browses)
            f.model.permission(false); runCurrent()
            assertEquals(LanPhase.CANCELLED, f.model.state.value.outcome(LanOperation.DISCOVERY).phase)
        } finally { f.close() }
    }
    @Test fun failedLoadBlocksWritesAndFailedSaveRetainsForm() = runTest {
        val f = Fixture(this)
        try {
            f.store.failLoad = true; f.open(); f.model.add(); f.model.save(); runCurrent()
            assertFalse(f.model.state.value.canModify); assertNull(f.model.state.value.form); assertEquals(0, f.store.saves)
            f.store.failLoad = false; f.model.reload(); runCurrent(); f.model.add()
            f.model.changeForm(f.model.state.value.form!!.copy(name = "PC", mac = "02:11:22:33:44:55"))
            f.store.failSave = true; f.model.save(); runCurrent()
            assertTrue(f.model.state.value.devices.isEmpty()); assertNotNull(f.model.state.value.form)
            f.store.failSave = false; f.model.save(); runCurrent()
            assertEquals(1, f.model.state.value.devices.size); assertNull(f.model.state.value.form)
            val id = f.model.state.value.devices.single().id
            f.model.requestDelete(id); assertEquals(1, f.store.devices.size)
            f.model.requestDelete(null); assertEquals(1, f.store.devices.size)
            f.model.requestDelete(id); f.model.delete(); runCurrent(); assertTrue(f.store.devices.isEmpty())
        } finally { f.close() }
    }
    @Test fun cancellationClosesWorkAndLateDiscoveryCannotReplaceNewRun() = runTest {
        val f = Fixture(this)
        val late = CompletableDeferred<Unit>()
        val oldSnapshot = LanDiscoverySnapshot(partial = true)
        var attempts = 0
        f.browse = { update ->
            if (++attempts == 1) withContext(NonCancellable) { late.await(); update(oldSnapshot); LanResult.Success(oldSnapshot) }
            else { update(LanDiscoverySnapshot()); awaitCancellation() }
        }
        try {
            f.open(); f.model.permission(true); f.model.discover(); runCurrent()
            f.model.cancel(LanOperation.DISCOVERY); f.model.discover(); runCurrent()
            late.complete(Unit); runCurrent()
            assertFalse(f.model.state.value.found.partial)
            assertEquals(LanPhase.RUNNING, f.model.state.value.outcome(LanOperation.DISCOVERY).phase)
            f.model.setActive(false); runCurrent()
            assertEquals(LanPhase.CANCELLED, f.model.state.value.outcome(LanOperation.DISCOVERY).phase)
        } finally { late.complete(Unit); f.close() }
    }
    @Test fun tcpValidatesBeforeIoAndRecordsCapturedEndpoint() = runTest {
        val f = Fixture(this)
        val pending = CompletableDeferred<Unit>()
        val device = SavedLanDevice(UUID.randomUUID(), "PC", ip("192.168.1.20"))
        f.store.devices = listOf(device)
        f.tcp = { pending.await(); LanResult.Failure(LanError.CONNECTION_FAILED) }
        try {
            f.open(); f.model.diagnoseSaved(device.id); f.model.tcp(); runCurrent(); assertTrue(f.tcpCalls.isEmpty())
            f.model.editInput { it.copy(port = "443") }; f.model.tcp(); runCurrent()
            f.model.editInput { it.copy(host = "192.168.1.99", port = "80") }; pending.complete(Unit); runCurrent()
            val check = f.model.state.value.checks["saved:${device.id}"]!!
            assertEquals("192.168.1.20", check.host); assertEquals(443, check.port)
            assertEquals(LanPhase.FAILURE, check.outcome.phase)
        } finally { pending.complete(Unit); f.close() }
    }
    @Test fun backgroundAndDeadlineCancelTcpAndReleaseItsResources() = runTest {
        val f = Fixture(this)
        var closed = 0
        f.tcp = { try { awaitCancellation() } finally { closed++ } }
        try {
            f.open(); f.model.editInput { it.copy(host = "192.168.1.20", port = "443", timeout = "100") }
            f.model.tcp(); runCurrent(); advanceTimeBy(101); runCurrent()
            assertEquals(1, closed); assertEquals(LanPhase.FAILURE, f.model.state.value.outcome(LanOperation.TCP).phase)
            f.model.tcp(); runCurrent(); f.model.setActive(false); runCurrent()
            assertEquals(2, closed); assertEquals(LanPhase.CANCELLED, f.model.state.value.outcome(LanOperation.TCP).phase)
        } finally { f.close() }
    }
    @Test fun gatewayMustStillBeReportedBySameInterface() = runTest {
        val f = Fixture(this)
        try {
            f.open(); f.model.binding(f.platform.entries.single().binding.key)
            f.model.editInput { it.copy(gateway = "192.168.1.1", gatewayPort = "443") }
            f.platform.entries = listOf(link().copy(gateways = emptyList()))
            f.model.gateway(); runCurrent()
            assertTrue(f.tcpCalls.isEmpty())
            assertEquals(LanPhase.FAILURE, f.model.state.value.outcome(LanOperation.GATEWAY).phase)
        } finally { f.close() }
    }
    @Test fun failedDiscoveryDoesNotLeaveObservedRowsCurrent() = runTest {
        val f = Fixture(this)
        f.browse = { update ->
            update(LanDiscoverySnapshot(listOf(DiscoveredLanDevice("test", "wifi", "PC", null, listOf(ip("192.168.1.20")), emptyList(), setOf("mDNS/DNS-SD"), Instant.EPOCH))))
            LanResult.Failure(LanError.PERMISSION_DENIED)
        }
        try {
            f.open(); f.model.permission(true); f.model.discover(); runCurrent()
            assertEquals(LanPhase.FAILURE, f.model.state.value.outcome(LanOperation.DISCOVERY).phase)
            assertTrue(f.model.state.value.found.devices.single().stale)
        } finally { f.close() }
    }
}
