package com.shterneregen.securelan.common.net.address

/** Local prefix knowledge is optional; an off-prefix address still requires UI confirmation. */
data class IpPrefix(val address: IpLiteral, val length: Int) {
    init { require(length in 0..if (address.isIpv4) 32 else 128) }
    fun contains(candidate: IpLiteral): Boolean {
        val a = address.bytes()
        val b = candidate.bytes()
        if (a.size != b.size) return false
        return (0 until length).all { bit ->
            (a[bit / 8].toInt() xor b[bit / 8].toInt()) and (128 shr (bit % 8)) == 0
        }
    }
    fun isDirectedBroadcast(candidate: IpLiteral): Boolean = address.isIpv4 && length < 31 && contains(candidate) &&
        (length until 32).all { bit -> candidate.bytes()[bit / 8].toInt() and (128 shr (bit % 8)) != 0 }
}

object LanAddressPolicy {
    fun portable(address: IpLiteral, localPrefixes: List<IpPrefix> = emptyList()): Boolean =
        address.isUnicast && (address.isIpv4 || !address.isLinkLocal) && localPrefixes.none { it.isDirectedBroadcast(address) }
}
