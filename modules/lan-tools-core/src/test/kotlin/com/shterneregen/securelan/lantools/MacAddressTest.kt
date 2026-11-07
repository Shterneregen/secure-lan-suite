package com.shterneregen.securelan.lantools

import com.shterneregen.securelan.common.net.address.MacAddress
import com.shterneregen.securelan.lantools.wol.MagicPacket
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class MacAddressTest {
    @ParameterizedTest
    @ValueSource(strings = ["02:AB:CD:EF:12:34", "02-ab-cd-ef-12-34", "02aBCDef1234", "02ab.cdef.1234"])
    fun acceptsSupportedFormats(raw: String) {
        val parsed = MacAddress.parse(raw)!!
        assertEquals("02:ab:cd:ef:12:34", parsed.value)
        assertEquals(mac, parsed)
        assertEquals(mac.hashCode(), parsed.hashCode())
        assertTrue(parsed.isWakeTarget)
    }

    @ParameterizedTest
    @ValueSource(strings = ["", " 02:ab:cd:ef:12:34", "02:ab:cd:ef:12:34 ", "02:ab-cd:ef:12:34", "2:ab:cd:ef:12:34",
        "02:ab:cd:ef:12", "02:ab:cd:ef:12:34:56", "02ab.cdef.123", "02ab.cdef.123g", "02_ab_cd_ef_12_34",
        "02ab cdef1234", "02030405060z", "０２ab.cdef.1234"])
    fun rejectsMalformedInput(raw: String) {
        assertNull(MacAddress.parse(raw))
    }

    @ParameterizedTest
    @ValueSource(strings = ["00:00:00:00:00:00", "ff:ff:ff:ff:ff:ff", "01:00:5e:00:00:01"])
    fun separatesSyntaxFromWakeTargetPolicy(raw: String) {
        val parsed = MacAddress.parse(raw)!!
        assertFalse(parsed.isWakeTarget)
        assertThrows(IllegalArgumentException::class.java) { MagicPacket.create(parsed) }
    }

    @Test
    fun generatesExactPacketAndDefendsValueFromMutation() {
        val bytes = mac.bytes()
        bytes.fill(0)
        val packet = MagicPacket.create(mac)
        assertEquals(102, packet.size)
        assertArrayEquals(ByteArray(6) { 0xff.toByte() }, packet.copyOfRange(0, 6))
        val expected = byteArrayOf(2, 0xab.toByte(), 0xcd.toByte(), 0xef.toByte(), 0x12, 0x34)
        repeat(16) { assertArrayEquals(expected, packet.copyOfRange(6 + it * 6, 12 + it * 6)) }
        packet.fill(0)
        assertEquals(0xff.toByte(), MagicPacket.create(mac)[0])
    }
}
