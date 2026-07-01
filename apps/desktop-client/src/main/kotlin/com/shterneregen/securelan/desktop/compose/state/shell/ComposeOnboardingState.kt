package com.shterneregen.securelan.desktop.compose.state.shell


data class ComposeOnboardingState(
    val headline: String = "Secure chat for people nearby",
    val body: String = "",
    val hostActionLabel: String = "Host secure room",
    val joinActionLabel: String = "Join nearby room",
    val secondaryLinks: List<String> = listOf("Advanced connection", "Settings"),
    val benefitChips: List<String> = listOf("LAN only", "Encrypted", "Files", "Calls"),
    val discoveryStatus: String = "Looking for nearby rooms…",
    val emptyNearbyTitle: String = "No nearby rooms yet",
    val emptyNearbyDetail: String = "Host a secure room or wait for trusted devices on this LAN.",
    val brandGlyph: String = "◈⌁",
) {
    val title: String = ComposeShellMetadata.APP_NAME
    val primaryActions: List<String> = listOf(hostActionLabel, joinActionLabel)
    val showsOnlyPrimaryConnectionChoices: Boolean = primaryActions == listOf("Host secure room", "Join nearby room")
    val hidesTechnicalDetails: Boolean = secondaryLinks.contains("Advanced connection")
    val guidanceSummary: String = if (body.isBlank()) headline else "$headline $body"
    val hasFullDesktopComposition: Boolean = true
    val primaryButtonWidthRange: IntRange = 180..240
    val secondaryButtonWidthRange: IntRange = 180..240
    val avoidsFirstRunTechnicalFields: Boolean = listOf("Your name", "Room password", "Hidden", "Ports").none { token ->
        guidanceSummary.contains(token, ignoreCase = true) || primaryActions.any {
            it.contains(
                token,
                ignoreCase = true
            )
        }
    }
}
