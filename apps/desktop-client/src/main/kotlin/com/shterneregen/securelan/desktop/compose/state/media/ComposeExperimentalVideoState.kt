package com.shterneregen.securelan.desktop.compose.state.media

import com.shterneregen.securelan.common.model.rtc.RtcSessionMode
import com.shterneregen.securelan.common.model.rtc.RtcSessionState
import com.shterneregen.securelan.desktop.compose.state.connection.ComposeStatusConnectionState
import com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListState
import com.shterneregen.securelan.desktop.ui.DesktopMainViewHelpers
import com.shterneregen.securelan.desktop.ui.DesktopRealtimeFormatters
import com.shterneregen.securelan.desktop.ui.MediaDeviceChoice
import com.shterneregen.securelan.webrtc.event.RtcVideoFrameEvent
import com.shterneregen.securelan.webrtc.runtime.RtcRuntimeStatus
import com.shterneregen.securelan.webrtc.service.RtcSessionSnapshot

data class ComposeExperimentalVideoState(
    val statusState: ComposeStatusConnectionState,
    val peerListState: ComposePeerListState,
    val cameras: List<MediaDeviceChoice> = listOf(MediaDeviceChoice.systemDefault("System default camera")),
    val selectedCameraId: String = "",
    val runtimeStatus: RtcRuntimeStatus? = null,
    val currentSession: RtcSessionSnapshot? = null,
    val localPreviewEnabled: Boolean = true,
    val remotePreviewEnabled: Boolean = true,
    val previewRunning: Boolean = false,
    val latestPreviewFrame: RtcVideoFrameEvent? = null,
    val latestLocalVideoFrame: RtcVideoFrameEvent? = null,
    val latestRemoteVideoFrame: RtcVideoFrameEvent? = null,
    val cameraTestStatus: String = "Not tested",
    val javaFxFallbackAvailable: Boolean = true,
) {
    val title: String = "Experimental camera and video"
    val selectedPeer = peerListState.selectedPeer
    val selectedPeerName: String = selectedPeer?.nickname ?: "No peer selected"
    val selectedCamera: MediaDeviceChoice = cameras.firstOrNull { it.matches(selectedCameraId) }
        ?: cameras.firstOrNull()
        ?: MediaDeviceChoice.systemDefault("System default camera")
    val physicalCameraCount: Int = cameras.count { !it.systemDefault }
    val runtimeLabel: String = DesktopRealtimeFormatters.formatRuntimeStatus(runtimeStatus)
    val permissionStatusLabel: String = when {
        runtimeStatus?.available == true -> "Camera permission: ready to test"
        runtimeStatus == null -> "Camera permission: not checked yet"
        else -> "Camera permission: unavailable"
    }
    val cameraEmptyState: String = if (physicalCameraCount == 0) {
        "No cameras found. Connect one and refresh devices."
    } else {
        "$physicalCameraCount camera option${if (physicalCameraCount == 1) "" else "s"} available."
    }
    val sessionMode: RtcSessionMode = currentSession?.mode ?: RtcSessionMode.AUDIO_VIDEO
    val sessionState: RtcSessionState = currentSession?.state ?: RtcSessionState.IDLE
    val stageTitle: String = DesktopRealtimeFormatters.videoStageTitle(sessionMode, currentSession?.remotePeer ?: selectedPeer?.nickname)
    val stageBadge: String = DesktopRealtimeFormatters.videoStageBadge(sessionState)
    val callTransitionLabel: String = when (sessionState) {
        RtcSessionState.CONNECTED -> "Video connected"
        RtcSessionState.NEGOTIATING, RtcSessionState.CONNECTING -> "Connecting video…"
        RtcSessionState.CLOSING, RtcSessionState.CLOSED -> "Video disconnected"
        RtcSessionState.FAILED -> "Video failed"
        else -> "Video idle"
    }
    val mediaLabel: String = DesktopRealtimeFormatters.videoMediaLabel(sessionMode)
    val localFrame: RtcVideoFrameEvent? = latestLocalVideoFrame ?: latestPreviewFrame?.takeIf { it.local() }
    val remoteFrame: RtcVideoFrameEvent? = latestRemoteVideoFrame ?: latestPreviewFrame?.takeIf { !it.local() }
    val previewStatus: String = localFrame?.let { frame -> DesktopRealtimeFormatters.cameraPreviewLiveStatus(frame.width(), frame.height()) }
        ?: if (previewRunning) "Camera preview starting…" else "Camera preview idle"
    val frameCaption: String = latestPreviewFrame?.let { frame ->
        DesktopRealtimeFormatters.videoFrameCaption(frame.local(), frame.peer(), frame.width(), frame.height())
    } ?: "No video frames yet."
    val remoteFrameCaption: String = remoteFrame?.let { frame ->
        DesktopRealtimeFormatters.videoFrameCaption(false, frame.peer(), frame.width(), frame.height())
    } ?: "Remote video will appear here when the call connects."
    val localFrameCaption: String = localFrame?.let { frame ->
        DesktopRealtimeFormatters.videoFrameCaption(true, frame.peer(), frame.width(), frame.height())
    } ?: "Local preview will appear when camera frames arrive."
    val stageFrameCaption: String = if (currentSession != null) remoteFrameCaption else localFrameCaption
    val previewStateLabel: String = when {
        cameraTestStatus.startsWith("Camera preview failed", ignoreCase = true) -> "Preview failed"
        localFrame != null -> "Preview is live"
        previewRunning -> "Preview is starting"
        else -> "Preview is off"
    }
    val previewActionHint: String = when {
        cameraTestStatus.startsWith("Camera preview failed", ignoreCase = true) -> "Preview could not start. Close other camera apps and try again."
        previewRunning && localFrame == null -> "Waiting for the first camera frame. This can take a few seconds."
        previewRunning -> "Preview is running. Stop preview before switching cameras."
        physicalCameraCount == 0 -> "No camera confirmed. Refresh devices or use the system default."
        else -> "Choose a camera and start preview to check image and lighting."
    }
    val videoTargetReady: Boolean = statusState.clientConnected && selectedPeer?.online == true && selectedPeer.videoCapable
    val canRefreshCameras: Boolean = javaFxFallbackAvailable
    val canTestCamera: Boolean = cameras.isNotEmpty() && javaFxFallbackAvailable
    val canStartPreview: Boolean = cameras.isNotEmpty() && !previewRunning && javaFxFallbackAvailable
    val canStopPreview: Boolean = previewRunning && javaFxFallbackAvailable
    val canStartVideo: Boolean = videoTargetReady && javaFxFallbackAvailable
    val canHangUp: Boolean = DesktopMainViewHelpers.hangUpAvailable(currentSession?.state) && javaFxFallbackAvailable
    val startVideoLabel: String = if (canStartVideo) "Start video call" else "Video call blocked"
    val startPreviewLabel: String = if (canStartPreview) "Start camera preview" else "Preview unavailable"
    val stopPreviewLabel: String = if (canStopPreview) "Stop camera preview" else "Stop preview unavailable"
    val previewConfigurationLabel: String = "Self preview ${if (localPreviewEnabled) "on" else "off"} • remote preview ${if (remotePreviewEnabled) "on" else "off"}"
    val fallbackLabel: String =
        if (javaFxFallbackAvailable) "JavaFX experimental video workspace remains production fallback" else "JavaFX video fallback unavailable"
    val blockedReasons: List<String> = buildList {
        if (!statusState.clientConnected) add("Connect to chat before starting experimental video.")
        if (selectedPeer == null) add("Select an online peer before starting video.") else if (!selectedPeer.online) add("Selected peer is offline; video must remain blocked.") else if (!selectedPeer.videoCapable) add("Selected peer does not advertise video support; video must remain blocked.")
        if (!javaFxFallbackAvailable) add("JavaFX fallback is unavailable; keep live Compose video actions disabled.")
    }
    val readinessSummary: String = if (blockedReasons.isEmpty()) {
        "Experimental video controls are ready."
    } else {
        blockedReasons.joinToString(" · ")
    }
}
