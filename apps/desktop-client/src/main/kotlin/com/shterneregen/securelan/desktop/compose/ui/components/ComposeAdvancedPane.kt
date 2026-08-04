package com.shterneregen.securelan.desktop.compose.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.motionTween
import com.shterneregen.securelan.desktop.compose.state.shell.ComposeShellMetadata

@Composable
internal fun ComposeAdvancedPane(
    title: String,
    tooltip: String? = null,
    expanded: Boolean? = null,
    onExpandedChange: ((Boolean) -> Unit)? = null,
    bounded: Boolean = false,
    content: @Composable () -> Unit,
) {
    val tokens = LocalSecureLanDesignTokens.current
    var internalExpanded by remember(title) { mutableStateOf(false) }
    val effectiveExpanded = expanded ?: internalExpanded
    val setExpanded: (Boolean) -> Unit = { value ->
        onExpandedChange?.invoke(value)
        if (expanded == null) internalExpanded = value
    }
    Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { setExpanded(!effectiveExpanded) },
            shape = RoundedCornerShape(tokens.radius.medium),
            color = tokens.colors.surfaceLevel2,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = tokens.spacing.sm, vertical = tokens.spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (effectiveExpanded) "▾" else "▸",
                    style = MaterialTheme.typography.subtitle2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
                )
                if (tooltip != null) {
                    TitleWithHelp(
                        title = title,
                        tooltip = tooltip,
                        titleStyle = MaterialTheme.typography.subtitle2,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Text(title, style = MaterialTheme.typography.subtitle2, modifier = Modifier.weight(1f))
                }
            }
        }
        AnimatedVisibility(
            visible = effectiveExpanded,
            enter = fadeIn(motionTween()) + expandVertically(motionTween(), expandFrom = Alignment.Top),
            exit = shrinkVertically(motionTween(), shrinkTowards = Alignment.Top) + fadeOut(motionTween()),
        ) {
            val surfaceModifier = if (bounded) {
                Modifier.fillMaxWidth().heightIn(max = ComposeShellMetadata.ADVANCED_PANE_MAX_HEIGHT)
            } else {
                Modifier.fillMaxWidth()
            }
            val contentModifier = if (bounded) {
                Modifier.padding(tokens.spacing.sm).verticalScroll(rememberScrollState())
            } else {
                Modifier.padding(tokens.spacing.sm)
            }
            Surface(
                modifier = surfaceModifier,
                shape = RoundedCornerShape(tokens.radius.medium),
                color = tokens.colors.surfaceLevel1.copy(alpha = 0.72f),
            ) {
                Column(
                    modifier = contentModifier,
                    verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
                    content = { content() },
                )
            }
        }
    }
}
