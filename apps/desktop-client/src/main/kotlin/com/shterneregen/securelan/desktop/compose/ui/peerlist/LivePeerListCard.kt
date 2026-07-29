package com.shterneregen.securelan.desktop.compose.ui.peerlist

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.shterneregen.securelan.desktop.compose.LocalReducedMotion
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.motionTween
import com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListState
import com.shterneregen.securelan.desktop.compose.ui.components.PeerListContentSurface

@Composable
internal fun LivePeerListCard(
    peerState: ComposePeerListState,
    activeCallPeer: String? = null,
    onPeerSelected: (String?) -> Unit,
) {
    val reduced = LocalReducedMotion.current
    val tokens = LocalSecureLanDesignTokens.current
    PeerListContentSurface(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = peerState.hasAnyPeers,
            transitionSpec = {
                fadeIn(motionTween(reduced)) togetherWith fadeOut(motionTween(reduced))
            },
            label = "PeerListContent",
        ) { hasPeers ->
            if (!hasPeers) {
                PeerListEmptyState(peerState)
            } else {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(horizontal = tokens.spacing.xs, vertical = tokens.spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
                ) {
                    peerState.peerSections.forEach { section ->
                        PeerListGroup(
                            section = section,
                            peerState = peerState,
                            activeCallPeer = activeCallPeer,
                            onPeerSelected = onPeerSelected,
                        )
                    }
                }
            }
        }
    }
}
