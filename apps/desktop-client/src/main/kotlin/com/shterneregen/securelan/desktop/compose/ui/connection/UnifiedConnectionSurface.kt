package com.shterneregen.securelan.desktop.compose.ui.connection

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.ComposeDesktopHostAdapter
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionHubMode
import com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionJoinTarget
import com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListItem
import com.shterneregen.securelan.desktop.compose.state.peer.toJoinTarget
import com.shterneregen.securelan.desktop.compose.state.shell.ComposeOnboardingState

/**
 * Single-screen welcome + connection surface.
 *
 * Replaces the three-step Welcome -> Host form / Join form -> Messenger flow with one
 * context-aware card: hero, nearby rooms, and a compact host/join hub.
 */
@Composable
internal fun UnifiedConnectionSurface(
    hostAdapter: ComposeDesktopHostAdapter,
    onboardingState: ComposeOnboardingState,
    nearbyPeers: List<ComposePeerListItem>,
    selectedJoinTarget: ComposeConnectionJoinTarget?,
    onRoomSelected: (ComposeConnectionJoinTarget?) -> Unit,
) {
    val tokens = LocalSecureLanDesignTokens.current
    val scrollState = rememberScrollState()

    var hubMode by remember { mutableStateOf(ComposeConnectionHubMode.HOST) }

    LaunchedEffect(selectedJoinTarget) {
        if (selectedJoinTarget != null) {
            hubMode = ComposeConnectionHubMode.JOIN
        }
    }

    val hostEnabled = !hostAdapter.statusState.clientConnected
    val joinEnabled = !hostAdapter.statusState.localServerRunning

    LaunchedEffect(hubMode, hostAdapter.statusState.localServerRunning, hostAdapter.statusState.clientConnected) {
        when (hubMode) {
            ComposeConnectionHubMode.HOST -> {
                if (!hostEnabled && joinEnabled) hubMode = ComposeConnectionHubMode.JOIN
            }
            ComposeConnectionHubMode.JOIN -> {
                if (!joinEnabled && hostEnabled) hubMode = ComposeConnectionHubMode.HOST
            }
        }
    }

    val nearbyTargets = remember(nearbyPeers) {
        nearbyPeers.filter { it.online && it.discovered }.mapNotNull { it.toJoinTarget() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = tokens.spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.md, Alignment.CenterVertically),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.xxs),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = onboardingState.brandGlyph,
                    style = MaterialTheme.typography.h5,
                    color = MaterialTheme.colors.primary,
                )
                Text(
                    text = onboardingState.headline,
                    style = MaterialTheme.typography.h6,
                    color = MaterialTheme.colors.onSurface,
                )
            }
            Text(
                text = onboardingState.body,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.68f),
            )
        }

        Column(
            modifier = Modifier.widthIn(max = 560.dp, min = 320.dp),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ConnectionModeSelector(
                mode = hubMode,
                hostLabel = "Host secure room",
                joinLabel = "Join nearby room",
                onModeChange = { hubMode = it },
                tooltip = "Choose whether to host a room on this computer or join one nearby.",
                modifier = Modifier.fillMaxWidth(),
                hostEnabled = hostEnabled,
                joinEnabled = joinEnabled,
            )

            ConnectionHub(
                hostAdapter = hostAdapter,
                initialMode = hubMode,
                selectedJoinTarget = selectedJoinTarget,
                wrapInCard = true,
            )

            if (nearbyTargets.isNotEmpty()) {
                Column(
                    modifier = Modifier.widthIn(max = 520.dp, min = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
                ) {
                    Text(
                        text = onboardingState.discoveryStatus,
                        style = MaterialTheme.typography.subtitle2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
                    )
                    nearbyTargets.take(5).forEach { target ->
                        CenterPanelRoomRow(
                            target = target,
                            selected = selectedJoinTarget?.nickname == target.nickname,
                            onSelected = { onRoomSelected(if (selectedJoinTarget?.nickname == target.nickname) null else target) },
                        )
                    }
                }
            }
        }
    }
}
