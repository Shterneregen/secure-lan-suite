package com.shterneregen.securelan.webrtc.event;

public sealed interface RtcEvent permits RtcRuntimeWarningEvent, RtcStateChangedEvent, RtcDataMessageEvent, RtcAudioLevelEvent, RtcVideoFrameEvent {
}
