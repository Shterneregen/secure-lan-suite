package com.shterneregen.securelan.desktop.compose.ui.lantools

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import com.shterneregen.securelan.desktop.compose.ui.icons.SecureLanIcons
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
    MaterialTheme(shapes = MaterialTheme.shapes.copy(small = RoundedCornerShape(tokens.radius.small))) {
        Column(modifier.fillMaxSize().onPreviewKeyEvent { handleLanToolsKey(it, controller, onClose) }
            .padding(tokens.spacing.md), verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm)) {
                    Icon(SecureLanIcons.Network, contentDescription = null, tint = tokens.colors.accent, modifier = Modifier.size(28.dp))
                    Text("LAN Tools", style = MaterialTheme.typography.h5)
                }
                TextButton(onClick = onClose, modifier = Modifier.focusRequester(initialFocus)) { Text("Close") }
            }
            Surface(shape = RoundedCornerShape(tokens.radius.medium), border = BorderStroke(tokens.border.subtle, tokens.colors.borderSubtle)) {
                TabRow(selectedTabIndex = state.tab.ordinal, backgroundColor = tokens.colors.surfaceLevel2, contentColor = tokens.colors.accent) {
                    LanToolsTab.entries.forEach { tab ->
                        Tab(selected = state.tab == tab, onClick = { controller.selectTab(tab) }, text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs)) {
                                Icon(if (tab == LanToolsTab.DEVICES) SecureLanIcons.Devices else SecureLanIcons.Diagnose,
                                    contentDescription = null, modifier = Modifier.size(18.dp))
                                Text(if (tab == LanToolsTab.DEVICES) "Devices" else "Diagnostics")
                            }
                        })
                    }
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
                val diagnostics = controller.diagnostics
                if (diagnostics != null) LanDiagnosticsPane(diagnostics, Modifier.weight(1f))
                else Text("Diagnostics unavailable in this environment.", modifier = Modifier.weight(1f))
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
