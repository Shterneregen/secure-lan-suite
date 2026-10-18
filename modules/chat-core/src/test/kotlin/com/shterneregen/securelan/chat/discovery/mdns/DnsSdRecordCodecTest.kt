package com.shterneregen.securelan.chat.discovery.mdns

import com.shterneregen.securelan.chat.discovery.HostedRoomAdvertisement
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DnsSdRecordCodecTest {
    private val room = HostedRoomAdvertisement("2da1e450-798a-401f-990b-cb1cc4f61d92", "ab817e84-78af-40b4-994d-41bfa5ff20fb", "Room", 5050, null, true)
    @Test fun `chat only records do not invent a file receiver`() {
        val encoded = DnsSdRecordCodec.encode(room)
        assertFalse(encoded.containsKey("fp"))
        assertNull(DnsSdRecordCodec.decode(encoded, 5050)!!.filePort)
        assertEquals(5051, DnsSdRecordCodec.decode(DnsSdRecordCodec.encode(room.copy(activeFilePort = 5051)), 5050)!!.filePort)
    }
    @Test fun `strict fields bytes duplicates versions and sizes`() {
        val entries = DnsSdRecordCodec.encode(room).toList()
        assertNull(DnsSdRecordCodec.decode(entries + ("V" to "1".toByteArray()), 5050))
        assertNull(DnsSdRecordCodec.decode(entries.filter { it.first != "v" } + ("v" to "2".toByteArray()), 5050))
        assertNull(DnsSdRecordCodec.decode(entries.filter { it.first != "name" } + ("name" to byteArrayOf(0xc0.toByte(), 0xaf.toByte())), 5050))
        assertNull(DnsSdRecordCodec.decode(entries + ("extra" to ByteArray(513)), 5050))
        assertNull(DnsSdRecordCodec.decode(entries, 0))
    }
}
