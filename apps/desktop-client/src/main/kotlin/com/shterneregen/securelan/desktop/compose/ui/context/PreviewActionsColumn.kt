package com.shterneregen.securelan.desktop.compose.ui.context

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.ui.context.ContextPanelCard
import com.shterneregen.securelan.desktop.compose.ui.context.ContextPanelSummary
import com.shterneregen.securelan.desktop.compose.state.shell.ComposeContextPanelResponsiveState
import com.shterneregen.securelan.desktop.compose.state.shell.ComposeShellMetadata

@Composable
internal fun PreviewActionsColumn(responsiveState: ComposeContextPanelResponsiveState) {
    val contextPanelState = ComposeShellMetadata.DEFAULT_CONTEXT_PANEL_STATE
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ContextPanelSummary(contextPanelState, responsiveState)
        contextPanelState.visibleCardsFor(responsiveState).forEach { card ->
            ContextPanelCard(card)
        }
    }
}
