package com.shterneregen.securelan.common.net.transport;

import java.net.InetSocketAddress;
import java.util.Objects;

public record TransportEndpoint(String host, int port) {
    public TransportEndpoint {
        Objects.requireNonNull(host, "host");
        host = host.trim();
        if (host.isBlank()) {
            throw new IllegalArgumentException("host must not be blank");
        }
        if (port < 1 || port > 65_535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
    }

    public static TransportEndpoint of(String host, int port) {
        return new TransportEndpoint(host, port);
    }

    public InetSocketAddress toSocketAddress() {
        return new InetSocketAddress(host, port);
    }
}
