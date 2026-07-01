package com.shterneregen.securelan.desktop.compose.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens

@Composable
internal fun HeaderCard(
    title: String,
    tooltip: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = LocalSecureLanDesignTokens.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(tokens.radius.large),
        border = BorderStroke(tokens.border.subtle, tokens.colors.borderSubtle),
        elevation = tokens.elevation.flat,
        backgroundColor = MaterialTheme.colors.surface,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = tokens.spacing.sm, vertical = tokens.spacing.xs),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
        ) {
            if (tooltip == null) {
                Text(title, style = MaterialTheme.typography.subtitle2)
            } else {
                TitleWithHelp(title = title, tooltip = tooltip)
            }
            content()
        }
    }
}

@Composable
internal fun ContentSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(LocalSecureLanDesignTokens.current.radius.medium),
        border = BorderStroke(1.dp, LocalSecureLanDesignTokens.current.colors.borderSubtle),
        color = LocalSecureLanDesignTokens.current.colors.surfaceLevel2,
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(LocalSecureLanDesignTokens.current.spacing.sm), content = content)
    }
}

@Composable
internal fun SubtleContentSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(LocalSecureLanDesignTokens.current.radius.medium),
        border = BorderStroke(1.dp, LocalSecureLanDesignTokens.current.colors.borderSubtle.copy(alpha = 0.55f)),
        color = LocalSecureLanDesignTokens.current.colors.surfaceLevel2.copy(alpha = 0.62f),
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(LocalSecureLanDesignTokens.current.spacing.sm), content = content)
    }
}

@Composable
internal fun PeerListContentSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = LocalSecureLanDesignTokens.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(tokens.radius.medium),
        color = tokens.colors.surfaceLevel1.copy(alpha = 0.64f),
    ) {
        Column(modifier = Modifier.fillMaxSize(), content = content)
    }
}
