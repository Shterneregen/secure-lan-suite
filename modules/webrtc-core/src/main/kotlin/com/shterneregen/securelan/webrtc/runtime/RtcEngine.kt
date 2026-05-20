package com.shterneregen.securelan.webrtc.runtime

import com.shterneregen.securelan.common.model.rtc.RtcSessionMode
import com.shterneregen.securelan.common.model.rtc.RtcSignalEnvelope
import com.shterneregen.securelan.webrtc.event.RtcEvent
import java.util.function.Consumer

interface RtcEngine : AutoCloseable {
    fun status(): RtcRuntimeStatus

    fun startSession(
        sessionId: String?,
        localPeer: String?,
        remotePeer: String?,
        mode: RtcSessionMode?,
        dataChannelLabel: String?,
        audioCaptureDeviceId: String?,
        videoCaptureDeviceId: String?,
        outboundSignalConsumer: Consumer<RtcSignalEnvelope>,
        eventConsumer: Consumer<RtcEvent>,
    )

    fun handleRemoteSignal(
        signal: RtcSignalEnvelope,
        outboundSignalConsumer: Consumer<RtcSignalEnvelope>,
        eventConsumer: Consumer<RtcEvent>,
    )

    fun sendData(sessionId: String?, payload: String?, eventConsumer: Consumer<RtcEvent>)

    fun closeSession(sessionId: String?, eventConsumer: Consumer<RtcEvent>)

    override fun close() {
    }
}
