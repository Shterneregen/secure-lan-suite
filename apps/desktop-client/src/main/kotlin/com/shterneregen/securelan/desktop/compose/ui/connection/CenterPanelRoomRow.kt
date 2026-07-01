package com.shterneregen.securelan.desktop.compose.ui.connection

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.state.connection.ComposeConnectionJoinTarget
import com.shterneregen.securelan.desktop.compose.util.calmFocusRing
import com.shterneregen.securelan.desktop.compose.util.interactiveSurfaceBorder
import com.shterneregen.securelan.desktop.compose.util.rememberInteractiveSurfaceState

@Composable
internal fun CenterPanelRoomRow(
    target: ComposeConnectionJoinTarget,
    selected: Boolean,
    onSelected: () -> Unit,
) {
    val tokens = LocalSecureLanDesignTokens.current
    val (interactionSource, interactive) = rememberInteractiveSurfaceState(selected = selected)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .calmFocusRing(interactive.focused, tokens.radius.medium)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onSelected,
            ),
        shape = RoundedCornerShape(tokens.radius.medium),
        border = interactiveSurfaceBorder(interactive),
        color = interactive.backgroundColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = tokens.spacing.sm, vertical = tokens.spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(tokens.spacing.xxs)) {
                Text(target.nickname, style = MaterialTheme.typography.button)
                Text(
                    text = target.endpointLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.62f),
                )
            }
            if (selected) {
                Text(
                    text = "Selected",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.primary,
                )
            }
        }
    }
}
