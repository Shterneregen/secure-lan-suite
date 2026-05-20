package com.shterneregen.securelan.chat.discovery.impl;

import com.shterneregen.securelan.chat.discovery.DiscoveryCodec;

final class DiscoveryMessageCodec {
    private DiscoveryMessageCodec() {
    }

    static String encode(DiscoveryMessage message) {
        return DiscoveryCodec.encode(new DiscoveryCodec.Message(
                message.peerId(),
                message.nickname(),
                message.chatPort(),
                message.filePort()
        ));
    }

    static DiscoveryMessage decode(String payload) {
        DiscoveryCodec.Message message = DiscoveryCodec.decode(payload);
        return new DiscoveryMessage(message.peerId(), message.nickname(), message.chatPort(), message.filePort());
    }

    static byte[] toBytes(DiscoveryMessage message) {
        return DiscoveryCodec.toBytes(new DiscoveryCodec.Message(
                message.peerId(),
                message.nickname(),
                message.chatPort(),
                message.filePort()
        ));
    }

    static DiscoveryMessage fromBytes(byte[] data, int length) {
        DiscoveryCodec.Message message = DiscoveryCodec.fromBytes(data, length);
        return new DiscoveryMessage(message.peerId(), message.nickname(), message.chatPort(), message.filePort());
    }
}
