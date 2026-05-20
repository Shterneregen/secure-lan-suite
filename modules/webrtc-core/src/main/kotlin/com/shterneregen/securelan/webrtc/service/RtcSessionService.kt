package com.shterneregen.securelan.webrtc.service

import com.shterneregen.securelan.common.model.rtc.RtcSignalEnvelope
import com.shterneregen.securelan.webrtc.runtime.RtcRuntimeStatus
import java.util.Optional

interface RtcSessionService : AutoCloseable {
    fun runtimeStatus(): RtcRuntimeStatus
    fun currentSession(): Optional<RtcSessionSnapshot>
    fun startSession(request: RtcSessionRequest): RtcSessionSnapshot
    fun acceptInboundSignal(localPeer: String?, signal: RtcSignalEnvelope)
    fun sendDataMessage(payload: String?)
    fun closeCurrentSession()

    override fun close() {
    }
}
