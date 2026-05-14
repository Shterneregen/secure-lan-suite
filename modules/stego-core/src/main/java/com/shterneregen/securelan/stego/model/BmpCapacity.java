package com.shterneregen.securelan.stego.model;

public record BmpCapacity(
        int width,
        int height,
        int bitsPerPixel,
        int carrierBytes,
        int headerBytes,
        int payloadCapacityBytes
) {
    public BmpCapacity {
        if (width <= 0) {
            throw new IllegalArgumentException("width must be positive");
        }
        if (height <= 0) {
            throw new IllegalArgumentException("height must be positive");
        }
        if (bitsPerPixel != 24 && bitsPerPixel != 32) {
            throw new IllegalArgumentException("bitsPerPixel must be 24 or 32");
        }
        if (carrierBytes < 0) {
            throw new IllegalArgumentException("carrierBytes must not be negative");
        }
        if (headerBytes <= 0) {
            throw new IllegalArgumentException("headerBytes must be positive");
        }
        if (payloadCapacityBytes < 0) {
            throw new IllegalArgumentException("payloadCapacityBytes must not be negative");
        }
    }
}
