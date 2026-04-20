package com.shterneregen.securelan.webrtc.service;

import com.shterneregen.securelan.common.model.rtc.RtcSessionMode;
import com.shterneregen.securelan.common.model.rtc.RtcSessionState;

public record RtcSessionSnapshot(
        String sessionId,
        String localPeer,
        String remotePeer,
        RtcSessionMode mode,
        String dataChannelLabel,
        RtcSessionState state,
        String message
) {
}
