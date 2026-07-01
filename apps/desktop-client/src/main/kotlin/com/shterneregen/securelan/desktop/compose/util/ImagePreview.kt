package com.shterneregen.securelan.desktop.compose.util

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.nio.file.Files
import java.nio.file.Path
import org.jetbrains.skia.Image

internal fun Path.toPreviewImageBitmap(): ImageBitmap? = runCatching {
    Image.makeFromEncoded(Files.readAllBytes(toAbsolutePath().normalize())).toComposeImageBitmap()
}.getOrNull()
