package com.shterneregen.securelan.desktop.compose.state.peer

public enum class ComposePeerAvailabilityKind {
    ONLINE,
    OFFLINE,
}

public data class ComposePeerCapabilityPresentation(
    val label: String,
    val accessibilityLabel: String,
)

public data class ComposePeerListItemPresentation(
    val peer: ComposePeerListItem,
    val key: String,
    val title: String,
    val availability: ComposePeerAvailabilityKind,
    val statusLabel: String,
    val statusAssistiveLabel: String,
    val secondaryText: String,
    val capabilityChips: List<ComposePeerCapabilityPresentation>,
    val capabilitySummary: String,
    val capabilityAssistiveLabel: String,
    val accessibilityLabel: String,
) {
    val hasCapabilityChips: Boolean = capabilityChips.isNotEmpty()

    companion object {
        fun from(peer: ComposePeerListItem): ComposePeerListItemPresentation {
            val availability = if (peer.online) ComposePeerAvailabilityKind.ONLINE else ComposePeerAvailabilityKind.OFFLINE
            val statusLabel = when (availability) {
                ComposePeerAvailabilityKind.ONLINE -> "Online"
                ComposePeerAvailabilityKind.OFFLINE -> "Offline"
            }
            val statusAssistiveLabel = when (availability) {
                ComposePeerAvailabilityKind.ONLINE -> "Online and available now"
                ComposePeerAvailabilityKind.OFFLINE -> "Offline; actions wait until this person returns"
            }
            val capabilities = buildList {
                if (peer.voiceCapable) add(ComposePeerCapabilityPresentation("Voice", "supports voice calls"))
                if (peer.videoCapable) add(ComposePeerCapabilityPresentation("Video", "supports video calls"))
                if (peer.fileCapabilityAdvertised) add(ComposePeerCapabilityPresentation("File", "supports encrypted file transfer"))
            }
            val capabilitySummary = if (capabilities.isEmpty()) {
                "Chat only"
            } else {
                capabilities.joinToString(" · ") { it.label }
            }
            val capabilityAssistiveLabel = if (capabilities.isEmpty()) {
                "No voice, video, or file capability advertised; chat only when online"
            } else {
                "Supports ${capabilities.joinToString(", ") { it.label.lowercase() }}"
            }
            val secondaryText = when {
                peer.online && peer.discovered -> "Nearby secure room member"
                peer.online -> ""
                peer.discovered -> "Previously seen nearby"
                else -> "Not currently reachable"
            }
            return ComposePeerListItemPresentation(
                peer = peer,
                key = peer.nickname.lowercase(),
                title = peer.nickname,
                availability = availability,
                statusLabel = statusLabel,
                statusAssistiveLabel = statusAssistiveLabel,
                secondaryText = secondaryText,
                capabilityChips = capabilities,
                capabilitySummary = capabilitySummary,
                capabilityAssistiveLabel = capabilityAssistiveLabel,
                accessibilityLabel = "${peer.nickname}, $statusAssistiveLabel, $capabilityAssistiveLabel",
            )
        }
    }
}

public data class ComposePeerListSectionPresentation(
    val key: String,
    val title: String,
    val availability: ComposePeerAvailabilityKind,
    val rows: List<ComposePeerListItemPresentation>,
) {
    val count: Int = rows.size
    val countLabel: String = "$count ${if (count == 1) "person" else "people"}"
    val accessibilityLabel: String = "$title peers, $countLabel"
}

