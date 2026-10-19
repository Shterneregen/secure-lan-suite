package com.shterneregen.securelan.desktop.compose.ui.qr

import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class QrCodeRendererTest {
    @Test fun `room and HTTP payloads decode from rendered pixels`() {
        listOf("securelan://join?v=1&proto=2&host=192.168.1.42&port=5050", "http://192.168.1.42:5053/share/example").forEach { content ->
            val image = renderQrCode(content)
            val pixels = IntArray(image.width * image.height)
            image.readPixels(pixels)
            // The source has one pixel per module; the UI interaction test scans its displayed size.
            assertEquals(content, QRCodeReader().decode(
                BinaryBitmap(HybridBinarizer(RGBLuminanceSource(image.width, image.height, pixels))),
                mapOf(DecodeHintType.PURE_BARCODE to true),
            ).text)
            assertEquals(-1, pixels.first())
        }
    }
}
