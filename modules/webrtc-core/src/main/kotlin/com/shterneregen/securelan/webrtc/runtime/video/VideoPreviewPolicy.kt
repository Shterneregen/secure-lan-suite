package com.shterneregen.securelan.webrtc.runtime.video

import java.util.Objects

class VideoPreviewPolicy(
    private val localPreviewEnabled: Boolean,
    private val remotePreviewEnabled: Boolean,
    previewIntervalNanos: Long,
) {
    private val previewIntervalNanos: Long = previewIntervalNanos.coerceAtLeast(MIN_PREVIEW_INTERVAL_NANOS)

    fun localPreviewEnabled(): Boolean = localPreviewEnabled

    fun remotePreviewEnabled(): Boolean = remotePreviewEnabled
    fun previewIntervalNanos(): Long = previewIntervalNanos

    fun enabled(local: Boolean): Boolean = if (local) localPreviewEnabled else remotePreviewEnabled

    override fun equals(other: Any?): Boolean =
        this === other ||
            (other is VideoPreviewPolicy &&
                localPreviewEnabled == other.localPreviewEnabled &&
                remotePreviewEnabled == other.remotePreviewEnabled &&
                previewIntervalNanos == other.previewIntervalNanos)

    override fun hashCode(): Int = Objects.hash(localPreviewEnabled, remotePreviewEnabled, previewIntervalNanos)

    override fun toString(): String =
        "VideoPreviewPolicy[localPreviewEnabled=$localPreviewEnabled, remotePreviewEnabled=$remotePreviewEnabled, previewIntervalNanos=$previewIntervalNanos]"

    companion object {
        private const val DEFAULT_PREVIEW_INTERVAL_NANOS = 150_000_000L
        private const val MIN_PREVIEW_INTERVAL_NANOS = 16_000_000L

        @JvmStatic
        fun fromSystemProperties(): VideoPreviewPolicy = VideoPreviewPolicy(
            java.lang.Boolean.parseBoolean(System.getProperty("securelan.rtc.videoPreview.local.enabled", "true")),
            java.lang.Boolean.parseBoolean(System.getProperty("securelan.rtc.videoPreview.remote.enabled", "true")),
            java.lang.Long.getLong("securelan.rtc.videoPreview.intervalNanos", DEFAULT_PREVIEW_INTERVAL_NANOS),
        )
    }
}
