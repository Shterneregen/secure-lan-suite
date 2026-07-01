package com.shterneregen.securelan.desktop.compose.ui.connection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionHubMode

@Composable
internal fun ConnectionModeChooser(
    mode: ComposeConnectionHubMode,
    hostLabel: String,
    hostSubtitle: String,
    joinLabel: String,
    joinSubtitle: String,
    onModeChange: (ComposeConnectionHubMode) -> Unit,
) {
    val tokens = LocalSecureLanDesignTokens.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
    ) {
        ConnectionModeChoiceCard(
            title = hostLabel,
            subtitle = hostSubtitle,
            selected = mode == ComposeConnectionHubMode.HOST,
            onClick = { onModeChange(ComposeConnectionHubMode.HOST) },
            modifier = Modifier.weight(1f),
        )
        ConnectionModeChoiceCard(
            title = joinLabel,
            subtitle = joinSubtitle,
            selected = mode == ComposeConnectionHubMode.JOIN,
            onClick = { onModeChange(ComposeConnectionHubMode.JOIN) },
            modifier = Modifier.weight(1f),
        )
    }
}
