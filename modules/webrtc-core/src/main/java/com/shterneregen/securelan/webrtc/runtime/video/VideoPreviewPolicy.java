package com.shterneregen.securelan.webrtc.runtime.video;

public record VideoPreviewPolicy(
        boolean localPreviewEnabled,
        boolean remotePreviewEnabled,
        long previewIntervalNanos
) {
    private static final long DEFAULT_PREVIEW_INTERVAL_NANOS = 150_000_000L;
    private static final long MIN_PREVIEW_INTERVAL_NANOS = 16_000_000L;

    public VideoPreviewPolicy {
        previewIntervalNanos = Math.max(previewIntervalNanos, MIN_PREVIEW_INTERVAL_NANOS);
    }

    public static VideoPreviewPolicy fromSystemProperties() {
        return new VideoPreviewPolicy(
                Boolean.parseBoolean(System.getProperty("securelan.rtc.videoPreview.local.enabled", "false")),
                Boolean.parseBoolean(System.getProperty("securelan.rtc.videoPreview.remote.enabled", "true")),
                Long.getLong("securelan.rtc.videoPreview.intervalNanos", DEFAULT_PREVIEW_INTERVAL_NANOS)
        );
    }

    public boolean enabled(boolean local) {
        return local ? localPreviewEnabled : remotePreviewEnabled;
    }
}
