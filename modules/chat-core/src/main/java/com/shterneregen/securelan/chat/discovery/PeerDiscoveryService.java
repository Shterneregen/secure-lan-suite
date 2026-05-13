package com.shterneregen.securelan.chat.discovery;

import java.util.List;

public interface PeerDiscoveryService extends AutoCloseable {
    void start(PeerDiscoveryConfig config, PeerDiscoveryListener listener);

    void stop();

    void setAnnounceEnabled(boolean announceEnabled);

    boolean isRunning();

    List<DiscoveredPeer> snapshot();

    @Override
    default void close() {
        stop();
    }
}
