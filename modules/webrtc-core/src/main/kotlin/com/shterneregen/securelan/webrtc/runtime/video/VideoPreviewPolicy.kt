package com.shterneregen.securelan.webrtc.runtime.video

import java.util.Objects

/**
 * Configuration policy controlling the enablement and throttling frequency
 * of local and remote video frame preview generation.
 *
 * Prevents UI rendering pipelines and CPU buffers from saturating by establishing
 * a minimum interval between consecutively processed preview frames.
 *
 * @param localPreviewEnabled `true` if local camera frames should be converted and previewed.
 * @param remotePreviewEnabled `true` if remote incoming video frames should be converted and previewed.
 * @param previewIntervalNanos minimum interval in nanoseconds between consecutive preview frames.
 */
class VideoPreviewPolicy(
    private val localPreviewEnabled: Boolean,
    private val remotePreviewEnabled: Boolean,
    previewIntervalNanos: Long,
) {
    private val previewIntervalNanos: Long = previewIntervalNanos.coerceAtLeast(MIN_PREVIEW_INTERVAL_NANOS)

    /** Returns whether local video preview conversion is enabled. */
    fun localPreviewEnabled(): Boolean = localPreviewEnabled

    /** Returns whether remote video preview conversion is enabled. */
    fun remotePreviewEnabled(): Boolean = remotePreviewEnabled

    /** Returns the minimum throttle interval in nanoseconds between preview frames. */
    fun previewIntervalNanos(): Long = previewIntervalNanos

    /**
     * Checks if preview conversion is enabled for the specified stream locality.
     *
     * @param local `true` for the local camera stream, `false` for remote streams.
     * @return `true` if enabled for the stream, otherwise `false`.
     */
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

        /**
         * Loads policy configuration from JVM system properties:
         * - `securelan.rtc.videoPreview.local.enabled` (default: true)
         * - `securelan.rtc.videoPreview.remote.enabled` (default: true)
         * - `securelan.rtc.videoPreview.intervalNanos` (default: 150,000,000 ns / ~6.6 fps)
         *
         * @return populated [VideoPreviewPolicy] instance.
         */
        @JvmStatic
        fun fromSystemProperties(): VideoPreviewPolicy = VideoPreviewPolicy(
            java.lang.Boolean.parseBoolean(System.getProperty("securelan.rtc.videoPreview.local.enabled", "true")),
            java.lang.Boolean.parseBoolean(System.getProperty("securelan.rtc.videoPreview.remote.enabled", "true")),
            java.lang.Long.getLong("securelan.rtc.videoPreview.intervalNanos", DEFAULT_PREVIEW_INTERVAL_NANOS),
        )
    }
}
