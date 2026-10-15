package com.shterneregen.securelan.webrtc.service

import com.shterneregen.securelan.webrtc.event.RtcVideoFrameEvent
import java.util.function.Consumer

/**
 * Service managing local audio and video media device discovery, hardware testing,
 * and live camera preview sessions.
 */
interface RtcMediaDeviceService : AutoCloseable {
    /**
     * Enumerates available microphone capture devices.
     *
     * @return list of detected audio input devices.
     */
    fun audioCaptureDevices(): List<RtcMediaDevice>

    /**
     * Enumerates available speaker and headphone playout devices.
     *
     * @return list of detected audio output devices.
     */
    fun audioRenderDevices(): List<RtcMediaDevice>

    /**
     * Enumerates available camera capture devices.
     *
     * @return list of detected video input devices.
     */
    fun videoCaptureDevices(): List<RtcMediaDevice>

    /**
     * Tests microphone accessibility for the given device ID.
     *
     * @param deviceId hardware descriptor of the microphone, or null/blank for default.
     * @return human-readable test summary or error message.
     */
    fun testAudioCaptureDevice(deviceId: String?): String

    /**
     * Tests speaker accessibility for the given device ID.
     *
     * @param deviceId hardware descriptor of the speaker, or null/blank for default.
     * @return human-readable test summary or error message.
     */
    fun testAudioRenderDevice(deviceId: String?): String

    /**
     * Tests camera capture readiness by opening and probing supported capabilities.
     *
     * @param deviceId hardware descriptor of the camera, or null/blank for default.
     * @return human-readable test summary or error message.
     */
    fun testVideoCaptureDevice(deviceId: String?): String

    /**
     * Starts an active camera preview session, feeding converted BGRA video frames to [frameConsumer].
     *
     * @param deviceId hardware descriptor of the camera, or null/blank for default.
     * @param frameConsumer receiver for preview [RtcVideoFrameEvent] instances.
     * @return an active [CameraPreviewSession] handle.
     */
    fun startVideoPreview(deviceId: String?, frameConsumer: Consumer<RtcVideoFrameEvent>): CameraPreviewSession

    /**
     * Handle to an active camera preview stream, implementing [AutoCloseable] to release
     * native video tracks and device resources when preview stops.
     */
    interface CameraPreviewSession : AutoCloseable {
        /**
         * Returns a diagnostic status message describing the active preview.
         *
         * @return status description string.
         */
        fun statusMessage(): String

        /**
         * Stops the preview stream and disposes of all underlying native resources.
         */
        override fun close()

        /** Explicit bounded wait for shutdown/tests; never called implicitly by close. */
        fun awaitClosed(timeoutMillis: Long): Boolean = true
    }

    /**
     * Closes the service, terminating all active preview sessions and background worker threads.
     */
    /** Explicit bounded wait after nonblocking close. */
    fun awaitClosed(timeoutMillis: Long): Boolean = true

    override fun close() {
    }
}
