package com.shterneregen.securelan.common.model.rtc;

public enum RtcSessionMode {
    DATA,
    AUDIO,
    VIDEO,
    AUDIO_VIDEO;

    public boolean audioEnabled() {
        return this == AUDIO || this == AUDIO_VIDEO;
    }

    public boolean videoEnabled() {
        return this == VIDEO || this == AUDIO_VIDEO;
    }
}
