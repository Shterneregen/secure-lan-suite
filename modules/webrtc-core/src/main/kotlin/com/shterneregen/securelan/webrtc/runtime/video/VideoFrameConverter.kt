package com.shterneregen.securelan.webrtc.runtime.video

import dev.onvoid.webrtc.media.video.I420Buffer
import dev.onvoid.webrtc.media.video.VideoFrame
import java.nio.ByteBuffer
import java.util.Objects

class VideoFrameConverter {
    fun convertToBgra(frame: VideoFrame): ByteArray {
        Objects.requireNonNull(frame, "frame must not be null")
        val buffer = Objects.requireNonNull(frame.buffer, "frame buffer must not be null")
        val i420 = buffer.toI420() ?: throw IllegalStateException("Video frame buffer cannot be converted to I420")
        try {
            return convertI420ToBgra(i420)
        } finally {
            i420.release()
        }
    }

    private fun convertI420ToBgra(i420: I420Buffer): ByteArray {
        val width = i420.width
        val height = i420.height
        val strideY = i420.strideY
        val strideU = i420.strideU
        val strideV = i420.strideV

        require(width > 0 && height > 0) { "Video frame dimensions must be positive: ${width}x${height}" }
        require(strideY >= width && strideU >= (width + 1) / 2 && strideV >= (width + 1) / 2) {
            "Invalid I420 strides for ${width}x${height}: y=$strideY, u=$strideU, v=$strideV"
        }

        val yPlane = normalizedPlane("Y", i420.dataY)
        val uPlane = normalizedPlane("U", i420.dataU)
        val vPlane = normalizedPlane("V", i420.dataV)
        validatePlaneCapacity("Y", yPlane, strideY, height, width)
        validatePlaneCapacity("U", uPlane, strideU, (height + 1) / 2, (width + 1) / 2)
        validatePlaneCapacity("V", vPlane, strideV, (height + 1) / 2, (width + 1) / 2)

        val bgra = ByteArray(Math.multiplyExact(Math.multiplyExact(width, height), 4))
        var outputIndex = 0

        for (y in 0 until height) {
            val yRow = y * strideY
            val uvRow = (y / 2) * strideU
            val vvRow = (y / 2) * strideV

            for (x in 0 until width) {
                val yValue = yPlane.get(yRow + x).toInt() and 0xFF
                val uValue = uPlane.get(uvRow + (x / 2)).toInt() and 0xFF
                val vValue = vPlane.get(vvRow + (x / 2)).toInt() and 0xFF

                val c = (yValue - 16).coerceAtLeast(0)
                val d = uValue - 128
                val e = vValue - 128

                val red = clampColor((298 * c + 409 * e + 128) shr 8)
                val green = clampColor((298 * c - 100 * d - 208 * e + 128) shr 8)
                val blue = clampColor((298 * c + 516 * d + 128) shr 8)

                bgra[outputIndex++] = blue.toByte()
                bgra[outputIndex++] = green.toByte()
                bgra[outputIndex++] = red.toByte()
                bgra[outputIndex++] = 0xFF.toByte()
            }
        }

        return bgra
    }

    private companion object {
        private fun normalizedPlane(planeName: String, plane: ByteBuffer?): ByteBuffer {
            Objects.requireNonNull(plane, "$planeName plane must not be null")
            return plane!!.slice()
        }

        private fun validatePlaneCapacity(planeName: String, plane: ByteBuffer?, stride: Int, rows: Int, rowWidth: Int) {
            Objects.requireNonNull(plane, "$planeName plane must not be null")
            require(rows > 0 && rowWidth > 0) { "$planeName plane dimensions must be positive" }
            val requiredCapacity = Math.addExact(Math.multiplyExact(stride, rows - 1), rowWidth)
            require(plane!!.limit() >= requiredCapacity) {
                "$planeName plane limit ${plane.limit()} is smaller than required $requiredCapacity"
            }
        }

        private fun clampColor(value: Int): Int = when {
            value < 0 -> 0
            else -> value.coerceAtMost(255)
        }
    }
}
