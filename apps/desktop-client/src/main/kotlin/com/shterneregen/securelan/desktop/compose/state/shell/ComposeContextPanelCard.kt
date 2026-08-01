package com.shterneregen.securelan.desktop.compose.state.shell

data class ComposeContextPanelCard(
    val kind: ComposeContextPanelCardKind,
    val title: String,
    val body: String,
    val badge: String? = null,
    val primaryAction: String? = null,
    val primary: Boolean = false,
    val collapsed: Boolean = false,
    val technical: Boolean = false,
    val maxBodyLines: Int = 3,
    val metadata: String? = null,
    val tooltip: String? = null,
)
