package com.shterneregen.securelan.webrtc.service

/** Applied before the next accepted/outgoing call; selection changes never restart an active call. */
data class RtcMediaConfiguration(
    val microphoneId: String = "",
    val speakerId: String = "",
    val cameraId: String = "",
)
