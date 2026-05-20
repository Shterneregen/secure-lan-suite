package com.shterneregen.securelan.chat.event

import com.shterneregen.securelan.common.model.rtc.RtcSignalEnvelope

@JvmRecord
data class ChatSignalReceivedEvent(val signal: RtcSignalEnvelope) : ChatCoreEvent
