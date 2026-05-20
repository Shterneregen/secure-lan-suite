package com.shterneregen.securelan.webrtc.runtime

@JvmRecord
data class RtcRuntimeStatus(
    val providerName: String?,
    val available: Boolean,
    val message: String?,
) {
    companion object {
        @JvmStatic
        fun unavailable(message: String?): RtcRuntimeStatus = RtcRuntimeStatus("unconfigured", false, message)
    }
}
