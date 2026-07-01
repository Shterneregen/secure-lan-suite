package com.shterneregen.securelan.desktop.compose.state.shell

data class ComposeWorkspaceLayout(
    val peersColumn: ComposeWorkspaceColumn = ComposeWorkspaceColumn(
        title = "Peers",
        weight = 0.20f,
    ),
    val conversationColumn: ComposeWorkspaceColumn = ComposeWorkspaceColumn(
        title = "Shared room chat",
        weight = 0.60f,
    ),
    val actionsColumn: ComposeWorkspaceColumn = ComposeWorkspaceColumn(
        title = "Actions",
        weight = 0.20f,
    ),
)
