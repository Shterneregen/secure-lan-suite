package com.shterneregen.securelan.chat.discovery

import com.shterneregen.securelan.common.net.address.IpLiteral
import com.shterneregen.securelan.common.net.interfaces.DiscoveryNetworkPolicy

enum class DiscoveryProviderId { UDP_BROADCAST, MDNS }
enum class ProviderAvailability { STOPPED, STARTING, RUNNING, UNAVAILABLE, FAILED }
data class DiscoveryProviderState(val provider: DiscoveryProviderId, val availability: ProviderAvailability, val limited: Boolean = false)
data class DiscoveryEndpoint(val address: IpLiteral, val chatPort: Int, val routeKey: String? = null, val ipv6ScopeId: Int? = null) {
    init {
        require(address.isUnicast && chatPort in 1..65535)
        require(address.isIpv4 || !address.isLinkLocal || ipv6ScopeId != null)
    }
    val host: String get() = address.value + (if (!address.isIpv4 && address.isLinkLocal) "%$ipv6ScopeId" else "")
}
data class ObservationKey(val provider: DiscoveryProviderId, val generation: Long, val networkKey: String, val identity: String) {
    init { require(networkKey.length <= 256 && identity.length <= 512) }
}
/** A null lease delegates removal to the DNS backend; UDP uses a monotonic 45-second lease. */
data class DiscoveryObservation(
    val key: ObservationKey,
    val peerId: String,
    val roomInstanceId: String?,
    val displayName: String?,
    val endpoints: List<DiscoveryEndpoint>,
    val filePortHint: Int?,
    val leaseNanos: Long? = null,
) {
    init {
        require(peerId.isNotBlank() && peerId.length <= 128)
        require(roomInstanceId == null || DiscoveryValidation.uuid(roomInstanceId))
        require(displayName == null || DiscoveryValidation.displayName(displayName))
        require(endpoints.size in 1..8 && (filePortHint == null || filePortHint in 1..65535))
        require(leaseNanos == null || leaseNanos > 0)
    }
}
data class DiscoveredRoom(
    val rowId: String,
    val peerId: String,
    val roomInstanceId: String?,
    val displayName: String?,
    val endpoints: List<DiscoveryEndpoint>,
    val filePortHint: Int?,
    val sources: Set<DiscoveryProviderId>,
    val metadataConflict: Boolean = false,
)
data class DiscoverySnapshot(val rooms: List<DiscoveredRoom> = emptyList(), val providers: List<DiscoveryProviderState> = emptyList())
sealed interface DiscoveryEvent {
    data class Upsert(val observation: DiscoveryObservation) : DiscoveryEvent
    data class Remove(val key: ObservationKey) : DiscoveryEvent
    data class ResetSource(val provider: DiscoveryProviderId, val generation: Long? = null, val networkKey: String? = null) : DiscoveryEvent
    data class ProviderState(val state: DiscoveryProviderState) : DiscoveryEvent
}
data class DiscoveryStartRequest(
    val udpConfig: PeerDiscoveryConfig,
    val hostedRoom: HostedRoomAdvertisement? = null,
    val networkPolicy: DiscoveryNetworkPolicy = DiscoveryNetworkPolicy(),
)
