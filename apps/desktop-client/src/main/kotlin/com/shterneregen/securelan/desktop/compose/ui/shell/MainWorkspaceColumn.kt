package com.shterneregen.securelan.desktop.compose.ui.shell

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.LocalReducedMotion
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.motionTween
import com.shterneregen.securelan.desktop.compose.ui.components.TitleWithHelp

@Composable
internal fun MainWorkspaceColumn(
    title: String?,
    modifier: Modifier,
    tooltip: String? = null,
    headerActions: @Composable RowScope.() -> Unit = {},
    content: @Composable () -> Unit,
) {
    val tokens = LocalSecureLanDesignTokens.current
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(tokens.radius.large),
        border = BorderStroke(1.dp, tokens.colors.borderSubtle),
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.surface,
    ) {
        Column(
            modifier = Modifier.padding(tokens.spacing.md),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
        ) {
            if (title != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 30.dp),
                    horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val reduced = LocalReducedMotion.current
                    AnimatedContent(
                        targetState = title,
                        transitionSpec = {
                            fadeIn(motionTween(reduced)) + slideInVertically(motionTween(reduced)) { it / 4 } togetherWith
                                    fadeOut(motionTween(reduced)) + slideOutVertically(motionTween(reduced)) { it / 4 }
                        },
                        modifier = Modifier.weight(1f),
                        label = "WorkspaceColumnTitle",
                    ) { animatedTitle ->
                        if (tooltip == null) {
                            Text(animatedTitle, style = MaterialTheme.typography.subtitle2)
                        } else {
                            TitleWithHelp(
                                title = animatedTitle,
                                tooltip = tooltip,
                                titleStyle = MaterialTheme.typography.subtitle2,
                            )
                        }
                    }
                    headerActions()
                }
            }
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.TopStart) { content() }
        }
    }
}
