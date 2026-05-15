package com.shterneregen.securelan.filetransfer.quickshare;

import com.shterneregen.securelan.common.net.NetworkConstants;

import java.util.List;
import java.util.Objects;

public record QuickShareServerConfig(
        int port,
        List<String> advertisedHosts
) {
    public QuickShareServerConfig() {
        this(NetworkConstants.DEFAULT_QUICK_SHARE_PORT, List.of());
    }

    public QuickShareServerConfig(int port) {
        this(port, List.of());
    }

    public QuickShareServerConfig {
        Objects.requireNonNull(advertisedHosts, "advertisedHosts must not be null");
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("port must be between 0 and 65535");
        }
        advertisedHosts = advertisedHosts.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(host -> !host.isBlank())
                .distinct()
                .toList();
    }
}
