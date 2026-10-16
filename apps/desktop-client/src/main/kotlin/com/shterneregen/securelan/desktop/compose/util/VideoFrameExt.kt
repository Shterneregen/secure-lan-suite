package com.shterneregen.securelan.desktop.compose.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.shterneregen.securelan.webrtc.event.RtcVideoFrameEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import java.awt.image.BufferedImage

/** Effect cancellation replaces pending frames; raster work never runs during composition. */
@Composable
internal fun rememberPreviewImage(frame: RtcVideoFrameEvent?): ImageBitmap? {
    val image by produceState<ImageBitmap?>(null, frame) {
        value = if (frame == null) null else withContext(Dispatchers.Default) { frame.toPreviewImageBitmap() }
    }
    return image
}

internal fun RtcVideoFrameEvent.toPreviewImageBitmap(): ImageBitmap? = runCatching {
    val raster = previewRaster()
    Image.makeRaster(ImageInfo(raster.width, raster.height, ColorType.BGRA_8888, ColorAlphaType.UNPREMUL),
        raster.pixels, raster.width * 4).use { it.toComposeImageBitmap() }
}.getOrNull()

private data class PreviewRaster(val width: Int, val height: Int, val pixels: ByteArray)

private fun RtcVideoFrameEvent.previewRaster(): PreviewRaster {
    val rotation = Math.floorMod(rotation(), 360)
    require(rotation % 90 == 0) { "Unsupported video frame rotation: $rotation" }
    val sourceWidth = width()
    val sourceHeight = height()
    val width = if (rotation % 180 == 0) sourceWidth else sourceHeight
    val height = if (rotation % 180 == 0) sourceHeight else sourceWidth
    val source = bgraPixels()
    if (rotation == 0) return PreviewRaster(width, height, source)
    val target = ByteArray(source.size)
    for (y in 0 until sourceHeight) {
        for (x in 0 until sourceWidth) {
            val outputX = when (rotation) { 90 -> sourceHeight - 1 - y; 180 -> sourceWidth - 1 - x; 270 -> y; else -> x }
            val outputY = when (rotation) { 90 -> x; 180 -> sourceHeight - 1 - y; 270 -> sourceWidth - 1 - x; else -> y }
            val from = (y * sourceWidth + x) * 4
            source.copyInto(target, (outputY * width + outputX) * 4, from, from + 4)
        }
    }
    return PreviewRaster(width, height, target)
}

/** Reference raster for pixel/rotation verification and non-Compose consumers. */
internal fun RtcVideoFrameEvent.toPreviewBufferedImage(): BufferedImage {
    val raster = previewRaster()
    val image = BufferedImage(raster.width, raster.height, BufferedImage.TYPE_INT_ARGB)
    for (y in 0 until raster.height) for (x in 0 until raster.width) {
        val offset = (y * raster.width + x) * 4
        val pixels = raster.pixels
        image.setRGB(x, y, ((pixels[offset + 3].toInt() and 0xff) shl 24) or
            ((pixels[offset + 2].toInt() and 0xff) shl 16) or ((pixels[offset + 1].toInt() and 0xff) shl 8) or
            (pixels[offset].toInt() and 0xff))
    }
    return image
}
