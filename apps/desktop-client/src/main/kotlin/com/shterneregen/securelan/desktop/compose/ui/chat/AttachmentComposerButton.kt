package com.shterneregen.securelan.desktop.compose.ui.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.state.transfer.ComposeAttachmentToolsState
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButton
import com.shterneregen.securelan.desktop.compose.ui.icons.SecureLanIcons

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AttachmentComposerButton(
    tools: ComposeAttachmentToolsState,
    onClick: () -> Unit,
) {
    val tokens = LocalSecureLanDesignTokens.current
    TooltipArea(
        tooltip = {
            Surface(
                shape = RoundedCornerShape(tokens.radius.small),
                border = BorderStroke(tokens.border.subtle, tokens.colors.borderSubtle),
                color = MaterialTheme.colors.surface,
            ) {
                Text(
                    text = tools.statusText,
                    modifier = Modifier
                        .widthIn(max = 300.dp)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.82f),
                )
            }
        },
    ) {
        CompactButton(
            onClick = onClick,
            modifier = Modifier.semantics { contentDescription = tools.accessibilityLabel },
            enabled = tools.enabled,
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
    }
}
