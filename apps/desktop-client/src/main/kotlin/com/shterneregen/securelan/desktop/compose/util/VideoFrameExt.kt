package com.shterneregen.securelan.desktop.compose.util

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.shterneregen.securelan.webrtc.event.RtcVideoFrameEvent
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import org.jetbrains.skia.Image

internal fun com.shterneregen.securelan.webrtc.event.RtcVideoFrameEvent.toPreviewImageBitmap(): ImageBitmap? {
    return try {
        val image = BufferedImage(width(), height(), BufferedImage.TYPE_INT_ARGB)
        val bgra = bgraPixels()
        var offset = 0
        for (y in 0 until height()) {
            for (x in 0 until width()) {
                val blue = bgra[offset].toInt() and 0xFF
                val green = bgra[offset + 1].toInt() and 0xFF
                val red = bgra[offset + 2].toInt() and 0xFF
                val alpha = bgra[offset + 3].toInt() and 0xFF
                image.setRGB(x, y, (alpha shl 24) or (red shl 16) or (green shl 8) or blue)
                offset += 4
            }
        }
        val output = ByteArrayOutputStream()
        ImageIO.write(image, "png", output)
        org.jetbrains.skia.Image.makeFromEncoded(output.toByteArray()).toComposeImageBitmap()
    } catch (_: Exception) {
        null
    }
}
