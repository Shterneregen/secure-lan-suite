package com.shterneregen.securelan.desktop.compose.ui.quickshare

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButton
import com.shterneregen.securelan.desktop.compose.ui.components.CompactIconButton
import com.shterneregen.securelan.desktop.compose.ui.icons.SecureLanIcons
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import org.jetbrains.skia.Image as SkiaImage

@Composable
internal fun QuickShareLinkActions(
    url: String,
    showQrCode: Boolean,
    onCopy: () -> Unit,
    onOpen: (() -> Unit)?,
    onStop: (() -> Unit)? = null,
) {
    var qrDialogOpen by remember(url) { mutableStateOf(false) }
    val enabled = url.isNotBlank()

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showQrCode) {
            CompactIconButton(
                onClick = { qrDialogOpen = true },
                icon = SecureLanIcons.QrCode,
                contentDescription = "Show QR code",
                enabled = enabled,
            )
        }
        CompactIconButton(
            onClick = onCopy,
            icon = SecureLanIcons.Copy,
            contentDescription = "Copy link",
            enabled = enabled,
        )
        if (onOpen != null) {
            CompactIconButton(
                onClick = onOpen,
                icon = SecureLanIcons.Open,
                contentDescription = "Open link",
                enabled = enabled,
            )
        }
        if (onStop != null) {
            CompactIconButton(
                onClick = onStop,
                icon = SecureLanIcons.Stop,
                contentDescription = "Stop sharing link",
            )
        }
    }

    if (qrDialogOpen) {
        QuickShareQrDialog(
            url = url,
            onCopy = onCopy,
            onClose = { qrDialogOpen = false },
        )
    }
}

@Composable
private fun QuickShareQrDialog(
    url: String,
    onCopy: () -> Unit,
    onClose: () -> Unit,
) {
    DialogWindow(
        onCloseRequest = onClose,
        state = rememberDialogState(size = DpSize(380.dp, 460.dp)),
        title = "Quick Share QR code",
        resizable = false,
        onPreviewKeyEvent = { event ->
            if (event.key == Key.Escape && event.type == KeyEventType.KeyUp) {
                onClose()
                true
            } else {
                false
            }
        },
    ) {
        val tokens = LocalSecureLanDesignTokens.current
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colors.background,
        ) {
            Column(
                modifier = Modifier.padding(tokens.spacing.md),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
                ) {
                    Icon(
                        imageVector = SecureLanIcons.QrCode,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colors.primary,
                    )
                    Text(
                        text = "Scan to open the shared link",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.h6,
                        color = tokens.colors.textPrimary,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(tokens.radius.medium),
                    color = Color.White,
                ) {
                    Image(
                        bitmap = remember(url) { createQrCodeBitmap(url) },
                        contentDescription = "QR code for the shared link",
                        modifier = Modifier.padding(12.dp).size(236.dp),
                        filterQuality = FilterQuality.None,
                    )
                }
                SelectionContainer {
                    Text(
                        text = url,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.caption,
                        color = tokens.colors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                }
                CompactButton(onClick = onCopy) { Text("Copy link") }
            }
        }
    }
}

private fun createQrCodeBitmap(content: String): ImageBitmap {
    val size = 512
    val matrix = QRCodeWriter().encode(
        content,
        BarcodeFormat.QR_CODE,
        size,
        size,
        mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 2,
            EncodeHintType.CHARACTER_SET to "UTF-8",
        ),
    )
    val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    for (y in 0 until size) {
        for (x in 0 until size) {
            image.setRGB(x, y, if (matrix[x, y]) 0xFF101418.toInt() else 0xFFFFFFFF.toInt())
        }
    }
    val encoded = ByteArrayOutputStream().use { output ->
        ImageIO.write(image, "png", output)
        output.toByteArray()
    }
    return SkiaImage.makeFromEncoded(encoded).toComposeImageBitmap()
}
