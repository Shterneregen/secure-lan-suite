package com.shterneregen.securelan.webrtc.runtime.video;

import dev.onvoid.webrtc.PeerConnectionFactory;
import dev.onvoid.webrtc.media.MediaDevices;
import dev.onvoid.webrtc.media.video.VideoCaptureCapability;
import dev.onvoid.webrtc.media.video.VideoDevice;
import dev.onvoid.webrtc.media.video.VideoDeviceSource;
import dev.onvoid.webrtc.media.video.VideoTrack;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class VideoCaptureSession implements AutoCloseable {
    private final VideoDeviceSource source;
    private final VideoTrack track;
    private final RtcVideoDiagnostics diagnostics;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private VideoCaptureSession(VideoDeviceSource source, VideoTrack track, RtcVideoDiagnostics diagnostics) {
        this.source = source;
        this.track = track;
        this.diagnostics = diagnostics;
    }

    public static VideoCaptureSession start(
            PeerConnectionFactory factory,
            String sessionId,
            String preferredCameraId,
            RtcVideoDiagnostics diagnostics,
            VideoCapabilitySelector capabilitySelector
    ) {
        Objects.requireNonNull(factory, "factory must not be null");
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(diagnostics, "diagnostics must not be null");
        Objects.requireNonNull(capabilitySelector, "capabilitySelector must not be null");

        List<VideoDevice> cameras = MediaDevices.getVideoCaptureDevices();
        if (cameras == null || cameras.isEmpty()) {
            diagnostics.warn("No camera devices were detected for realtime video.");
            return null;
        }

        VideoDevice camera = selectCamera(cameras, preferredCameraId);
        diagnostics.diag("detected video devices count=" + cameras.size() + " selected=" + safeToString(camera));
        VideoDeviceSource source = new VideoDeviceSource();
        boolean started = false;
        try {
            source.setVideoCaptureDevice(camera);

            List<VideoCaptureCapability> capabilities = MediaDevices.getVideoCaptureCapabilities(camera);
            diagnostics.diag("video capabilities for " + safeToString(camera) + " -> " + capabilitySelector.summarize(capabilities));

            if (capabilities != null && !capabilities.isEmpty()) {
                VideoCaptureCapability selectedCapability = capabilitySelector.select(capabilities);
                if (selectedCapability != null) {
                    source.setVideoCaptureCapability(selectedCapability);
                    diagnostics.diag("selected video capability " + capabilitySelector.describe(selectedCapability) + " for " + safeToString(camera));
                } else {
                    diagnostics.diag("camera reported only invalid capabilities, using default capture settings for " + safeToString(camera));
                }
            } else {
                diagnostics.diag("camera reported no explicit capabilities, using default capture settings for " + safeToString(camera));
            }

            diagnostics.diag("starting video source for " + safeToString(camera));
            source.start();
            started = true;
            diagnostics.diag("video source started for " + safeToString(camera));

            VideoTrack track = factory.createVideoTrack("video-" + sessionId, source);
            if (track == null) {
                throw new IllegalStateException("PeerConnectionFactory returned null VideoTrack");
            }
            return new VideoCaptureSession(source, track, diagnostics);
        } catch (Throwable error) {
            if (started) {
                try {
                    source.stop();
                } catch (Throwable ignored) {
                    diagnostics.error("Failed while stopping video source after startup failure", ignored);
                }
            }
            try {
                source.dispose();
            } catch (Throwable ignored) {
                diagnostics.error("Failed while disposing video source after startup failure", ignored);
            }
            throw error;
        }
    }

    public VideoTrack track() {
        return track;
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        try {
            track.dispose();
        } catch (Throwable error) {
            diagnostics.error("Failed while disposing video track", error);
        }
        try {
            source.stop();
        } catch (Throwable error) {
            diagnostics.error("Failed while stopping video source", error);
        }
        try {
            source.dispose();
        } catch (Throwable error) {
            diagnostics.error("Failed while disposing video source", error);
        }
    }

    private static String safeToString(Object value) {
        try {
            return String.valueOf(value);
        } catch (Throwable error) {
            return value == null ? "<null>" : value.getClass().getSimpleName();
        }
    }

    private static VideoDevice selectCamera(List<VideoDevice> cameras, String preferredCameraId) {
        String normalized = preferredCameraId == null ? "" : preferredCameraId.trim();
        if (!normalized.isBlank()) {
            for (VideoDevice camera : cameras) {
                try {
                    if (normalized.equals(camera.getDescriptor())) {
                        return camera;
                    }
                } catch (Throwable ignored) {
                    // Fall back to the first camera when native metadata access fails.
                }
            }
        }
        return cameras.getFirst();
    }
}
