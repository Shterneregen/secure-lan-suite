package com.shterneregen.securelan.webcam.service

/**
 * Immutable configuration profile defining video capture and streaming parameters
 * for WebRTC video calls and screen sharing.
 *
 * Instances encapsulate output resolution, target frame rate, and whether the
 * configuration is optimized for screen sharing content.
 *
 * @property width The horizontal frame resolution in pixels. Must be greater than zero.
 * @property height The vertical frame resolution in pixels. Must be greater than zero.
 * @property framesPerSecond The target capture frame rate in frames per second. Must be greater than zero.
 * @property screenShareReady `true` if this profile is suitable for screen sharing; `false` otherwise.
 * @throws IllegalArgumentException If [width], [height], or [framesPerSecond] is less than or equal to zero.
 */
@JvmRecord
data class VideoCallProfile(
    val width: Int,
    val height: Int,
    val framesPerSecond: Int,
    val screenShareReady: Boolean,
) {
    init {
        require(width > 0) { "width must be positive, but was $width" }
        require(height > 0) { "height must be positive, but was $height" }
        require(framesPerSecond > 0) { "framesPerSecond must be positive, but was $framesPerSecond" }
    }
}
