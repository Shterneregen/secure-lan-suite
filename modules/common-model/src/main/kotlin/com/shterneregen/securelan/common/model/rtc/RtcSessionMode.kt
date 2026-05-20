package com.shterneregen.securelan.common.model.rtc

enum class RtcSessionMode {
    DATA,
    AUDIO,
    VIDEO,
    AUDIO_VIDEO;

    fun audioEnabled(): Boolean = this == AUDIO || this == AUDIO_VIDEO

    fun videoEnabled(): Boolean = this == VIDEO || this == AUDIO_VIDEO
}
