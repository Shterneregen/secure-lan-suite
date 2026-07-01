package com.shterneregen.securelan.desktop.compose.ui.shell

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.state.connection.ComposeStatusConnectionState
import com.shterneregen.securelan.desktop.compose.state.shell.ComposeGlobalStatusIndicatorState
import com.shterneregen.securelan.desktop.compose.ui.components.StatusChip

@Composable
internal fun ComposeStatusBar(
    state: ComposeStatusConnectionState,
    peerStatus: String = "Peer not selected",
    voiceStatus: String = "Voice idle",
    transferStatus: String = "Transfers idle",
    darkTheme: Boolean,
    onThemeToggle: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(LocalSecureLanDesignTokens.current.radius.large),
        border = BorderStroke(1.dp, LocalSecureLanDesignTokens.current.colors.borderSubtle),
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.surface,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val globalState = ComposeGlobalStatusIndicatorState(
                statusState = state,
                peerStatus = peerStatus,
                voiceStatus = voiceStatus,
                transferStatus = transferStatus,
            )
            StatusChip(globalState.label)
            Text(
                text = globalState.detailText,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.56f),
            )
            ThemeToggleButton(darkTheme = darkTheme, onThemeToggle = onThemeToggle)
        }
    }
}
