package com.shterneregen.securelan.desktop.compose.ui.shell

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.motionTween
import com.shterneregen.securelan.desktop.compose.state.shell.ComposeContextPanelResponsiveState
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButton

@Composable
internal fun ContextAssistantDrawer(
    visible: Boolean,
    responsiveState: ComposeContextPanelResponsiveState,
    onClose: () -> Unit,
    content: @Composable () -> Unit,
) {
    val tokens = LocalSecureLanDesignTokens.current
    val motion = motionTween<IntOffset>()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.key == Key.Escape && event.type == KeyEventType.KeyUp) {
                    onClose()
                    true
                } else {
                    false
                }
            },
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(motionTween()),
            exit = fadeOut(motionTween()),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.28f))
                    .clickable(onClick = onClose),
            )
        }
        AnimatedVisibility(
            visible = visible,
            enter = slideInHorizontally(motion) { it } + fadeIn(motionTween()),
            exit = slideOutHorizontally(motion) { it } + fadeOut(motionTween()),
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(min = 320.dp, max = 380.dp)
                    .semantics { contentDescription = responsiveState.drawerContentDescription },
                shape = RoundedCornerShape(topStart = tokens.radius.large, bottomStart = tokens.radius.large),
                border = BorderStroke(1.dp, tokens.colors.borderSubtle),
                color = MaterialTheme.colors.surface,
                elevation = 8.dp,
            ) {
                Column(
                    modifier = Modifier.padding(tokens.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Context Assistant", style = MaterialTheme.typography.h6, modifier = Modifier.weight(1f))
                        CompactButton(
                            onClick = onClose,
                            modifier = Modifier.semantics {
                                contentDescription = responsiveState.drawerCloseContentDescription
                            },
                        ) { Text("Close") }
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.TopStart,
                    ) {
                        content()
                    }
                }
            }
        }
    }
}
