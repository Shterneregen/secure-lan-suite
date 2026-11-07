package com.shterneregen.securelan.neighbor

import com.shterneregen.securelan.common.net.interfaces.NetworkKind

/**
 * Rejects requests that cannot be answered by an on-link neighbor table before any platform call.
 * Off-link targets never return a gateway MAC as if it belonged to the target.
 */
internal object NeighborMacPolicy {
    fun rejection(query: NeighborQuery): NeighborMacResult? {
        val target = query.target
        val binding = query.binding
        if (!target.isIpv4 || !target.isUnicast || target == binding.address) return NeighborMacResult.InvalidTarget
        if (!binding.up || binding.kind != NetworkKind.LAN || !binding.address.isIpv4) {
            return NeighborMacResult.Failed(NeighborFailure.INTERFACE_UNAVAILABLE)
        }
        if (binding.prefixLength !in 1..30) return NeighborMacResult.OffLink
        if (!binding.prefix.contains(target)) return NeighborMacResult.OffLink
        if (binding.prefix.isDirectedBroadcast(target)) return NeighborMacResult.InvalidTarget
        return null
    }
}
