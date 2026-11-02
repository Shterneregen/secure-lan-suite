package com.shterneregen.securelan.desktop.compose.ui.lantools

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.state.lantools.LanToolsTab
import com.shterneregen.securelan.desktop.lantools.DesktopLanToolsController

/** Resizable modal utility; the underlying messenger remains composed and its services keep running. */
@Composable
internal fun LiveLanToolsDialog(controller: DesktopLanToolsController, onClose: () -> Unit) {
    val density = LocalDensity.current
    val available = LocalWindowInfo.current.containerSize
    val size = with(density) { DpSize(minOf(1000.dp, (available.width.toDp() - 48.dp).coerceAtLeast(400.dp)),
        minOf(760.dp, (available.height.toDp() - 48.dp).coerceAtLeast(320.dp))) }
    DialogWindow(
        onCloseRequest = onClose, title = "SecureLanSuite · LAN Tools", resizable = true,
        state = rememberDialogState(size = size),
        onPreviewKeyEvent = { handleLanToolsKey(it, controller, onClose) },
    ) {
        Surface(color = MaterialTheme.colors.background, modifier = Modifier.fillMaxSize()) {
            LanToolsContent(controller, onClose)
        }
    }
}

/** Production content shared by the dialog and Compose interaction tests. No network work in composition. */
@Composable
internal fun LanToolsContent(controller: DesktopLanToolsController, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val state by controller.state.collectAsState()
    val tokens = LocalSecureLanDesignTokens.current
    val initialFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (state.editor == null) initialFocus.requestFocus() }
    Column(modifier.fillMaxSize().onPreviewKeyEvent { handleLanToolsKey(it, controller, onClose) }
        .padding(tokens.spacing.md), verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("LAN Tools", style = MaterialTheme.typography.h6)
            TextButton(onClick = onClose, modifier = Modifier.focusRequester(initialFocus)) { Text("Close") }
        }
        TabRow(selectedTabIndex = state.tab.ordinal) {
            LanToolsTab.entries.forEach { tab ->
                Tab(selected = state.tab == tab, onClick = { controller.selectTab(tab) }, text = {
                    Text(if (tab == LanToolsTab.DEVICES) "Devices" else "Diagnostics")
                })
            }
        }
        if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        state.notice?.let { Text(it, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
        (state.storageError ?: state.storageWarning)?.let { message ->
            Text(message, color = MaterialTheme.colors.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            TextButton(onClick = controller::reload, enabled = !state.saving && !state.loading) { Text("Reload saved devices") }
        }
        if (state.tab == LanToolsTab.DEVICES) {
            LanDevicesPane(state, controller, Modifier.weight(1f))
        } else {
            Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
                Text("Network diagnostics are not available yet.", style = MaterialTheme.typography.subtitle1)
                Text("You can manage saved devices and send Wake-on-LAN packets in Devices. No connectivity checks are performed here.")
            }
        }
    }
}

/** The active confirmation receives Escape before its containing utility; no room callbacks are used. */
private fun handleLanToolsKey(event: KeyEvent, controller: DesktopLanToolsController, onClose: () -> Unit): Boolean {
    if (event.key != Key.Escape || event.type != KeyEventType.KeyUp) return false
    if (controller.state.value.deletingId != null) controller.cancelDelete() else onClose()
    return true
}
