package com.shterneregen.securelan.desktop.lantools.discovery

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import javax.jmdns.ServiceInfo

class LanDnsSdBrowserTest {
    @Test fun binaryTxtIsNeverDecodedOrExposedAndOversizedTxtIsRejected() {
        val type = "_http._tcp.local."
        val binary = byteArrayOf(3, -1, 0, -2)
        val info = ServiceInfo.create(type, "Local service", 8080, 0, 0, binary)
        val result = copyDnsSdEvent(type, "Local service", info)
        assertTrue(result is DnsSdEvent.Resolved)
        val oversized = ServiceInfo.create(type, "Local service", 8080, 0, 0, ByteArray(4097))
        assertEquals(DnsSdEvent.Limited, copyDnsSdEvent(type, "Local service", oversized))
        assertEquals(DnsSdEvent.Limited, copyDnsSdEvent(type, "x".repeat(256), info))
    }
}
