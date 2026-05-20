package com.shterneregen.securelan.audio.service.impl

import com.shterneregen.securelan.audio.service.AudioCallProfile
import com.shterneregen.securelan.audio.service.AudioProfileService

class DefaultAudioProfileService : AudioProfileService {
    override fun defaultProfile(): AudioCallProfile {
        return AudioCallProfile(48_000, 2, echoCancellation = true, noiseSuppression = true)
    }
}
