package com.shterneregen.securelan.common.net.interfaces

object NetworkBindingSelector {
    fun select(bindings: List<NetworkBinding>, policy: DiscoveryNetworkPolicy = DiscoveryNetworkPolicy()): List<NetworkBinding> =
        bindings.filter { it.up && it.multicast && it.address.isUnicast &&
            (it.address.isIpv4 || policy.enableIpv6) &&
            (it.kind == NetworkKind.LAN || it.interfaceKey in policy.explicitlyAllowedInterfaces) }
            .sortedWith(compareBy<NetworkBinding> { !it.address.isIpv4 }.thenBy { it.key })
            .groupBy { it.interfaceKey }.values.take(4).flatMap { it.take(2) }.take(8)
}
