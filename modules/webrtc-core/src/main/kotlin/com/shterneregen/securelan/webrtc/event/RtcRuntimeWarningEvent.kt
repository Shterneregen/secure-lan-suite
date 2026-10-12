package com.shterneregen.securelan.webrtc.event

/**
 * Event published when non-fatal runtime warnings, diagnostic messages, or fallback
 * notifications occur in the WebRTC subsystem.
 *
 * @property message diagnostic or warning message text.
 */
@JvmRecord
data class RtcRuntimeWarningEvent(val message: String?) : RtcEvent
