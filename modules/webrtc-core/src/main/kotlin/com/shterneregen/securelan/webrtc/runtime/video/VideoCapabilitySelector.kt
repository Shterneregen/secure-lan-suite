package com.shterneregen.securelan.webrtc.runtime.video

import dev.onvoid.webrtc.media.video.VideoCaptureCapability

/**
 * Selects an optimal [VideoCaptureCapability] from a list of hardware-supported camera modes.
 *
 * Prioritizes safe, low-overhead VGA resolution (640x480) at 15 or 30 fps over high-bandwidth HD modes
 * to ensure responsive local-network streaming and lower CPU load during software encoding and preview conversion.
 */
internal class VideoCapabilitySelector {
    /**
     * Chooses the most suitable capability from the supplied [capabilities] list.
     *
     * Preference order:
     * 1. VGA (<= 640x480) at <= 15 fps (lowest CPU overhead)
     * 2. VGA (<= 640x480) at <= 30 fps
     * 3. Smallest valid resolution with a positive frame rate
     * 4. First non-null capability fallback
     *
     * @param capabilities list of camera capabilities reported by the native device, or null.
     * @return selected [VideoCaptureCapability], or null if the list is null or empty.
     */
    fun select(capabilities: List<VideoCaptureCapability?>?): VideoCaptureCapability? {
        if (capabilities.isNullOrEmpty()) {
            return null
        }

        var bestVga15: VideoCaptureCapability? = null
        var bestVga30: VideoCaptureCapability? = null
        var safestFallback: VideoCaptureCapability? = null

        for (capability in capabilities) {
            capability ?: continue

            val width = capability.width.coerceAtLeast(0)
            val height = capability.height.coerceAtLeast(0)
            val fps = capability.frameRate.coerceAtLeast(0)
            if (width == 0 || height == 0) {
                continue
            }

            val currentSafestFallback = safestFallback
            if (currentSafestFallback == null || compareCapabilitySafety(capability, currentSafestFallback) < 0) {
                safestFallback = capability
            }

            if (width <= SAFE_WIDTH && height <= SAFE_HEIGHT && fps > 0 && fps <= LOW_FRAME_RATE) {
                val currentBestVga15 = bestVga15
                if (currentBestVga15 == null || compareCapabilityQuality(capability, currentBestVga15) > 0) {
                    bestVga15 = capability
                }
                continue
            }

            if (width <= SAFE_WIDTH && height <= SAFE_HEIGHT && fps > 0 && fps <= STANDARD_FRAME_RATE) {
                val currentBestVga30 = bestVga30
                if (currentBestVga30 == null || compareCapabilityQuality(capability, currentBestVga30) > 0) {
                    bestVga30 = capability
                }
            }
        }

        return bestVga15 ?: bestVga30 ?: safestFallback ?: firstNonNull(capabilities)
    }

    /**
     * Formats a capability into a concise human-readable string (e.g. `640x480@15fps`).
     *
     * @param capability the capability to describe.
     * @return formatted string, or `"<null>"` if null.
     */
    fun describe(capability: VideoCaptureCapability?): String {
        if (capability == null) {
            return "<null>"
        }
        return "${capability.width}x${capability.height}@${capability.frameRate}fps"
    }

    /**
     * Formats a collection of capabilities into a summarized string list for diagnostics.
     *
     * @param capabilities list of capabilities to summarize.
     * @return formatted summary string (e.g. `[640x480@15fps, 1280x720@30fps]`).
     */
    fun summarize(capabilities: List<VideoCaptureCapability?>?): String {
        if (capabilities.isNullOrEmpty()) {
            return "[]"
        }

        val builder = StringBuilder("[")
        val limit = minOf(capabilities.size, 8)
        for (index in 0 until limit) {
            if (index > 0) {
                builder.append(", ")
            }
            builder.append(describe(capabilities[index]))
        }
        if (capabilities.size > limit) {
            builder.append(", ... total=").append(capabilities.size)
        }
        builder.append(']')
        return builder.toString()
    }

    private fun compareCapabilityQuality(left: VideoCaptureCapability, right: VideoCaptureCapability): Int {
        val leftArea = left.width.coerceAtLeast(0).toLong() * left.height.coerceAtLeast(0)
        val rightArea = right.width.coerceAtLeast(0).toLong() * right.height.coerceAtLeast(0)
        if (leftArea != rightArea) {
            return leftArea.compareTo(rightArea)
        }
        return left.frameRate.coerceAtLeast(0).compareTo(right.frameRate.coerceAtLeast(0))
    }

    private fun compareCapabilitySafety(left: VideoCaptureCapability, right: VideoCaptureCapability): Int {
        val leftHasFrameRate = left.frameRate.coerceAtLeast(0) > 0
        val rightHasFrameRate = right.frameRate.coerceAtLeast(0) > 0
        if (leftHasFrameRate != rightHasFrameRate) {
            return if (leftHasFrameRate) -1 else 1
        }

        val leftArea = left.width.coerceAtLeast(0).toLong() * left.height.coerceAtLeast(0)
        val rightArea = right.width.coerceAtLeast(0).toLong() * right.height.coerceAtLeast(0)
        if (leftArea != rightArea) {
            return leftArea.compareTo(rightArea)
        }
        return left.frameRate.coerceAtLeast(0).compareTo(right.frameRate.coerceAtLeast(0))
    }

    private fun firstNonNull(capabilities: List<VideoCaptureCapability?>): VideoCaptureCapability? =
        capabilities.firstOrNull { it != null }

    private companion object {
        private const val SAFE_WIDTH = 640
        private const val SAFE_HEIGHT = 480
        private const val LOW_FRAME_RATE = 15
        private const val STANDARD_FRAME_RATE = 30
    }
}
