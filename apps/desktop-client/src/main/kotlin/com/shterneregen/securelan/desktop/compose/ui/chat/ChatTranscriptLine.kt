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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.SecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.motionTween
import com.shterneregen.securelan.desktop.compose.state.chat.ComposeChatMessage
import com.shterneregen.securelan.desktop.compose.state.chat.ComposeChatTranscriptLineKind
import com.shterneregen.securelan.desktop.compose.state.chat.ComposeChatTranscriptLinePresentation

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
            if (style.framed) {
                Surface(
                    modifier = Modifier.fillMaxWidth(style.width),
                    shape = bubbleShape,
                    color = style.backgroundColor,
                ) {
                    ChatTranscriptLineContent(presentation, style, tokens)
                }
            } else {
                ChatTranscriptLineContent(presentation, style, tokens)
            }
        }
    }
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
