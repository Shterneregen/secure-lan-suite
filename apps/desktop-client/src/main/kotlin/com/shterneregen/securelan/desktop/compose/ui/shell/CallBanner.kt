package com.shterneregen.securelan.desktop.compose.ui.shell

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.state.shell.ComposeWorkspaceMode
import com.shterneregen.securelan.desktop.compose.state.shell.ComposeWorkspaceState
import com.shterneregen.securelan.desktop.compose.ui.components.MicroFeedbackPill
import com.shterneregen.securelan.desktop.compose.util.MicrointeractionTone

@Composable
internal fun CallBanner(workspaceState: ComposeWorkspaceState) {
    val tokens = LocalSecureLanDesignTokens.current
    val tone = when {
        workspaceState.mode == ComposeWorkspaceMode.VOICE_CALL || workspaceState.mode == ComposeWorkspaceMode.VIDEO_CALL -> MicrointeractionTone.SUCCESS
        workspaceState.subtitle.contains("disconnected", ignoreCase = true) -> MicrointeractionTone.FAILURE
        else -> MicrointeractionTone.NEUTRAL
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(tokens.radius.medium),
        color = tokens.colors.success.copy(alpha = if (MaterialTheme.colors.isLight) 0.09f else 0.14f),
        border = BorderStroke(tokens.border.subtle, tokens.colors.success.copy(alpha = 0.36f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = tokens.spacing.sm, vertical = tokens.spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = workspaceState.title,
                style = MaterialTheme.typography.subtitle2,
                color = MaterialTheme.colors.primary,
                modifier = Modifier.weight(1f),
            )
            MicroFeedbackPill(text = workspaceState.subtitle, tone = tone)
        }
    }
}
