package com.shterneregen.securelan.stego.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StegoInspectionResultTest {
    @Test
    fun shouldSupportEqualityAndHashCodeContract() {
        val points = listOf(StegoInspectionPoint(10, 1.5))
        val entropy = listOf(StegoInspectionPoint(10, 0.8))
        val preview1 = intArrayOf(1, 2, 3)
        val preview2 = intArrayOf(1, 2, 3)
        val previewDiff = intArrayOf(1, 2, 4)

        val result1 = StegoInspectionResult(100, 100, 0, 10, 10, points, entropy, preview1)
        val result2 = StegoInspectionResult(100, 100, 0, 10, 10, points, entropy, preview2)
        val result3 = StegoInspectionResult(100, 100, 0, 10, 10, points, entropy, previewDiff)

        assertEquals(result1, result2)
        assertEquals(result1.hashCode(), result2.hashCode())
        assertNotEquals(result1, result3)
    }

    @Test
    fun shouldProduceInformativeToString() {
        val result = StegoInspectionResult(
            width = 10,
            height = 10,
            intervalStart = 0,
            intervalEnd = 100,
            interval = 50,
            movingAverage = emptyList(),
            entropy = emptyList(),
            lastBitArgb = intArrayOf(0),
        )
        val str = result.toString()

        assertTrue(str.contains("width=10"))
        assertTrue(str.contains("height=10"))
        assertTrue(str.contains("interval=50"))
    }
}
