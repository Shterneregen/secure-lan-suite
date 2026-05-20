package com.shterneregen.securelan.webcam.service.impl

import com.shterneregen.securelan.webcam.service.VideoCallProfile
import com.shterneregen.securelan.webcam.service.VideoProfileService

class DefaultVideoProfileService : VideoProfileService {
    override fun defaultProfile(): VideoCallProfile {
        return VideoCallProfile(1280, 720, 30, screenShareReady = true)
    }
}
