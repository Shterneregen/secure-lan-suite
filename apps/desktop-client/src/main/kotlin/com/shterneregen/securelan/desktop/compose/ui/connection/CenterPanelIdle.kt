package com.shterneregen.securelan.desktop.compose.ui.connection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionJoinTarget
import com.shterneregen.securelan.desktop.compose.state.shell.ComposeOnboardingState
import com.shterneregen.securelan.desktop.compose.ui.components.CalmFocusButton
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButton

@Composable
internal fun CenterPanelIdle(
    state: ComposeOnboardingState,
    nearbyTargets: List<ComposeConnectionJoinTarget>,
    onCreateRoom: () -> Unit,
    onJoinRoom: () -> Unit,
    onRoomSelected: (ComposeConnectionJoinTarget) -> Unit,
) {
    val tokens = LocalSecureLanDesignTokens.current
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = tokens.spacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.xl, Alignment.CenterVertically),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.md),
        ) {
            Text(
                text = state.brandGlyph,
                style = MaterialTheme.typography.h3,
                color = MaterialTheme.colors.primary,
            )
            Text(
                text = state.headline,
                style = MaterialTheme.typography.h4,
                color = MaterialTheme.colors.onSurface,
            )
            Text(
                text = state.body,
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.68f),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
            CalmFocusButton(onClick = onCreateRoom) { Text(state.hostActionLabel) }
            CompactButton(onClick = onJoinRoom) { Text(state.joinActionLabel) }
        }

        if (nearbyTargets.isNotEmpty()) {
            Column(
                modifier = Modifier.widthIn(max = 520.dp, min = 320.dp),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
            ) {
                Text(
                    text = state.discoveryStatus,
                    style = MaterialTheme.typography.subtitle2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
                )
                nearbyTargets.take(5).forEach { target ->
                    CenterPanelRoomRow(
                        target = target,
                        selected = false,
                        onSelected = { onRoomSelected(target) },
                    )
                }
            }
        }
    }
}
