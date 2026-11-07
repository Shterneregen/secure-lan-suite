package com.shterneregen.securelan.neighbor

import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.common.net.interfaces.NetworkBinding
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NeighborResolversTest {
    private val target = requireNotNull(IpLiteral.parse("192.168.1.20"))
    private val binding = NetworkBinding("eth0:192.168.1.42", "eth0", requireNotNull(IpLiteral.parse("192.168.1.42")), 24)

    @Test
    fun unsupportedPlatformsNeverTouchNativeCode() {
        assertEquals(NeighborMacResult.Unsupported,
            NeighborResolvers.forPlatform("Plan 9").resolve(NeighborQuery(target, binding)))
    }
}
