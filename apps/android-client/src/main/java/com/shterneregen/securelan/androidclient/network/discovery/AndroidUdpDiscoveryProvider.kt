package com.shterneregen.securelan.androidclient.network.discovery

import com.shterneregen.securelan.androidclient.network.PeerDiscoveryRepository
import com.shterneregen.securelan.chat.discovery.*
import com.shterneregen.securelan.common.net.address.IpLiteral
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicLong

class AndroidUdpDiscoveryProvider(private val repository: PeerDiscoveryRepository) : DiscoveryProvider {
    override val id = DiscoveryProviderId.UDP_BROADCAST
    private val generation = AtomicLong()
    private var job: Job? = null
    override fun start(request: DiscoveryStartRequest, emit: (DiscoveryEvent) -> Unit) {
        stop()
        val run = generation.incrementAndGet()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        job = scope.coroutineContext[Job]
        scope.launch {
            try {
                emit(DiscoveryEvent.ProviderState(DiscoveryProviderState(id, ProviderAvailability.RUNNING)))
                repository.discoverPeers().collect { peer ->
                    if (generation.get() != run || peer.peerId == request.udpConfig.peerId) return@collect
                    val ip = IpLiteral.parse(peer.host) ?: return@collect
                    if (!ip.isUnicast) return@collect
                    emit(DiscoveryEvent.Upsert(DiscoveryObservation(ObservationKey(id, run, peer.host, "${peer.peerId}:${peer.chatPort}"), peer.peerId, null,
                        peer.nickname.takeIf(DiscoveryValidation::displayName), listOf(DiscoveryEndpoint(ip, peer.chatPort)), peer.filePort, 45_000_000_000L)))
                }
            } catch (error: Exception) {
                if (error !is CancellationException && generation.get() == run) emit(DiscoveryEvent.ProviderState(DiscoveryProviderState(id, ProviderAvailability.FAILED)))
            }
        }
    }
    override fun updateAdvertisement(value: HostedRoomAdvertisement?) = Unit
    override fun stop() { generation.incrementAndGet(); job?.cancel(); job = null }
}
