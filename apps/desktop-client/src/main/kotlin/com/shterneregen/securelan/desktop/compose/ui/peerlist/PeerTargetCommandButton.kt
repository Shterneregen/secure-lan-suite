package com.shterneregen.securelan.desktop.compose.ui.peerlist

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerTargetCommand
import com.shterneregen.securelan.desktop.compose.ui.components.CalmFocusButton

@Composable
internal fun PeerTargetCommandButton(
    command: ComposePeerTargetCommand,
    onCommand: (ComposePeerTargetCommand) -> Unit,
) {
    CalmFocusButton(
        onClick = { onCommand(command) },
        enabled = command.enabled,
        fillMaxWidth = true,
    ) {
        Text(command.displayLabel)
    }
}
