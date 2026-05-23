package com.shterneregen.securelan.webrtc.runtime.video

import dev.onvoid.webrtc.media.video.VideoCaptureCapability

class VideoCapabilitySelector {
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

    fun describe(capability: VideoCaptureCapability?): String {
        if (capability == null) {
            return "<null>"
        }
        return "${capability.width}x${capability.height}@${capability.frameRate}fps"
    }

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
