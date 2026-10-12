package com.shterneregen.securelan.webrtc.runtime

/**
 * Value record describing the availability and health of the active WebRTC engine.
 *
 * @property providerName name of the active provider (e.g. "webrtc-java" or "unconfigured").
 * @property available `true` if native WebRTC capabilities are initialized and operational.
 * @property message informational or diagnostic message describing provider readiness or failure causes.
 */
@JvmRecord
data class RtcRuntimeStatus(
    val providerName: String?,
    val available: Boolean,
    val message: String?,
) {
    companion object {
        /**
         * Creates an unavailable status descriptor with the provided diagnostic [message].
         *
         * @param message failure explanation or setup instructions.
         * @return an unavailable [RtcRuntimeStatus] instance.
         */
        @JvmStatic
        fun unavailable(message: String?): RtcRuntimeStatus = RtcRuntimeStatus("unconfigured", false, message)
    }
}
