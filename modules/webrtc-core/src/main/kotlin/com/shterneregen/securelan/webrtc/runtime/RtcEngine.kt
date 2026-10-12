package com.shterneregen.securelan.webrtc.runtime

import com.shterneregen.securelan.common.model.rtc.RtcSessionMode
import com.shterneregen.securelan.common.model.rtc.RtcSignalEnvelope
import com.shterneregen.securelan.webrtc.event.RtcEvent
import java.util.function.Consumer

/**
 * Low-level WebRTC media engine contract managing peer connections, tracks,
 * RTCDataChannel communication, and signaling dispatch.
 */
interface RtcEngine : AutoCloseable {
    /**
     * Queries the current operational status of the WebRTC runtime provider.
     *
     * @return runtime availability and provider diagnostic details.
     */
    fun status(): RtcRuntimeStatus

    /**
     * Initializes and starts a new outbound WebRTC peer session, creating a local offer.
     *
     * @param sessionId unique session identifier.
     * @param localPeer nickname or identifier of the local peer.
     * @param remotePeer nickname or identifier of the target remote peer.
     * @param mode desired session mode (DATA, AUDIO, VIDEO, or AUDIO_VIDEO).
     * @param dataChannelLabel label for the primary RTCDataChannel.
     * @param audioCaptureDeviceId optional preferred audio capture device descriptor.
     * @param videoCaptureDeviceId optional preferred video capture device descriptor.
     * @param outboundSignalConsumer consumer for outbound signaling envelopes (offers, candidates).
     * @param eventConsumer consumer for runtime events and state updates.
     */
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

    /**
     * Handles an incoming signaling envelope from a remote peer (offer, answer, ICE candidate, hangup, error).
     *
     * @param signal incoming signaling envelope.
     * @param outboundSignalConsumer consumer for outbound response signals (such as answers or ICE candidates).
     * @param eventConsumer consumer for runtime events and state transitions.
     */
    fun handleRemoteSignal(
        signal: RtcSignalEnvelope,
        outboundSignalConsumer: Consumer<RtcSignalEnvelope>,
        eventConsumer: Consumer<RtcEvent>,
    )

    /**
     * Sends an arbitrary string payload across the open RTCDataChannel in the specified session.
     *
     * @param sessionId session identifier whose data channel should be used.
     * @param payload UTF-8 string payload to transmit.
     * @param eventConsumer consumer for diagnostic warnings or error notifications.
     */
    fun sendData(sessionId: String?, payload: String?, eventConsumer: Consumer<RtcEvent>)

    /**
     * Closes an active WebRTC session and releases all associated native media and transport resources.
     *
     * @param sessionId identifier of the session to terminate.
     * @param eventConsumer consumer for state notifications upon termination.
     */
    fun closeSession(sessionId: String?, eventConsumer: Consumer<RtcEvent>)

    /**
     * Releases all sessions, audio modules, and native factory resources managed by this engine.
     */
    override fun close() {
    }
}
