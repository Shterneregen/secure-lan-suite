package com.shterneregen.securelan.desktop.compose.ui.media

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.motionTween
import com.shterneregen.securelan.desktop.compose.state.media.ComposeExperimentalVideoState
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButton
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButtonTone
import com.shterneregen.securelan.desktop.compose.util.toPreviewImageBitmap
import com.shterneregen.securelan.webrtc.event.RtcVideoFrameEvent
import java.awt.Dimension

@Composable
internal fun ComposeVideoStage(state: ComposeExperimentalVideoState) {
    var fillVideo by remember { mutableStateOf(false) }
    var localIsPrimary by remember { mutableStateOf(false) }
    var detailsVisible by remember { mutableStateOf(false) }
    var expandedWindowOpen by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            VideoStageSurface(
                state = state,
                fillVideo = fillVideo,
                localIsPrimary = localIsPrimary,
                expanded = false,
                onToggleScale = { fillVideo = !fillVideo },
                onSwap = { localIsPrimary = !localIsPrimary },
                onToggleDetails = { detailsVisible = !detailsVisible },
                onExpand = { expandedWindowOpen = true },
                modifier = Modifier.fillMaxWidth().height(270.dp),
            )
            AnimatedVisibility(
                visible = detailsVisible,
                enter = fadeIn(motionTween()) + expandVertically(motionTween()),
                exit = shrinkVertically(motionTween()) + fadeOut(motionTween()),
            ) {
                CallDetails(state)
            }
        }
    }

    if (expandedWindowOpen) {
        ExpandedVideoWindow(
            state = state,
            fillVideo = fillVideo,
            localIsPrimary = localIsPrimary,
            onToggleScale = { fillVideo = !fillVideo },
            onSwap = { localIsPrimary = !localIsPrimary },
            onClose = { expandedWindowOpen = false },
        )
    }
}

@Composable
private fun VideoStageSurface(
    state: ComposeExperimentalVideoState,
    fillVideo: Boolean,
    localIsPrimary: Boolean,
    expanded: Boolean,
    onToggleScale: () -> Unit,
    onSwap: () -> Unit,
    onToggleDetails: (() -> Unit)?,
    onExpand: () -> Unit,
    modifier: Modifier,
) {
    val primaryFrame = if (localIsPrimary) state.localFrame else state.remoteFrame
    val secondaryFrame = if (localIsPrimary) state.remoteFrame else state.localFrame
    val primaryTitle = if (localIsPrimary) "You" else state.selectedPeerName
    val secondaryTitle = if (localIsPrimary) state.selectedPeerName else "You"
    val primaryPlaceholder = if (localIsPrimary) state.localFrameCaption else state.remoteFrameCaption
    val secondaryPlaceholder = if (localIsPrimary) state.remoteFrameCaption else state.localFrameCaption
    val contentScale = if (fillVideo) ContentScale.Crop else ContentScale.Fit

    Surface(
        modifier = modifier,
        color = MaterialTheme.colors.background,
        shape = RoundedCornerShape(LocalSecureLanDesignTokens.current.radius.medium),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            VideoFrameSurface(
                title = primaryTitle,
                frame = primaryFrame,
                placeholder = primaryPlaceholder,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
            )
            VideoFrameSurface(
                title = secondaryTitle,
                frame = secondaryFrame,
                placeholder = secondaryPlaceholder,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .width(if (expanded) 280.dp else 190.dp)
                    .height(if (expanded) 170.dp else 112.dp),
            )
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
                color = MaterialTheme.colors.surface.copy(alpha = 0.90f),
                shape = RoundedCornerShape(LocalSecureLanDesignTokens.current.radius.medium),
            ) {
                FlowRow(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    CompactButton(onClick = onToggleScale, tone = CompactButtonTone.TERTIARY) {
                        Text(if (fillVideo) "Fit" else "Fill")
                    }
                    CompactButton(
                        onClick = onSwap,
                        enabled = state.localFrame != null && state.remoteFrame != null,
                        tone = CompactButtonTone.TERTIARY,
                    ) {
                        Text("Swap")
                    }
                    if (onToggleDetails != null) {
                        CompactButton(onClick = onToggleDetails, tone = CompactButtonTone.TERTIARY) {
                            Text("Details")
                        }
                    }
                    CompactButton(onClick = onExpand, tone = CompactButtonTone.TERTIARY) {
                        Text(if (expanded) "Close" else "Expand")
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpandedVideoWindow(
    state: ComposeExperimentalVideoState,
    fillVideo: Boolean,
    localIsPrimary: Boolean,
    onToggleScale: () -> Unit,
    onSwap: () -> Unit,
    onClose: () -> Unit,
) {
    DialogWindow(
        onCloseRequest = onClose,
        state = rememberDialogState(size = DpSize(1100.dp, 720.dp)),
        title = "SecureLanSuite · ${state.stageTitle}",
        resizable = true,
        onPreviewKeyEvent = { event ->
            if (event.key == Key.Escape && event.type == KeyEventType.KeyUp) {
                onClose()
                true
            } else {
                false
            }
        },
    ) {
        LaunchedEffect(window) {
            window.minimumSize = Dimension(760, 520)
        }
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colors.background,
        ) {
            VideoStageSurface(
                state = state,
                fillVideo = fillVideo,
                localIsPrimary = localIsPrimary,
                expanded = true,
                onToggleScale = onToggleScale,
                onSwap = onSwap,
                onToggleDetails = null,
                onExpand = onClose,
                modifier = Modifier.fillMaxSize().padding(12.dp),
            )
        }
    }
}

@Composable
private fun CallDetails(state: ComposeExperimentalVideoState) {
    val tokens = LocalSecureLanDesignTokens.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Media: ${state.mediaLabel}",
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.68f),
        )
        Text(
            state.previewStatus,
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.68f),
        )
        Text(
            state.frameCaption,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.60f),
            maxLines = 1,
        )
    }
}

@Composable
private fun VideoFrameSurface(
    title: String,
    frame: RtcVideoFrameEvent?,
    placeholder: String,
    contentScale: ContentScale,
    modifier: Modifier,
) {
    val image: ImageBitmap? = remember(frame) { frame?.toPreviewImageBitmap() }
    if (image != null) {
        Surface(
            modifier = modifier,
            color = MaterialTheme.colors.background,
            shape = RoundedCornerShape(LocalSecureLanDesignTokens.current.radius.small),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    bitmap = image,
                    contentDescription = "$title live video frame",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale,
                )
                VideoLabel(title, Modifier.align(Alignment.TopStart))
            }
        }
    } else {
        VideoSurfacePlaceholder(title, placeholder, modifier)
    }
}

@Composable
private fun VideoLabel(
    title: String,
    modifier: Modifier,
) {
    Surface(
        modifier = modifier.padding(8.dp),
        color = MaterialTheme.colors.surface.copy(alpha = 0.82f),
        shape = RoundedCornerShape(LocalSecureLanDesignTokens.current.radius.small),
    ) {
        Text(
            title,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.caption,
        )
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
        shape = RoundedCornerShape(LocalSecureLanDesignTokens.current.radius.small),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                title,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.76f),
            )
            Text(
                body,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.60f),
            )
        }
    }
}
