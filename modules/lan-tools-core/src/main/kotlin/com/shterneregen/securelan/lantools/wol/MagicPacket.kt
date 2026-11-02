package com.shterneregen.securelan.lantools.wol

import com.shterneregen.securelan.lantools.model.MacAddress

/** Standard WoL payload, without SecureOn or any SecureLanSuite credentials. */
object MagicPacket {
    /** Six synchronization octets followed by sixteen six-octet MAC repetitions. */
    const val SIZE = 102

    /**
     * Allocates a fresh standard packet without retaining caller-owned mutable arrays.
     *
     * @throws IllegalArgumentException if [mac] is zero, multicast or broadcast.
     */
    fun create(mac: MacAddress): ByteArray {
        require(mac.isWakeTarget) { "WoL requires a nonzero unicast MAC" }
        val address = mac.bytes()
        // The target block has no padding or separators between successive MAC repetitions.
        return ByteArray(SIZE) { if (it < 6) 0xff.toByte() else address[(it - 6) % 6] }
    }
}
