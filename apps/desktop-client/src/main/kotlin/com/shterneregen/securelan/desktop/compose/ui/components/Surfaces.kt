package com.shterneregen.securelan.desktop.compose.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens

@Composable
internal fun SubtleContentSurface(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(LocalSecureLanDesignTokens.current.radius.medium),
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
