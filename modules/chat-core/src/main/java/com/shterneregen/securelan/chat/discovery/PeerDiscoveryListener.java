package com.shterneregen.securelan.chat.discovery;

public interface PeerDiscoveryListener {
    void onPeerDiscovered(DiscoveredPeer peer);

    void onPeerExpired(DiscoveredPeer peer);

    void onDiscoveryError(String message, Throwable cause);
}
