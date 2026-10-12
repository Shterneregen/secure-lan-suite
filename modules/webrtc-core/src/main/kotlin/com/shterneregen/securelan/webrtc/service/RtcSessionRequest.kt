package com.shterneregen.securelan.webrtc.service

import com.shterneregen.securelan.common.model.rtc.RtcSessionMode
import java.util.Objects

/**
 * Request parameter object containing configuration to initiate an outbound WebRTC session.
 *
 * @param localPeer nickname or identifier of the initiating local peer.
 * @param remotePeer nickname or identifier of the recipient remote peer.
 * @param mode requested session mode (DATA, AUDIO, VIDEO, AUDIO_VIDEO).
 * @param dataChannelLabel label for the primary RTCDataChannel (defaults to "securelan-data").
 * @param audioCaptureDeviceId optional hardware descriptor of the preferred microphone device.
 * @param videoCaptureDeviceId optional hardware descriptor of the preferred camera device.
 */
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

    /**
     * Convenience constructor without custom audio/video device overrides.
     *
     * @param localPeer nickname of the initiating local peer.
     * @param remotePeer nickname of the target remote peer.
     * @param mode requested session mode.
     * @param dataChannelLabel label for the data channel.
     */
    constructor(
        localPeer: String?,
        remotePeer: String?,
        mode: RtcSessionMode?,
        dataChannelLabel: String?,
    ) : this(localPeer, remotePeer, mode, dataChannelLabel, "", "")

    /** Returns the local peer nickname. */
    fun localPeer(): String = localPeer

    /** Returns the remote peer nickname. */
    fun remotePeer(): String = remotePeer

    /** Returns the requested session mode. */
    fun mode(): RtcSessionMode = mode

    /** Returns the data channel label. */
    fun dataChannelLabel(): String = dataChannelLabel

    /** Returns the preferred audio capture device descriptor, or empty if default. */
    fun audioCaptureDeviceId(): String = audioCaptureDeviceId

    /** Returns the preferred video capture device descriptor, or empty if default. */
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
