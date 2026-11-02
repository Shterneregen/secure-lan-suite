package com.shterneregen.securelan.desktop.network.discovery

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class LinuxDefaultRouteHintProviderTest {
    @TempDir
    lateinit var directory: Path

    private val header = "Iface Destination Gateway Flags RefCnt Use Metric Mask MTU Window IRTT"

    private fun route(name: String, metric: String = "100", flags: String = "0003", destination: String = "00000000", mask: String = "00000000") =
        "$name\t$destination\t010AA8C0\t$flags\t0\t0\t$metric\t$mask\t0\t0\t0"

    @Test
    fun prefersUniqueLowestMetricAndIgnoresNonDefaultDownAndRejectedRoutes() {
        val lines = listOf(header, route("eth0", "200"), route("wifi0", "50"), route("down", "0", "0002"),
            route("reject", "0", "0201"), route("subnet", "0", destination = "000AA8C0", mask = "00FFFFFF"))
        assertEquals("wifi0", LinuxDefaultRouteHintProvider.parse(lines))
    }

    @Test
    fun ambiguousRoutesReturnNoHintButDuplicateRoutesOnOneInterfaceAreFine() {
        assertNull(LinuxDefaultRouteHintProvider.parse(listOf(header, route("eth0"), route("wifi0"))))
        assertEquals("eth0", LinuxDefaultRouteHintProvider.parse(listOf(header, route("eth0"), route("eth0"))))
    }

    @Test
    fun ignoresMalformedRowsAndRejectsUnknownHeaderOrOversizedTable() {
        val lines = listOf(header, "broken row", route("invalid", "-1"), route("invalid", "NaN"),
            route("invalid", flags = "invalid"), route("eth0"))
        assertEquals("eth0", LinuxDefaultRouteHintProvider.parse(lines))
        assertNull(LinuxDefaultRouteHintProvider.parse(listOf("unknown header", route("eth0"))))
        assertNull(LinuxDefaultRouteHintProvider.parse(listOf(header) + List(513) { route("eth0") }))
        assertEquals("eth0", LinuxDefaultRouteHintProvider.parse(listOf(header) + List(512) { route("eth0") } + ""))
    }

    @Test
    fun rereadsTopologyInsteadOfCachingFirstResult() {
        val file = directory.resolve("route")
        val provider = LinuxDefaultRouteHintProvider(file, "Linux")
        Files.writeString(file, "$header\n${route("eth0")}\n")
        assertEquals("eth0", provider.interfaceKey())
        Files.writeString(file, "$header\n${route("wifi0")}\n")
        assertEquals("wifi0", provider.interfaceKey())
    }

    @Test
    fun missingUnsupportedAndOversizedSourcesReturnNoHint() {
        val file = directory.resolve("route")
        assertNull(LinuxDefaultRouteHintProvider(file, "Linux").interfaceKey())
        Files.writeString(file, "$header\n${route("eth0")}\n")
        assertNull(LinuxDefaultRouteHintProvider(file, "Windows 11").interfaceKey())
        Files.writeString(file, "a".repeat(65_537))
        assertNull(LinuxDefaultRouteHintProvider(file, "Linux").interfaceKey())
    }
}
