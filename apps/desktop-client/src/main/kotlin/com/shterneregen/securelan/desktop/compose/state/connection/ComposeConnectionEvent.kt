package com.shterneregen.securelan.desktop.compose.state.connection

data class ComposeConnectionEvent(
    val kind: ComposeConnectionEventKind,
    val message: String,
) {
    val displayText: String = "${kind.name.lowercase()}: $message"
}
