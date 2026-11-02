package com.shterneregen.securelan.lantools.wol

import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.common.net.interfaces.NetworkKind
import com.shterneregen.securelan.lantools.model.*

/**
 * Fully resolved single-interface send. Never represents a fan-out or a trusted peer.
 *
 * @property network Fresh source binding and its platform capabilities.
 * @property target Validated directed or explicitly requested limited IPv4 broadcast.
 * @property port Destination UDP port; no persistent listener is opened.
 */
data class WakeRoute(val network: LanInterfaceSnapshot, val target: IpLiteral, val port: Int)

/** Pure request validation and source selection, deliberately independent of OS route hints. */
internal object WakeTargetResolver {
    private val limitedBroadcast = IpLiteral.parse("255.255.255.255")!!

    /** Rejects scalar input errors before asking a platform adapter for any metadata. */
    fun validate(request: WakeRequest): LanResult.Failure? {
        val config = request.configuration
        val field = when {
            !config.mac.isWakeTarget -> "mac"
            config.port !in 1..65535 -> "port"
            request.timeoutMillis !in 1..30_000 -> "timeoutMillis"
            config.broadcast?.isIpv4 == false -> "broadcast"
            config.preferredInterfaceKey?.let { it.isBlank() || it.length > 256 } == true -> "preferredInterfaceKey"
            request.bindingKey?.let { it.isBlank() || it.length > 256 } == true -> "bindingKey"
            else -> null
        }
        return field?.let { LanResult.Failure(LanError.INVALID_INPUT, it) }
    }

    /** Resolves exactly one eligible source; stale saved preferences never fall back to another NIC. */
    fun resolve(request: WakeRequest, snapshot: LanNetworkSnapshot): LanResult<WakeRoute> {
        val config = request.configuration
        val explicit = request.bindingKey != null || config.preferredInterfaceKey != null
        val matching = when {
            request.bindingKey != null -> snapshot.interfaces.filter { it.binding.key == request.bindingKey }
            config.preferredInterfaceKey != null -> snapshot.interfaces.filter { it.binding.interfaceKey == config.preferredInterfaceKey }
            else -> snapshot.interfaces
        }
        if (matching.isEmpty()) return LanResult.Failure(if (explicit) LanError.INTERFACE_UNAVAILABLE else LanError.NETWORK_UNAVAILABLE)
        val eligible = matching.filter(::suitable)
        if (eligible.isEmpty()) return LanResult.Failure(if (explicit) LanError.INTERFACE_UNSUITABLE else LanError.NETWORK_UNAVAILABLE)
        // A default-route recommendation is a picker hint, not authorization to choose a subnet.
        if (eligible.size != 1) return LanResult.Failure(LanError.INTERFACE_SELECTION_REQUIRED)
        val selected = eligible.single()
        val target = config.broadcast ?: Ipv4Broadcast.directed(selected.binding.prefix)!!
        if (target == limitedBroadcast) {
            if (!explicit) return LanResult.Failure(LanError.INTERFACE_SELECTION_REQUIRED)
        } else if (!selected.binding.prefix.isDirectedBroadcast(target)) {
            return LanResult.Failure(LanError.INVALID_INPUT, "broadcast")
        }
        return LanResult.Success(WakeRoute(selected, target, config.port))
    }

    private fun suitable(network: LanInterfaceSnapshot): Boolean {
        val binding = network.binding
        return binding.up && binding.kind == NetworkKind.LAN && binding.address.isIpv4 && binding.address.isUnicast &&
            binding.prefixLength in 1..30 && network.broadcastCapable &&
            // Missing OS metadata can be derived; contradictory OS metadata is never silently repaired.
            (network.broadcast == null || binding.prefix.isDirectedBroadcast(network.broadcast)) &&
            !binding.prefix.isDirectedBroadcast(binding.address) &&
            !isSubnetAddress(binding.address, binding.prefixLength)
    }

    private fun isSubnetAddress(address: IpLiteral, prefixLength: Int): Boolean {
        val bytes = address.bytes()
        return (prefixLength until 32).all { bit -> bytes[bit / 8].toInt() and (128 shr (bit % 8)) == 0 }
    }
}
