package com.shterneregen.securelan.desktop.compose.state.peer

public data class ComposePeerTargetActions(
    val chatReady: Boolean,
    val fileReady: Boolean,
    val voiceReady: Boolean,
    val videoReady: Boolean,
    val dataChannelReady: Boolean,
    val blockedReasons: List<String>,
) {
    val title: String = "Peer target action readiness"
    val chatLabel: String = if (chatReady) "Chat target ready" else "Chat target blocked"
    val fileLabel: String = if (fileReady) "File transfer target ready" else "File transfer target blocked"
    val voiceLabel: String = if (voiceReady) "Voice target ready" else "Voice target blocked"
    val videoLabel: String = if (videoReady) "Experimental video target ready" else "Experimental video target blocked"
    val dataChannelLabel: String = if (dataChannelReady) "Real-time data target ready" else "Real-time data target blocked"
    val summary: String = listOf(chatLabel, fileLabel, voiceLabel, videoLabel, dataChannelLabel).joinToString(" · ")
    val blockedSummary: String = if (blockedReasons.isEmpty()) {
        "All selected-peer actions are ready."
    } else {
        blockedReasons.joinToString(" · ")
    }

    companion object {
        fun from(peer: ComposePeerListItem?): ComposePeerTargetActions {
            val online = peer?.online == true
            val fileCapable = peer?.fileCapable == true
            val voiceCapable = peer?.voiceCapable == true
            val videoCapable = peer?.videoCapable == true
            val dataChannelCapable = peer?.dataChannelCapable == true
            return ComposePeerTargetActions(
                chatReady = online,
                fileReady = online && fileCapable,
                voiceReady = online && voiceCapable,
                videoReady = online && videoCapable,
                dataChannelReady = online && dataChannelCapable,
                blockedReasons = blockedReasonsFor(peer, online, fileCapable, voiceCapable, videoCapable, dataChannelCapable),
            )
        }

        private fun blockedReasonsFor(
            peer: ComposePeerListItem?,
            online: Boolean,
            fileCapable: Boolean,
            voiceCapable: Boolean,
            videoCapable: Boolean,
            dataChannelCapable: Boolean,
        ): List<String> = buildList {
            if (peer == null) {
                add("Select an online peer before sending messages, files, or starting calls.")
                return@buildList
            }
            if (!online) {
                add("Selected peer is offline; wait for presence or discovery refresh.")
            }
            if (online && !fileCapable) {
                add("Encrypted file transfer is blocked until the peer has an available file receiver endpoint.")
            }
            if (online && (!voiceCapable || !videoCapable)) {
                add("Voice and video are not available for this peer.")
            }
            if (online && !dataChannelCapable) {
                add("Real-time data is not available for this peer.")
            }
        }
    }
}
