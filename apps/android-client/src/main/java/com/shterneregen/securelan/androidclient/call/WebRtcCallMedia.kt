package com.shterneregen.securelan.androidclient.call

import android.content.Context
import com.shterneregen.securelan.common.model.rtc.RtcSignalEnvelope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.webrtc.*
import org.webrtc.audio.JavaAudioDeviceModule
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Detaches every renderer before the underlying native track is disposed. Main-thread only. */
class VideoOutput(private val track: VideoTrack, val eglContext: EglBase.Context) {
    private val sinks = mutableSetOf<VideoSink>()
    private var closed = false
    fun attach(sink: VideoSink) { if (!closed && sinks.add(sink)) track.addSink(sink) }
    fun detach(sink: VideoSink) { if (!closed && sinks.remove(sink)) track.removeSink(sink) }
    fun close() { if (!closed) { sinks.forEach(track::removeSink); sinks.clear(); closed = true } }
}

class WebRtcCallMedia(private val context: Context, private val scope: CoroutineScope) : CallMedia {
    private var egl: EglBase? = null
    private var factory: PeerConnectionFactory? = null
    private var peer: PeerConnection? = null
    private var audioSource: AudioSource? = null
    private var audioTrack: AudioTrack? = null
    private var videoSource: VideoSource? = null
    private var videoTrack: VideoTrack? = null
    private var capturer: CameraVideoCapturer? = null
    private var texture: SurfaceTextureHelper? = null
    private var generation = 0
    private var capturing = false
    private var disconnected: Job? = null
    private val local = MutableStateFlow<VideoOutput?>(null)
    private val remote = MutableStateFlow<VideoOutput?>(null)
    val localVideo = local.asStateFlow()
    val remoteVideo = remote.asStateFlow()
    private val mutableMuted = MutableStateFlow(false)
    private val mutableCamera = MutableStateFlow(false)
    private val mutableFront = MutableStateFlow(true)
    val muted = mutableMuted.asStateFlow()
    val cameraEnabled = mutableCamera.asStateFlow()
    val frontCamera = mutableFront.asStateFlow()

    override suspend fun start(video: Boolean, listener: CallMedia.Listener) {
        check(peer == null)
        val token = ++generation
        fun dispatch(block: () -> Unit) { scope.launch { if (generation == token) block() } }
        PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(context).createInitializationOptions())
        val base = EglBase.create().also { egl = it }
        val audioModule = JavaAudioDeviceModule.builder(context).createAudioDeviceModule()
        try {
            factory = PeerConnectionFactory.builder()
                .setAudioDeviceModule(audioModule)
                .setVideoEncoderFactory(DefaultVideoEncoderFactory(base.eglBaseContext, true, true))
                .setVideoDecoderFactory(DefaultVideoDecoderFactory(base.eglBaseContext))
                .createPeerConnectionFactory()
        } finally { audioModule.release() }
        val rtcFactory = requireNotNull(factory)
        val config = PeerConnection.RTCConfiguration(emptyList()).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }
        peer = requireNotNull(rtcFactory.createPeerConnection(config, object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState?) = Unit
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                dispatch {
                    when (state) {
                        PeerConnection.IceConnectionState.FAILED -> listener.failed()
                        PeerConnection.IceConnectionState.DISCONNECTED -> {
                            disconnected?.cancel()
                            disconnected = scope.launch { delay(10_000); if (generation == token) listener.failed() }
                        }
                        PeerConnection.IceConnectionState.CONNECTED, PeerConnection.IceConnectionState.COMPLETED -> {
                            disconnected?.cancel(); disconnected = null
                        }
                        else -> Unit
                    }
                }
            }
            override fun onConnectionChange(state: PeerConnection.PeerConnectionState?) {
                when (state) {
                    PeerConnection.PeerConnectionState.CONNECTED -> dispatch { listener.connected() }
                    PeerConnection.PeerConnectionState.FAILED -> dispatch { listener.failed() }
                    else -> Unit
                }
            }
            override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) = Unit
            override fun onIceCandidate(candidate: IceCandidate) {
                dispatch { listener.ice(candidate.sdp, candidate.sdpMid.orEmpty(), candidate.sdpMLineIndex) }
            }
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) = Unit
            override fun onAddStream(stream: MediaStream?) = Unit
            override fun onRemoveStream(stream: MediaStream?) = Unit
            override fun onDataChannel(channel: DataChannel?) { channel?.dispose() }
            override fun onRenegotiationNeeded() = Unit
            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                val track = receiver?.track() as? VideoTrack ?: return
                dispatch {
                    remote.value?.close()
                    remote.value = VideoOutput(track, base.eglBaseContext)
                }
            }
        }))
        audioSource = rtcFactory.createAudioSource(MediaConstraints())
        audioTrack = rtcFactory.createAudioTrack("audio", audioSource).also { requireNotNull(peer).addTrack(it, listOf("securelan")) }
        mutableMuted.value = false
        if (video) {
            val enumerator = if (Camera2Enumerator.isSupported(context)) Camera2Enumerator(context) else Camera1Enumerator(false)
            val device = enumerator.deviceNames.firstOrNull { enumerator.isFrontFacing(it) }
                ?: enumerator.deviceNames.firstOrNull() ?: error("No camera")
            mutableFront.value = enumerator.isFrontFacing(device)
            capturer = requireNotNull(enumerator.createCapturer(device, object : CameraVideoCapturer.CameraEventsHandler {
                override fun onCameraError(error: String?) = dispatch { listener.failed() }
                override fun onCameraDisconnected() = dispatch { listener.failed() }
                override fun onCameraFreezed(error: String?) = dispatch { listener.failed() }
                override fun onCameraOpening(name: String?) = Unit
                override fun onFirstFrameAvailable() = Unit
                override fun onCameraClosed() = Unit
            }))
            videoSource = rtcFactory.createVideoSource(false)
            texture = SurfaceTextureHelper.create("SecureLanCamera", base.eglBaseContext)
            capturer!!.initialize(texture, context, videoSource!!.capturerObserver)
            videoTrack = rtcFactory.createVideoTrack("video", videoSource)
            peer!!.addTrack(videoTrack, listOf("securelan"))
            local.value = VideoOutput(videoTrack!!, base.eglBaseContext)
            capturer!!.startCapture(640, 480, 24)
            capturing = true
            mutableCamera.value = true
        }
    }

    override suspend fun offer(): String {
        val pc = requireNotNull(peer)
        val sdp = createSdp(pc, true)
        setSdp(pc, sdp, true)
        return sdp.description
    }
    override suspend fun answer(sdp: String): String {
        val pc = requireNotNull(peer)
        setSdp(pc, SessionDescription(SessionDescription.Type.OFFER, sdp), false)
        val answer = createSdp(pc, false)
        setSdp(pc, answer, true)
        return answer.description
    }
    override suspend fun setAnswer(sdp: String) = setSdp(requireNotNull(peer), SessionDescription(SessionDescription.Type.ANSWER, sdp), false)
    override fun addIce(signal: RtcSignalEnvelope) {
        check(requireNotNull(peer).addIceCandidate(IceCandidate(signal.sdpMid(), signal.sdpMLineIndex(), signal.iceCandidate())))
    }
    fun toggleMute() {
        val track = audioTrack ?: return
        mutableMuted.value = !mutableMuted.value
        track.setEnabled(!mutableMuted.value)
    }
    fun toggleCamera() {
        val camera = capturer ?: return
        if (capturing) camera.stopCapture() else camera.startCapture(640, 480, 24)
        capturing = !capturing
        videoTrack?.setEnabled(capturing)
        mutableCamera.value = capturing
    }
    fun switchCamera() {
        val token = generation
        capturer?.switchCamera(object : CameraVideoCapturer.CameraSwitchHandler {
            override fun onCameraSwitchDone(front: Boolean) { scope.launch { if (token == generation) mutableFront.value = front } }
            override fun onCameraSwitchError(error: String?) = Unit
        })
    }

    override fun close() {
        generation++
        disconnected?.cancel(); disconnected = null
        local.value?.close(); remote.value?.close()
        local.value = null; remote.value = null
        // One failing device cleanup must not prevent release of the remaining native resources.
        runCatching { if (capturing) capturer?.stopCapture() }; capturing = false
        runCatching { capturer?.dispose() }; capturer = null
        runCatching { peer?.close(); peer?.dispose() }; peer = null
        runCatching { videoTrack?.dispose() }; videoTrack = null
        runCatching { audioTrack?.dispose() }; audioTrack = null
        runCatching { videoSource?.dispose() }; videoSource = null
        runCatching { audioSource?.dispose() }; audioSource = null
        runCatching { texture?.dispose() }; texture = null
        runCatching { factory?.dispose() }; factory = null
        runCatching { egl?.release() }; egl = null
        mutableMuted.value = false; mutableCamera.value = false
    }

    private suspend fun createSdp(pc: PeerConnection, offer: Boolean): SessionDescription = suspendCancellableCoroutine { continuation ->
        val observer = object : SdpAdapter() {
            override fun onCreateSuccess(sdp: SessionDescription) { if (continuation.isActive) continuation.resume(sdp) }
            override fun onCreateFailure(error: String) { if (continuation.isActive) continuation.resumeWithException(IllegalStateException(error)) }
        }
        if (offer) pc.createOffer(observer, MediaConstraints()) else pc.createAnswer(observer, MediaConstraints())
    }
    private suspend fun setSdp(pc: PeerConnection, sdp: SessionDescription, local: Boolean): Unit = suspendCancellableCoroutine { continuation ->
        val observer = object : SdpAdapter() {
            override fun onSetSuccess() { if (continuation.isActive) continuation.resume(Unit) }
            override fun onSetFailure(error: String) { if (continuation.isActive) continuation.resumeWithException(IllegalStateException(error)) }
        }
        if (local) pc.setLocalDescription(observer, sdp) else pc.setRemoteDescription(observer, sdp)
    }
    private open class SdpAdapter : SdpObserver {
        override fun onCreateSuccess(sdp: SessionDescription) = Unit
        override fun onSetSuccess() = Unit
        override fun onCreateFailure(error: String) = Unit
        override fun onSetFailure(error: String) = Unit
    }
}
