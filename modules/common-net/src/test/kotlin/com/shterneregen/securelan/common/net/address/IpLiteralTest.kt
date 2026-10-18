package com.shterneregen.securelan.common.net.address

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class IpLiteralTest {
    @Test fun `numeric parsing canonicalizes without resolving names`() {
        assertEquals("2001:db8::1", IpLiteral.parse("2001:0DB8:0:0:0:0:0:1")?.value)
        assertEquals("192.168.1.2", IpLiteral.parse("::ffff:192.168.1.2")?.value)
        listOf("localhost", "example.com", "192.168.01.2", "2130706433", "0x7f000001", "1::2::3", "::ffff:1.2.3.999", "fe80::1%7", "[::1]").forEach {
            assertNull(IpLiteral.parse(it), it)
        }
    }
    @Test fun `portable endpoints reject special addresses and prefix broadcasts`() {
        listOf("0.0.0.0", "127.2.3.4", "224.0.0.1", "255.255.255.255", "::", "::1", "ff02::1", "fe80::1", "::ffff:127.0.0.1").forEach {
            assertFalse(LanAddressPolicy.portable(IpLiteral.parse(it)!!), it)
        }
        val prefix = IpPrefix(IpLiteral.parse("192.168.5.1")!!, 24)
        assertFalse(LanAddressPolicy.portable(IpLiteral.parse("192.168.5.255")!!, listOf(prefix)))
        assertTrue(LanAddressPolicy.portable(IpLiteral.parse("169.254.2.3")!!))
        assertTrue(LanAddressPolicy.portable(IpLiteral.parse("fd12::5")!!))
    }
}
