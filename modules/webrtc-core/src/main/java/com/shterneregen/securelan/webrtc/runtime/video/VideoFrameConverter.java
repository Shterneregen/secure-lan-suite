package com.shterneregen.securelan.webrtc.runtime.video;

import dev.onvoid.webrtc.media.video.I420Buffer;
import dev.onvoid.webrtc.media.video.VideoFrame;

import java.nio.ByteBuffer;
import java.util.Objects;

public final class VideoFrameConverter {
    public byte[] convertToBgra(VideoFrame frame) {
        Objects.requireNonNull(frame, "frame must not be null");
        Objects.requireNonNull(frame.buffer, "frame buffer must not be null");
        I420Buffer i420 = frame.buffer.toI420();
        if (i420 == null) {
            throw new IllegalStateException("Video frame buffer cannot be converted to I420");
        }
        try {
            return convertI420ToBgra(i420);
        } finally {
            i420.release();
        }
    }

    private byte[] convertI420ToBgra(I420Buffer i420) {
        int width = i420.getWidth();
        int height = i420.getHeight();
        int strideY = i420.getStrideY();
        int strideU = i420.getStrideU();
        int strideV = i420.getStrideV();

        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Video frame dimensions must be positive: " + width + "x" + height);
        }
        if (strideY < width || strideU < (width + 1) / 2 || strideV < (width + 1) / 2) {
            throw new IllegalArgumentException("Invalid I420 strides for " + width + "x" + height
                    + ": y=" + strideY + ", u=" + strideU + ", v=" + strideV);
        }

        ByteBuffer yPlane = normalizedPlane("Y", i420.getDataY());
        ByteBuffer uPlane = normalizedPlane("U", i420.getDataU());
        ByteBuffer vPlane = normalizedPlane("V", i420.getDataV());
        validatePlaneCapacity("Y", yPlane, strideY, height, width);
        validatePlaneCapacity("U", uPlane, strideU, (height + 1) / 2, (width + 1) / 2);
        validatePlaneCapacity("V", vPlane, strideV, (height + 1) / 2, (width + 1) / 2);

        byte[] bgra = new byte[Math.multiplyExact(Math.multiplyExact(width, height), 4)];
        int outputIndex = 0;

        for (int y = 0; y < height; y++) {
            int yRow = y * strideY;
            int uvRow = (y / 2) * strideU;
            int vvRow = (y / 2) * strideV;

            for (int x = 0; x < width; x++) {
                int yValue = yPlane.get(yRow + x) & 0xFF;
                int uValue = uPlane.get(uvRow + (x / 2)) & 0xFF;
                int vValue = vPlane.get(vvRow + (x / 2)) & 0xFF;

                int c = Math.max(yValue - 16, 0);
                int d = uValue - 128;
                int e = vValue - 128;

                int red = clampColor((298 * c + 409 * e + 128) >> 8);
                int green = clampColor((298 * c - 100 * d - 208 * e + 128) >> 8);
                int blue = clampColor((298 * c + 516 * d + 128) >> 8);

                bgra[outputIndex++] = (byte) blue;
                bgra[outputIndex++] = (byte) green;
                bgra[outputIndex++] = (byte) red;
                bgra[outputIndex++] = (byte) 0xFF;
            }
        }

        return bgra;
    }

    private static ByteBuffer normalizedPlane(String planeName, ByteBuffer plane) {
        Objects.requireNonNull(plane, planeName + " plane must not be null");
        return plane.slice();
    }

    private static void validatePlaneCapacity(String planeName, ByteBuffer plane, int stride, int rows, int rowWidth) {
        Objects.requireNonNull(plane, planeName + " plane must not be null");
        if (rows <= 0 || rowWidth <= 0) {
            throw new IllegalArgumentException(planeName + " plane dimensions must be positive");
        }
        int requiredCapacity = Math.addExact(Math.multiplyExact(stride, rows - 1), rowWidth);
        if (plane.limit() < requiredCapacity) {
            throw new IllegalArgumentException(planeName + " plane limit " + plane.limit()
                    + " is smaller than required " + requiredCapacity);
        }
    }

    private static int clampColor(int value) {
        if (value < 0) {
            return 0;
        }
        return Math.min(value, 255);
    }
}
