package com.shterneregen.securelan.desktop.compose.ui.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.motionTween
import com.shterneregen.securelan.desktop.compose.state.shell.ComposeWorkspaceState

@Composable
internal fun WorkspaceCenterColumn(
    workspaceState: ComposeWorkspaceState,
    connectionHubExpanded: Boolean,
    onConnectionHubToggle: () -> Unit,
    startupSurface: @Composable () -> Unit,
    chatSurface: @Composable () -> Unit,
    hubTooltip: String? = null,
) {
    val layoutContract = workspaceState.layoutContract
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val spacing = layoutContract.centerColumnSpacing
        val reservedForComposer = layoutContract.composerSafeVerticalSpace
        val minChatHeight = layoutContract.minChatSurfaceHeight
        val maxHubHeightByComposer = (maxHeight - reservedForComposer - spacing * 2f - minChatHeight)
            .coerceAtLeast(160.dp)
        val maxHubHeightByFraction = maxHeight * layoutContract.connectionHubExpandedMaxFraction
        val maxHubHeight = maxHubHeightByFraction.coerceAtMost(maxHubHeightByComposer)

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(spacing),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHubHeight),
            ) {
                CollapsibleConnectionHub(
                    expanded = connectionHubExpanded,
                    onToggle = onConnectionHubToggle,
                    workspaceState = workspaceState,
                    tooltip = hubTooltip,
                    expandedContent = startupSurface,
                )
            }
            AnimatedVisibility(
                visible = workspaceState.callBannerVisible,
                enter = fadeIn(motionTween()) + expandVertically(motionTween(), expandFrom = Alignment.Top),
                exit = shrinkVertically(motionTween(), shrinkTowards = Alignment.Top) + fadeOut(motionTween()),
            ) {
                CallBanner(workspaceState = workspaceState)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .heightIn(min = minChatHeight),
            ) {
                chatSurface()
            }
        }
    }
}
