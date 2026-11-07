package com.shterneregen.securelan.desktop.compose.ui.lantools

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.ui.icons.SecureLanIcons

/** A bounded visual group; scrolling is owned by the containing pane. */
@Composable
internal fun LanToolCard(title: String, modifier: Modifier = Modifier, icon: ImageVector? = null,
    headerAction: @Composable (() -> Unit)? = null,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit) {
    val tokens = LocalSecureLanDesignTokens.current
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(tokens.radius.small), elevation = tokens.elevation.flat,
        backgroundColor = tokens.colors.surfaceLevel2,
        border = BorderStroke(tokens.border.subtle, tokens.colors.borderSubtle)) {
        Column(Modifier.padding(tokens.spacing.xs), verticalArrangement = Arrangement.spacedBy(tokens.spacing.xxs)) {
            if (title.isNotBlank()) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
                if (icon != null) Icon(icon, contentDescription = null, tint = tokens.colors.accent,
                    modifier = Modifier.size(22.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.h6)
                    subtitle?.let { Text(it, style = MaterialTheme.typography.caption) }
                }
                if (headerAction != null) {
                    headerAction()
                }
            }
            content()
        }
    }
}

/** A compact label/value row for technical details; values wrap without stretching the label column. */
@Composable
internal fun LanDetailField(label: String, value: String) {
    val tokens = LocalSecureLanDesignTokens.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
        Text(label, modifier = Modifier.width(112.dp), style = MaterialTheme.typography.caption,
            color = tokens.colors.textSecondary)
        Text(value, modifier = Modifier.weight(1f), style = MaterialTheme.typography.body2)
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
            Text(title)
            Spacer(Modifier.width(LocalSecureLanDesignTokens.current.spacing.xs))
            Icon(SecureLanIcons.ExpandMore, contentDescription = null,
                modifier = Modifier.size(16.dp).rotate(if (expanded) 180f else 0f))
        }
        if (expanded) content()
    }
}
