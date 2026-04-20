package com.shterneregen.securelan.webrtc.service;

import com.shterneregen.securelan.common.model.rtc.RtcSessionMode;

import java.util.Objects;

public record RtcSessionRequest(String localPeer, String remotePeer, RtcSessionMode mode, String dataChannelLabel) {
    public RtcSessionRequest {
        Objects.requireNonNull(mode, "mode must not be null");
        localPeer = localPeer == null ? "" : localPeer.trim();
        remotePeer = remotePeer == null ? "" : remotePeer.trim();
        dataChannelLabel = dataChannelLabel == null || dataChannelLabel.isBlank() ? "securelan-data" : dataChannelLabel.trim();
    }
}
