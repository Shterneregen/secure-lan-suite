package com.shterneregen.securelan.desktop.compose.ui.lantools

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.state.lantools.LanDeviceDraft
import com.shterneregen.securelan.desktop.compose.state.lantools.LanToolsUiState
import com.shterneregen.securelan.desktop.compose.state.lantools.MacLookupStatus
import com.shterneregen.securelan.desktop.compose.ui.components.CompactTextField
import com.shterneregen.securelan.desktop.compose.ui.icons.SecureLanIcons
import com.shterneregen.securelan.desktop.lantools.DesktopLanToolsController

/** Responsive editor with one scroll owner. The controller retains input, validation and persistence. */
@Composable
internal fun LanDeviceEditor(state: LanToolsUiState, controller: DesktopLanToolsController, modifier: Modifier = Modifier) {
    val draft = state.editor ?: return
    val tokens = LocalSecureLanDesignTokens.current
    val nameFocus = remember { FocusRequester() }
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val paired = maxWidth >= 680.dp
        val sideNote = maxWidth >= 900.dp
        Surface(Modifier.fillMaxSize(), shape = RoundedCornerShape(tokens.radius.medium),
            color = tokens.colors.surfaceLevel1, border = BorderStroke(tokens.border.subtle, tokens.colors.borderSubtle)) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(tokens.spacing.md),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.md)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
                    Icon(SecureLanIcons.DeviceAdd, contentDescription = null, tint = tokens.colors.accent, modifier = Modifier.size(40.dp))
                    Column {
                        Text(if (state.devices.any { it.id == draft.id }) "Edit saved device" else "Add saved device", style = MaterialTheme.typography.h5)
                        Text("Save a device for quick access and optional Wake-on-LAN.", style = MaterialTheme.typography.body2,
                            color = tokens.colors.textSecondary)
                    }
                }
                if (sideNote) Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.md)) {
                    EditorBasics(draft, state, controller, nameFocus, paired, Modifier.weight(2.9f))
                    MacRequirement(Modifier.weight(1f))
                } else {
                    EditorBasics(draft, state, controller, nameFocus, paired)
                    MacRequirement()
                }
                EditorSection("Wake-on-LAN", "Optional settings for waking the device over the network.", SecureLanIcons.Wake) {
                    if (paired) Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.md)) {
                        EditorMac(draft, state, controller, Modifier.weight(1f))
                        EditorBroadcast(draft, state, controller, Modifier.weight(1f))
                    } else {
                        EditorMac(draft, state, controller)
                        EditorBroadcast(draft, state, controller)
                    }
                    if (paired) Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.md)) {
                        EditorPort(draft, state, controller, Modifier.width(160.dp))
                        EditorPreferredInterface(draft, state, controller, Modifier.weight(1f))
                    } else {
                        EditorPort(draft, state, controller)
                        EditorPreferredInterface(draft, state, controller)
                    }
                }
                state.editorError?.let { Text(it, color = tokens.colors.error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
                Divider()
                if (paired) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
                    verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = controller::saveEditor, enabled = state.canModify) {
                        Icon(SecureLanIcons.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(tokens.spacing.xs)); Text("Save device")
                    }
                    OutlinedButton(onClick = controller::cancelEditor, enabled = !state.saving) { Text("Cancel editing") }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = controller::cancelEditor, enabled = !state.saving) {
                        Icon(SecureLanIcons.NavigateNext, contentDescription = null, modifier = Modifier.size(18.dp).rotate(180f))
                        Spacer(Modifier.width(tokens.spacing.xs)); Text("Back to devices")
                    }
                } else FlowRow(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs)) {
                    Button(onClick = controller::saveEditor, enabled = state.canModify) {
                        Icon(SecureLanIcons.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(tokens.spacing.xs)); Text("Save device")
                    }
                    OutlinedButton(onClick = controller::cancelEditor, enabled = !state.saving) { Text("Cancel editing") }
                    TextButton(onClick = controller::cancelEditor, enabled = !state.saving) {
                        Icon(SecureLanIcons.NavigateNext, contentDescription = null, modifier = Modifier.size(18.dp).rotate(180f))
                        Spacer(Modifier.width(tokens.spacing.xs)); Text("Back to devices")
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorBasics(draft: LanDeviceDraft, state: LanToolsUiState, controller: DesktopLanToolsController,
    focus: FocusRequester, paired: Boolean, modifier: Modifier = Modifier) {
    val spacing = LocalSecureLanDesignTokens.current.spacing
    EditorSection("Basics", "Essential details to identify the device", SecureLanIcons.Layers, modifier) {
        if (paired) Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
            EditorField(draft.name, { controller.editDraft(draft.copy(name = it)) }, "Device name *", "lan-name", !state.saving,
                Modifier.weight(1.35f), focus = focus)
            EditorField(draft.address, { controller.editDraft(draft.copy(address = it)) }, "IP address (optional)", "lan-address", !state.saving, Modifier.weight(1f))
        } else {
            EditorField(draft.name, { controller.editDraft(draft.copy(name = it)) }, "Device name *", "lan-name", !state.saving,
                focus = focus)
            EditorField(draft.address, { controller.editDraft(draft.copy(address = it)) }, "IP address (optional)", "lan-address", !state.saving)
        }
    }
}

@Composable
private fun MacRequirement(modifier: Modifier = Modifier) {
    val tokens = LocalSecureLanDesignTokens.current
    Surface(modifier, shape = RoundedCornerShape(tokens.radius.medium), color = tokens.colors.surfaceLevel2,
        border = BorderStroke(tokens.border.subtle, tokens.colors.accent.copy(alpha = 0.5f))) {
        Row(Modifier.padding(tokens.spacing.sm), horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
            Icon(SecureLanIcons.Information, contentDescription = null, tint = tokens.colors.accent, modifier = Modifier.size(22.dp))
            Text("A valid MAC address is required for Wake-on-LAN. Leave it empty to save a device without Wake up.",
                style = MaterialTheme.typography.body2, fontStyle = FontStyle.Italic)
        }
    }
}

@Composable
private fun EditorMac(draft: LanDeviceDraft, state: LanToolsUiState, controller: DesktopLanToolsController, modifier: Modifier = Modifier) {
    val tokens = LocalSecureLanDesignTokens.current
    val running = state.macLookup is MacLookupStatus.Running
    Column(modifier, verticalArrangement = Arrangement.spacedBy(tokens.spacing.xxs)) {
        EditorField(draft.mac, { controller.editDraft(draft.copy(mac = it)) }, "MAC address (optional)", "lan-mac", !state.saving,
            trailingIcon = {
                IconButton(onClick = controller::resolveMac, enabled = state.canLookupMac,
                    modifier = Modifier.size(24.dp).testTag("lan-mac-lookup").semantics {
                        contentDescription = "Look up MAC address from IP"
                    }) {
                    if (running) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(SecureLanIcons.Search, contentDescription = null,
                            modifier = Modifier.size(18.dp))
                    }
                }
            })
        val status = state.macLookup
        val caption = when (status) {
            MacLookupStatus.Idle -> "Device MAC, e.g. 02:11:22:33:44:55. Required for Wake-on-LAN."
            MacLookupStatus.Running -> "Looking up the MAC address…"
            is MacLookupStatus.Found -> "MAC from the local neighbor cache. Verify it before saving; it may be stale or randomized."
            is MacLookupStatus.Unavailable -> status.message
        }
        Text(caption, style = MaterialTheme.typography.caption,
            color = if (status is MacLookupStatus.Unavailable) tokens.colors.error else tokens.colors.textSecondary,
            modifier = if (status != MacLookupStatus.Idle) {
                Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            } else Modifier)
    }
}

@Composable
private fun EditorBroadcast(draft: LanDeviceDraft, state: LanToolsUiState, controller: DesktopLanToolsController, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(LocalSecureLanDesignTokens.current.spacing.xxs)) {
        EditorField(draft.broadcast, { controller.editDraft(draft.copy(broadcast = it)) }, "Broadcast override (optional)", "lan-broadcast", !state.saving)
        Text("Leave empty to use the selected subnet's broadcast. An override must match that subnet, or be 255.255.255.255 with an explicit interface.",
            style = MaterialTheme.typography.caption)
    }
}

@Composable
private fun EditorPort(draft: LanDeviceDraft, state: LanToolsUiState, controller: DesktopLanToolsController, modifier: Modifier = Modifier) {
    EditorField(draft.port, { controller.editDraft(draft.copy(port = it)) }, "UDP port", "lan-port", !state.saving, modifier)
}

/** The picker copies an interface key only. Typed or unavailable saved preferences remain editable. */
@Composable
private fun EditorPreferredInterface(draft: LanDeviceDraft, state: LanToolsUiState, controller: DesktopLanToolsController,
    modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(LocalSecureLanDesignTokens.current.spacing.xxs)) {
        Box {
            EditorField(draft.preferredInterface, { controller.editDraft(draft.copy(preferredInterface = it)) },
                "Network interface (optional)", "lan-preferred-interface", !state.saving,
                placeholder = "Choose an interface or enter its key…",
                trailingIcon = {
                    IconButton(onClick = { expanded = true }, enabled = !state.saving, modifier = Modifier.size(24.dp).testTag("lan-preferred-picker")) {
                        Icon(SecureLanIcons.ExpandMore, contentDescription = "Choose network interface", modifier = Modifier.size(18.dp))
                    }
                })
            DropdownMenu(expanded, { expanded = false }) {
                DropdownMenuItem(onClick = { controller.editDraft(draft.copy(preferredInterface = "")); expanded = false }) { Text("No saved interface") }
                state.networks.interfaces.distinctBy { it.binding.interfaceKey }.forEach { entry ->
                    DropdownMenuItem(onClick = { controller.editDraft(draft.copy(preferredInterface = entry.binding.interfaceKey)); expanded = false }) {
                        Text("${entry.displayName} (${entry.binding.interfaceKey})")
                    }
                }
            }
        }
        Text("Used for MAC lookup and Wake up. Leave empty to use the only matching interface; choose one when this IP exists on more than one network.",
            style = MaterialTheme.typography.caption)
    }
}

/** A visible field label and semantic name stay available when the field has a value. */
@Composable
private fun EditorField(value: String, change: (String) -> Unit, label: String, tag: String, enabled: Boolean,
    modifier: Modifier = Modifier, trailingIcon: @Composable (() -> Unit)? = null, focus: FocusRequester? = null,
    placeholder: String? = null) {
    if (focus != null) LaunchedEffect(focus) { focus.requestFocus() }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(LocalSecureLanDesignTokens.current.spacing.xxs)) {
        Text(label, style = MaterialTheme.typography.subtitle2)
        CompactTextField(value, change, outlined = true, label = "", enabled = enabled,
            placeholder = placeholder, trailingContent = trailingIcon,
            modifier = Modifier.fillMaxWidth(),
            inputModifier = Modifier.then(if (focus != null) Modifier.focusRequester(focus) else Modifier).testTag(tag).semantics {
                contentDescription = if (tag == "lan-name") "Device name, required" else label
            })
    }
}

/** Section headers stay inline at desktop widths, while fields retain their own labelled groups. */
@Composable
private fun EditorSection(title: String, subtitle: String, icon: ImageVector, modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit) {
    val tokens = LocalSecureLanDesignTokens.current
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(tokens.radius.medium),
        elevation = tokens.elevation.flat, backgroundColor = tokens.colors.surfaceLevel1,
        border = BorderStroke(tokens.border.subtle, tokens.colors.borderSubtle)) {
        Column {
            Surface(color = tokens.colors.surfaceLevel2) {
                FlowRow(Modifier.fillMaxWidth().padding(horizontal = tokens.spacing.md, vertical = tokens.spacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(tokens.spacing.lg), verticalArrangement = Arrangement.spacedBy(tokens.spacing.xxs)) {
                    Row(Modifier.align(Alignment.CenterVertically), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
                        Icon(icon, contentDescription = null, tint = tokens.colors.accent, modifier = Modifier.size(22.dp))
                        Text(title, style = MaterialTheme.typography.h6)
                    }
                    Text(subtitle, modifier = Modifier.align(Alignment.CenterVertically), style = MaterialTheme.typography.body2, color = tokens.colors.textSecondary)
                }
            }
            Divider()
            Column(Modifier.padding(tokens.spacing.md), verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm), content = content)
        }
    }
}
