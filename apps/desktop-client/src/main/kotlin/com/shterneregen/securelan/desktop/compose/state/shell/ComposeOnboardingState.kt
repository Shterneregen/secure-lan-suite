package com.shterneregen.securelan.desktop.compose.state.shell

data class ComposeOnboardingState(
    val headline: String = "Secure chat for people nearby",
    val body: String = "",
    val discoveryStatus: String = "Looking for nearby rooms…",
) {
    val title: String = ComposeShellMetadata.APP_NAME
}
