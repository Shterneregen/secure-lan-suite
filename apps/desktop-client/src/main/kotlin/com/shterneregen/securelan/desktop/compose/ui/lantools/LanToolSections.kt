package com.shterneregen.securelan.desktop.compose.ui.lantools

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens

/** A bounded visual group; scrolling is owned by the containing pane. */
@Composable
internal fun LanToolCard(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val tokens = LocalSecureLanDesignTokens.current
    Card(modifier.fillMaxWidth(), elevation = tokens.elevation.flat,
        border = BorderStroke(tokens.border.subtle, tokens.colors.borderSubtle)) {
        Column(Modifier.padding(tokens.spacing.sm), verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs)) {
            Text(title, style = MaterialTheme.typography.subtitle1)
            content()
        }
    }
}

/** Keeps optional technical detail reachable by keyboard without dominating the default view. */
@Composable
internal fun LanDisclosure(title: String, content: @Composable ColumnScope.() -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(LocalSecureLanDesignTokens.current.spacing.xs)) {
        TextButton(onClick = { expanded = !expanded }, modifier = Modifier.semantics {
            stateDescription = if (expanded) "Expanded" else "Collapsed"
        }) { Text(title); Text(if (expanded) "  -" else "  +") }
        if (expanded) content()
    }
}
