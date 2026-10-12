package com.shterneregen.securelan.stego.model

/**
 * Represents the storage capacity and structural metrics of a BMP carrier image.
 *
 * @property width The width of the BMP image in pixels.
 * @property height The height of the BMP image in pixels.
 * @property bitsPerPixel The color depth of the image (must be 24 or 32 bits per pixel).
 * @property carrierBytes The total number of color channel bytes available for LSB embedding (3 bytes per pixel).
 * @property headerBytes The number of bytes reserved for the steganography protocol header.
 * @property payloadCapacityBytes The maximum number of raw payload bytes that can be embedded into the image.
 */
@JvmRecord
data class BmpCapacity(
    val width: Int,
    val height: Int,
    val bitsPerPixel: Int,
    val carrierBytes: Int,
    val headerBytes: Int,
    val payloadCapacityBytes: Int,
) {
    init {
        require(width > 0) { "width must be positive" }
        require(height > 0) { "height must be positive" }
        require(bitsPerPixel == 24 || bitsPerPixel == 32) { "bitsPerPixel must be 24 or 32" }
        require(carrierBytes >= 0) { "carrierBytes must not be negative" }
        require(headerBytes > 0) { "headerBytes must be positive" }
        require(payloadCapacityBytes >= 0) { "payloadCapacityBytes must not be negative" }
    }
}
