package com.shterneregen.securelan.desktop.compose.ui.media

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.SecureLanTheme
import com.shterneregen.securelan.desktop.compose.state.connection.ComposeStatusConnectionState
import com.shterneregen.securelan.desktop.compose.state.media.ComposeExperimentalVideoState
import com.shterneregen.securelan.desktop.compose.state.peer.ComposePeerListState
import com.shterneregen.securelan.webrtc.event.RtcVideoFrameEvent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class VideoStageInteractionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun bothVideoSurfacesKeepUpdatingAcrossFrameSizeAndRotationChanges() {
        val state = mutableStateOf(ComposeExperimentalVideoState(
            ComposeStatusConnectionState(), ComposePeerListState(peers = emptyList()),
        ))
        compose.setContent {
            SecureLanTheme { ComposeVideoStage(state.value, Modifier.size(900.dp, 600.dp)) }
        }
        repeat(90) { index ->
            val localColor = if (index % 2 == 0) 0xff2196f3.toInt() else 0xff4caf50.toInt()
            val remoteColor = if (index % 2 == 0) 0xffff9800.toInt() else 0xffe91e63.toInt()
            compose.runOnIdle {
                state.value = state.value.copy(
                    latestLocalVideoFrame = frame(true, localColor, if (index < 30) 320 else 640, 0),
                    latestRemoteVideoFrame = frame(false, remoteColor, 640, if (index < 60) 270 else 0),
                )
            }
            compose.waitUntil(2000) {
                pixel("You live video frame") == localColor && pixel("No peer selected live video frame") == remoteColor
            }
        }
        compose.runOnIdle {
            assertEquals(640, state.value.latestLocalVideoFrame!!.width())
        }
    }

    private fun pixel(description: String): Int? = runCatching {
        val image = compose.onNodeWithContentDescription(description).captureToImage()
        IntArray(1).also { image.readPixels(it, image.width / 2, image.height / 2, 1, 1) }[0]
    }.getOrNull()

    private fun frame(local: Boolean, color: Int, width: Int, rotation: Int): RtcVideoFrameEvent {
        val height = width * 3 / 4
        val pixels = ByteArray(width * height * 4) { offset -> (color ushr ((offset % 4) * 8)).toByte() }
        return RtcVideoFrameEvent("call", if (local) "desktop" else "phone", local, width, height, rotation, pixels)
    }
}
