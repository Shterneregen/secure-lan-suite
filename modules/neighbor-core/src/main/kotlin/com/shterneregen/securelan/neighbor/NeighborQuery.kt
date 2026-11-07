package com.shterneregen.securelan.neighbor

import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.common.net.interfaces.NetworkBinding

/** A single IPv4 target and the local source binding used to resolve its hardware address. */
data class NeighborQuery(val target: IpLiteral, val binding: NetworkBinding)
