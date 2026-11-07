package com.shterneregen.securelan.neighbor

import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.common.net.address.MacAddress
import com.shterneregen.securelan.common.net.interfaces.NetworkBinding
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class LinuxNeighborResolverTest {
    private val target = requireNotNull(IpLiteral.parse("192.168.1.20"))
    private val binding = NetworkBinding("eth0:192.168.1.42", "eth0", requireNotNull(IpLiteral.parse("192.168.1.42")), 24)
    private val mac = requireNotNull(MacAddress.parse("02:11:22:33:44:55"))
    private val table = """
        IP address       HW type     Flags       HW address            Mask     Device
        192.168.1.20     0x1         0x2         02:11:22:33:44:55     *        eth0
    """.trimIndent()

    @Test
    fun parsesCompleteEntryForTargetAndInterface() {
        assertEquals(mac, LinuxNeighborResolver.parseLinuxArpTable(table, target, "eth0"))
        assertNull(LinuxNeighborResolver.parseLinuxArpTable(table, target, "wlan0"))
        assertNull(LinuxNeighborResolver.parseLinuxArpTable(table, requireNotNull(IpLiteral.parse("192.168.1.21")), "eth0"))
    }

    @Test
    fun ignoresIncompleteZeroAndMulticastEntries() {
        val incomplete = table.replace("0x2", "0x0")
        val zero = table.replace("02:11:22:33:44:55", "00:00:00:00:00:00")
        val multicast = table.replace("02:11:22:33:44:55", "01:11:22:33:44:55")
        assertNull(LinuxNeighborResolver.parseLinuxArpTable(incomplete, target, "eth0"))
        assertNull(LinuxNeighborResolver.parseLinuxArpTable(zero, target, "eth0"))
        assertNull(LinuxNeighborResolver.parseLinuxArpTable(multicast, target, "eth0"))
    }

    @Test
    fun ignoresMalformedRows() {
        assertNull(LinuxNeighborResolver.parseLinuxArpTable("192.168.1.20 0x1", target, "eth0"))
        assertNull(LinuxNeighborResolver.parseLinuxArpTable(table.replace("0x2", "zz"), target, "eth0"))
    }

    @Test
    fun resolverReadsInjectedTable(@TempDir directory: Path) {
        val file = directory.resolve("arp")
        Files.writeString(file, table)
        val resolver = LinuxNeighborResolver(file, NeighborProbe { }, { }, attempts = 1)
        assertEquals(NeighborMacResult.Found(mac), resolver.resolve(NeighborQuery(target, binding)))
    }

    @Test
    fun resolverReportsMissingTableAsIoFailure(@TempDir directory: Path) {
        val resolver = LinuxNeighborResolver(directory.resolve("missing"), NeighborProbe { }, { }, attempts = 1)
        assertEquals(NeighborMacResult.Failed(NeighborFailure.IO), resolver.resolve(NeighborQuery(target, binding)))
    }
}
