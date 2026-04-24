package com.shterneregen.securelan.webrtc.service;

public record RtcMediaDevice(String id, String name, boolean defaultDevice) {
    public RtcMediaDevice {
        id = id == null ? "" : id.trim();
        name = name == null || name.isBlank() ? "Unknown media device" : name.trim();
    }
}
