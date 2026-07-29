package com.shterneregen.securelan.desktop.compose.ui.media

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.motionTween
import com.shterneregen.securelan.desktop.compose.state.media.ComposeExperimentalVideoState
import com.shterneregen.securelan.desktop.compose.state.media.ComposeVideoPreviewCorner
import com.shterneregen.securelan.desktop.compose.state.media.settleVideoPreviewCorner
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButton
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButtonTone
import com.shterneregen.securelan.desktop.compose.util.toPreviewImageBitmap
import com.shterneregen.securelan.webrtc.event.RtcVideoFrameEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.awt.Dimension

private const val VIDEO_CONTROLS_HIDE_DELAY_MS = 2_000L

@Composable
internal fun ComposeVideoStage(
    state: ComposeExperimentalVideoState,
    modifier: Modifier = Modifier,
) {
    var fillVideo by remember { mutableStateOf(false) }
    var localIsPrimary by remember { mutableStateOf(false) }
    var detailsVisible by remember { mutableStateOf(false) }
    var expandedWindowOpen by remember { mutableStateOf(false) }
    var previewCorner by remember { mutableStateOf(ComposeVideoPreviewCorner.BOTTOM_END) }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(10.dp),
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
                previewCorner = previewCorner,
                onPreviewCornerChange = { previewCorner = it },
                modifier = Modifier.fillMaxWidth().weight(1f),
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
            previewCorner = previewCorner,
            onPreviewCornerChange = { previewCorner = it },
            onClose = { expandedWindowOpen = false },
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
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
    previewCorner: ComposeVideoPreviewCorner,
    onPreviewCornerChange: (ComposeVideoPreviewCorner) -> Unit,
    modifier: Modifier,
) {
    val primaryFrame = if (localIsPrimary) state.localFrame else state.remoteFrame
    val secondaryFrame = if (localIsPrimary) state.remoteFrame else state.localFrame
    val primaryTitle = if (localIsPrimary) "You" else state.selectedPeerName
    val secondaryTitle = if (localIsPrimary) state.selectedPeerName else "You"
    val primaryPlaceholder = if (localIsPrimary) state.localFrameCaption else state.remoteFrameCaption
    val secondaryPlaceholder = if (localIsPrimary) state.remoteFrameCaption else state.localFrameCaption
    val contentScale = if (fillVideo) ContentScale.Crop else ContentScale.Fit
    var controlsVisible by remember { mutableStateOf(true) }
    var previewDragX by remember { mutableFloatStateOf(0f) }
    var previewDragY by remember { mutableFloatStateOf(0f) }
    val controlsScope = rememberCoroutineScope()
    val hideControlsJob = remember { arrayOfNulls<Job>(1) }
    val previewAlignment = when (previewCorner) {
        ComposeVideoPreviewCorner.TOP_START -> Alignment.TopStart
        ComposeVideoPreviewCorner.TOP_END -> Alignment.TopEnd
        ComposeVideoPreviewCorner.BOTTOM_START -> Alignment.BottomStart
        ComposeVideoPreviewCorner.BOTTOM_END -> Alignment.BottomEnd
    }

    fun revealControls() {
        controlsVisible = true
        hideControlsJob[0]?.cancel()
        hideControlsJob[0] = controlsScope.launch {
            delay(VIDEO_CONTROLS_HIDE_DELAY_MS)
            controlsVisible = false
        }
    }

    LaunchedEffect(Unit) {
        delay(VIDEO_CONTROLS_HIDE_DELAY_MS)
        controlsVisible = false
    }

    Surface(
        modifier = modifier
            .onPointerEvent(PointerEventType.Enter) {
                revealControls()
            }
            .onPointerEvent(PointerEventType.Move) {
                revealControls()
            }
            .onPointerEvent(PointerEventType.Exit) {
                controlsVisible = false
            },
        color = MaterialTheme.colors.background,
        shape = RoundedCornerShape(LocalSecureLanDesignTokens.current.radius.medium),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            VideoFrameSurface(
                title = primaryTitle,
                frame = primaryFrame,
                placeholder = primaryPlaceholder,
                contentScale = contentScale,
                labelAlignment = if (previewCorner == ComposeVideoPreviewCorner.TOP_START) {
                    Alignment.TopEnd
                } else {
                    Alignment.TopStart
                },
                modifier = Modifier.fillMaxSize(),
            )
            VideoFrameSurface(
                title = secondaryTitle,
                frame = secondaryFrame,
                placeholder = secondaryPlaceholder,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(previewAlignment)
                    .padding(12.dp)
                    .width(if (expanded) 280.dp else 190.dp)
                    .height(if (expanded) 170.dp else 112.dp)
                    .graphicsLayer {
                        translationX = previewDragX
                        translationY = previewDragY
                    }
                    .pointerInput(previewCorner) {
                        detectDragGestures(
                            onDragEnd = {
                                onPreviewCornerChange(
                                    settleVideoPreviewCorner(
                                        current = previewCorner,
                                        dragX = previewDragX,
                                        dragY = previewDragY,
                                    )
                                )
                                previewDragX = 0f
                                previewDragY = 0f
                            },
                            onDragCancel = {
                                previewDragX = 0f
                                previewDragY = 0f
                            },
                        ) { change, dragAmount ->
                            change.consume()
                            previewDragX += dragAmount.x
                            previewDragY += dragAmount.y
                            revealControls()
                        }
                    },
            )
            AnimatedVisibility(
                visible = controlsVisible,
                modifier = Modifier.align(Alignment.TopCenter).padding(10.dp),
                enter = fadeIn(motionTween()),
                exit = fadeOut(motionTween()),
            ) {
                Surface(
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
}

@Composable
private fun ExpandedVideoWindow(
    state: ComposeExperimentalVideoState,
    fillVideo: Boolean,
    localIsPrimary: Boolean,
    onToggleScale: () -> Unit,
    onSwap: () -> Unit,
    previewCorner: ComposeVideoPreviewCorner,
    onPreviewCornerChange: (ComposeVideoPreviewCorner) -> Unit,
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
                previewCorner = previewCorner,
                onPreviewCornerChange = onPreviewCornerChange,
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
    labelAlignment: Alignment = Alignment.TopStart,
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
                VideoLabel(title, Modifier.align(labelAlignment))
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
