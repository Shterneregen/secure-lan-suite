package com.shterneregen.securelan.androidclient.ui.qr

import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer

class QrFrameAnalyzerTest {
    @Test fun paddedCroppedLuminanceAndEveryRotationDecodeOffline() {
        val content = "securelan://join?v=1&proto=2&host=192.168.1.42&port=5050"
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 240, 240)
        val stride = 500
        val data = ByteArray(stride * 244) { 127 }
        for (y in 0 until 240) for (x in 0 until 240) data[(y + 2) * stride + (x + 3) * 2] = if (matrix[x, y]) 0 else 255.toByte()
        listOf(0, 90, 180, 270).forEach { rotation ->
            val pixels = QrFrameAnalyzer.luminance(ByteBuffer.wrap(data), stride, 2, 3, 2, 240, 240, rotation)
            assertEquals(content, QrFrameAnalyzer.decode(pixels, 240, 240))
        }
        assertNull(QrFrameAnalyzer.decode(ByteArray(240 * 240) { 255.toByte() }, 240, 240))
    }
}
