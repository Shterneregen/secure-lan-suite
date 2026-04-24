package com.shterneregen.securelan.webrtc.runtime.video;

import dev.onvoid.webrtc.media.video.VideoCaptureCapability;

import java.util.List;

public final class VideoCapabilitySelector {
    private static final int SAFE_WIDTH = 640;
    private static final int SAFE_HEIGHT = 480;
    private static final int LOW_FRAME_RATE = 15;
    private static final int STANDARD_FRAME_RATE = 30;

    public VideoCaptureCapability select(List<VideoCaptureCapability> capabilities) {
        if (capabilities == null || capabilities.isEmpty()) {
            return null;
        }

        VideoCaptureCapability bestVga15 = null;
        VideoCaptureCapability bestVga30 = null;
        VideoCaptureCapability safestFallback = null;

        for (VideoCaptureCapability capability : capabilities) {
            if (capability == null) {
                continue;
            }

            int width = Math.max(capability.width, 0);
            int height = Math.max(capability.height, 0);
            int fps = Math.max(capability.frameRate, 0);
            if (width == 0 || height == 0) {
                continue;
            }

            if (safestFallback == null || compareCapabilitySafety(capability, safestFallback) < 0) {
                safestFallback = capability;
            }

            if (width <= SAFE_WIDTH && height <= SAFE_HEIGHT && fps > 0 && fps <= LOW_FRAME_RATE) {
                if (bestVga15 == null || compareCapabilityQuality(capability, bestVga15) > 0) {
                    bestVga15 = capability;
                }
                continue;
            }

            if (width <= SAFE_WIDTH && height <= SAFE_HEIGHT && fps > 0 && fps <= STANDARD_FRAME_RATE) {
                if (bestVga30 == null || compareCapabilityQuality(capability, bestVga30) > 0) {
                    bestVga30 = capability;
                }
            }
        }

        if (bestVga15 != null) {
            return bestVga15;
        }
        if (bestVga30 != null) {
            return bestVga30;
        }
        return safestFallback != null ? safestFallback : firstNonNull(capabilities);
    }

    public String describe(VideoCaptureCapability capability) {
        if (capability == null) {
            return "<null>";
        }
        return capability.width + "x" + capability.height + "@" + capability.frameRate + "fps";
    }

    public String summarize(List<VideoCaptureCapability> capabilities) {
        if (capabilities == null || capabilities.isEmpty()) {
            return "[]";
        }

        StringBuilder builder = new StringBuilder("[");
        int limit = Math.min(capabilities.size(), 8);
        for (int i = 0; i < limit; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(describe(capabilities.get(i)));
        }
        if (capabilities.size() > limit) {
            builder.append(", ... total=").append(capabilities.size());
        }
        builder.append(']');
        return builder.toString();
    }

    private int compareCapabilityQuality(VideoCaptureCapability left, VideoCaptureCapability right) {
        long leftArea = (long) Math.max(left.width, 0) * Math.max(left.height, 0);
        long rightArea = (long) Math.max(right.width, 0) * Math.max(right.height, 0);
        if (leftArea != rightArea) {
            return Long.compare(leftArea, rightArea);
        }
        return Integer.compare(Math.max(left.frameRate, 0), Math.max(right.frameRate, 0));
    }

    private int compareCapabilitySafety(VideoCaptureCapability left, VideoCaptureCapability right) {
        boolean leftHasFrameRate = Math.max(left.frameRate, 0) > 0;
        boolean rightHasFrameRate = Math.max(right.frameRate, 0) > 0;
        if (leftHasFrameRate != rightHasFrameRate) {
            return leftHasFrameRate ? -1 : 1;
        }

        long leftArea = (long) Math.max(left.width, 0) * Math.max(left.height, 0);
        long rightArea = (long) Math.max(right.width, 0) * Math.max(right.height, 0);
        if (leftArea != rightArea) {
            return Long.compare(leftArea, rightArea);
        }
        return Integer.compare(Math.max(left.frameRate, 0), Math.max(right.frameRate, 0));
    }

    private VideoCaptureCapability firstNonNull(List<VideoCaptureCapability> capabilities) {
        for (VideoCaptureCapability capability : capabilities) {
            if (capability != null) {
                return capability;
            }
        }
        return null;
    }
}
