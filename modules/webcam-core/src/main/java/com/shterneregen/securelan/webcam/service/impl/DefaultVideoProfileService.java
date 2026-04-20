package com.shterneregen.securelan.webcam.service.impl;

import com.shterneregen.securelan.webcam.service.VideoCallProfile;
import com.shterneregen.securelan.webcam.service.VideoProfileService;

public class DefaultVideoProfileService implements VideoProfileService {
    @Override
    public VideoCallProfile defaultProfile() {
        return new VideoCallProfile(1280, 720, 30, true);
    }
}
