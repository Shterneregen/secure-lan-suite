package com.shterneregen.securelan.desktop.compose

import com.shterneregen.securelan.webrtc.event.RtcVideoFrameEvent
import com.shterneregen.securelan.webrtc.service.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

class DesktopMediaControllerTest {
    @Test fun lateFramesCannotReviveClosedPreviewAndHandoffIsLatestOnly() {
        val tasks = LinkedBlockingQueue<() -> Unit>()
        val callbacks = LinkedBlockingQueue<Consumer<RtcVideoFrameEvent>>()
        val states = mutableListOf<PreviewUpdate>()
        val service = object : RtcMediaDeviceService {
            override fun audioCaptureDevices() = emptyList<RtcMediaDevice>()
            override fun audioRenderDevices() = emptyList<RtcMediaDevice>()
            override fun videoCaptureDevices() = emptyList<RtcMediaDevice>()
            override fun testAudioCaptureDevice(deviceId: String?) = "unused"
            override fun testAudioRenderDevice(deviceId: String?) = "unused"
            override fun testVideoCaptureDevice(deviceId: String?) = "unused"
            override fun startVideoPreview(deviceId: String?, frameConsumer: Consumer<RtcVideoFrameEvent>): RtcMediaDeviceService.CameraPreviewSession {
                callbacks.add(frameConsumer)
                return object : RtcMediaDeviceService.CameraPreviewSession {
                    override fun statusMessage() = "Preview ready"
                    override fun close() = Unit
                }
            }
        }
        DesktopMediaController(service, { tasks.add(it) }, states::add).use { controller ->
            controller.start("camera-a")
            val callback = callbacks.poll(2, TimeUnit.SECONDS)!!
            tasks.poll(2, TimeUnit.SECONDS)!!.invoke()
            val frame = RtcVideoFrameEvent("preview", "camera", true, 1, 1, 0, byteArrayOf(0, 0, 0, 0))
            repeat(1000) { callback.accept(frame) }
            assertEquals(1, tasks.size)
            controller.stop()
            tasks.poll()!!.invoke()
            callback.accept(frame)
            tasks.poll()!!.invoke()
            assertFalse(states.last().running)
            assertNull(states.last().frame)
        }
    }
}
