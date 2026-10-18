package com.shterneregen.securelan.androidclient.call

import android.Manifest
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import com.shterneregen.securelan.androidclient.MainActivity
import com.shterneregen.securelan.androidclient.MainViewModel
import com.shterneregen.securelan.androidclient.R
import kotlinx.coroutines.delay
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import kotlin.math.roundToInt
import com.shterneregen.securelan.androidclient.ui.appStringResource as stringResource

/** Hosts permission requests and call overlays while preserving the underlying workspace. */
@Composable
fun CallHost(viewModel: MainViewModel, activity: MainActivity, content: @Composable ((String, Boolean) -> Unit) -> Unit) {
    val context = LocalContext.current
    val call by viewModel.callState.collectAsState()
    var pendingPeer by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingVideo by rememberSaveable { mutableStateOf(false) }
    var microphoneExplanation by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        val mic = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val camera = context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (mic) {
            if (pendingVideo && !camera) Toast.makeText(context, R.string.call_camera_denied, Toast.LENGTH_LONG).show()
            val id = pendingId
            if (id != null) viewModel.acceptCall(id, pendingVideo && camera)
            else pendingPeer?.let { peer -> viewModel.startCall(peer, pendingVideo && camera) }
        } else microphoneExplanation = true
        pendingPeer = null; pendingId = null
    }
    fun request(peer: String?, id: String?, video: Boolean) {
        val captureVideo = video && context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
        if (video && !captureVideo) Toast.makeText(context, R.string.call_camera_denied, Toast.LENGTH_LONG).show()
        pendingPeer = peer; pendingId = id; pendingVideo = captureVideo
        launcher.launch(buildList {
            add(Manifest.permission.RECORD_AUDIO)
            if (captureVideo) add(Manifest.permission.CAMERA)
            if (Build.VERSION.SDK_INT >= 31) add(Manifest.permission.BLUETOOTH_CONNECT)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }.toTypedArray())
    }
    Box(Modifier.fillMaxSize()) {
        content { peer, video -> request(peer, null, video) }
        if (call.phase != CallPhase.IDLE) CallScreen(viewModel, call, activity.inCallPip, { video -> request(null, call.id, video) })
    }
    if (microphoneExplanation) AlertDialog(
        onDismissRequest = { microphoneExplanation = false },
        title = { Text(stringResource(R.string.call_title)) },
        text = { Text(stringResource(R.string.call_microphone_required)) },
        confirmButton = { TextButton(onClick = {
            microphoneExplanation = false
            activity.startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                "package:${context.packageName}".toUri()))
        }) { Text(stringResource(R.string.call_app_settings)) } },
        dismissButton = { TextButton(onClick = { microphoneExplanation = false }) { Text(stringResource(R.string.call_close)) } },
    )
}

/** Offers calls only to advertised voice-capable peers; map values indicate video capability. */
@Composable
fun CallButtons(peers: Map<String, Boolean>, connected: Boolean, onCall: (String, Boolean) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.call_title), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        Box {
            TextButton(onClick = { expanded = true }, enabled = connected && peers.isNotEmpty()) {
                Icon(Icons.Default.Call, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.call_start))
            }
            DropdownMenu(expanded, { expanded = false }) {
                peers.toSortedMap().forEach { (peer, video) ->
                    DropdownMenuItem(text = { Text(stringResource(R.string.call_audio_peer, peer)) },
                        onClick = { expanded = false; onCall(peer, false) }, leadingIcon = { Icon(Icons.Default.Call, null) })
                    if (video) DropdownMenuItem(text = { Text(stringResource(R.string.call_video_peer, peer)) },
                        onClick = { expanded = false; onCall(peer, true) }, leadingIcon = { Icon(Icons.Default.Videocam, null) })
                }
            }
        }
    }
}

@Composable
private fun CallScreen(vm: MainViewModel, call: CallState, pip: Boolean, accept: (Boolean) -> Unit) {
    val context = LocalContext.current
    val ui by vm.callUiState.collectAsState()
    val local = ui.localVideo
    val remote = ui.remoteVideo
    val muted = ui.muted
    val camera = ui.cameraEnabled
    val front = ui.frontCamera
    val speaker = ui.speaker
    var controls by remember(call.id) { mutableStateOf(true) }
    var seconds by remember(call.id) { mutableLongStateOf(0) }
    BackHandler(enabled = !pip) { if (call.ongoing) vm.endCall() else vm.dismissCall() }
    DisposableEffect(call.id, call.phase) {
        val notificationsAllowed = androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
        val ringtone = if (call.phase == CallPhase.INCOMING && !notificationsAllowed) runCatching {
            RingtoneManager.getRingtone(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)).also { it?.play() }
        }.getOrNull() else null
        onDispose { ringtone?.stop() }
    }
    LaunchedEffect(call.phase, call.startedAt) {
        while (call.phase == CallPhase.ACTIVE) { seconds = (SystemClock.elapsedRealtime() - call.startedAt) / 1000; delay(1000) }
    }
    LaunchedEffect(controls, call.phase, remote) {
        if (controls && call.phase == CallPhase.ACTIVE && remote != null) { delay(5_000); controls = false }
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black).clickable { controls = !controls }) {
        val availableWidth = this.maxWidth
        val availableHeight = this.maxHeight
        remote?.let { VideoView(it, false, Modifier.fillMaxSize()) }
        if (remote == null) Text(call.peer, color = Color.White, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.align(Alignment.Center))
        if (!pip && local != null && camera) {
            val density = androidx.compose.ui.platform.LocalDensity.current
            val maxX = with(density) { (availableWidth - 120.dp).toPx().coerceAtLeast(0f) }
            val maxY = with(density) { (availableHeight - 160.dp).toPx().coerceAtLeast(0f) }
            var x by remember(call.id) { mutableFloatStateOf(0f) }
            var y by remember(call.id) { mutableFloatStateOf(0f) }
            VideoView(local, front, Modifier.offset { IntOffset(x.coerceIn(0f, maxX).roundToInt(), y.coerceIn(0f, maxY).roundToInt()) }
                .size(120.dp, 160.dp).pointerInput(maxX, maxY) {
                    detectDragGestures { change, amount -> change.consume(); x = (x + amount.x).coerceIn(0f, maxX); y = (y + amount.y).coerceIn(0f, maxY) }
                }, overlay = true)
        }
        if (!pip && (controls || call.phase != CallPhase.ACTIVE)) {
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(alpha = 0.8f)).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(call.peer, color = Color.White, style = MaterialTheme.typography.titleLarge)
                ui.signalingError?.let { Text(it, color = Color.White) }
                val status = when (call.phase) {
                    CallPhase.INCOMING -> R.string.call_incoming
                    CallPhase.OUTGOING -> R.string.call_outgoing
                    CallPhase.CONNECTING -> R.string.call_connecting
                    CallPhase.ENDED -> when (call.reason) {
                        CallEndReason.DECLINED -> R.string.call_declined
                        CallEndReason.BUSY -> R.string.call_busy
                        CallEndReason.TIMEOUT -> R.string.call_timeout
                        CallEndReason.CONNECTION_LOST -> R.string.call_lost
                        CallEndReason.ERROR -> R.string.call_error
                        else -> R.string.call_ended
                    }
                    else -> R.string.call_active
                }
                Text(if (call.phase == CallPhase.ACTIVE) "%02d:%02d".format(seconds / 60, seconds % 60) else stringResource(status), color = Color.White)
                if (call.phase == CallPhase.INCOMING) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { accept(false) }) { Text(stringResource(R.string.call_accept_audio)) }
                        if (call.video) Button(onClick = { accept(true) }) { Text(stringResource(R.string.call_accept_video)) }
                    }
                } else if (call.ongoing) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        CallControl(if (muted) Icons.Default.MicOff else Icons.Default.Mic, if (muted) R.string.call_unmute else R.string.call_mute, vm::toggleCallMute)
                        if (local != null) {
                            CallControl(if (camera) Icons.Default.Videocam else Icons.Default.VideocamOff, R.string.call_camera, vm::toggleCallCamera)
                            CallControl(Icons.Default.Cameraswitch, R.string.call_switch_camera, vm::switchCallCamera)
                        }
                        CallControl(if (speaker) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.Hearing, R.string.call_speaker, vm::toggleCallSpeaker)
                    }
                }
                if (call.ongoing) Button(onClick = vm::endCall, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Icon(Icons.Default.CallEnd, null); Spacer(Modifier.width(8.dp))
                    Text(stringResource(if (call.phase == CallPhase.INCOMING) R.string.call_reject else R.string.call_end))
                } else Button(onClick = vm::dismissCall) { Text(stringResource(R.string.call_close)) }
            }
        }
    }
}

@Composable
private fun CallControl(icon: androidx.compose.ui.graphics.vector.ImageVector, label: Int, action: () -> Unit) {
    IconButton(onClick = action) { Icon(icon, stringResource(label), tint = Color.White) }
}

@Composable
private fun VideoView(output: VideoOutput, mirror: Boolean, modifier: Modifier, overlay: Boolean = false) {
    key(output) {
    val context = LocalContext.current
    val renderer = remember(output) { SurfaceViewRenderer(context).apply {
        init(output.eglContext, null)
        setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
        setEnableHardwareScaler(true)
        setZOrderMediaOverlay(overlay)
    } }
    DisposableEffect(output, renderer) {
        output.attach(renderer)
        onDispose { output.detach(renderer); renderer.release() }
    }
    AndroidView(factory = { renderer }, modifier = modifier, update = { it.setMirror(mirror) })
    }
}
