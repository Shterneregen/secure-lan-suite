package com.shterneregen.securelan.webrtc.runtime

/**
 * Factory provider responsible for instantiating the runtime [RtcEngine].
 *
 * Attempts to initialize the native [WebRtcJavaEngine]. If native library loading
 * or device initialization fails, safely falls back to a [NoOpRtcEngine] with detailed
 * root-cause diagnostics logged to [RtcFileLogger].
 */
object RtcEngineProvider {
    /**
     * Creates an [RtcEngine] instance with default system audio and video capture devices.
     *
     * @return initialized native engine or fallback [NoOpRtcEngine] if initialization fails.
     */
    @JvmStatic
    fun createDefault(): RtcEngine = try {
        WebRtcJavaEngine()
    } catch (error: Throwable) {
        error.printStackTrace()
        RtcFileLogger.error("Failed to initialize webrtc-java provider", error)
        NoOpRtcEngine(buildFailureMessage(error))
    }

    /**
     * Creates an [RtcEngine] instance configured with specific preferred device descriptors.
     *
     * @param preferredAudioCaptureDeviceId descriptor of the preferred microphone device, or null for default.
     * @param preferredVideoCaptureDeviceId descriptor of the preferred camera device, or null for default.
     * @return initialized native engine or fallback [NoOpRtcEngine] if initialization fails.
     */
    @JvmStatic
    @Suppress("unused")
    fun createDefault(preferredAudioCaptureDeviceId: String?, preferredVideoCaptureDeviceId: String?): RtcEngine = try {
        WebRtcJavaEngine(preferredAudioCaptureDeviceId, preferredVideoCaptureDeviceId)
    } catch (error: Throwable) {
        error.printStackTrace()
        RtcFileLogger.error("Failed to initialize webrtc-java provider", error)
        NoOpRtcEngine(buildFailureMessage(error))
    }

    fun createDefault(configuration: com.shterneregen.securelan.webrtc.service.RtcMediaConfiguration): RtcEngine = try {
        WebRtcJavaEngine(configuration.microphoneId, configuration.cameraId, configuration.speakerId)
    } catch (error: Throwable) {
        RtcFileLogger.error("Failed to initialize selected media devices", error)
        NoOpRtcEngine(buildFailureMessage(error))
    }

    private fun buildFailureMessage(error: Throwable): String {
        var root = error
        while (root.cause != null && root.cause !== root) {
            root = root.cause!!
        }

        val outer = error.javaClass.simpleName + (error.message?.let { ": $it" } ?: "")
        val inner = root.javaClass.simpleName + (root.message?.let { ": $it" } ?: "")

        return "Failed to initialize webrtc-java provider: $outer | root cause: $inner"
    }
}
