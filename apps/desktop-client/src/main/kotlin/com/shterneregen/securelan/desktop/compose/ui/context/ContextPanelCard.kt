package com.shterneregen.securelan.desktop.compose.ui.context

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.state.shell.ComposeContextPanelCard
import com.shterneregen.securelan.desktop.compose.state.shell.ComposeContextPanelCardKind
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.motionTween
import com.shterneregen.securelan.desktop.compose.ui.components.TitleWithHelp
import com.shterneregen.securelan.desktop.compose.ui.icons.SecureLanIcons

@Composable
internal fun ContextPanelCard(
    card: ComposeContextPanelCard,
    expandedContent: @Composable (() -> Unit)? = null,
    initialExpanded: Boolean? = null,
) {
    val tokens = LocalSecureLanDesignTokens.current
    var expanded by remember(card.kind, card.title, card.collapsed, initialExpanded) {
        mutableStateOf(initialExpanded ?: !card.collapsed)
    }
    val canCollapse = card.collapsed || expandedContent != null
    val borderAlpha = if (card.primary) 0.40f else 0.16f
    Surface(
        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
        shape = RoundedCornerShape(tokens.radius.medium),
        border = BorderStroke(
            1.dp,
            (if (card.primary) MaterialTheme.colors.primary else tokens.colors.borderSubtle).copy(alpha = borderAlpha)
        ),
        color = if (card.primary) {
            tokens.colors.surfaceLevel3.copy(alpha = 0.60f)
        } else {
            tokens.colors.surfaceLevel2.copy(alpha = 0.50f)
        },
    ) {
        Column(
            modifier = Modifier.padding(horizontal = tokens.spacing.sm, vertical = tokens.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val leadingIcon = contextPanelCardIcon(card.kind)
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (card.primary) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(
                            alpha = 0.78f
                        ),
                    )
                }
                Column(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(tokens.spacing.xxs),
                ) {
                    val titleStyle =
                        if (card.primary) MaterialTheme.typography.subtitle1 else MaterialTheme.typography.subtitle2
                    if (card.tooltip == null) {
                        Text(card.title, style = titleStyle)
                    } else {
                        TitleWithHelp(
                            title = card.title,
                            tooltip = card.tooltip,
                            titleStyle = titleStyle,
                        )
                    }
                    if (!card.badge.isNullOrBlank()) {
                        Text(
                            card.badge,
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.58f)
                        )
                    }
                }
                if (canCollapse) {
                    Text(
                        text = if (expanded) "Hide" else "Show",
                        modifier = Modifier.clickable { expanded = !expanded }
                            .padding(horizontal = tokens.spacing.xxs, vertical = tokens.spacing.xxs),
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.primary,
                    )
                }
            }
            AnimatedVisibility(
                visible = expanded || !canCollapse,
                enter = fadeIn(motionTween()) + expandVertically(motionTween(), expandFrom = Alignment.Top),
                exit = shrinkVertically(motionTween(), shrinkTowards = Alignment.Top) + fadeOut(motionTween()),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs)) {
                    Text(
                        card.body,
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.74f),
                        maxLines = card.maxBodyLines.coerceAtLeast(1),
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!card.metadata.isNullOrBlank()) {
                        CapabilityChipRow(summary = card.metadata)
                    }
                    if (!card.primaryAction.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(tokens.radius.small),
                            border = BorderStroke(tokens.border.subtle, MaterialTheme.colors.primary.copy(alpha = 0.30f)),
                            color = tokens.colors.surfaceLevel2.copy(alpha = 0.64f)
                        ) {
                            Text(
                                text = card.primaryAction,
                                modifier = Modifier.padding(horizontal = tokens.spacing.xs, vertical = tokens.spacing.xxs),
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.primary,
                            )
                        }
                    }
                    expandedContent?.invoke()
                }
            }
        }
    }
}

@Composable
private fun CapabilityChipRow(summary: String) {
    val tokens = LocalSecureLanDesignTokens.current
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xxs),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.xxs),
    ) {
        summary.split("·").map(String::trim).filter(String::isNotEmpty).forEach { label ->
            val icon = SecureLanIcons.forCapability(label)
            Surface(
                shape = RoundedCornerShape(tokens.radius.pill),
                color = tokens.colors.surfaceLevel1.copy(alpha = 0.72f),
                border = BorderStroke(1.dp, tokens.colors.borderSubtle.copy(alpha = 0.40f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = tokens.spacing.xs, vertical = tokens.spacing.xxs),
                    horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xxs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
                        )
                    }
                    Text(
                        text = label,
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.78f),
                    )
                }
            }
        }
    }
}

private fun contextPanelCardIcon(kind: ComposeContextPanelCardKind): ImageVector? = when (kind) {
    ComposeContextPanelCardKind.RECENT_FILES -> SecureLanIcons.History
    ComposeContextPanelCardKind.QUICK_SHARE -> SecureLanIcons.QuickShare
    ComposeContextPanelCardKind.TRANSFER_DETAILS -> SecureLanIcons.File
    ComposeContextPanelCardKind.CALL_CONTROLS -> SecureLanIcons.Voice
    ComposeContextPanelCardKind.MEDIA -> SecureLanIcons.Video
    ComposeContextPanelCardKind.QUICK_ACTIONS -> null
}
