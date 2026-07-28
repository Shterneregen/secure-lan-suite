package com.shterneregen.securelan.desktop.ui

import com.shterneregen.securelan.webrtc.service.RtcMediaDevice

@JvmRecord
data class MediaDeviceChoice(
    val deviceId: String,
    val label: String,
    val systemDefault: Boolean,
    val defaultDevice: Boolean,
) {
    fun matches(selectedDeviceId: String?): Boolean = deviceId == selectedDeviceId.orEmpty().trim()

    override fun toString(): String = when {
        systemDefault -> label
        defaultDevice -> "$label (default)"
        else -> label
    }

    companion object {
        @JvmStatic
        fun systemDefault(label: String): MediaDeviceChoice = MediaDeviceChoice("", label, true, true)

        @JvmStatic
        fun of(device: RtcMediaDevice): MediaDeviceChoice =
            MediaDeviceChoice(device.id(), device.name(), false, device.defaultDevice())
    }
}
