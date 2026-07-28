package com.shterneregen.securelan.desktop.compose.ui.quickshare

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.state.quickshare.ComposeQuickShareState
import com.shterneregen.securelan.desktop.compose.ui.icons.SecureLanIcons

@Composable
internal fun QuickShareHeader(state: ComposeQuickShareState) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = SecureLanIcons.QuickShare,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colors.primary,
        )
        Text(state.title, style = MaterialTheme.typography.subtitle1)
        Spacer(Modifier.weight(1f))
        QuickShareStatusPill(state.statusText, active = state.running)
    }
}
