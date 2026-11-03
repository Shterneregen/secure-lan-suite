package com.shterneregen.securelan.desktop.compose.ui.lantools

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens

/** A bounded visual group; scrolling is owned by the containing pane. */
@Composable
internal fun LanToolCard(title: String, modifier: Modifier = Modifier, icon: ImageVector? = null,
    headerAction: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit) {
    val tokens = LocalSecureLanDesignTokens.current
    Card(modifier.fillMaxWidth(), elevation = tokens.elevation.flat,
        backgroundColor = tokens.colors.surfaceLevel2,
        border = BorderStroke(tokens.border.subtle, tokens.colors.borderSubtle)) {
        Column(Modifier.padding(tokens.spacing.sm), verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs)) {
            if (title.isNotBlank()) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
                if (icon != null) Icon(icon, contentDescription = null, tint = tokens.colors.accent,
                    modifier = Modifier.size(22.dp))
                Text(title, style = MaterialTheme.typography.subtitle1)
                if (headerAction != null) {
                    Spacer(Modifier.weight(1f))
                    headerAction()
                }
            }
            content()
        }
    }
}

/** Keeps optional technical detail reachable by keyboard without dominating the default view. */
@Composable
internal fun LanDisclosure(title: String, icon: ImageVector? = null,
    content: @Composable ColumnScope.() -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(LocalSecureLanDesignTokens.current.spacing.xs)) {
        TextButton(onClick = { expanded = !expanded }, modifier = Modifier.semantics {
            stateDescription = if (expanded) "Expanded" else "Collapsed"
        }) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(LocalSecureLanDesignTokens.current.spacing.xs))
            }
            Text(title); Text(if (expanded) "  -" else "  +")
        }
        if (expanded) content()
    }
}
