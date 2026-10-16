package com.shterneregen.securelan.webrtc.service

import com.shterneregen.securelan.common.model.rtc.RtcSignalEnvelope
import com.shterneregen.securelan.webrtc.runtime.RtcRuntimeStatus
import java.util.Optional

/**
 * High-level domain service orchestrating WebRTC session lifecycles, signaling integration,
 * data channel messaging, and runtime status reporting.
 */
interface RtcSessionService : AutoCloseable {
    /**
     * Returns the operational health and availability status of the underlying WebRTC engine.
     *
     * @return runtime status descriptor.
     */
    fun configureMediaDevices(configuration: RtcMediaConfiguration) {}

    fun runtimeStatus(): RtcRuntimeStatus

    /**
     * Returns a snapshot of the current active or negotiating WebRTC session, if present.
     *
     * @return optional containing the active session snapshot, or empty if no session is active.
     */
    fun currentSession(): Optional<RtcSessionSnapshot>

    /**
     * Initiates a new outbound WebRTC session matching the parameters in [request].
     * Closes any previously active session before starting negotiation.
     *
     * @param request configuration specifying peers, media mode, and device preferences.
     * @return initial [RtcSessionSnapshot] in negotiating state.
     */
    fun startSession(request: RtcSessionRequest): RtcSessionSnapshot

    /**
     * Processes an incoming signaling envelope received via chat signaling from a remote peer.
     *
     * @param localPeer nickname of the local user.
     * @param signal incoming signaling envelope (offer, answer, ICE candidate, hangup, error).
     */
    fun acceptInboundSignal(localPeer: String?, signal: RtcSignalEnvelope?)

    /** Accepts the matching pending offer. Media must not be prepared before this decision. */
    fun acceptIncomingSession(sessionId: String): Boolean = false

    /**
     * Transmits a string message across the active session's RTCDataChannel.
     *
     * @param payload UTF-8 string payload to send.
     */
    fun sendDataMessage(payload: String?)

    /**
     * Gracefully closes the currently active session, sending a HANGUP signal to the remote peer.
     */
    fun closeCurrentSession()

    /**
     * Terminates any active session and releases underlying engine and thread executor resources.
     */
    override fun close() {
    }
}
