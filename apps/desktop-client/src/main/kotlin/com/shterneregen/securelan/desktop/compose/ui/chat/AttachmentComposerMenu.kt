package com.shterneregen.securelan.desktop.compose.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.state.transfer.ComposeAttachmentToolItem
import com.shterneregen.securelan.desktop.compose.state.transfer.ComposeAttachmentToolKind
import com.shterneregen.securelan.desktop.compose.state.transfer.ComposeAttachmentToolsState
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButton
import com.shterneregen.securelan.desktop.compose.ui.icons.SecureLanIcons

@Composable
internal fun AttachmentComposerMenu(
    tools: ComposeAttachmentToolsState,
    expanded: Boolean,
    statusText: String,
    onExpandedChange: (Boolean) -> Unit,
    onStatusTextChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onItemSelected: (ComposeAttachmentToolItem) -> Unit,
    attachButtonFocusRequester: FocusRequester,
) {
    val tokens = LocalSecureLanDesignTokens.current
    Box {
        CompactButton(
            onClick = { onExpandedChange(true) },
            modifier = Modifier
                .focusRequester(attachButtonFocusRequester)
                .semantics { contentDescription = "Open attachment menu" },
            enabled = tools.menuItems.isNotEmpty(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = SecureLanIcons.Attach,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(tools.title)
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismiss,
            offset = DpOffset(x = 0.dp, y = (-8).dp),
            modifier = Modifier
                .widthIn(
                    min = tools.layoutContract.minWidth,
                    max = tools.layoutContract.maxWidth,
                )
                .heightIn(max = tools.layoutContract.maxHeight)
                .semantics { contentDescription = "Attachment actions" },
        ) {
            Column(
                modifier = Modifier.padding(horizontal = tokens.spacing.xs, vertical = tokens.spacing.xs),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.xxs),
            ) {
                Text(
                    text = "Add to this conversation",
                    modifier = Modifier
                        .padding(horizontal = tokens.spacing.xs, vertical = tokens.spacing.xxs)
                        .semantics { heading() },
                    style = MaterialTheme.typography.caption,
                    color = tokens.colors.textTertiary,
                )
                val groups = remember(tools.menuItems) { buildAttachmentMenuGroups(tools.menuItems) }
                groups.forEachIndexed { index, group ->
                    if (index > 0) {
                        Divider(
                            color = tokens.colors.borderSubtle,
                            thickness = tokens.border.subtle,
                            modifier = Modifier.padding(vertical = tokens.spacing.xxs),
                        )
                    }
                    Text(
                        text = group.title,
                        modifier = Modifier
                            .padding(horizontal = tokens.spacing.xs, vertical = tokens.spacing.xxs)
                            .semantics { heading() },
                        style = MaterialTheme.typography.caption,
                        color = tokens.colors.textTertiary,
                    )
                    group.items.forEach { item ->
                        AttachmentComposerMenuItem(
                            item = item,
                            icon = attachmentMenuIcon(item.kind),
                            onStatusTextChange = onStatusTextChange,
                            onSelected = { onItemSelected(item) },
                        )
                    }
                }
                AttachmentComposerStatus(statusText)
            }
        }
    }
}

private data class AttachmentMenuGroup(
    val title: String,
    val items: List<ComposeAttachmentToolItem>,
)

private fun buildAttachmentMenuGroups(items: List<ComposeAttachmentToolItem>): List<AttachmentMenuGroup> {
    val byKind = items.associateBy { it.kind }
    return listOf(
        AttachmentMenuGroup(
            title = "Send & share",
            items = listOf(
                ComposeAttachmentToolKind.SECURE_FILE,
                ComposeAttachmentToolKind.QUICK_SHARE,
            ).mapNotNull(byKind::get),
        ),
        AttachmentMenuGroup(
            title = "Privacy tools",
            items = listOf(
                ComposeAttachmentToolKind.ENCRYPTED_TEXT_OR_FILE,
                ComposeAttachmentToolKind.STEGANOGRAPHY,
            ).mapNotNull(byKind::get),
        ),
    ).filter { it.items.isNotEmpty() }
}

private fun attachmentMenuIcon(kind: ComposeAttachmentToolKind): ImageVector = when (kind) {
    ComposeAttachmentToolKind.SECURE_FILE -> SecureLanIcons.File
    ComposeAttachmentToolKind.QUICK_SHARE -> SecureLanIcons.QuickShare
    ComposeAttachmentToolKind.ENCRYPTED_TEXT_OR_FILE -> SecureLanIcons.EncryptedTextOrFile
    ComposeAttachmentToolKind.STEGANOGRAPHY -> SecureLanIcons.Steganography
}
