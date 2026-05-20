package com.shterneregen.securelan.common.model.rtc

import java.util.Objects
import java.util.UUID

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

    fun sessionId(): String = sessionId
    fun fromPeer(): String = fromPeer
    fun toPeer(): String = toPeer
    fun type(): RtcSignalType = type
    fun mode(): RtcSessionMode = mode
    fun dataChannelLabel(): String = dataChannelLabel
    fun audioEnabled(): Boolean = audioEnabled
    fun videoEnabled(): Boolean = videoEnabled
    fun sdp(): String = sdp
    fun iceCandidate(): String = iceCandidate
    fun sdpMid(): String = sdpMid
    fun sdpMLineIndex(): Int = sdpMLineIndex
    fun message(): String = message

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

        @JvmStatic
        fun offer(fromPeer: String, toPeer: String, mode: RtcSessionMode, dataChannelLabel: String, sdp: String): RtcSignalEnvelope =
            RtcSignalEnvelope(UUID.randomUUID().toString(), fromPeer, toPeer, RtcSignalType.OFFER, mode, dataChannelLabel, mode.audioEnabled(), mode.videoEnabled(), sdp, "", "", -1, "")

        @JvmStatic
        fun answer(sessionId: String, fromPeer: String, toPeer: String, mode: RtcSessionMode, dataChannelLabel: String, sdp: String): RtcSignalEnvelope =
            RtcSignalEnvelope(sessionId, fromPeer, toPeer, RtcSignalType.ANSWER, mode, dataChannelLabel, mode.audioEnabled(), mode.videoEnabled(), sdp, "", "", -1, "")

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
        ): RtcSignalEnvelope = RtcSignalEnvelope(sessionId, fromPeer, toPeer, RtcSignalType.ICE_CANDIDATE, mode, dataChannelLabel, mode.audioEnabled(), mode.videoEnabled(), "", candidate, sdpMid, sdpMLineIndex, "")

        @JvmStatic
        fun hangup(sessionId: String, fromPeer: String, toPeer: String, mode: RtcSessionMode, dataChannelLabel: String, message: String): RtcSignalEnvelope =
            RtcSignalEnvelope(sessionId, fromPeer, toPeer, RtcSignalType.HANGUP, mode, dataChannelLabel, mode.audioEnabled(), mode.videoEnabled(), "", "", "", -1, message)

        @JvmStatic
        fun error(sessionId: String, fromPeer: String, toPeer: String, mode: RtcSessionMode, dataChannelLabel: String, message: String): RtcSignalEnvelope =
            RtcSignalEnvelope(sessionId, fromPeer, toPeer, RtcSignalType.ERROR, mode, dataChannelLabel, mode.audioEnabled(), mode.videoEnabled(), "", "", "", -1, message)
    }
}
