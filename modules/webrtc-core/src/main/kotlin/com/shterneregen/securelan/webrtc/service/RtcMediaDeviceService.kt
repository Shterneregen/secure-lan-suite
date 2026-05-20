package com.shterneregen.securelan.webrtc.service

import com.shterneregen.securelan.webrtc.event.RtcVideoFrameEvent
import java.util.function.Consumer

interface RtcMediaDeviceService : AutoCloseable {
    fun audioCaptureDevices(): List<RtcMediaDevice>

    fun videoCaptureDevices(): List<RtcMediaDevice>

    fun testAudioCaptureDevice(deviceId: String?): String

    fun testVideoCaptureDevice(deviceId: String?): String

    fun startVideoPreview(deviceId: String?, frameConsumer: Consumer<RtcVideoFrameEvent>): CameraPreviewSession

    interface CameraPreviewSession : AutoCloseable {
        fun statusMessage(): String

        override fun close()
    }

    override fun close() {
    }
}
