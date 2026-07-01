package com.shterneregen.securelan.desktop.compose.ui.connection

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.shterneregen.securelan.desktop.compose.ui.shell.CallBanner
import com.shterneregen.securelan.desktop.compose.motionTween
import com.shterneregen.securelan.desktop.compose.state.shell.ComposeWorkspaceState

@Composable
internal fun CenterPanelConnected(
    workspaceState: ComposeWorkspaceState,
    chatSurface: @Composable () -> Unit,
) {
    val layoutContract = workspaceState.layoutContract
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(layoutContract.centerColumnSpacing),
    ) {
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
                .heightIn(min = layoutContract.minChatSurfaceHeight),
        ) {
            chatSurface()
        }
    }
}
