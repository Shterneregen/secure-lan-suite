package com.shterneregen.securelan.webrtc.runtime

object RtcEngineProvider {
    @JvmStatic
    fun createDefault(): RtcEngine = try {
        WebRtcJavaEngine()
    } catch (error: Throwable) {
        error.printStackTrace()
        RtcFileLogger.error("Failed to initialize webrtc-java provider", error)
        NoOpRtcEngine(buildFailureMessage(error))
    }

    @JvmStatic
    fun createDefault(preferredAudioCaptureDeviceId: String?, preferredVideoCaptureDeviceId: String?): RtcEngine = try {
        WebRtcJavaEngine(preferredAudioCaptureDeviceId, preferredVideoCaptureDeviceId)
    } catch (error: Throwable) {
        error.printStackTrace()
        RtcFileLogger.error("Failed to initialize webrtc-java provider", error)
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
