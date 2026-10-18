package com.shterneregen.securelan.chat.discovery.impl

import com.shterneregen.securelan.chat.discovery.*
import com.shterneregen.securelan.common.net.address.IpLiteral
import java.util.concurrent.atomic.AtomicLong

/** Adapts every packet callback, preserving addresses lost by the old single-address snapshot. */
class UdpBroadcastDiscoveryProvider(private val service: PeerDiscoveryService = UdpBroadcastPeerDiscoveryService()) : DiscoveryProvider {
    override val id = DiscoveryProviderId.UDP_BROADCAST
    private val generation = AtomicLong()
    override fun start(request: DiscoveryStartRequest, emit: (DiscoveryEvent) -> Unit) {
        stop()
        val run = generation.incrementAndGet()
        fun key(peer: DiscoveredPeer) = ObservationKey(id, run, peer.host, "${peer.peerId}:${peer.chatPort}")
        service.start(request.udpConfig, object : PeerDiscoveryListener {
            override fun onPeerDiscovered(peer: DiscoveredPeer) {
                if (run != generation.get()) return
                val ip = IpLiteral.parse(peer.host) ?: return
                if (!ip.isUnicast) return
                val name = peer.nickname.takeIf(DiscoveryValidation::displayName)
                runCatching { emit(DiscoveryEvent.Upsert(DiscoveryObservation(key(peer), peer.peerId, null, name,
                    listOf(DiscoveryEndpoint(ip, peer.chatPort)), peer.filePort, request.udpConfig.staleTimeout.toNanos()))) }
            }
            override fun onPeerExpired(peer: DiscoveredPeer) { if (run == generation.get()) emit(DiscoveryEvent.Remove(key(peer))) }
            override fun onDiscoveryError(message: String, cause: Throwable) {
                if (run == generation.get() && !service.isRunning()) emit(DiscoveryEvent.ProviderState(DiscoveryProviderState(id, ProviderAvailability.FAILED)))
            }
        })
        service.setAnnounceEnabled(request.hostedRoom?.discoverable == true && request.udpConfig.announceEnabled)
        emit(DiscoveryEvent.ProviderState(DiscoveryProviderState(id, if (service.isRunning()) ProviderAvailability.RUNNING else ProviderAvailability.FAILED)))
    }
    override fun updateAdvertisement(value: HostedRoomAdvertisement?) { service.setAnnounceEnabled(value?.discoverable == true) }
    override fun stop() { generation.incrementAndGet(); service.stop() }
}
