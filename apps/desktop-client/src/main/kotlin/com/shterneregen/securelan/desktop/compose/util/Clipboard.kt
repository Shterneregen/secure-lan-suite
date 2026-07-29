package com.shterneregen.securelan.desktop.compose.util

import java.awt.Desktop
import java.awt.Image
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.image.BufferedImage
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO

internal fun copyToSystemClipboard(text: String) {
    if (text.isBlank()) return
    runCatching {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
    }
}

internal fun pasteImageFromSystemClipboard(): Path? = runCatching {
    val contents = Toolkit.getDefaultToolkit().systemClipboard.getContents(null) ?: return@runCatching null
    if (contents.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
        val files = contents.getTransferData(DataFlavor.javaFileListFlavor) as? List<*>
        return@runCatching files
            ?.filterIsInstance<java.io.File>()
            ?.map(java.io.File::toPath)
            ?.firstOrNull(::isSupportedImagePath)
            ?.toAbsolutePath()
            ?.normalize()
    }
    if (!contents.isDataFlavorSupported(DataFlavor.imageFlavor)) return@runCatching null
    val image = contents.getTransferData(DataFlavor.imageFlavor) as? Image ?: return@runCatching null
    val buffered = image.toBufferedImage()
    val output = Files.createTempFile("secure-lan-stego-clipboard-", ".png")
    output.toFile().deleteOnExit()
    check(ImageIO.write(buffered, "png", output.toFile())) { "PNG writer is unavailable" }
    output.toAbsolutePath().normalize()
}.getOrNull()

internal fun openInFileManager(path: Path): Boolean = runCatching {
    val target = path.toAbsolutePath().normalize().let { if (Files.isDirectory(it)) it else it.parent ?: it }
    if (!Desktop.isDesktopSupported()) return@runCatching false
    Desktop.getDesktop().open(target.toFile())
    true
}.getOrDefault(false)

internal fun openInBrowser(url: String): Boolean = runCatching {
    if (url.isBlank() || !Desktop.isDesktopSupported()) return@runCatching false
    Desktop.getDesktop().browse(URI(url))
    true
}.getOrDefault(false)

internal fun isSupportedImagePath(path: Path): Boolean {
    val extension = path.fileName?.toString()?.substringAfterLast('.', "")?.lowercase().orEmpty()
    return Files.isRegularFile(path) && extension in setOf("bmp", "png", "jpg", "jpeg", "gif")
}

private fun Image.toBufferedImage(): BufferedImage {
    if (this is BufferedImage) return this
    val buffered = BufferedImage(getWidth(null), getHeight(null), BufferedImage.TYPE_INT_ARGB)
    val graphics = buffered.createGraphics()
    try {
        graphics.drawImage(this, 0, 0, null)
    } finally {
        graphics.dispose()
    }
    return buffered
}
