package com.shterneregen.securelan.common.net.interfaces

import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.common.net.address.IpPrefix

enum class NetworkKind { LAN, VPN, VIRTUAL, CELLULAR }

/** Primitive snapshot only. Platform network handles stay in their application owner. */
data class NetworkBinding(
    val key: String,
    val interfaceKey: String,
    val address: IpLiteral,
    val prefixLength: Int,
    val kind: NetworkKind = NetworkKind.LAN,
    val up: Boolean = true,
    val multicast: Boolean = true,
    val scopeId: Int? = null,
) {
    val prefix: IpPrefix get() = IpPrefix(address, prefixLength)
}

data class DiscoveryNetworkPolicy(val explicitlyAllowedInterfaces: Set<String> = emptySet(), val enableIpv6: Boolean = false)
