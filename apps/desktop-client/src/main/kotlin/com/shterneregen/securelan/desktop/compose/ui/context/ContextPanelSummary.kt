package com.shterneregen.securelan.desktop.compose.ui.context

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import com.shterneregen.securelan.desktop.compose.state.shell.ComposeContextPanelState
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens

@Composable
internal fun ContextPanelSummary(
    state: ComposeContextPanelState,
) {
    val tokens = LocalSecureLanDesignTokens.current
    Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.xxs)) {
        Text(
            text = state.nextActionSummary,
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.68f),
        )
        if (state.hiddenFeatureNames.isNotEmpty()) {
            Text(
                text = state.hiddenFeatureSummary,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.48f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
