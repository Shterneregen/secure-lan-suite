package com.shterneregen.securelan.desktop.compose.ui.qr

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import org.jetbrains.skia.Image as SkiaImage

internal fun renderQrCode(content: String): ImageBitmap {
    // Keep one source pixel per module: downscaling an already enlarged raster can
    // distort module widths differently across Skia backends and display scales.
    val matrix = QRCodeWriter().encode(
        content,
        BarcodeFormat.QR_CODE,
        0,
        0,
        mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 4,
            EncodeHintType.CHARACTER_SET to "UTF-8",
        ),
    )
    val image = BufferedImage(matrix.width, matrix.height, BufferedImage.TYPE_INT_ARGB)
    for (y in 0 until matrix.height) {
        for (x in 0 until matrix.width) {
            image.setRGB(x, y, if (matrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
        }
    }
    val encoded = ByteArrayOutputStream().use { output ->
        ImageIO.write(image, "png", output)
        output.toByteArray()
    }
    return SkiaImage.makeFromEncoded(encoded).toComposeImageBitmap()
}
