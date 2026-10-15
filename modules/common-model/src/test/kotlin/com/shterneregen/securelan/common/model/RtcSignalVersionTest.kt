package com.shterneregen.securelan.common.model

import com.shterneregen.securelan.common.model.rtc.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RtcSignalVersionTest {
    @Test fun explicitV1IsAcceptedAndOtherVersionsAreRejected() {
        val signal = RtcSignalEnvelope.hangup("id", "alice", "bob", RtcSessionMode.AUDIO, "data", "done")
        val payload = RtcSignalCodec.serialize(signal)
        assertEquals("id", RtcSignalCodec.deserialize(payload).sessionId())
        for (version in listOf("v=2", "v=", "v=x", "v=01")) {
            assertThrows(IllegalArgumentException::class.java) { RtcSignalCodec.deserialize(payload.replaceFirst("v=1", version)) }
        }
        assertThrows(IllegalArgumentException::class.java) { RtcSignalCodec.deserialize(payload.substringAfter(';')) }
        assertThrows(IllegalArgumentException::class.java) { RtcSignalCodec.deserialize("v=2;" + payload) }
    }
}
