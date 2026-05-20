package com.shterneregen.securelan.chat.discovery

interface PeerDiscoveryService : AutoCloseable {
    fun start(config: PeerDiscoveryConfig, listener: PeerDiscoveryListener)

    fun stop()

    fun setAnnounceEnabled(announceEnabled: Boolean)

    fun isRunning(): Boolean


    fun snapshot(): List<DiscoveredPeer>

    override fun close() {
        stop()
    }
}
