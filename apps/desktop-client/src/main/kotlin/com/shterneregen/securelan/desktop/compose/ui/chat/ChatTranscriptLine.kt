package com.shterneregen.securelan.desktop.compose.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.SecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.motionTween
import com.shterneregen.securelan.desktop.compose.state.chat.ComposeChatMessage
import com.shterneregen.securelan.desktop.compose.state.chat.ComposeChatTranscriptLineKind
import com.shterneregen.securelan.desktop.compose.state.chat.ComposeChatTranscriptLinePresentation
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButton
import com.shterneregen.securelan.desktop.compose.ui.components.CompactIconButton
import com.shterneregen.securelan.desktop.compose.ui.icons.SecureLanIcons
import com.shterneregen.securelan.desktop.compose.util.copyToSystemClipboard
import com.shterneregen.securelan.desktop.compose.util.openInBrowser
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import org.jetbrains.skia.Image as SkiaImage

@Composable
internal fun ChatTranscriptLine(message: ComposeChatMessage, localNickname: String = "") {
    val tokens = LocalSecureLanDesignTokens.current
    val presentation = ComposeChatTranscriptLinePresentation.from(message.displayText, localNickname, message.timestamp)
    val style = rememberChatTranscriptLineStyle(presentation.kind, tokens)
    val bubbleShape = when (presentation.kind) {
        ComposeChatTranscriptLineKind.USER_LOCAL -> RoundedCornerShape(
            topStart = tokens.radius.medium,
            topEnd = tokens.radius.medium,
            bottomStart = tokens.radius.small,
            bottomEnd = tokens.radius.medium,
        )
        ComposeChatTranscriptLineKind.USER_REMOTE -> RoundedCornerShape(
            topStart = tokens.radius.medium,
            topEnd = tokens.radius.medium,
            bottomStart = tokens.radius.medium,
            bottomEnd = tokens.radius.small,
        )
        else -> RoundedCornerShape(tokens.radius.medium)
    }

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(motionTween()) + slideInVertically(motionTween()) { it / 4 },
        exit = fadeOut(motionTween()),
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = style.alignment) {
            if (
                presentation.kind == ComposeChatTranscriptLineKind.SYSTEM ||
                presentation.kind == ComposeChatTranscriptLineKind.PRESENCE
            ) {
                CompactSystemTranscriptEvent(presentation, tokens)
            } else if (style.framed) {
                Surface(
                    modifier = Modifier.fillMaxWidth(style.width),
                    shape = bubbleShape,
                    color = style.backgroundColor,
                ) {
                    if (presentation.kind == ComposeChatTranscriptLineKind.QUICK_SHARE) {
                        QuickShareTranscriptEvent(presentation, tokens)
                    } else {
                        ChatTranscriptLineContent(presentation, style, tokens)
                    }
                }
            } else {
                ChatTranscriptLineContent(presentation, style, tokens)
            }
        }
    }
}

@Composable
private fun CompactSystemTranscriptEvent(
    presentation: ComposeChatTranscriptLinePresentation,
    tokens: SecureLanDesignTokens,
) {
    Surface(
        shape = RoundedCornerShape(tokens.radius.pill),
        color = tokens.colors.surfaceLevel3.copy(alpha = 0.78f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = tokens.spacing.sm, vertical = tokens.spacing.xxs),
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = presentation.body,
                style = MaterialTheme.typography.caption,
                color = tokens.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = presentation.displayTime,
                style = MaterialTheme.typography.caption,
                color = tokens.colors.textTertiary,
            )
        }
    }
}

@Composable
private fun QuickShareTranscriptEvent(
    presentation: ComposeChatTranscriptLinePresentation,
    tokens: SecureLanDesignTokens,
) {
    val qrDialogOpen = remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.padding(horizontal = tokens.spacing.sm, vertical = tokens.spacing.xs),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = SecureLanIcons.QuickShare,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colors.primary,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.xxs),
            ) {
                Text(
                    text = presentation.body.substringBefore("http").trim().trimEnd(':'),
                    style = MaterialTheme.typography.body2,
                    color = tokens.colors.textPrimary,
                )
                presentation.actionUrl?.let { url ->
                    Text(
                        text = url,
                        style = MaterialTheme.typography.caption,
                        color = tokens.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            presentation.actionUrl?.let { url ->
                if (presentation.showsQrCode) {
                    CompactIconButton(
                        onClick = { qrDialogOpen.value = true },
                        icon = SecureLanIcons.QrCode,
                        contentDescription = "Show QR code",
                    )
                }
                CompactButton(onClick = { copyToSystemClipboard(url) }) { Text("Copy") }
                CompactButton(onClick = { openInBrowser(url) }) { Text("Open") }
            }
        }
    }
    if (qrDialogOpen.value) {
        presentation.actionUrl?.let { url ->
            FileLinkQrDialog(url = url, onClose = { qrDialogOpen.value = false })
        }
    }
}

@Composable
private fun FileLinkQrDialog(url: String, onClose: () -> Unit) {
    DialogWindow(
        onCloseRequest = onClose,
        state = rememberDialogState(size = DpSize(380.dp, 460.dp)),
        title = "File link QR code",
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
                        text = "Scan to open the file link",
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
                        contentDescription = "QR code for the created file link",
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CompactButton(onClick = { copyToSystemClipboard(url) }) { Text("Copy link") }
                }
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

@Composable
internal fun ChatTranscriptLineContent(
    presentation: ComposeChatTranscriptLinePresentation,
    style: ChatTranscriptLineStyle,
    tokens: SecureLanDesignTokens,
) {
    Column(
        modifier = if (style.framed) {
            Modifier.padding(horizontal = tokens.spacing.sm, vertical = tokens.spacing.xs)
        } else {
            Modifier.padding(vertical = tokens.spacing.xxs)
        },
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.xxs),
    ) {
        if (style.showMeta) {
            ChatTranscriptLineMetaRow(presentation, style, tokens)
        }
        ChatTranscriptLineBody(presentation, style)
    }
}

@Composable
internal fun ChatTranscriptLineMetaRow(
    presentation: ComposeChatTranscriptLinePresentation,
    style: ChatTranscriptLineStyle,
    tokens: SecureLanDesignTokens,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SelectionContainer(modifier = Modifier.weight(1f)) {
            Text(
                text = presentation.label,
                style = MaterialTheme.typography.caption,
                color = style.accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = presentation.displayTime,
            style = MaterialTheme.typography.caption,
            color = tokens.colors.textTertiary,
        )
    }
}

@Composable
internal fun ChatTranscriptLineBody(
    presentation: ComposeChatTranscriptLinePresentation,
    style: ChatTranscriptLineStyle,
) {
    SelectionContainer {
        Text(
            text = presentation.body,
            modifier = Modifier.fillMaxWidth(),
            style = style.bodyTextStyle,
            color = style.bodyColor,
            textAlign = style.textAlign,
        )
    }
}

internal data class ChatTranscriptLineStyle(
    val alignment: Alignment,
    val width: Float,
    val framed: Boolean,
    val backgroundColor: Color,
    val accentColor: Color,
    val bodyColor: Color,
    val bodyTextStyle: TextStyle,
    val showMeta: Boolean,
    val textAlign: TextAlign,
)

@Composable
internal fun rememberChatTranscriptLineStyle(kind: ComposeChatTranscriptLineKind, tokens: SecureLanDesignTokens): ChatTranscriptLineStyle {
    val body1Style = MaterialTheme.typography.body1
    val body2Style = MaterialTheme.typography.body2
    val captionStyle = MaterialTheme.typography.caption
    return remember(kind, tokens) {
        when (kind) {
            ComposeChatTranscriptLineKind.USER_LOCAL -> ChatTranscriptLineStyle(
                alignment = Alignment.CenterEnd,
                width = 0.82f,
                framed = true,
                backgroundColor = tokens.colors.accent.copy(alpha = 0.14f),
                accentColor = tokens.colors.accent,
                bodyColor = tokens.colors.textPrimary,
                bodyTextStyle = body1Style,
                showMeta = true,
                textAlign = TextAlign.Start,
            )
            ComposeChatTranscriptLineKind.USER_REMOTE -> ChatTranscriptLineStyle(
                alignment = Alignment.CenterStart,
                width = 0.82f,
                framed = true,
                backgroundColor = tokens.colors.surfaceLevel2,
                accentColor = tokens.colors.textSecondary,
                bodyColor = tokens.colors.textPrimary,
                bodyTextStyle = body1Style,
                showMeta = true,
                textAlign = TextAlign.Start,
            )
            ComposeChatTranscriptLineKind.PRESENCE -> ChatTranscriptLineStyle(
                alignment = Alignment.Center,
                width = 0.72f,
                framed = false,
                backgroundColor = Color.Transparent,
                accentColor = tokens.colors.textTertiary,
                bodyColor = tokens.colors.textTertiary,
                bodyTextStyle = captionStyle,
                showMeta = false,
                textAlign = TextAlign.Center,
            )
            ComposeChatTranscriptLineKind.TRANSFER -> ChatTranscriptLineStyle(
                alignment = Alignment.Center,
                width = 0.96f,
                framed = true,
                backgroundColor = tokens.colors.success.copy(alpha = 0.08f),
                accentColor = tokens.colors.success,
                bodyColor = tokens.colors.textSecondary,
                bodyTextStyle = body2Style,
                showMeta = true,
                textAlign = TextAlign.Start,
            )
            ComposeChatTranscriptLineKind.QUICK_SHARE -> ChatTranscriptLineStyle(
                alignment = Alignment.Center,
                width = 0.96f,
                framed = true,
                backgroundColor = tokens.colors.accent.copy(alpha = 0.08f),
                accentColor = tokens.colors.accent,
                bodyColor = tokens.colors.textPrimary,
                bodyTextStyle = body2Style,
                showMeta = false,
                textAlign = TextAlign.Start,
            )
            ComposeChatTranscriptLineKind.SECURITY -> ChatTranscriptLineStyle(
                alignment = Alignment.Center,
                width = 0.96f,
                framed = true,
                backgroundColor = tokens.colors.error.copy(alpha = 0.08f),
                accentColor = tokens.colors.error,
                bodyColor = tokens.colors.error,
                bodyTextStyle = body2Style,
                showMeta = true,
                textAlign = TextAlign.Start,
            )
            ComposeChatTranscriptLineKind.CALL -> ChatTranscriptLineStyle(
                alignment = Alignment.Center,
                width = 0.96f,
                framed = true,
                backgroundColor = tokens.colors.warning.copy(alpha = 0.08f),
                accentColor = tokens.colors.warning,
                bodyColor = tokens.colors.textSecondary,
                bodyTextStyle = body2Style,
                showMeta = true,
                textAlign = TextAlign.Start,
            )
            ComposeChatTranscriptLineKind.SYSTEM -> ChatTranscriptLineStyle(
                alignment = Alignment.Center,
                width = 0.82f,
                framed = true,
                backgroundColor = tokens.colors.surfaceLevel3,
                accentColor = tokens.colors.warning,
                bodyColor = tokens.colors.textPrimary,
                bodyTextStyle = body1Style,
                showMeta = true,
                textAlign = TextAlign.Start,
            )
            ComposeChatTranscriptLineKind.DIAGNOSTIC -> ChatTranscriptLineStyle(
                alignment = Alignment.Center,
                width = 0.96f,
                framed = false,
                backgroundColor = Color.Transparent,
                accentColor = tokens.colors.textTertiary,
                bodyColor = tokens.colors.textTertiary,
                bodyTextStyle = captionStyle,
                showMeta = false,
                textAlign = TextAlign.Center,
            )
        }
    }
}
