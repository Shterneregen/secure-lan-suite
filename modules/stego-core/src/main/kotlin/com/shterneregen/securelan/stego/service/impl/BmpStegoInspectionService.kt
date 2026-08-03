package com.shterneregen.securelan.stego.service.impl

import com.shterneregen.securelan.stego.model.StegoInspectionPoint
import com.shterneregen.securelan.stego.model.StegoInspectionResult
import com.shterneregen.securelan.stego.service.StegoInspectionService
import com.shterneregen.securelan.stego.service.impl.internal.BmpImage
import kotlin.math.ln
import java.nio.ByteBuffer
import java.nio.ByteOrder

class BmpStegoInspectionService : StegoInspectionService {
    override fun inspect(bmpBytes: ByteArray, intervalStart: Int, intervalEnd: Int?, interval: Int): StegoInspectionResult {
        val image = BmpImage.parse(bmpBytes)
        require(interval > 0) { "Interval must be greater than zero" }
        val pixelCount = Math.multiplyExact(image.width, image.height)
        val topDown = ByteBuffer.wrap(bmpBytes).order(ByteOrder.LITTLE_ENDIAN).getInt(22) < 0
        val end = intervalEnd ?: pixelCount
        require(intervalStart in 0 until pixelCount) { "Interval start must be between 0 and ${pixelCount - 1}" }
        require(end in (intervalStart + 1)..pixelCount) { "Interval end must be between ${intervalStart + 1} and $pixelCount" }

        val values = IntArray(pixelCount)
        val preview = IntArray(pixelCount)
        for (pixel in 0 until pixelCount) {
            val row = pixel / image.width
            val column = pixel % image.width
            val sourceRow = if (topDown) row else image.height - 1 - row
            val offset = image.pixelDataOffset + sourceRow * image.rowStride + column * image.bytesPerPixel
            val b = bmpBytes[offset].toInt() and 1
            val g = bmpBytes[offset + 1].toInt() and 1
            val r = bmpBytes[offset + 2].toInt() and 1
            values[pixel] = (r shl 2) or (g shl 1) or b
            preview[pixel] = (0xff shl 24) or ((r * 255) shl 16) or ((g * 255) shl 8) or (b * 255)
        }

        val averages = ArrayList<StegoInspectionPoint>()
        val entropies = ArrayList<StegoInspectionPoint>()
        var from = intervalStart
        while (from < end) {
            val to = minOf(from + interval, end)
            var valueSum = 0L
            var ones = 0
            for (index in from until to) {
                valueSum += values[index]
                ones += Integer.bitCount(values[index])
            }
            val count = to - from
            averages += StegoInspectionPoint(to, valueSum.toDouble() / count)
            val bitCount = count * 3
            val p1 = ones.toDouble() / bitCount
            val p0 = 1.0 - p1
            val entropy = entropyTerm(p0) + entropyTerm(p1)
            entropies += StegoInspectionPoint(to, entropy)
            from = to
        }
        return StegoInspectionResult(image.width, image.height, intervalStart, end, interval, averages, entropies, preview)
    }

    private fun entropyTerm(probability: Double): Double =
        if (probability == 0.0) 0.0 else -probability * (ln(probability) / ln(2.0))
}
