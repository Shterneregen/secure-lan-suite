package com.shterneregen.securelan.webrtc.service

import com.shterneregen.securelan.common.model.rtc.RtcSessionMode
import java.util.Objects

class RtcSessionRequest(
    localPeer: String?,
    remotePeer: String?,
    mode: RtcSessionMode?,
    dataChannelLabel: String?,
    audioCaptureDeviceId: String?,
    videoCaptureDeviceId: String?,
) {
    private val localPeer: String = localPeer?.trim() ?: ""
    private val remotePeer: String = remotePeer?.trim() ?: ""
    private val mode: RtcSessionMode = Objects.requireNonNull(mode, "mode must not be null")!!
    private val dataChannelLabel: String = if (dataChannelLabel.isNullOrBlank()) "securelan-data" else dataChannelLabel.trim()
    private val audioCaptureDeviceId: String = audioCaptureDeviceId?.trim() ?: ""
    private val videoCaptureDeviceId: String = videoCaptureDeviceId?.trim() ?: ""

    constructor(
        localPeer: String?,
        remotePeer: String?,
        mode: RtcSessionMode?,
        dataChannelLabel: String?,
    ) : this(localPeer, remotePeer, mode, dataChannelLabel, "", "")

    fun localPeer(): String = localPeer
    fun remotePeer(): String = remotePeer
    fun mode(): RtcSessionMode = mode
    fun dataChannelLabel(): String = dataChannelLabel
    fun audioCaptureDeviceId(): String = audioCaptureDeviceId
    fun videoCaptureDeviceId(): String = videoCaptureDeviceId

    override fun equals(other: Any?): Boolean =
        this === other ||
            (other is RtcSessionRequest &&
                localPeer == other.localPeer &&
                remotePeer == other.remotePeer &&
                mode == other.mode &&
                dataChannelLabel == other.dataChannelLabel &&
                audioCaptureDeviceId == other.audioCaptureDeviceId &&
                videoCaptureDeviceId == other.videoCaptureDeviceId)

    override fun hashCode(): Int = Objects.hash(localPeer, remotePeer, mode, dataChannelLabel, audioCaptureDeviceId, videoCaptureDeviceId)

    override fun toString(): String =
        "RtcSessionRequest[localPeer=$localPeer, remotePeer=$remotePeer, mode=$mode, dataChannelLabel=$dataChannelLabel, audioCaptureDeviceId=$audioCaptureDeviceId, videoCaptureDeviceId=$videoCaptureDeviceId]"
}
