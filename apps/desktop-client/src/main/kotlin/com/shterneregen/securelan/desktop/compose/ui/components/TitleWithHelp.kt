package com.shterneregen.securelan.desktop.compose.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens

@Composable
internal fun TitleWithHelp(
    title: String,
    tooltip: String,
    modifier: Modifier = Modifier,
    titleStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.subtitle2,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = titleStyle)
        HelpTooltip(tooltip)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun HelpTooltip(text: String) {
    TooltipArea(
        tooltip = {
            Surface(
                shape = RoundedCornerShape(LocalSecureLanDesignTokens.current.radius.small),
                border = BorderStroke(1.dp, LocalSecureLanDesignTokens.current.colors.borderSubtle),
                color = MaterialTheme.colors.surface,
            ) {
                Text(
                    text = text,
                    modifier = Modifier.widthIn(max = 320.dp).padding(horizontal = 10.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.82f),
                )
            }
        },
    ) {
        Surface(
            shape = RoundedCornerShape(LocalSecureLanDesignTokens.current.radius.pill),
            border = BorderStroke(1.dp, LocalSecureLanDesignTokens.current.colors.borderSubtle),
            color = LocalSecureLanDesignTokens.current.colors.surfaceLevel2,
        ) {
            Text(
                text = "?",
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
            )
        }
    }
}
