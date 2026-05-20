package com.shterneregen.securelan.webrtc.runtime

import com.shterneregen.securelan.common.model.rtc.RtcSessionMode
import com.shterneregen.securelan.common.model.rtc.RtcSignalEnvelope
import com.shterneregen.securelan.webrtc.event.RtcDataMessageEvent
import com.shterneregen.securelan.webrtc.event.RtcEvent
import com.shterneregen.securelan.webrtc.event.RtcRuntimeWarningEvent
import java.util.function.Consumer

class NoOpRtcEngine(message: String?) : RtcEngine {
    private val message: String = if (message.isNullOrBlank()) {
        "Native WebRTC provider is unavailable."
    } else {
        message
    }

    constructor() : this(
        "Native WebRTC provider is not configured yet. The signaling architecture is integrated, but live RTCDataChannel and audio/video negotiation still require a provider module such as webrtc-java.",
    )

    override fun status(): RtcRuntimeStatus = RtcRuntimeStatus.unavailable(message)

    override fun startSession(
        sessionId: String?,
        localPeer: String?,
        remotePeer: String?,
        mode: RtcSessionMode?,
        dataChannelLabel: String?,
        audioCaptureDeviceId: String?,
        videoCaptureDeviceId: String?,
        outboundSignalConsumer: Consumer<RtcSignalEnvelope>,
        eventConsumer: Consumer<RtcEvent>,
    ) {
        eventConsumer.accept(RtcRuntimeWarningEvent(message))
    }

    override fun handleRemoteSignal(
        signal: RtcSignalEnvelope,
        outboundSignalConsumer: Consumer<RtcSignalEnvelope>,
        eventConsumer: Consumer<RtcEvent>,
    ) {
        eventConsumer.accept(
            RtcRuntimeWarningEvent(
                "$message Incoming signal from ${signal.fromPeer()} was recorded but not executed.",
            ),
        )
    }

    override fun sendData(sessionId: String?, payload: String?, eventConsumer: Consumer<RtcEvent>) {
        eventConsumer.accept(RtcDataMessageEvent(sessionId, "", true, payload))
        eventConsumer.accept(RtcRuntimeWarningEvent(message))
    }

    override fun closeSession(sessionId: String?, eventConsumer: Consumer<RtcEvent>) {
        eventConsumer.accept(RtcRuntimeWarningEvent(message))
    }
}
