package com.shterneregen.securelan.androidclient.call

import android.app.Application
import com.shterneregen.securelan.common.model.rtc.RtcSignalEnvelope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

data class CallUiState(
    val call: CallState = CallState(),
    val localVideo: VideoOutput? = null,
    val remoteVideo: VideoOutput? = null,
    val muted: Boolean = false,
    val cameraEnabled: Boolean = false,
    val frontCamera: Boolean = true,
    val speaker: Boolean = false,
    val signalingError: String? = null,
)

/** Application call boundary: UI observes snapshots/actions and never owns native media or routing. */
interface CallSession : AutoCloseable {
    val state: StateFlow<CallState>
    val uiState: StateFlow<CallUiState>
    fun start(peer: String, video: Boolean)
    fun accept(id: String, video: Boolean)
    fun receive(sender: String, signal: RtcSignalEnvelope)
    fun connectionLost()
    fun peerLeft(peer: String)
    fun end()
    fun dismiss()
    fun toggleMute()
    fun toggleCamera()
    fun switchCamera()
    fun toggleSpeaker()
}

/** Activity-retained owner. Home/PiP retains it; clearing the ViewModel ends the call and foreground work. */
class AndroidCallSession(
    private val application: Application,
    scope: CoroutineScope,
    nickname: () -> String,
    available: () -> Boolean,
    send: suspend (RtcSignalEnvelope) -> Unit,
) : CallSession {
    private val closed = AtomicBoolean()
    private val media = WebRtcCallMedia(application, scope)
    private val notifications = IncomingCallNotification(application)
    private val audio: AudioRouteManager = AudioRouteManager(application) { end() }
    private val coordinator: CallCoordinator = CallCoordinator(scope, nickname, available, send, media,
        prepare = { id, video ->
            CallForegroundService.start(application, id, state.value.peer, video, ::end, ::toggleMute)
            audio.start(video)
        },
        release = { try { audio.stop() } finally { CallForegroundService.stop(application) } },
        now = android.os.SystemClock::elapsedRealtime)
    override val state: StateFlow<CallState> = coordinator.state
    private val mutableUiState = MutableStateFlow(CallUiState())
    override val uiState = mutableUiState.asStateFlow()
    private val observer: Job = scope.launch {
        merge(state.map { Unit }, media.localVideo.map { Unit }, media.remoteVideo.map { Unit },
            media.muted.map { Unit }, media.cameraEnabled.map { Unit }, media.frontCamera.map { Unit },
            audio.speaker.map { Unit }, coordinator.signalingError.map { Unit }).collect {
            notifications.update(state.value)
            mutableUiState.value = CallUiState(state.value, media.localVideo.value, media.remoteVideo.value,
                media.muted.value, media.cameraEnabled.value, media.frontCamera.value, audio.speaker.value,
                coordinator.signalingError.value)
        }
    }
    override fun start(peer: String, video: Boolean) = coordinator.start(peer, video)
    override fun accept(id: String, video: Boolean) = coordinator.accept(id, video)
    override fun receive(sender: String, signal: RtcSignalEnvelope) = coordinator.receive(sender, signal)
    override fun connectionLost() = coordinator.connectionLost()
    override fun peerLeft(peer: String) = coordinator.peerLeft(peer)
    override fun end() = coordinator.end()
    override fun dismiss() = coordinator.dismiss()
    override fun toggleMute() { runCatching { media.toggleMute() }.onFailure { end() } }
    override fun toggleCamera() { runCatching { media.toggleCamera() }.onFailure { end() } }
    override fun switchCamera() { runCatching { media.switchCamera() }.onFailure { end() } }
    override fun toggleSpeaker() { runCatching { audio.toggleSpeaker() }.onFailure { end() } }
    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        try { coordinator.close() } finally { observer.cancel(); notifications.clear() }
    }
}
