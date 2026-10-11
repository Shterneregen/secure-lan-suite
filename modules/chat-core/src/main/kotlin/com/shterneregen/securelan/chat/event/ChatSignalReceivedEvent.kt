package com.shterneregen.securelan.chat.event

import com.shterneregen.securelan.common.model.rtc.RtcSignalEnvelope

/**
 * Event fired on the **client side** when a WebRTC signalling message is received from the server.
 *
 * The [signal] envelope contains all routing information (sender, recipient, session ID) as well
 * as the SDP or ICE payload required for peer-to-peer connection establishment.
 *
 * @property signal The deserialized [RtcSignalEnvelope] carrying the signalling data.
 */
@JvmRecord
data class ChatSignalReceivedEvent(val signal: RtcSignalEnvelope) : ChatCoreEvent
