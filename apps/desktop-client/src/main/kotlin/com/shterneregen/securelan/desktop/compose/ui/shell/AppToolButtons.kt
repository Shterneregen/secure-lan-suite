package com.shterneregen.securelan.desktop.compose.ui.shell

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButton
import com.shterneregen.securelan.desktop.compose.ui.icons.SecureLanIcons

@Composable
internal fun AppToolButtons(
    onOpenQuickShare: () -> Unit,
    onOpenSteganography: () -> Unit,
) {
    AppToolButton(
        label = "Quick Share",
        icon = SecureLanIcons.QuickShare,
        onClick = onOpenQuickShare,
    )
    AppToolButton(
        label = "Steganography",
        icon = SecureLanIcons.Steganography,
        onClick = onOpenSteganography,
    )
}

@Composable
private fun AppToolButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    CompactButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(label)
    }
}
