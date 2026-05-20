package com.shterneregen.securelan.common.model.rtc

enum class RtcSessionState {
    IDLE,
    NEGOTIATING,
    CONNECTING,
    CONNECTED,
    CLOSING,
    CLOSED,
    FAILED,
    UNAVAILABLE,
}
