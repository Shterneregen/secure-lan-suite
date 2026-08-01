package com.shterneregen.securelan.desktop.compose.state.peer

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ComposePeerListItemTest {
    @Test
    fun shouldNotAdvertiseFileReceivingWhenDirectTransferIsUnavailable() {
        val peer = ComposePeerListItem(
            nickname = "Morpheus",
            online = true,
            discovered = true,
            listMeta = "Online",
            selectedMeta = "Online in chat",
            filePort = 5051,
            fileCapableOverride = false,
        )

        assertFalse(peer.fileCapable)
        assertFalse("File" in peer.capabilityLabels)
        assertTrue(peer.actionSummary.contains("needs a file receiver"))
    }
}
