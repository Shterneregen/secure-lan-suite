package com.shterneregen.securelan.androidclient.ui.qr

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.shterneregen.securelan.androidclient.R
import java.util.concurrent.Executors
import com.shterneregen.securelan.androidclient.ui.appStringResource as stringResource

@Composable
fun QrScanScreen(onResult: (String) -> Unit, onPaste: () -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(Unit) { if (!granted) permission.launch(Manifest.permission.CAMERA) }
    var cameraFailed by remember { mutableStateOf(false) }
    val latestResult by rememberUpdatedState(onResult)
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.room_scan), style = MaterialTheme.typography.headlineSmall)
                if (granted && !cameraFailed) {
                    val preview = remember { PreviewView(context) }
                    AndroidView(factory = { preview }, modifier = Modifier.weight(1f).fillMaxWidth())
                    DisposableEffect(lifecycle, preview) {
                        val executor = Executors.newSingleThreadExecutor()
                        val future = ProcessCameraProvider.getInstance(context)
                        var disposed = false
                        var provider: ProcessCameraProvider? = null
                        val display = Preview.Builder().build().apply { surfaceProvider = preview.surfaceProvider }
                        val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                        analysis.setAnalyzer(executor, QrFrameAnalyzer(result = { raw -> ContextCompat.getMainExecutor(context).execute {
                            if (!disposed) { analysis.clearAnalyzer(); provider?.unbind(display, analysis); latestResult(raw) }
                        } }))
                        future.addListener({
                            if (!disposed) runCatching {
                                provider = future.get()
                                provider!!.bindToLifecycle(lifecycle, CameraSelector.DEFAULT_BACK_CAMERA, display, analysis)
                            }.onFailure { cameraFailed = true }
                        }, ContextCompat.getMainExecutor(context))
                        onDispose { disposed = true; analysis.clearAnalyzer(); provider?.unbind(display, analysis); executor.shutdownNow() }
                    }
                } else {
                    Text(stringResource(R.string.room_camera_unavailable))
                    TextButton(onClick = { cameraFailed = false; permission.launch(Manifest.permission.CAMERA) }) { Text(stringResource(R.string.room_retry)) }
                    TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))) }) {
                        Text(stringResource(R.string.settings))
                    }
                }
                TextButton(onClick = onPaste) { Text(stringResource(R.string.room_paste)) }
                TextButton(onClick = onClose) { Text(stringResource(R.string.room_close)) }
            }
        }
    }
}
