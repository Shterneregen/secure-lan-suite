package com.shterneregen.securelan.desktop.compose.ui.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.state.shell.ComposeContextPanelResponsiveState
import com.shterneregen.securelan.desktop.compose.state.shell.ComposeWorkspaceLayout
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButton

@Composable
internal fun MainWorkspaceRow(
    layout: ComposeWorkspaceLayout,
    peersTooltip: String? = null,
    chatTooltip: String? = null,
    rightColumnTitle: String = layout.actionsColumn.title,
    peersColumn: @Composable () -> Unit,
    chatColumn: @Composable (@Composable RowScope.() -> Unit) -> Unit,
    actionsColumn: @Composable (ComposeContextPanelResponsiveState) -> Unit,
) {
    val tokens = LocalSecureLanDesignTokens.current
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
        val responsiveState = ComposeContextPanelResponsiveState.forWidth(maxWidth.value.toInt())
        var contextDrawerOpen by remember(responsiveState.mode) { mutableStateOf(false) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
        ) {
            MainWorkspaceColumn(
                title = layout.peersColumn.title,
                tooltip = peersTooltip,
                modifier = Modifier.weight(layout.peersColumn.weight).fillMaxHeight(),
                content = peersColumn,
            )
            MainWorkspaceColumn(
                title = null,
                tooltip = chatTooltip,
                modifier = Modifier.weight(layout.conversationColumn.weight).fillMaxHeight(),
                content = {
                    chatColumn {
                        if (responsiveState.drawerEntryVisible) {
                            CompactButton(
                                onClick = { contextDrawerOpen = true },
                                modifier = Modifier.semantics {
                                    contentDescription = responsiveState.drawerOpenContentDescription
                                },
                            ) { Text("Context") }
                        }
                    }
                },
            )
            if (responsiveState.inlinePanelVisible) {
                MainWorkspaceColumn(
                    title = rightColumnTitle,
                    modifier = Modifier.weight(layout.actionsColumn.weight).fillMaxHeight(),
                    content = { actionsColumn(responsiveState) },
                )
            }
        }
        if (responsiveState.drawerMode) {
            ContextAssistantDrawer(
                visible = contextDrawerOpen,
                responsiveState = responsiveState,
                onClose = { contextDrawerOpen = false },
                content = { actionsColumn(responsiveState) },
            )
        }
    }
}
