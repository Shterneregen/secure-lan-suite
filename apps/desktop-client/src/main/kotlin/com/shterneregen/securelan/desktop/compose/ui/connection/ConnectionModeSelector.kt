package com.shterneregen.securelan.desktop.compose.ui.connection

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionHubMode

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ConnectionModeSelector(
    mode: ComposeConnectionHubMode,
    hostLabel: String,
    joinLabel: String,
    onModeChange: (ComposeConnectionHubMode) -> Unit,
    modifier: Modifier = Modifier,
    tooltip: String? = null,
    hostEnabled: Boolean = true,
    joinEnabled: Boolean = true,
) {
    val tokens = LocalSecureLanDesignTokens.current
    val selector: @Composable (Modifier) -> Unit = { surfaceModifier ->
        Surface(
            modifier = surfaceModifier,
            shape = RoundedCornerShape(tokens.radius.small),
            border = BorderStroke(1.dp, tokens.colors.borderSubtle),
            color = tokens.colors.surfaceLevel2,
        ) {
            Row(modifier = Modifier.padding(3.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                ConnectionModeSegment(
                    label = hostLabel,
                    selected = mode == ComposeConnectionHubMode.HOST,
                    enabled = hostEnabled,
                    onClick = { onModeChange(ComposeConnectionHubMode.HOST) },
                    modifier = Modifier.weight(1f),
                )
                ConnectionModeSegment(
                    label = joinLabel,
                    selected = mode == ComposeConnectionHubMode.JOIN,
                    enabled = joinEnabled,
                    onClick = { onModeChange(ComposeConnectionHubMode.JOIN) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
    if (tooltip != null) {
        TooltipArea(
            modifier = modifier,
            tooltip = {
                Surface(
                    shape = RoundedCornerShape(tokens.radius.small),
                    border = BorderStroke(1.dp, tokens.colors.borderSubtle),
                    color = MaterialTheme.colors.surface,
                ) {
                    Text(
                        text = tooltip,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.82f),
                    )
                }
            },
        ) {
            selector(Modifier.fillMaxWidth())
        }
    } else {
        selector(modifier)
    }
}
