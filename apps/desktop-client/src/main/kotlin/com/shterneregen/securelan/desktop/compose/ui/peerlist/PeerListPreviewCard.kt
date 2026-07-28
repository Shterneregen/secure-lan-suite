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
internal fun PeerListPreviewCard(initialState: ComposePeerListState) {
    var selectedPeerIndex by remember { mutableStateOf(initialState.selectedPeerIndex) }
    val previewState = initialState.copy(selectedPeerIndex = selectedPeerIndex)

    val reduced = LocalReducedMotion.current
    val tokens = LocalSecureLanDesignTokens.current
    PeerListContentSurface(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = previewState.hasAnyPeers,
            transitionSpec = {
                fadeIn(motionTween(reduced)) togetherWith fadeOut(motionTween(reduced))
            },
            label = "PeerListContentPreview",
        ) { hasPeers ->
            if (!hasPeers) {
                PeerListEmptyState(previewState)
            } else {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(horizontal = tokens.spacing.xs, vertical = tokens.spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
                ) {
                    previewState.peerSections.forEach { section ->
                        PeerListGroup(
                            section = section,
                            peerState = previewState,
                            onPeerSelected = { nickname ->
                                selectedPeerIndex = previewState.visiblePeers.indexOfFirst { it.nickname == nickname }
                            },
                        )
                    }
                }
            }
        }
    }
}
