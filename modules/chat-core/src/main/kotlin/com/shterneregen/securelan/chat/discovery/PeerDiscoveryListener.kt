package com.shterneregen.securelan.chat.discovery

interface PeerDiscoveryListener {
    fun onPeerDiscovered(peer: DiscoveredPeer)

    fun onPeerExpired(peer: DiscoveredPeer)

    fun onDiscoveryError(message: String, cause: Throwable)
}
