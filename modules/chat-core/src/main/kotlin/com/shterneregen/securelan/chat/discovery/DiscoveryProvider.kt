package com.shterneregen.securelan.chat.discovery

interface DiscoveryProvider : AutoCloseable {
    val id: DiscoveryProviderId
    fun start(request: DiscoveryStartRequest, emit: (DiscoveryEvent) -> Unit)
    fun updateAdvertisement(value: HostedRoomAdvertisement?)
    fun stop()
    override fun close() = stop()
}
