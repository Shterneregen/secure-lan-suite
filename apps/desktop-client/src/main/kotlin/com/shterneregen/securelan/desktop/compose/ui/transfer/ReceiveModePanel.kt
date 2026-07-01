package com.shterneregen.securelan.desktop.compose.ui.transfer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.ComposeFileTransferState
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.ui.components.TitleWithHelp

@Composable
internal fun ReceiveModePanel(
    transferState: ComposeFileTransferState,
    autoAcceptFiles: Boolean,
    onAutoAcceptChanged: (Boolean) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LocalSecureLanDesignTokens.current.radius.medium),
        border = BorderStroke(1.dp, LocalSecureLanDesignTokens.current.colors.borderSubtle.copy(alpha = 0.72f)),
        color = if (autoAcceptFiles) MaterialTheme.colors.primary.copy(alpha = 0.08f) else LocalSecureLanDesignTokens.current.colors.surfaceLevel2.copy(
            alpha = 0.62f
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(autoAcceptFiles, onAutoAcceptChanged)
            TitleWithHelp(
                title = transferState.receiveModeLabel,
                tooltip = transferState.receiveModeDescription,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
