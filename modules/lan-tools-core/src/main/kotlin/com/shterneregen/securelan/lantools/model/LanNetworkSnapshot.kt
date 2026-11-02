package com.shterneregen.securelan.lantools.model

import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.common.net.interfaces.NetworkBinding
import java.util.Collections

/**
 * Primitive platform snapshot. No Network, Socket or NetworkInterface handle is retained.
 *
 * @property binding Source address, prefix, interface identity and current availability.
 * @property displayName Platform label for an interface picker, not a trusted device name.
 * @property broadcastCapable Explicit platform capability; a prefix alone does not grant it.
 * @property broadcast OS-provided directed broadcast, or null when it must be derived from the prefix.
 */
data class LanInterfaceSnapshot(
    val binding: NetworkBinding,
    val displayName: String,
    val broadcastCapable: Boolean,
    val broadcast: IpLiteral?,
)

/**
 * Owns a defensive copy; route keys are ephemeral and must be resolved for every operation.
 *
 * @param interfaces Observed source bindings, including unsuitable ones for explanatory UI.
 * @property defaultRouteInterfaceKey Optional platform recommendation for display only. It never
 * replaces an explicit selection or permits automatic sending when multiple bindings are eligible.
 */
class LanNetworkSnapshot(interfaces: List<LanInterfaceSnapshot>, val defaultRouteInterfaceKey: String? = null) {
    /** Read-only snapshot independent of the provider's mutable working collection. */
    val interfaces: List<LanInterfaceSnapshot> = Collections.unmodifiableList(ArrayList(interfaces))
}

/** Implementations must be cancellation-cooperative and return bounded snapshots, off the UI thread. */
fun interface LanNetworkInfoProvider {
    /** Reads current local metadata without starting discovery; failures must not expose partial data. */
    suspend fun snapshot(): LanResult<LanNetworkSnapshot>
}
