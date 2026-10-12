package com.shterneregen.securelan.webcam.service

/**
 * Service providing recommended video profiles and default configurations
 * for video calling and media streams.
 *
 * Implementations are expected to be thread-safe.
 */
interface VideoProfileService {

    /**
     * Returns the default [VideoCallProfile] for video sessions.
     *
     * @return The default video call profile; never `null`.
     */
    fun defaultProfile(): VideoCallProfile
}
