package com.shterneregen.securelan.desktop.compose.state.transfer

public enum class ComposeIncomingTransferPromptStatus(val label: String) {
    WAITING("Needs your decision"),
    ACCEPTED("Accepted"),
    AUTO_ACCEPTED("Auto-accepted"),
    REJECTED("Rejected"),
}
