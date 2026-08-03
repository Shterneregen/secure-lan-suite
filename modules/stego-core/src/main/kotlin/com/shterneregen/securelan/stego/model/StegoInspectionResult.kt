package com.shterneregen.securelan.stego.model

data class StegoInspectionPoint(val pixelNumber: Int, val value: Double)

data class StegoInspectionResult(
    val width: Int,
    val height: Int,
    val intervalStart: Int,
    val intervalEnd: Int,
    val interval: Int,
    val movingAverage: List<StegoInspectionPoint>,
    val entropy: List<StegoInspectionPoint>,
    val lastBitArgb: IntArray,
)
