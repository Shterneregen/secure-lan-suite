package com.shterneregen.securelan.common.model.rtc

/** Current state of an RTC session. */
enum class RtcSessionState {
    /** No session is active. */
    IDLE,
    /** Session parameters are being negotiated. */
    NEGOTIATING,
    /** A connection is being established. */
    CONNECTING,
    /** The session is connected. */
    CONNECTED,
    /** The session is shutting down. */
    CLOSING,
    /** The session has closed. */
    CLOSED,
    /** Session setup or operation failed. */
    FAILED,
    /** RTC support is unavailable. */
    UNAVAILABLE,
}
