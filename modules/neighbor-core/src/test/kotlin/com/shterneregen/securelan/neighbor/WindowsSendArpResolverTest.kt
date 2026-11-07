package com.shterneregen.securelan.neighbor

import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.common.net.address.MacAddress
import com.shterneregen.securelan.common.net.interfaces.NetworkBinding
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WindowsSendArpResolverTest {
    private val target = requireNotNull(IpLiteral.parse("192.168.31.138"))
    private val binding = NetworkBinding("eth0:192.168.31.199", "eth0",
        requireNotNull(IpLiteral.parse("192.168.31.199")), 24)
    private val mac = requireNotNull(MacAddress.parse("02:11:22:33:44:55"))

    @Test
    fun convertsIpv4ToNetworkOrderValue() {
        assertEquals(0xC0A81F8A.toInt(), WindowsSendArpResolver.ipv4ToInt(target))
    }

    @Test
    fun returnsFoundForCompleteSuccessfulResponse() {
        val resolver = WindowsSendArpResolver { _, _, buffer, length ->
            mac.bytes().copyInto(buffer)
            length.value = 6
            WindowsSendArpResolver.NO_ERROR
        }
        assertEquals(NeighborMacResult.Found(mac), resolver.resolve(NeighborQuery(target, binding)))
    }

    @Test
    fun mapsErrorCodesAndRejectsNonUnicastResults() {
        fun resolverReturning(code: Int, bytes: ByteArray = mac.bytes()) = WindowsSendArpResolver { _, _, buffer, length ->
            bytes.copyInto(buffer)
            length.value = bytes.size
            code
        }
        assertEquals(NeighborMacResult.Found(mac), resolverReturning(0).resolve(NeighborQuery(target, binding)))
        assertEquals(NeighborMacResult.NotFound, resolverReturning(WindowsSendArpResolver.ERROR_BAD_NET_RESP)
            .resolve(NeighborQuery(target, binding)))
        assertEquals(NeighborMacResult.NotFound, resolverReturning(WindowsSendArpResolver.ERROR_GEN_FAILURE)
            .resolve(NeighborQuery(target, binding)))
        assertEquals(NeighborMacResult.Unsupported, resolverReturning(WindowsSendArpResolver.ERROR_NOT_SUPPORTED)
            .resolve(NeighborQuery(target, binding)))
        assertEquals(NeighborMacResult.Failed(NeighborFailure.PERMISSION_DENIED),
            resolverReturning(WindowsSendArpResolver.ERROR_ACCESS_DENIED).resolve(NeighborQuery(target, binding)))
        assertEquals(NeighborMacResult.NotFound, resolverReturning(0, ByteArray(6))
            .resolve(NeighborQuery(target, binding)))
        assertEquals(NeighborMacResult.NotFound, resolverReturning(0, byteArrayOf(1, 0x11, 0x22, 0x33, 0x44, 0x55))
            .resolve(NeighborQuery(target, binding)))
    }
}
