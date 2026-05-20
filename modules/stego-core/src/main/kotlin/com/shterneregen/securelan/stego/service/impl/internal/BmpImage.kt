package com.shterneregen.securelan.stego.service.impl.internal

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Objects
import kotlin.math.absoluteValue

@JvmRecord
data class BmpImage(
    val width: Int,
    val height: Int,
    val bitsPerPixel: Int,
    val pixelDataOffset: Int,
    val rowStride: Int,
    val bytesPerPixel: Int,
    val carrierByteCount: Int,
) {
    fun maxPayloadBytes(headerBytes: Int): Int {
        val hiddenBytes = carrierByteCount / Byte.SIZE_BITS
        return maxOf(0, hiddenBytes - headerBytes)
    }

    fun writeLeastSignificantBits(targetBmpBytes: ByteArray, hiddenBytes: ByteArray) {
        Objects.requireNonNull(targetBmpBytes, "targetBmpBytes")
        Objects.requireNonNull(hiddenBytes, "hiddenBytes")
        val bitsToWrite = Math.multiplyExact(hiddenBytes.size, Byte.SIZE_BITS)
        if (bitsToWrite > carrierByteCount) {
            throw IllegalArgumentException("Hidden data exceeds BMP carrier capacity")
        }
        var bitIndex = 0
        var row = 0
        while (row < height && bitIndex < bitsToWrite) {
            val rowStart = pixelDataOffset + row * rowStride
            var x = 0
            while (x < width && bitIndex < bitsToWrite) {
                val pixelStart = rowStart + x * bytesPerPixel
                var channel = 0
                while (channel < 3 && bitIndex < bitsToWrite) {
                    val bit = (hiddenBytes[bitIndex / Byte.SIZE_BITS].toInt() shr (7 - (bitIndex % Byte.SIZE_BITS))) and 1
                    val carrierIndex = pixelStart + channel
                    targetBmpBytes[carrierIndex] = ((targetBmpBytes[carrierIndex].toInt() and 0xfe) or bit).toByte()
                    bitIndex++
                    channel++
                }
                x++
            }
            row++
        }
    }

    fun readLeastSignificantBits(sourceBmpBytes: ByteArray, byteCount: Int): ByteArray {
        Objects.requireNonNull(sourceBmpBytes, "sourceBmpBytes")
        if (byteCount < 0) {
            throw IllegalArgumentException("byteCount must not be negative")
        }
        val bitsToRead = Math.multiplyExact(byteCount, Byte.SIZE_BITS)
        if (bitsToRead > carrierByteCount) {
            throw IllegalArgumentException("Requested hidden data exceeds BMP carrier capacity")
        }
        val result = ByteArray(byteCount)
        var bitIndex = 0
        var row = 0
        while (row < height && bitIndex < bitsToRead) {
            val rowStart = pixelDataOffset + row * rowStride
            var x = 0
            while (x < width && bitIndex < bitsToRead) {
                val pixelStart = rowStart + x * bytesPerPixel
                var channel = 0
                while (channel < 3 && bitIndex < bitsToRead) {
                    val carrierIndex = pixelStart + channel
                    val bit = sourceBmpBytes[carrierIndex].toInt() and 1
                    result[bitIndex / Byte.SIZE_BITS] = (result[bitIndex / Byte.SIZE_BITS].toInt() or (bit shl (7 - (bitIndex % Byte.SIZE_BITS)))).toByte()
                    bitIndex++
                    channel++
                }
                x++
            }
            row++
        }
        return result
    }

    companion object {
        private const val MIN_BMP_HEADER_BYTES = 54
        private const val BI_RGB = 0

        @JvmStatic
        fun parse(bmpBytes: ByteArray): BmpImage {
            Objects.requireNonNull(bmpBytes, "bmpBytes")
            if (bmpBytes.size < MIN_BMP_HEADER_BYTES) {
                throw IllegalArgumentException("BMP data is too short")
            }
            if (bmpBytes[0] != 'B'.code.toByte() || bmpBytes[1] != 'M'.code.toByte()) {
                throw IllegalArgumentException("Only BMP images are supported")
            }
            val header = ByteBuffer.wrap(bmpBytes).order(ByteOrder.LITTLE_ENDIAN)
            val pixelDataOffset = header.getInt(10)
            val dibHeaderSize = header.getInt(14)
            val width = header.getInt(18)
            val rawHeight = header.getInt(22)
            val planes = header.getShort(26).toInt() and 0xffff
            val bitsPerPixel = header.getShort(28).toInt() and 0xffff
            val compression = header.getInt(30)

            if (dibHeaderSize < 40) {
                throw IllegalArgumentException("Unsupported BMP DIB header")
            }
            if (planes != 1) {
                throw IllegalArgumentException("Invalid BMP planes count")
            }
            if (width <= 0 || rawHeight == 0) {
                throw IllegalArgumentException("BMP width and height must be non-zero positive dimensions")
            }
            val height = rawHeight.absoluteValue
            if (bitsPerPixel != 24 && bitsPerPixel != 32) {
                throw IllegalArgumentException("Only uncompressed 24-bit and 32-bit BMP images are supported")
            }
            if (compression != BI_RGB) {
                throw IllegalArgumentException("Only uncompressed BMP images are supported")
            }
            if (pixelDataOffset < MIN_BMP_HEADER_BYTES || pixelDataOffset >= bmpBytes.size) {
                throw IllegalArgumentException("Invalid BMP pixel data offset")
            }

            val rowStride = ((width * bitsPerPixel + 31) / 32) * 4
            val requiredBytes = pixelDataOffset.toLong() + rowStride.toLong() * height.toLong()
            if (requiredBytes > bmpBytes.size) {
                throw IllegalArgumentException("BMP pixel data is truncated")
            }
            val bytesPerPixel = bitsPerPixel / 8
            val carrierByteCount = Math.multiplyExact(Math.multiplyExact(width, height), 3)
            return BmpImage(width, height, bitsPerPixel, pixelDataOffset, rowStride, bytesPerPixel, carrierByteCount)
        }
    }
}
