package com.shterneregen.securelan.lantools.discovery

import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.lantools.model.LanResult
import java.time.Instant

/** Explicit, bounded DNS-SD coverage. A service declaration does not establish a hardware type. */
object LanServiceTypes {
    val supported: Set<String> = linkedSetOf("_http._tcp.local.", "_https._tcp.local.",
        "_ipp._tcp.local.", "_ipps._tcp.local.", "_ssh._tcp.local.", "_smb._tcp.local.")
}

/** A service key is scoped to this browse run/network, never an authenticated or persistent identity. */
data class LanServiceKey(val scope: String, val type: String, val instance: String)

/** Validated backend observation. TXT is deliberately not retained or interpreted as device identity. */
data class LanServiceObservation(
    val key: LanServiceKey,
    val hostname: String?,
    val addresses: List<IpLiteral>,
    val port: Int,
    val seenAt: Instant,
    val seenNanos: Long,
    val leaseMillis: Long = 30_000,
    val source: String = "mDNS/DNS-SD",
)

/** An observed endpoint; the port/service was advertised, not verified by a connectivity probe. */
data class LanAdvertisedService(val instance: String, val type: String, val port: Int, val addresses: List<IpLiteral>)

/** Session-only grouping of overlapping endpoints with the same advertised SRV target on one network. */
data class DiscoveredLanDevice(
    val id: String,
    val scope: String,
    val displayName: String?,
    val hostname: String?,
    val addresses: List<IpLiteral>,
    val services: List<LanAdvertisedService>,
    val sources: Set<String>,
    val lastSeen: Instant,
    val stale: Boolean = false,
    val deviceType: String? = null,
)

/** Partial means a resource/network limit or dropped event prevented a complete supported-type browse. */
data class LanDiscoverySnapshot(val devices: List<DiscoveredLanDevice> = emptyList(), val partial: Boolean = false)

/**
 * One manual browse, at most thirty seconds plus backend cleanup. Cancellation must close only its
 * own resources. Updates are serial, immutable snapshots; the final snapshot is historical/stale.
 */
fun interface LanDeviceDiscovery {
    suspend fun browse(update: suspend (LanDiscoverySnapshot) -> Unit): LanResult<LanDiscoverySnapshot>
}
