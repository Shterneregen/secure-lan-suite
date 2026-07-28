package com.shterneregen.securelan.desktop.compose.ui.peerlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListSectionPresentation
import com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListState

@Composable
internal fun PeerListGroup(
    section: ComposePeerListSectionPresentation,
    peerState: ComposePeerListState,
    onPeerSelected: (String?) -> Unit,
) {
    if (section.rows.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        PeerListSectionHeader(title = section.title, countLabel = section.countLabel)
        section.rows.forEach { row ->
            key(row.key) {
                val index = peerState.visiblePeers.indexOfFirst { it.nickname == row.peer.nickname }
                PeerPreviewRow(
                    row = row,
                    selected = index == peerState.resolvedSelectedPeerIndex,
                    onSelect = { onPeerSelected(row.peer.nickname) },
                )
            }
        }
    }
}
