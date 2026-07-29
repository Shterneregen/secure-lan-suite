package com.shterneregen.securelan.desktop.compose.ui.context

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButton
import com.shterneregen.securelan.desktop.compose.ui.icons.SecureLanIcons

@Composable
internal fun QuickShareActivitySummary(
    running: Boolean,
    activeLinkCount: Int,
    onManage: () -> Unit,
) {
    if (!running && activeLinkCount == 0) return

    val tokens = LocalSecureLanDesignTokens.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(tokens.radius.medium),
        border = BorderStroke(1.dp, tokens.colors.borderSubtle.copy(alpha = 0.72f)),
        color = tokens.colors.surfaceLevel2.copy(alpha = 0.52f),
    ) {
        Row(
            modifier = Modifier.padding(tokens.spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = SecureLanIcons.QuickShare,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = tokens.colors.success,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(tokens.spacing.xxs)) {
                Text("Quick Share active", style = MaterialTheme.typography.subtitle2)
                Text(
                    "$activeLinkCount active ${if (activeLinkCount == 1) "link" else "links"}",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
                )
            }
            CompactButton(onClick = onManage) {
                Text("Manage")
            }
        }
    }
}
