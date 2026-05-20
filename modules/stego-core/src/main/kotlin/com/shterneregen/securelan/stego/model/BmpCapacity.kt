package com.shterneregen.securelan.stego.model

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
