package com.shterneregen.securelan.desktop.compose.ui.lantools

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.state.lantools.LanToolsUiState
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButton
import com.shterneregen.securelan.desktop.lantools.DesktopLanToolsController

/** Scrollable single-column editor, shared across dialog sizes; validation belongs to its state owner. */
@Composable
internal fun LanDeviceEditor(state: LanToolsUiState, controller: DesktopLanToolsController, modifier: Modifier = Modifier) {
    val draft = state.editor ?: return
    val tokens = LocalSecureLanDesignTokens.current
    val nameFocus = remember { FocusRequester() }
    LaunchedEffect(draft.id) { nameFocus.requestFocus() }
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
        Text(if (state.devices.any { it.id == draft.id }) "Edit saved device" else "Add saved device", style = MaterialTheme.typography.h6)
        OutlinedTextField(draft.name, { controller.editDraft(draft.copy(name = it)) }, label = { Text("Device name") }, singleLine = true,
            enabled = !state.saving, modifier = Modifier.fillMaxWidth().focusRequester(nameFocus).testTag("lan-name"))
        OutlinedTextField(draft.address, { controller.editDraft(draft.copy(address = it)) }, label = { Text("IP address (optional)") }, singleLine = true,
            enabled = !state.saving, modifier = Modifier.fillMaxWidth().testTag("lan-address"))
        OutlinedTextField(draft.mac, { controller.editDraft(draft.copy(mac = it)) }, label = { Text("MAC address (optional)") }, singleLine = true,
            enabled = !state.saving, modifier = Modifier.fillMaxWidth().testTag("lan-mac"))
        Text("A valid MAC address is required for Wake-on-LAN. Leave it empty to save a device without Wake up.", style = MaterialTheme.typography.caption)
        OutlinedTextField(draft.broadcast, { controller.editDraft(draft.copy(broadcast = it)) }, label = { Text("Broadcast override (optional)") }, singleLine = true,
            enabled = !state.saving, modifier = Modifier.fillMaxWidth().testTag("lan-broadcast"))
        Text("Leave empty to use the selected subnet's broadcast. An override must match that subnet, or be 255.255.255.255 with an explicit interface.", style = MaterialTheme.typography.caption)
        OutlinedTextField(draft.port, { controller.editDraft(draft.copy(port = it)) }, label = { Text("UDP port") }, singleLine = true,
            enabled = !state.saving, modifier = Modifier.fillMaxWidth().testTag("lan-port"))
        OutlinedTextField(draft.preferredInterface, { controller.editDraft(draft.copy(preferredInterface = it)) },
            label = { Text("Preferred interface key (optional)") }, singleLine = true, enabled = !state.saving,
            modifier = Modifier.fillMaxWidth().testTag("lan-preferred-interface"))
        Text("Use the platform interface key, such as eth0. You can also choose a current interface in device details before sending.", style = MaterialTheme.typography.caption)
        state.editorError?.let { Text(it, color = MaterialTheme.colors.error) }
        Row {
            CompactButton(onClick = controller::saveEditor, enabled = state.canModify) { Text("Save device") }
            Spacer(Modifier.width(tokens.spacing.sm))
            TextButton(onClick = controller::cancelEditor, enabled = !state.saving) { Text("Cancel editing") }
        }
    }
}
