package com.shterneregen.securelan.desktop.compose.util

import com.shterneregen.securelan.chat.discovery.DiscoveredPeer
import com.shterneregen.securelan.desktop.compose.ComposeDesktopHostAdapter
import com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionJoinTarget
import com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListItem

internal fun resolveSelectedJoinTarget(
    hostAdapter: ComposeDesktopHostAdapter,
    selectedPeer: ComposePeerListItem?,
): ComposeConnectionJoinTarget? = selectedPeer
    ?.takeIf { it.online }
    ?.let { peer -> hostAdapter.joinTargetFor(peer.nickname) }

internal fun resolveAttachCandidatePeer(
    selectedPeer: ComposePeerListItem?,
    resolvePeer: (String) -> DiscoveredPeer?,
): DiscoveredPeer? = selectedPeer
    ?.takeIf { it.online && it.fileCapable }
    ?.let { selected -> resolvePeer(selected.nickname) }
