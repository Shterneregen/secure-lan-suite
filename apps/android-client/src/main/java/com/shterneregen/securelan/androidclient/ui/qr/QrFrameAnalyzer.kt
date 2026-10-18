package com.shterneregen.securelan.androidclient.ui.qr

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

/** Reads only luminance. Frames are never retained; every ImageProxy is closed, including decoder failures. */
class QrFrameAnalyzer(private val result: (String) -> Unit, private val nanoTime: () -> Long = System::nanoTime) : ImageAnalysis.Analyzer {
    private val delivered = AtomicBoolean()
    private var previous = Long.MIN_VALUE
    override fun analyze(image: ImageProxy) {
        try {
            val now = nanoTime()
            if (delivered.get() || previous != Long.MIN_VALUE && now - previous < 200_000_000L) return
            previous = now
            val crop = image.cropRect
            val plane = image.planes.firstOrNull() ?: return
            val pixels = luminance(plane.buffer, plane.rowStride, plane.pixelStride, crop.left, crop.top, crop.width(), crop.height(), image.imageInfo.rotationDegrees)
            val rotated = image.imageInfo.rotationDegrees % 180 != 0
            val width = if (rotated) crop.height() else crop.width()
            val height = if (rotated) crop.width() else crop.height()
            decode(pixels, width, height)?.let { if (it.length <= 4096 && delivered.compareAndSet(false, true)) result(it) }
        } catch (_: Exception) { /* A malformed/blank frame is not a camera or connection failure. */ }
        finally { image.close() }
    }

    companion object {
        fun luminance(buffer: ByteBuffer, rowStride: Int, pixelStride: Int, left: Int, top: Int, width: Int, height: Int, rotation: Int): ByteArray {
            require(width in 1..4096 && height in 1..4096 && width.toLong() * height <= 16_777_216)
            val output = ByteArray(width * height)
            val outWidth = if (rotation % 180 == 0) width else height
            for (y in 0 until height) for (x in 0 until width) {
                val (ox, oy) = when (rotation) {
                    90 -> height - 1 - y to x
                    180 -> width - 1 - x to height - 1 - y
                    270 -> y to width - 1 - x
                    else -> x to y
                }
                output[oy * outWidth + ox] = buffer.get(buffer.position() + (top + y) * rowStride + (left + x) * pixelStride)
            }
            return output
        }
        fun decode(pixels: ByteArray, width: Int, height: Int): String? {
            val reader = QRCodeReader()
            return try {
                val source = PlanarYUVLuminanceSource(pixels, width, height, 0, 0, width, height, false)
                reader.decode(BinaryBitmap(HybridBinarizer(source)), mapOf(DecodeHintType.CHARACTER_SET to "UTF-8")).text
            } catch (_: com.google.zxing.ReaderException) { null } finally { reader.reset() }
        }
    }
}
