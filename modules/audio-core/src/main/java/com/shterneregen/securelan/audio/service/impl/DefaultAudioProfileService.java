package com.shterneregen.securelan.audio.service.impl;

import com.shterneregen.securelan.audio.service.AudioCallProfile;
import com.shterneregen.securelan.audio.service.AudioProfileService;

public class DefaultAudioProfileService implements AudioProfileService {
    @Override
    public AudioCallProfile defaultProfile() {
        return new AudioCallProfile(48_000, 2, true, true);
    }
}
