package com.shterneregen.securelan.chat.event;

import com.shterneregen.securelan.common.model.rtc.RtcSignalEnvelope;

public record ChatSignalReceivedEvent(RtcSignalEnvelope signal) implements ChatCoreEvent {
}
