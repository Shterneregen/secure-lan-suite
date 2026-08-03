package com.shterneregen.securelan.stego.service

import com.shterneregen.securelan.stego.model.StegoInspectionResult

interface StegoInspectionService {
    fun inspect(
        bmpBytes: ByteArray,
        intervalStart: Int = 0,
        intervalEnd: Int? = null,
        interval: Int = 1024,
    ): StegoInspectionResult
}
