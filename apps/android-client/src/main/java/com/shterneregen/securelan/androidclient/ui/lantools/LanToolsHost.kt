package com.shterneregen.securelan.androidclient.ui.lantools

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shterneregen.securelan.androidclient.lantools.LanToolsDependencies
import com.shterneregen.securelan.androidclient.lantools.LanToolsViewModel

/** Full-screen secondary surface keeps the workspace composed, with its own focus/window and Back handling. */
@Composable
internal fun LanToolsHost(content: @Composable (open: () -> Unit) -> Unit) {
    val context = LocalContext.current
    val factory = remember(context.applicationContext) { LanToolsDependencies(context.applicationContext) }
    val tools: LanToolsViewModel = viewModel(factory = factory)
    var visible by rememberSaveable { mutableStateOf(false) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> tools.permission(granted) }
    fun granted() = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED
    DisposableEffect(visible, lifecycle) {
        fun sync() {
            if (visible) tools.permission(granted())
            tools.setActive(visible && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
        }
        val observer = LifecycleEventObserver { _, _ -> sync() }
        lifecycle.addObserver(observer); sync()
        onDispose { lifecycle.removeObserver(observer); tools.setActive(false) }
    }
    content { visible = true }
    if (visible) Dialog(onDismissRequest = { visible = false }, properties = DialogProperties(decorFitsSystemWindows = false)) {
        val window = (LocalView.current.parent as DialogWindowProvider).window
        DisposableEffect(window) {
            // Let Android measure the real window/IME area. Compose 1.7's non-default-width dialog
            // instead forces configuration.screenHeightDp, which can extend under system bars.
            window.setBackgroundDrawable(android.graphics.Color.TRANSPARENT.toDrawable())
            window.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT)
            onDispose { }
        }
        Surface(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding(), color = MaterialTheme.colorScheme.background) {
            LanToolsScreen(tools, onBack = { visible = false }, onRequestNearby = {
                if (granted()) { tools.permission(true); tools.discover() }
                else if (Build.VERSION.SDK_INT >= 33) {
                    tools.discover() // Explain the missing grant; receiving a grant never silently starts browsing.
                    launcher.launch(Manifest.permission.NEARBY_WIFI_DEVICES)
                }
            })
        }
    }
}
