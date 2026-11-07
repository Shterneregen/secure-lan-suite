package com.shterneregen.securelan.neighbor

import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.common.net.address.MacAddress
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class MacNeighborResolverTest {
    private val target = requireNotNull(IpLiteral.parse("192.168.1.20"))
    private val mac = requireNotNull(MacAddress.parse("02:11:22:33:44:55"))

    @Test
    fun parsesIpv4DestinationAndLinkGateway() {
        assertEquals(mac, MacNeighborResolver.parseMacNeighborTable(entry(target.bytes(), mac.bytes()), target))
    }

    @Test
    fun ignoresOtherDestinationsAndTruncatedMessages() {
        val other = requireNotNull(IpLiteral.parse("192.168.1.21"))
        assertNull(MacNeighborResolver.parseMacNeighborTable(entry(target.bytes(), mac.bytes()), other))
        assertNull(MacNeighborResolver.parseMacNeighborTable(entry(target.bytes(), mac.bytes()).copyOf(80), target))
    }

    @Test
    fun ignoresZeroAndMulticastHardwareAddresses() {
        assertNull(MacNeighborResolver.parseMacNeighborTable(entry(target.bytes(), ByteArray(6)), target))
        assertNull(MacNeighborResolver.parseMacNeighborTable(entry(target.bytes(), byteArrayOf(1, 0x11, 0x22, 0x33, 0x44, 0x55)), target))
    }

    @Test
    fun selectsOnlyTheEntryForTheExpectedInterfaceIndex() {
        val other = requireNotNull(MacAddress.parse("02:aa:bb:cc:dd:ee"))
        val wrongInterface = entry(target.bytes(), other.bytes(), interfaceIndex = 7)
        val rightInterface = entry(target.bytes(), mac.bytes(), interfaceIndex = 9)
        assertEquals(mac,
            MacNeighborResolver.parseMacNeighborTable(wrongInterface + rightInterface, target, expectedInterfaceIndex = 9))
    }

    @Test
    fun ignoresMalformedLinkSockaddrWithoutThrowing() {
        val malformed = entry(target.bytes(), mac.bytes())
        malformed[108] = 2
        assertNull(MacNeighborResolver.parseMacNeighborTable(malformed, target))
    }

    private fun entry(destination: ByteArray, hardware: ByteArray, interfaceIndex: Int = 0): ByteArray {
        val messageLength = 92 + 16 + 16
        val bytes = ByteArray(messageLength)
        put16(bytes, 0, messageLength)
        bytes[2] = 5
        bytes[3] = 3
        put16(bytes, 4, interfaceIndex)
        put32(bytes, 8, 0x400)
        put32(bytes, 12, 0x3)
        bytes[92] = 16
        bytes[93] = 2
        destination.copyInto(bytes, 96)
        bytes[108] = 14
        bytes[109] = 18
        bytes[114] = 6
        hardware.copyInto(bytes, 116)
        return bytes
    }

    private fun put16(bytes: ByteArray, offset: Int, value: Int) {
        bytes[offset] = value.toByte()
        bytes[offset + 1] = (value shr 8).toByte()
    }

    private fun put32(bytes: ByteArray, offset: Int, value: Int) {
        bytes[offset] = value.toByte()
        bytes[offset + 1] = (value shr 8).toByte()
        bytes[offset + 2] = (value shr 16).toByte()
        bytes[offset + 3] = (value shr 24).toByte()
    }
}
