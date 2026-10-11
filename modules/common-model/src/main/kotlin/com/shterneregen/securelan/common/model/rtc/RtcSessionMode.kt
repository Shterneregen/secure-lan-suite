package com.shterneregen.securelan.common.model.rtc

/** Media capabilities requested for an RTC session. */
enum class RtcSessionMode {
    /** Data channel only. */
    DATA,
    /** Audio enabled. */
    AUDIO,
    /** Video enabled. */
    VIDEO,
    /** Audio and video enabled. */
    AUDIO_VIDEO;

    /** Returns whether this mode includes audio. */
    fun audioEnabled(): Boolean = this == AUDIO || this == AUDIO_VIDEO

    /** Returns whether this mode includes video. */
    fun videoEnabled(): Boolean = this == VIDEO || this == AUDIO_VIDEO
}
