package com.shterneregen.securelan.desktop.compose.ui.connection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.shterneregen.securelan.desktop.compose.ComposeDesktopHostAdapter
import com.shterneregen.securelan.desktop.compose.state.shell.ComposeShellMetadata
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionHubMode
import com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionJoinTarget
import com.shterneregen.securelan.desktop.compose.state.shell.ComposeWorkspaceState
import com.shterneregen.securelan.desktop.compose.ui.shell.WorkspaceCenterColumn
import com.shterneregen.securelan.desktop.compose.settings.DesktopConnectionMode

@Composable
internal fun MessengerCenterPanel(
    hostAdapter: ComposeDesktopHostAdapter?,
    workspaceState: ComposeWorkspaceState,
    selectedJoinTarget: ComposeConnectionJoinTarget?,
    hubTooltip: String? = null,
    headerActions: @Composable RowScope.() -> Unit = {},
    chatSurface: @Composable () -> Unit,
) {
    var connectionHubExpanded by remember(workspaceState.connectionHubExpandedByDefault) {
        mutableStateOf(workspaceState.connectionHubExpandedByDefault)
    }
    LaunchedEffect(workspaceState.connectionHubExpandedByDefault) {
        connectionHubExpanded = workspaceState.connectionHubExpandedByDefault
    }

    WorkspaceCenterColumn(
        workspaceState = workspaceState,
        connectionHubExpanded = connectionHubExpanded,
        onConnectionHubToggle = { connectionHubExpanded = !connectionHubExpanded },
        startupSurface = {
            if (hostAdapter != null) {
                LiveConnectionHubSurface(
                    hostAdapter = hostAdapter,
                    selectedJoinTarget = selectedJoinTarget,
                )
            } else {
                PreviewConnectionHubSurface()
            }
        },
        chatSurface = chatSurface,
        hubTooltip = hubTooltip,
        headerActions = headerActions,
    )
}

@Composable
private fun LiveConnectionHubSurface(
    hostAdapter: ComposeDesktopHostAdapter,
    selectedJoinTarget: ComposeConnectionJoinTarget?,
) {
    var hubMode by remember {
        mutableStateOf(
            if (hostAdapter.preferredConnectionMode == DesktopConnectionMode.JOIN) {
                ComposeConnectionHubMode.JOIN
            } else {
                ComposeConnectionHubMode.HOST
            },
        )
    }
    LaunchedEffect(selectedJoinTarget) {
        if (selectedJoinTarget != null) {
            hubMode = ComposeConnectionHubMode.JOIN
        }
    }

    val hostEnabled = !hostAdapter.statusState.clientConnected
    val joinEnabled = !hostAdapter.statusState.localServerRunning

    LaunchedEffect(hubMode, hostAdapter.statusState.localServerRunning, hostAdapter.statusState.clientConnected) {
        when (hubMode) {
            ComposeConnectionHubMode.HOST -> {
                if (!hostEnabled && joinEnabled) hubMode = ComposeConnectionHubMode.JOIN
            }
            ComposeConnectionHubMode.JOIN -> {
                if (!joinEnabled && hostEnabled) hubMode = ComposeConnectionHubMode.HOST
            }
        }
    }

    val tokens = LocalSecureLanDesignTokens.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
    ) {
        ConnectionModeSelector(
            mode = hubMode,
            hostLabel = ComposeShellMetadata.DEFAULT_CONNECTION_HUB_STATE.hostTabLabel,
            joinLabel = ComposeShellMetadata.DEFAULT_CONNECTION_HUB_STATE.joinTabLabel,
            onModeChange = {
                hubMode = it
                hostAdapter.updateLastConnectionMode(
                    if (it == ComposeConnectionHubMode.JOIN) DesktopConnectionMode.JOIN else DesktopConnectionMode.HOST,
                )
            },
            tooltip = ComposeShellMetadata.DEFAULT_CONNECTION_HUB_STATE.modeSelectorTooltip,
            modifier = Modifier.fillMaxWidth(),
            hostEnabled = hostEnabled,
            joinEnabled = joinEnabled,
        )
        ConnectionHub(
            hostAdapter = hostAdapter,
            initialMode = hubMode,
            selectedJoinTarget = selectedJoinTarget,
            wrapInCard = false,
        )
    }
}

@Composable
private fun PreviewConnectionHubSurface() {
    var hubMode by remember { mutableStateOf(ComposeConnectionHubMode.HOST) }
    val tokens = LocalSecureLanDesignTokens.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
    ) {
        ConnectionModeSelector(
            mode = hubMode,
            hostLabel = ComposeShellMetadata.DEFAULT_CONNECTION_HUB_STATE.hostTabLabel,
            joinLabel = ComposeShellMetadata.DEFAULT_CONNECTION_HUB_STATE.joinTabLabel,
            onModeChange = { hubMode = it },
            tooltip = ComposeShellMetadata.DEFAULT_CONNECTION_HUB_STATE.modeSelectorTooltip,
            modifier = Modifier.fillMaxWidth(),
        )
        ConnectionHubPreview(
            state = ComposeShellMetadata.DEFAULT_CONNECTION_HUB_STATE.copy(mode = hubMode),
            wrapInCard = false,
        )
    }
}
