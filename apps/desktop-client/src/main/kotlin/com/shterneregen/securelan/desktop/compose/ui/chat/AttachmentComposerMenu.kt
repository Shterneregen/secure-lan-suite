package com.shterneregen.securelan.desktop.compose.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.DropdownMenu
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.state.transfer.ComposeAttachmentToolItem
import com.shterneregen.securelan.desktop.compose.state.transfer.ComposeAttachmentToolsState
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButton
import com.shterneregen.securelan.desktop.compose.ui.icons.SecureLanIcons

@Composable
internal fun AttachmentComposerMenu(
    tools: ComposeAttachmentToolsState,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
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
                tools.menuItems.forEach { item ->
                    AttachmentComposerMenuItem(
                        item = item,
                        icon = SecureLanIcons.File,
                        onSelected = { onItemSelected(item) },
                    )
                }
            }
        }
    }
}
