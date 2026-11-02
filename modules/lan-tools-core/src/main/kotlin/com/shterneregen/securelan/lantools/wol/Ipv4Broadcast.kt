package com.shterneregen.securelan.lantools.wol

import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.common.net.address.IpPrefix

/** Numeric subnet arithmetic, independent of DNS, platform interfaces and room discovery. */
object Ipv4Broadcast {
    /**
     * Derives a directed broadcast by setting every host bit in [prefix] to one.
     *
     * Prefixes /0, /31, /32 and IPv6 return null: they are outside the WoL subnet policy.
     * The result is an address calculation, not proof that an interface supports broadcast.
     */
    fun directed(prefix: IpPrefix): IpLiteral? {
        if (!prefix.address.isIpv4 || prefix.length !in 1..30) return null
        val octets = prefix.address.bytes().map { it.toInt() and 0xff }.toMutableList()
        // Work byte-by-byte to avoid signed 32-bit overflow around prefixes such as /1.
        for (bit in prefix.length until 32) {
            octets[bit / 8] = octets[bit / 8] or (128 shr (bit % 8))
        }
        return IpLiteral.parse(octets.joinToString("."))
    }
}
