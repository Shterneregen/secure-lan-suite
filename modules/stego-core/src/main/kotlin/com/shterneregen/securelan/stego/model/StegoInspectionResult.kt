package com.shterneregen.securelan.stego.model

/**
 * Represents a statistical data point produced during steganography inspection.
 *
 * @property pixelNumber The ending pixel index of the evaluated interval.
 * @property value The calculated metric value (e.g., average LSB value or Shannon entropy).
 */
data class StegoInspectionPoint(val pixelNumber: Int, val value: Double)

/**
 * Result of statistical and visual inspection of a carrier BMP image.
 *
 * @property width The width of the inspected image in pixels.
 * @property height The height of the inspected image in pixels.
 * @property intervalStart The 0-based start index of the inspected pixel range.
 * @property intervalEnd The exclusive end index of the inspected pixel range.
 * @property interval The step size in pixels used for aggregating metrics.
 * @property movingAverage Aggregated moving average values of the least significant bits across intervals.
 * @property entropy Calculated Shannon entropy of the least significant bits across intervals.
 * @property lastBitArgb Array of ARGB values representing an amplified visualization of the least significant bits.
 */
data class StegoInspectionResult(
    val width: Int,
    val height: Int,
    val intervalStart: Int,
    val intervalEnd: Int,
    val interval: Int,
    val movingAverage: List<StegoInspectionPoint>,
    val entropy: List<StegoInspectionPoint>,
    val lastBitArgb: IntArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StegoInspectionResult) return false
        return width == other.width &&
            height == other.height &&
            intervalStart == other.intervalStart &&
            intervalEnd == other.intervalEnd &&
            interval == other.interval &&
            movingAverage == other.movingAverage &&
            entropy == other.entropy &&
            lastBitArgb.contentEquals(other.lastBitArgb)
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + intervalStart
        result = 31 * result + intervalEnd
        result = 31 * result + interval
        result = 31 * result + movingAverage.hashCode()
        result = 31 * result + entropy.hashCode()
        result = 31 * result + lastBitArgb.contentHashCode()
        return result
    }

    override fun toString(): String =
        "StegoInspectionResult(width=$width, height=$height, intervalStart=$intervalStart, " +
            "intervalEnd=$intervalEnd, interval=$interval, points=${movingAverage.size}, previewPixels=${lastBitArgb.size})"
}
