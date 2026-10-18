package com.shterneregen.securelan.chat.discovery

interface RoomDiscoveryService : PeerDiscoveryService {
    fun startSession(request: DiscoveryStartRequest, listener: PeerDiscoveryListener, onSnapshot: (DiscoverySnapshot) -> Unit)
    fun detailedSnapshot(): DiscoverySnapshot
    fun updateHostedRoom(value: HostedRoomAdvertisement?)
}
