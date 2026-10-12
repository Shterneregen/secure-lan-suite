package com.shterneregen.securelan.stego.service

import com.shterneregen.securelan.stego.model.StegoInspectionResult

/**
 * Service for statistical and visual analysis of BMP images to detect or inspect steganographic data.
 */
interface StegoInspectionService {
    /**
     * Performs statistical inspection on the least significant bits of a carrier BMP image.
     *
     * Evaluates moving averages, Shannon entropy across specified intervals, and generates
     * an amplified preview of the lowest color bits.
     *
     * @param bmpBytes The raw bytes of the BMP image to inspect.
     * @param intervalStart The 0-based starting pixel index (defaults to 0).
     * @param intervalEnd The ending pixel index (defaults to the total pixel count if `null`).
     * @param interval The interval length in pixels for aggregating statistical metrics (defaults to 1024).
     * @return A [StegoInspectionResult] containing aggregated metrics and pixel preview data.
     * @throws IllegalArgumentException If intervals are out of bounds or the BMP image is invalid.
     */
    fun inspect(
        bmpBytes: ByteArray,
        intervalStart: Int = 0,
        intervalEnd: Int? = null,
        interval: Int = 1024,
    ): StegoInspectionResult
}
