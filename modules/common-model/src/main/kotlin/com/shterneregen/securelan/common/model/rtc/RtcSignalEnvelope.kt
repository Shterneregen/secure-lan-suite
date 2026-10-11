package com.shterneregen.securelan.common.model.rtc

import java.util.Objects
import java.util.UUID

/**
 * Value object carrying an RTC signalling message between peers.
 *
 * Nullable text fields are normalized to defaults. A null [sessionId] receives a new UUID,
 * while a null [mode] becomes [RtcSessionMode.DATA]. Empty strings are preserved.
 *
 * @param sessionId identifier shared by signals in one session.
 * @param fromPeer sender's peer name.
 * @param toPeer intended recipient; empty means any peer.
 * @param type signalling action.
 * @param mode requested media mode.
 * @param dataChannelLabel label for the data channel.
 * @param audioEnabled whether audio is enabled for this signal.
 * @param videoEnabled whether video is enabled for this signal.
 * @param sdp session description for an offer or answer.
 * @param iceCandidate ICE candidate text.
 * @param sdpMid media stream identifier for an ICE candidate.
 * @param sdpMLineIndex media line index for an ICE candidate; -1 when absent.
 * @param message optional explanatory text.
 */
class RtcSignalEnvelope(
    sessionId: String?,
    fromPeer: String?,
    toPeer: String?,
    type: RtcSignalType,
    mode: RtcSessionMode?,
    dataChannelLabel: String?,
    audioEnabled: Boolean,
    videoEnabled: Boolean,
    sdp: String?,
    iceCandidate: String?,
    sdpMid: String?,
    private val sdpMLineIndex: Int,
    message: String?,
) {
    private val sessionId: String = normalize(sessionId, UUID.randomUUID().toString())
    private val fromPeer: String = normalize(fromPeer, "")
    private val toPeer: String = normalize(toPeer, "")
    private val type: RtcSignalType = Objects.requireNonNull(type, "type must not be null")
    private val mode: RtcSessionMode = mode ?: RtcSessionMode.DATA
    private val dataChannelLabel: String = normalize(dataChannelLabel, "securelan-data")
    private val audioEnabled: Boolean = audioEnabled
    private val videoEnabled: Boolean = videoEnabled
    private val sdp: String = normalize(sdp, "")
    private val iceCandidate: String = normalize(iceCandidate, "")
    private val sdpMid: String = normalize(sdpMid, "")
    private val message: String = normalize(message, "")

    /** Returns the session identifier. */
    fun sessionId(): String = sessionId
    /** Returns the sender's peer name. */
    fun fromPeer(): String = fromPeer
    /** Returns the intended recipient, or an empty string for any peer. */
    fun toPeer(): String = toPeer
    /** Returns the signalling action. */
    fun type(): RtcSignalType = type
    /** Returns the requested media mode. */
    fun mode(): RtcSessionMode = mode
    /** Returns the data channel label. */
    fun dataChannelLabel(): String = dataChannelLabel
    /** Returns whether audio is enabled. */
    fun audioEnabled(): Boolean = audioEnabled
    /** Returns whether video is enabled. */
    fun videoEnabled(): Boolean = videoEnabled
    /** Returns the session description. */
    fun sdp(): String = sdp
    /** Returns the ICE candidate text. */
    fun iceCandidate(): String = iceCandidate
    /** Returns the media stream identifier. */
    fun sdpMid(): String = sdpMid
    /** Returns the media line index, or -1 when absent. */
    fun sdpMLineIndex(): Int = sdpMLineIndex
    /** Returns the optional explanatory message. */
    fun message(): String = message

    /** Returns a copy of this signal with [sender] as its sender. */
    fun withSender(sender: String): RtcSignalEnvelope = RtcSignalEnvelope(
        sessionId,
        sender,
        toPeer,
        type,
        mode,
        dataChannelLabel,
        audioEnabled,
        videoEnabled,
        sdp,
        iceCandidate,
        sdpMid,
        sdpMLineIndex,
        message,
    )

    /** Returns whether this signal is addressed to [nickname] or has no recipient. */
    fun targets(nickname: String): Boolean = toPeer.isBlank() || toPeer.equals(nickname, ignoreCase = true)

    override fun equals(other: Any?): Boolean =
        this === other ||
            (other is RtcSignalEnvelope &&
                sessionId == other.sessionId &&
                fromPeer == other.fromPeer &&
                toPeer == other.toPeer &&
                type == other.type &&
                mode == other.mode &&
                dataChannelLabel == other.dataChannelLabel &&
                audioEnabled == other.audioEnabled &&
                videoEnabled == other.videoEnabled &&
                sdp == other.sdp &&
                iceCandidate == other.iceCandidate &&
                sdpMid == other.sdpMid &&
                sdpMLineIndex == other.sdpMLineIndex &&
                message == other.message)

    override fun hashCode(): Int = Objects.hash(
        sessionId,
        fromPeer,
        toPeer,
        type,
        mode,
        dataChannelLabel,
        audioEnabled,
        videoEnabled,
        sdp,
        iceCandidate,
        sdpMid,
        sdpMLineIndex,
        message,
    )

    override fun toString(): String =
        "RtcSignalEnvelope[sessionId=$sessionId, fromPeer=$fromPeer, toPeer=$toPeer, type=$type, mode=$mode, " +
            "dataChannelLabel=$dataChannelLabel, audioEnabled=$audioEnabled, videoEnabled=$videoEnabled, sdp=$sdp, " +
            "iceCandidate=$iceCandidate, sdpMid=$sdpMid, sdpMLineIndex=$sdpMLineIndex, message=$message]"

    companion object {
        private fun normalize(value: String?, fallback: String): String = value ?: fallback

        /** Creates a new offer with a generated session identifier. */
        @JvmStatic
        fun offer(fromPeer: String, toPeer: String, mode: RtcSessionMode, dataChannelLabel: String, sdp: String): RtcSignalEnvelope =
            RtcSignalEnvelope(
                UUID.randomUUID().toString(), fromPeer, toPeer, RtcSignalType.OFFER, mode,
                dataChannelLabel, mode.audioEnabled(), mode.videoEnabled(), sdp, "", "", -1, "",
            )

        /** Creates an answer for an existing [sessionId]. */
        @JvmStatic
        fun answer(sessionId: String, fromPeer: String, toPeer: String, mode: RtcSessionMode, dataChannelLabel: String, sdp: String): RtcSignalEnvelope =
            RtcSignalEnvelope(
                sessionId, fromPeer, toPeer, RtcSignalType.ANSWER, mode,
                dataChannelLabel, mode.audioEnabled(), mode.videoEnabled(), sdp, "", "", -1, "",
            )

        /** Creates an ICE candidate signal for an existing [sessionId]. */
        @JvmStatic
        fun iceCandidate(
            sessionId: String,
            fromPeer: String,
            toPeer: String,
            mode: RtcSessionMode,
            dataChannelLabel: String,
            candidate: String,
            sdpMid: String,
            sdpMLineIndex: Int,
        ): RtcSignalEnvelope = RtcSignalEnvelope(
            sessionId, fromPeer, toPeer, RtcSignalType.ICE_CANDIDATE, mode,
            dataChannelLabel, mode.audioEnabled(), mode.videoEnabled(), "", candidate, sdpMid, sdpMLineIndex, "",
        )

        /** Creates a hangup signal for an existing [sessionId]. */
        @JvmStatic
        fun hangup(sessionId: String, fromPeer: String, toPeer: String, mode: RtcSessionMode, dataChannelLabel: String, message: String): RtcSignalEnvelope =
            RtcSignalEnvelope(
                sessionId, fromPeer, toPeer, RtcSignalType.HANGUP, mode,
                dataChannelLabel, mode.audioEnabled(), mode.videoEnabled(), "", "", "", -1, message,
            )

        /** Creates an error signal for an existing [sessionId]. */
        @JvmStatic
        fun error(sessionId: String, fromPeer: String, toPeer: String, mode: RtcSessionMode, dataChannelLabel: String, message: String): RtcSignalEnvelope =
            RtcSignalEnvelope(
                sessionId, fromPeer, toPeer, RtcSignalType.ERROR, mode,
                dataChannelLabel, mode.audioEnabled(), mode.videoEnabled(), "", "", "", -1, message,
            )
    }
}
