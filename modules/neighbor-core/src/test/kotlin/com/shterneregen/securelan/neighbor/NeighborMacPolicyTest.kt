package com.shterneregen.securelan.neighbor

import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.common.net.interfaces.NetworkBinding
import com.shterneregen.securelan.common.net.interfaces.NetworkKind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class NeighborMacPolicyTest {
    private fun ip(raw: String) = requireNotNull(IpLiteral.parse(raw))

    private fun binding(raw: String, prefix: Int = 24, kind: NetworkKind = NetworkKind.LAN, up: Boolean = true) =
        NetworkBinding("eth0:$raw", "eth0", ip(raw), prefix, kind, up)

    @Test
    fun acceptsOneOnLinkUnicastIpv4Target() {
        assertNull(NeighborMacPolicy.rejection(NeighborQuery(ip("192.168.1.20"), binding("192.168.1.42"))))
    }

    @Test
    fun rejectsInvalidOrSelfTargets() {
        val source = binding("192.168.1.42")
        assertEquals(NeighborMacResult.InvalidTarget, NeighborMacPolicy.rejection(NeighborQuery(ip("192.168.1.42"), source)))
        assertEquals(NeighborMacResult.InvalidTarget, NeighborMacPolicy.rejection(NeighborQuery(ip("224.0.0.1"), source)))
        assertEquals(NeighborMacResult.InvalidTarget, NeighborMacPolicy.rejection(NeighborQuery(ip("192.168.1.255"), source)))
        assertEquals(NeighborMacResult.InvalidTarget, NeighborMacPolicy.rejection(NeighborQuery(ip("::1"), source)))
    }

    @Test
    fun rejectsOffLinkAndUnsuitableBindings() {
        val source = binding("192.168.1.42")
        assertEquals(NeighborMacResult.OffLink, NeighborMacPolicy.rejection(NeighborQuery(ip("192.168.2.20"), source)))
        assertEquals(NeighborMacResult.OffLink,
            NeighborMacPolicy.rejection(NeighborQuery(ip("192.168.1.20"), binding("192.168.1.42", prefix = 32))))
        assertEquals(NeighborMacResult.Failed(NeighborFailure.INTERFACE_UNAVAILABLE),
            NeighborMacPolicy.rejection(NeighborQuery(ip("192.168.1.20"), binding("192.168.1.42", kind = NetworkKind.VPN))))
        assertEquals(NeighborMacResult.Failed(NeighborFailure.INTERFACE_UNAVAILABLE),
            NeighborMacPolicy.rejection(NeighborQuery(ip("192.168.1.20"), binding("192.168.1.42", up = false))))
    }
}
