package com.shterneregen.securelan.androidclient.network

import org.junit.Assert.assertEquals
import org.junit.Test

class AndroidChatHostServiceTest {
    @Test
    fun `remoteHost extracts IPv4 address from socket address`() {
        assertEquals("192.168.1.25", AndroidChatHostService.remoteHost("/192.168.1.25:43120"))
    }

    @Test
    fun `remoteHost extracts bracketed IPv6 address`() {
        assertEquals("fe80::1234", AndroidChatHostService.remoteHost("/[fe80::1234]:43120"))
    }

    @Test
    fun `remoteHost tolerates a host without port`() {
        assertEquals("android-peer", AndroidChatHostService.remoteHost("android-peer"))
    }
}
