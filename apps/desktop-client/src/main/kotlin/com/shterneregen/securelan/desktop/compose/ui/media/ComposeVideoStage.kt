package com.shterneregen.securelan.desktop.compose.ui.media

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.state.media.ComposeExperimentalVideoState
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.util.toPreviewImageBitmap
import com.shterneregen.securelan.webrtc.event.RtcVideoFrameEvent

@Composable
internal fun ComposeVideoStage(state: ComposeExperimentalVideoState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(state.stageTitle, style = MaterialTheme.typography.subtitle1)
                    Text(
                        state.stageFrameCaption,
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f)
                    )
                }
                Text(state.stageBadge, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.primary)
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Participants: ${state.selectedPeerName}", style = MaterialTheme.typography.caption)
                Text("Media: ${state.mediaLabel}", style = MaterialTheme.typography.caption)
                Text("Preview: ${state.previewStatus}", style = MaterialTheme.typography.caption)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                VideoFrameSurface(
                    title = "Remote video",
                    frame = state.remoteFrame,
                    placeholder = state.remoteFrameCaption,
                    modifier = Modifier.weight(1f).height(180.dp),
                )
                VideoFrameSurface(
                    title = "Local preview",
                    frame = state.localFrame,
                    placeholder = state.localFrameCaption,
                    modifier = Modifier.width(220.dp).height(150.dp),
                )
            }
        }
    }
}

@Composable
private fun VideoFrameSurface(
    title: String,
    frame: RtcVideoFrameEvent?,
    placeholder: String,
    modifier: Modifier,
) {
    val image = remember(frame) { frame?.toPreviewImageBitmap() }
    if (image != null) {
        Surface(
            modifier = modifier,
            color = MaterialTheme.colors.background,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    bitmap = image,
                    contentDescription = "$title live video frame",
                    modifier = Modifier.fillMaxSize(),
                )
                Surface(
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                    color = MaterialTheme.colors.surface.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(LocalSecureLanDesignTokens.current.radius.small),
                ) {
                    Text(
                        title,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.caption,
                    )
                }
            }
        }
    } else {
        VideoSurfacePlaceholder(title, placeholder, modifier)
    }
}

@Composable
internal fun VideoSurfacePlaceholder(
    title: String,
    body: String,
    modifier: Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colors.background,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                title,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f)
            )
            Text(
                body,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.56f)
            )
        }
    }
}
