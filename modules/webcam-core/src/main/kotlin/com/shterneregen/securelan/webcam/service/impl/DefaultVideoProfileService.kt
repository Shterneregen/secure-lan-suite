package com.shterneregen.securelan.webcam.service.impl

import com.shterneregen.securelan.webcam.service.VideoCallProfile
import com.shterneregen.securelan.webcam.service.VideoProfileService

/**
 * Default implementation of [VideoProfileService].
 *
 * Provides standard 720p HD (1280x720 at 30 fps) video profile settings
 * configured for both camera video and screen sharing.
 */
class DefaultVideoProfileService : VideoProfileService {

    override fun defaultProfile(): VideoCallProfile = DEFAULT_PROFILE

    companion object {
        /**
         * Standard default profile: 1280x720 resolution at 30 frames per second
         * with screen share support enabled.
         */
        val DEFAULT_PROFILE: VideoCallProfile = VideoCallProfile(
            width = 1280,
            height = 720,
            framesPerSecond = 30,
            screenShareReady = true,
        )
    }
}
