package com.shterneregen.securelan.desktop.compose.state.shell

data class ComposeWorkspaceConsistencyReviewItem(
    val area: ComposeWorkspaceConsistencyReviewArea,
    val label: String,
    val evidence: String,
    val passed: Boolean = true,
)
