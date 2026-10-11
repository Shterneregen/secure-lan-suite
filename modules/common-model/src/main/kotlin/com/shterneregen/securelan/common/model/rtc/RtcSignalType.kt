package com.shterneregen.securelan.common.model.rtc

/** Type of message exchanged during RTC session signalling. */
enum class RtcSignalType {
    /** Starts negotiation with an SDP offer. */
    OFFER,
    /** Responds to an offer with an SDP answer. */
    ANSWER,
    /** Supplies an ICE candidate. */
    ICE_CANDIDATE,
    /** Ends an RTC session. */
    HANGUP,
    /** Reports a signalling or session error. */
    ERROR,
}
