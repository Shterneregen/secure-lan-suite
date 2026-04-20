package com.shterneregen.securelan.webrtc.event;

import com.shterneregen.securelan.common.model.rtc.RtcSessionMode;
import com.shterneregen.securelan.common.model.rtc.RtcSessionState;

public record RtcStateChangedEvent(
        String sessionId,
        String remotePeer,
        RtcSessionMode mode,
        RtcSessionState state,
        String message
) implements RtcEvent {
}
