package com.shterneregen.securelan.webrtc.runtime;

public record RtcRuntimeStatus(String providerName, boolean available, String message) {
    public static RtcRuntimeStatus unavailable(String message) {
        return new RtcRuntimeStatus("unconfigured", false, message);
    }
}
