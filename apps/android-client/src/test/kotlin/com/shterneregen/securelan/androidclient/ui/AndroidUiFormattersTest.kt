package com.shterneregen.securelan.androidclient.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class AndroidUiFormattersTest {
    @Test
    fun `room endpoint contains the LAN address and configured chat port`() {
        assertEquals("192.168.1.25:5050", AndroidUiFormatters.roomEndpoint("192.168.1.25", "5050"))
    }
}
