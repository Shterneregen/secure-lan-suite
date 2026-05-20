package com.shterneregen.securelan.common.model.rtc

import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.LinkedHashMap
import java.util.Objects

class RtcSignalCodec private constructor() {
    companion object {
        @JvmStatic
        fun serialize(envelope: RtcSignalEnvelope): String {
            Objects.requireNonNull(envelope, "envelope must not be null")
            val fields = linkedMapOf(
                "v" to "1",
                "sessionId" to encode(envelope.sessionId()),
                "fromPeer" to encode(envelope.fromPeer()),
                "toPeer" to encode(envelope.toPeer()),
                "type" to envelope.type().name,
                "mode" to envelope.mode().name,
                "dataChannelLabel" to encode(envelope.dataChannelLabel()),
                "audioEnabled" to envelope.audioEnabled().toString(),
                "videoEnabled" to envelope.videoEnabled().toString(),
                "sdp" to encode(envelope.sdp()),
                "iceCandidate" to encode(envelope.iceCandidate()),
                "sdpMid" to encode(envelope.sdpMid()),
                "sdpMLineIndex" to envelope.sdpMLineIndex().toString(),
                "message" to encode(envelope.message()),
            )
            return fields.entries.joinToString(";") { entry -> "${entry.key}=${entry.value}" }
        }

        @JvmStatic
        fun deserialize(payload: String): RtcSignalEnvelope {
            Objects.requireNonNull(payload, "payload must not be null")
            val fields = LinkedHashMap<String, String>()
            for (token in payload.split(';')) {
                val splitIndex = token.indexOf('=')
                if (splitIndex <= 0) {
                    continue
                }
                fields[token.substring(0, splitIndex)] = token.substring(splitIndex + 1)
            }
            return RtcSignalEnvelope(
                decode(fields["sessionId"]),
                decode(fields["fromPeer"]),
                decode(fields["toPeer"]),
                RtcSignalType.valueOf(fields.getOrDefault("type", RtcSignalType.ERROR.name)),
                RtcSessionMode.valueOf(fields.getOrDefault("mode", RtcSessionMode.DATA.name)),
                decode(fields["dataChannelLabel"]),
                fields.getOrDefault("audioEnabled", "false").toBoolean(),
                fields.getOrDefault("videoEnabled", "false").toBoolean(),
                decode(fields["sdp"]),
                decode(fields["iceCandidate"]),
                decode(fields["sdpMid"]),
                fields.getOrDefault("sdpMLineIndex", "-1").toInt(),
                decode(fields["message"]),
            )
        }

        private fun encode(value: String?): String {
            val safeValue = value ?: ""
            return Base64.getUrlEncoder().withoutPadding().encodeToString(safeValue.toByteArray(StandardCharsets.UTF_8))
        }

        private fun decode(value: String?): String {
            if (value.isNullOrBlank()) {
                return ""
            }
            return String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
        }
    }
}
