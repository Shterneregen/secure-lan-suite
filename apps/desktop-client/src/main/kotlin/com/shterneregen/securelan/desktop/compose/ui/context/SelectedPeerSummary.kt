package com.shterneregen.securelan.desktop.compose.ui.context

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.ComposeSelectedPeerQuickActionsState

@Composable
internal fun SelectedPeerSummary(state: ComposeSelectedPeerQuickActionsState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(state.title, style = MaterialTheme.typography.h6)
        Text(
            text = state.meta,
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
        )
    }
}
