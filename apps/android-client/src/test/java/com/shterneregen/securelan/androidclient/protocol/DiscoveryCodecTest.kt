package com.shterneregen.securelan.androidclient.protocol

import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.InetAddress

class DiscoveryCodecTest {
    @Test
    fun encodeAndDecodeKeepsDesktopCompatibleFields() {
        val bytes = DiscoveryCodec.encode("peer-1", "Android User", 5050, 5051)

        val decoded = DiscoveryCodec.decode(bytes, bytes.size, InetAddress.getByName("192.168.1.10"))

        assertEquals("peer-1", decoded.peerId)
        assertEquals("Android User", decoded.nickname)
        assertEquals("192.168.1.10", decoded.host)
        assertEquals(5050, decoded.chatPort)
        assertEquals(5051, decoded.filePort)
    }
}
