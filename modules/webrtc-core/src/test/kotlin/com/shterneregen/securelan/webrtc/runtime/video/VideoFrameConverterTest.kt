package com.shterneregen.securelan.webrtc.runtime.video

import dev.onvoid.webrtc.media.video.I420Buffer
import dev.onvoid.webrtc.media.video.VideoFrame
import dev.onvoid.webrtc.media.video.VideoFrameBuffer
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

class VideoFrameConverterTest {
    private val converter = VideoFrameConverter()

    @Test
    fun shouldConvertI420FrameToBgraAndReleaseConvertedBuffer() {
        val i420 = TestI420Buffer(
            width = 2,
            height = 2,
            strideY = 2,
            strideU = 1,
            strideV = 1,
            yPlane = byteArrayOf(16, 235.toByte(), 81, 145.toByte()),
            uPlane = byteArrayOf(128.toByte()),
            vPlane = byteArrayOf(128.toByte()),
        )
        val frame = VideoFrame(TestVideoFrameBuffer(2, 2, i420), 0L)

        val bgra = converter.convertToBgra(frame)

        assertArrayEquals(
            byteArrayOf(
                0, 0, 0, 0xFF.toByte(),
                255.toByte(), 255.toByte(), 255.toByte(), 0xFF.toByte(),
                76, 76, 76, 0xFF.toByte(),
                150.toByte(), 150.toByte(), 150.toByte(), 0xFF.toByte(),
            ),
            bgra,
        )
        assertEquals(1, i420.releaseCount)
    }

    @Test
    fun shouldRejectInvalidFrameDimensionsAndStillReleaseConvertedBuffer() {
        val i420 = TestI420Buffer(
            width = 0,
            height = 2,
            strideY = 2,
            strideU = 1,
            strideV = 1,
            yPlane = byteArrayOf(16, 16, 16, 16),
            uPlane = byteArrayOf(128.toByte()),
            vPlane = byteArrayOf(128.toByte()),
        )
        val frame = VideoFrame(TestVideoFrameBuffer(0, 2, i420), 0L)

        assertThrows(IllegalArgumentException::class.java) { converter.convertToBgra(frame) }
        assertEquals(1, i420.releaseCount)
    }

    @Test
    fun shouldRejectTooSmallPlaneLimit() {
        val i420 = TestI420Buffer(
            width = 2,
            height = 2,
            strideY = 2,
            strideU = 1,
            strideV = 1,
            yPlane = byteArrayOf(16, 16, 16),
            uPlane = byteArrayOf(128.toByte()),
            vPlane = byteArrayOf(128.toByte()),
        )
        val frame = VideoFrame(TestVideoFrameBuffer(2, 2, i420), 0L)

        assertThrows(IllegalArgumentException::class.java) { converter.convertToBgra(frame) }
        assertEquals(1, i420.releaseCount)
    }

    private class TestVideoFrameBuffer(
        private val width: Int,
        private val height: Int,
        private val i420: I420Buffer?,
    ) : VideoFrameBuffer {
        override fun getWidth(): Int = width

        override fun getHeight(): Int = height

        override fun toI420(): I420Buffer? = i420

        override fun cropAndScale(
            cropX: Int,
            cropY: Int,
            cropWidth: Int,
            cropHeight: Int,
            scaleWidth: Int,
            scaleHeight: Int,
        ): VideoFrameBuffer = this

        override fun retain() {
        }

        override fun release() {
        }
    }

    private class TestI420Buffer(
        private val width: Int,
        private val height: Int,
        private val strideY: Int,
        private val strideU: Int,
        private val strideV: Int,
        yPlane: ByteArray,
        uPlane: ByteArray,
        vPlane: ByteArray,
    ) : I420Buffer {
        private val yData = ByteBuffer.wrap(yPlane)
        private val uData = ByteBuffer.wrap(uPlane)
        private val vData = ByteBuffer.wrap(vPlane)
        var releaseCount: Int = 0
            private set

        override fun getDataY(): ByteBuffer = yData

        override fun getDataU(): ByteBuffer = uData

        override fun getDataV(): ByteBuffer = vData

        override fun getStrideY(): Int = strideY

        override fun getStrideU(): Int = strideU

        override fun getStrideV(): Int = strideV

        override fun getWidth(): Int = width

        override fun getHeight(): Int = height

        override fun toI420(): I420Buffer = this

        override fun cropAndScale(
            cropX: Int,
            cropY: Int,
            cropWidth: Int,
            cropHeight: Int,
            scaleWidth: Int,
            scaleHeight: Int,
        ): VideoFrameBuffer = this

        override fun retain() {
        }

        override fun release() {
            releaseCount++
        }
    }
}
