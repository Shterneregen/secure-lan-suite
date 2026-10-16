package com.shterneregen.securelan.desktop.compose

import com.shterneregen.securelan.desktop.compose.state.connection.ComposeStatusConnectionState
import com.shterneregen.securelan.desktop.compose.state.media.ComposeExperimentalVideoState
import com.shterneregen.securelan.desktop.compose.state.media.ComposeMediaVoiceState
import com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DesktopMediaSelectionTest {
    @Test fun missingOrStillLoadingDeviceDoesNotSilentlyBecomeSystemDefault() {
        val connection = ComposeStatusConnectionState()
        val peers = ComposePeerListState()
        val voice = ComposeMediaVoiceState(connection, peers, selectedMicrophoneId = "mic-usb", selectedOutputDeviceId = "speaker-usb")
        val video = ComposeExperimentalVideoState(connection, peers, selectedCameraId = "camera-usb")
        assertEquals("mic-usb", voice.selectedMicrophone.deviceId)
        assertEquals("speaker-usb", voice.selectedOutputDevice.deviceId)
        assertEquals("camera-usb", video.selectedCamera.deviceId)
        assertTrue(voice.selectedMicrophone.label.contains("unavailable"))
        assertFalse(voice.selectedOutputDevice.systemDefault)
        assertFalse(video.selectedCamera.systemDefault)
    }
}
