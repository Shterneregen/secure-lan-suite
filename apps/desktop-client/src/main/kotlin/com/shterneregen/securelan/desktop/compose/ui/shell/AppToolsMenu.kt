package com.shterneregen.securelan.desktop.compose.ui.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.ui.components.CompactIconButton
import com.shterneregen.securelan.desktop.compose.ui.icons.SecureLanIcons

@Composable
internal fun AppToolsMenu(
    onOpenQuickShare: () -> Unit,
    onOpenSteganography: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        CompactIconButton(
            onClick = { expanded = !expanded },
            icon = SecureLanIcons.Commands,
            contentDescription = "Open command menu",
            modifier = Modifier.size(32.dp),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .widthIn(min = 300.dp, max = 360.dp)
                .semantics { contentDescription = "Application tools" },
        ) {
            AppToolsMenuItem(
                label = "Quick Share",
                description = "Create a temporary trusted-LAN browser link.",
                icon = SecureLanIcons.QuickShare,
                onClick = {
                    expanded = false
                    onOpenQuickShare()
                },
            )
            AppToolsMenuItem(
                label = "Steganography",
                description = "Hide a message in an image or extract one from a stego BMP.",
                icon = SecureLanIcons.Steganography,
                onClick = {
                    expanded = false
                    onOpenSteganography()
                },
            )
        }
    }
}

@Composable
private fun AppToolsMenuItem(
    label: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    val tokens = LocalSecureLanDesignTokens.current
    DropdownMenuItem(
        onClick = onClick,
        modifier = Modifier
            .heightIn(min = 64.dp)
            .semantics { contentDescription = "$label. $description" },
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = tokens.colors.textSecondary,
        )
        Spacer(Modifier.width(tokens.spacing.xs))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.body1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.caption,
                color = tokens.colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
