package com.shterneregen.securelan.common.model.rtc;

import java.util.Objects;
import java.util.UUID;

public record RtcSignalEnvelope(
        String sessionId,
        String fromPeer,
        String toPeer,
        RtcSignalType type,
        RtcSessionMode mode,
        String dataChannelLabel,
        boolean audioEnabled,
        boolean videoEnabled,
        String sdp,
        String iceCandidate,
        String sdpMid,
        int sdpMLineIndex,
        String message
) {
    public RtcSignalEnvelope {
        sessionId = normalize(sessionId, UUID.randomUUID().toString());
        fromPeer = normalize(fromPeer, "");
        toPeer = normalize(toPeer, "");
        Objects.requireNonNull(type, "type must not be null");
        mode = mode == null ? RtcSessionMode.DATA : mode;
        dataChannelLabel = normalize(dataChannelLabel, "securelan-data");
        sdp = normalize(sdp, "");
        iceCandidate = normalize(iceCandidate, "");
        sdpMid = normalize(sdpMid, "");
        message = normalize(message, "");
    }

    private static String normalize(String value, String fallback) {
        return value == null ? fallback : value;
    }

    public RtcSignalEnvelope withSender(String sender) {
        return new RtcSignalEnvelope(sessionId, sender, toPeer, type, mode, dataChannelLabel, audioEnabled, videoEnabled, sdp, iceCandidate, sdpMid, sdpMLineIndex, message);
    }

    public boolean targets(String nickname) {
        return toPeer.isBlank() || toPeer.equalsIgnoreCase(nickname);
    }

    public static RtcSignalEnvelope offer(String fromPeer, String toPeer, RtcSessionMode mode, String dataChannelLabel, String sdp) {
        return new RtcSignalEnvelope(UUID.randomUUID().toString(), fromPeer, toPeer, RtcSignalType.OFFER, mode, dataChannelLabel, mode.audioEnabled(), mode.videoEnabled(), sdp, "", "", -1, "");
    }

    public static RtcSignalEnvelope answer(String sessionId, String fromPeer, String toPeer, RtcSessionMode mode, String dataChannelLabel, String sdp) {
        return new RtcSignalEnvelope(sessionId, fromPeer, toPeer, RtcSignalType.ANSWER, mode, dataChannelLabel, mode.audioEnabled(), mode.videoEnabled(), sdp, "", "", -1, "");
    }

    public static RtcSignalEnvelope iceCandidate(String sessionId, String fromPeer, String toPeer, RtcSessionMode mode, String dataChannelLabel, String candidate, String sdpMid, int sdpMLineIndex) {
        return new RtcSignalEnvelope(sessionId, fromPeer, toPeer, RtcSignalType.ICE_CANDIDATE, mode, dataChannelLabel, mode.audioEnabled(), mode.videoEnabled(), "", candidate, sdpMid, sdpMLineIndex, "");
    }

    public static RtcSignalEnvelope hangup(String sessionId, String fromPeer, String toPeer, RtcSessionMode mode, String dataChannelLabel, String message) {
        return new RtcSignalEnvelope(sessionId, fromPeer, toPeer, RtcSignalType.HANGUP, mode, dataChannelLabel, mode.audioEnabled(), mode.videoEnabled(), "", "", "", -1, message);
    }

    public static RtcSignalEnvelope error(String sessionId, String fromPeer, String toPeer, RtcSessionMode mode, String dataChannelLabel, String message) {
        return new RtcSignalEnvelope(sessionId, fromPeer, toPeer, RtcSignalType.ERROR, mode, dataChannelLabel, mode.audioEnabled(), mode.videoEnabled(), "", "", "", -1, message);
    }
}
