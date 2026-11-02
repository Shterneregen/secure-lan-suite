package com.shterneregen.securelan.desktop.lantools

import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.common.net.interfaces.NetworkBinding
import com.shterneregen.securelan.lantools.diagnostics.MetadataAvailability
import com.shterneregen.securelan.lantools.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class WindowsNetworkInfoBackendTest {
    private val fixture = "SecureLanNetworkInfo/1\r\n192.168.1.42|192.168.1.1|192.168.1.1,fe80::1%12\r\n10.0.0.42||\r\n"

    @Test fun decodesMultipleInterfacesEmptyMetadataAndScopedIpv6Dns() {
        val result = WindowsNetworkInfoBackend.parse(fixture)
        assertEquals(MetadataAvailability.AVAILABLE, result.availability)
        assertEquals(listOf(IpLiteral.parse("192.168.1.1")), result.records.getValue("192.168.1.42").gateways)
        assertEquals(listOf("192.168.1.1", "fe80::1%12"), result.records.getValue("192.168.1.42").dnsServers)
        assertTrue(result.records.getValue("10.0.0.42").gateways.isEmpty())
    }

    @Test fun rejectsMalformedConflictingAndOversizedOutputWithoutPartialRecords() {
        val variants = listOf("unexpected\n", fixture + "bad row", fixture + "192.168.1.42|10.0.0.1|\n",
            "SecureLanNetworkInfo/1\n192.168.1.42|not-ip|\n", "SecureLanNetworkInfo/1\n192.168.1.42||fe80::1%oops\n",
            "x".repeat(131073), "SecureLanNetworkInfo/1\n" + "10.0.0.42||\n".repeat(513))
        variants.forEach { assertEquals(PlatformNetworkInfo(MetadataAvailability.UNAVAILABLE), WindowsNetworkInfoBackend.parse(it)) }
    }

    @Test fun unavailableAndTimedOutBackendsDoNotInventRoutes() = runTest {
        assertEquals(MetadataAvailability.UNSUPPORTED, WindowsNetworkInfoBackend("Linux") { error("must not query") }.read().availability)
        assertEquals(MetadataAvailability.PERMISSION_DENIED, WindowsNetworkInfoBackend("Windows") { throw SecurityException() }.read().availability)
        var cleaned = false
        val slow = WindowsNetworkInfoBackend("Windows") { try { awaitCancellation() } finally { cleaned = true } }
        assertEquals(MetadataAvailability.TIMEOUT, slow.read().availability)
        assertTrue(cleaned)
        assertEquals(5000, testScheduler.currentTime)
    }

    @Test fun joinsOnlyMatchingLocalAddressAndLeavesWolSnapshotsUntouched() = runTest {
        val ip = IpLiteral.parse("192.168.1.42")!!
        val source = LanInterfaceSnapshot(NetworkBinding("eth0:$ip", "eth0", ip, 24), "Ethernet", true, IpLiteral.parse("192.168.1.255"))
        val base = LanNetworkSnapshot(listOf(source))
        val provider = DesktopNetworkInfoProvider(LanNetworkInfoProvider { LanResult.Success(base) }, WindowsNetworkInfoBackend("Windows") { fixture })
        val snapshot = (provider.snapshot() as LanResult.Success).value
        assertEquals(source, snapshot.interfaces.single().source)
        assertEquals(source, base.interfaces.single())
        assertEquals(MetadataAvailability.AVAILABLE, snapshot.interfaces.single().metadata)
    }

    @Test fun boundedProcessIsDestroyedAndStreamsClosedOnCancelAndOutputLimit() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val waiting = FakeProcess(ByteArray(0), true)
        assertNull(withTimeoutOrNull(50) { runBoundedPlatformProcess({ waiting }, dispatcher) })
        assertTrue(waiting.destroyed); assertTrue(waiting.inputClosed)
        val oversized = FakeProcess(ByteArray(131073) { 'x'.code.toByte() }, true)
        try { runBoundedPlatformProcess({ oversized }, dispatcher); fail("Expected output limit") } catch (_: java.io.IOException) { }
        assertTrue(oversized.destroyed); assertTrue(oversized.inputClosed)
        val complete = FakeProcess(fixture.toByteArray(), false)
        assertEquals(fixture, runBoundedPlatformProcess({ complete }, dispatcher))
        assertTrue(complete.inputClosed)
    }

    private class FakeProcess(bytes: ByteArray, var running: Boolean) : Process() {
        var destroyed = false
        var inputClosed = false
        val input = object : ByteArrayInputStream(bytes) { override fun close() { inputClosed = true; super.close() } }
        override fun getInputStream(): InputStream = input
        override fun getErrorStream(): InputStream = ByteArrayInputStream(ByteArray(0))
        override fun getOutputStream() = ByteArrayOutputStream()
        override fun isAlive(): Boolean = running
        override fun waitFor(): Int = 0
        override fun waitFor(timeout: Long, unit: TimeUnit): Boolean = !running
        override fun exitValue(): Int = 0
        override fun destroy() { running = false; destroyed = true }
        override fun destroyForcibly(): Process { destroy(); return this }
    }
}
