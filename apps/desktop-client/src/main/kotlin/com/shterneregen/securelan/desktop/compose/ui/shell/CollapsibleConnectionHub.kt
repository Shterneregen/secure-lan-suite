package com.shterneregen.securelan.desktop.compose.ui.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.motionTween
import com.shterneregen.securelan.desktop.compose.state.shell.ComposeWorkspaceState
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButton
import com.shterneregen.securelan.desktop.compose.ui.components.TitleWithHelp

@Composable
internal fun CollapsibleConnectionHub(
    expanded: Boolean,
    onToggle: () -> Unit,
    workspaceState: ComposeWorkspaceState,
    tooltip: String? = null,
    headerActions: @Composable RowScope.() -> Unit = {},
    expandedContent: @Composable () -> Unit,
) {
    val tokens = LocalSecureLanDesignTokens.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(tokens.radius.large),
        border = BorderStroke(1.dp, tokens.colors.borderSubtle),
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.surface,
    ) {
        Column(
            modifier = Modifier.padding(tokens.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(tokens.spacing.xxs)) {
                    if (tooltip == null) {
                        Text(
                            text = workspaceState.title,
                            style = MaterialTheme.typography.subtitle2,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    } else {
                        TitleWithHelp(
                            title = workspaceState.title,
                            tooltip = tooltip,
                            titleStyle = MaterialTheme.typography.subtitle2,
                        )
                    }
                    if (workspaceState.subtitle.isNotBlank()) {
                        Text(
                            text = workspaceState.subtitle,
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.62f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                headerActions()
                CompactButton(onClick = onToggle) {
                    Text(if (expanded) "Hide" else "Show")
                }
            }
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(motionTween()) + expandVertically(motionTween(), expandFrom = Alignment.Top),
                exit = shrinkVertically(motionTween(), shrinkTowards = Alignment.Top) + fadeOut(motionTween()),
            ) {
                val hubScrollState = rememberScrollState()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(hubScrollState),
                    contentAlignment = Alignment.TopStart,
                ) {
                    expandedContent()
                }
            }
        }
    }
}
