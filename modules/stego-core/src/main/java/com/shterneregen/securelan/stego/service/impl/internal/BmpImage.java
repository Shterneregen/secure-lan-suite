package com.shterneregen.securelan.stego.service.impl.internal;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

public record BmpImage(
        int width,
        int height,
        int bitsPerPixel,
        int pixelDataOffset,
        int rowStride,
        int bytesPerPixel,
        int carrierByteCount
) {
    private static final int MIN_BMP_HEADER_BYTES = 54;
    private static final int BI_RGB = 0;

    public static BmpImage parse(byte[] bmpBytes) {
        Objects.requireNonNull(bmpBytes, "bmpBytes");
        if (bmpBytes.length < MIN_BMP_HEADER_BYTES) {
            throw new IllegalArgumentException("BMP data is too short");
        }
        if (bmpBytes[0] != 'B' || bmpBytes[1] != 'M') {
            throw new IllegalArgumentException("Only BMP images are supported");
        }
        ByteBuffer header = ByteBuffer.wrap(bmpBytes).order(ByteOrder.LITTLE_ENDIAN);
        int pixelDataOffset = header.getInt(10);
        int dibHeaderSize = header.getInt(14);
        int width = header.getInt(18);
        int rawHeight = header.getInt(22);
        int planes = Short.toUnsignedInt(header.getShort(26));
        int bitsPerPixel = Short.toUnsignedInt(header.getShort(28));
        int compression = header.getInt(30);

        if (dibHeaderSize < 40) {
            throw new IllegalArgumentException("Unsupported BMP DIB header");
        }
        if (planes != 1) {
            throw new IllegalArgumentException("Invalid BMP planes count");
        }
        if (width <= 0 || rawHeight == 0) {
            throw new IllegalArgumentException("BMP width and height must be non-zero positive dimensions");
        }
        int height = Math.abs(rawHeight);
        if (bitsPerPixel != 24 && bitsPerPixel != 32) {
            throw new IllegalArgumentException("Only uncompressed 24-bit and 32-bit BMP images are supported");
        }
        if (compression != BI_RGB) {
            throw new IllegalArgumentException("Only uncompressed BMP images are supported");
        }
        if (pixelDataOffset < MIN_BMP_HEADER_BYTES || pixelDataOffset >= bmpBytes.length) {
            throw new IllegalArgumentException("Invalid BMP pixel data offset");
        }

        int rowStride = ((width * bitsPerPixel + 31) / 32) * 4;
        long requiredBytes = (long) pixelDataOffset + (long) rowStride * height;
        if (requiredBytes > bmpBytes.length) {
            throw new IllegalArgumentException("BMP pixel data is truncated");
        }
        int bytesPerPixel = bitsPerPixel / 8;
        int carrierByteCount = Math.multiplyExact(Math.multiplyExact(width, height), 3);
        return new BmpImage(width, height, bitsPerPixel, pixelDataOffset, rowStride, bytesPerPixel, carrierByteCount);
    }

    public int maxPayloadBytes(int headerBytes) {
        int hiddenBytes = carrierByteCount / Byte.SIZE;
        return Math.max(0, hiddenBytes - headerBytes);
    }

    public void writeLeastSignificantBits(byte[] targetBmpBytes, byte[] hiddenBytes) {
        Objects.requireNonNull(targetBmpBytes, "targetBmpBytes");
        Objects.requireNonNull(hiddenBytes, "hiddenBytes");
        int bitsToWrite = Math.multiplyExact(hiddenBytes.length, Byte.SIZE);
        if (bitsToWrite > carrierByteCount) {
            throw new IllegalArgumentException("Hidden data exceeds BMP carrier capacity");
        }
        int bitIndex = 0;
        for (int row = 0; row < height && bitIndex < bitsToWrite; row++) {
            int rowStart = pixelDataOffset + row * rowStride;
            for (int x = 0; x < width && bitIndex < bitsToWrite; x++) {
                int pixelStart = rowStart + x * bytesPerPixel;
                for (int channel = 0; channel < 3 && bitIndex < bitsToWrite; channel++) {
                    int bit = (hiddenBytes[bitIndex / Byte.SIZE] >> (7 - (bitIndex % Byte.SIZE))) & 1;
                    int carrierIndex = pixelStart + channel;
                    targetBmpBytes[carrierIndex] = (byte) ((targetBmpBytes[carrierIndex] & 0xFE) | bit);
                    bitIndex++;
                }
            }
        }
    }

    public byte[] readLeastSignificantBits(byte[] sourceBmpBytes, int byteCount) {
        Objects.requireNonNull(sourceBmpBytes, "sourceBmpBytes");
        if (byteCount < 0) {
            throw new IllegalArgumentException("byteCount must not be negative");
        }
        int bitsToRead = Math.multiplyExact(byteCount, Byte.SIZE);
        if (bitsToRead > carrierByteCount) {
            throw new IllegalArgumentException("Requested hidden data exceeds BMP carrier capacity");
        }
        byte[] result = new byte[byteCount];
        int bitIndex = 0;
        for (int row = 0; row < height && bitIndex < bitsToRead; row++) {
            int rowStart = pixelDataOffset + row * rowStride;
            for (int x = 0; x < width && bitIndex < bitsToRead; x++) {
                int pixelStart = rowStart + x * bytesPerPixel;
                for (int channel = 0; channel < 3 && bitIndex < bitsToRead; channel++) {
                    int carrierIndex = pixelStart + channel;
                    int bit = sourceBmpBytes[carrierIndex] & 1;
                    result[bitIndex / Byte.SIZE] = (byte) (result[bitIndex / Byte.SIZE]
                            | (bit << (7 - (bitIndex % Byte.SIZE))));
                    bitIndex++;
                }
            }
        }
        return result;
    }
}
