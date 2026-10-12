package com.shterneregen.securelan.desktop.compose.util

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.shterneregen.securelan.webrtc.event.RtcVideoFrameEvent
import org.jetbrains.skia.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

internal fun RtcVideoFrameEvent.toPreviewImageBitmap(): ImageBitmap? {
    return try {
        val image = toPreviewBufferedImage()
        val output = ByteArrayOutputStream()
        ImageIO.write(image, "png", output)
        Image.makeFromEncoded(output.toByteArray()).toComposeImageBitmap()
    } catch (_: Exception) {
        null
    }
}

/** Apply WebRTC's clockwise display rotation before Compose measures and scales the image. */
internal fun RtcVideoFrameEvent.toPreviewBufferedImage(): BufferedImage {
    val rotation = Math.floorMod(rotation(), 360)
    require(rotation % 90 == 0) { "Unsupported video frame rotation: $rotation" }
    val sourceWidth = width()
    val sourceHeight = height()
    val swapDimensions = rotation == 90 || rotation == 270
    val image = BufferedImage(
        if (swapDimensions) sourceHeight else sourceWidth,
        if (swapDimensions) sourceWidth else sourceHeight,
        BufferedImage.TYPE_INT_ARGB,
    )
    val bgra = bgraPixels()
    var offset = 0
    for (y in 0 until sourceHeight) {
        for (x in 0 until sourceWidth) {
            val blue = bgra[offset].toInt() and 0xFF
            val green = bgra[offset + 1].toInt() and 0xFF
            val red = bgra[offset + 2].toInt() and 0xFF
            val alpha = bgra[offset + 3].toInt() and 0xFF
            val outputX = when (rotation) {
                90 -> sourceHeight - 1 - y
                180 -> sourceWidth - 1 - x
                270 -> y
                else -> x
            }
            val outputY = when (rotation) {
                90 -> x
                180 -> sourceHeight - 1 - y
                270 -> sourceWidth - 1 - x
                else -> y
            }
            image.setRGB(outputX, outputY, (alpha shl 24) or (red shl 16) or (green shl 8) or blue)
            offset += 4
        }
    }
    return image
}
