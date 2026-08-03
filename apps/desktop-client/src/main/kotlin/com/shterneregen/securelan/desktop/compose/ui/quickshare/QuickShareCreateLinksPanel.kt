package com.shterneregen.securelan.desktop.compose.ui.quickshare

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.state.quickshare.ComposeQuickShareState
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButton
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButtonTone
import com.shterneregen.securelan.desktop.compose.ui.icons.SecureLanIcons
import com.shterneregen.securelan.desktop.compose.util.calmFocusRing
import com.shterneregen.securelan.desktop.compose.util.rememberInteractiveSurfaceState
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path

private enum class LinkCreationMode { FILE, TEXT }

@Composable
internal fun QuickShareCreateLinksPanel(
    state: ComposeQuickShareState,
    filePath: String,
    onChooseFile: () -> Unit,
    onFileDropped: (Path) -> Unit,
    onCreateFile: () -> Unit,
    textDraft: String,
    onTextDraftChange: (String) -> Unit,
    onCreateText: () -> Unit,
) {
    val tokens = LocalSecureLanDesignTokens.current
    var mode by remember { mutableStateOf(LinkCreationMode.FILE) }
    val serverRunning = state.running

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Create a link",
            style = MaterialTheme.typography.subtitle2,
            color = tokens.colors.textPrimary,
        )

        ModeSelector(
            selected = mode,
            onSelect = { mode = it },
        )

        Crossfade(targetState = mode, label = "QuickShareCreateMode") { currentMode ->
            when (currentMode) {
            LinkCreationMode.FILE -> FileLinkForm(
                state = state,
                filePath = filePath,
                onChooseFile = onChooseFile,
                onFileDropped = onFileDropped,
                onCreateFile = onCreateFile,
            )

            LinkCreationMode.TEXT -> TextLinkForm(
                state = state,
                textDraft = textDraft,
                onTextDraftChange = onTextDraftChange,
                onCreateText = onCreateText,
            )
            }
        }

        Text(
            text = state.linkPolicySummary,
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.48f),
        )

        if (!serverRunning) {
            InlineGuidance(text = state.createLinkHint)
        }
    }
}

@Composable
private fun ModeSelector(
    selected: LinkCreationMode,
    onSelect: (LinkCreationMode) -> Unit,
) {
    val tokens = LocalSecureLanDesignTokens.current
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = 30.dp),
        shape = RoundedCornerShape(tokens.radius.medium),
        color = tokens.colors.surfaceLevel2.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, tokens.colors.borderSubtle.copy(alpha = 0.48f)),
    ) {
        Row(modifier = Modifier.padding(2.dp)) {
            ModeButton(
                label = "File link",
                selected = selected == LinkCreationMode.FILE,
                onClick = { onSelect(LinkCreationMode.FILE) },
                modifier = Modifier.weight(1f),
            )
            ModeButton(
                label = "Text link",
                selected = selected == LinkCreationMode.TEXT,
                onClick = { onSelect(LinkCreationMode.TEXT) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ModeButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (interactionSource, interactive) = rememberInteractiveSurfaceState(selected = selected)
    val tokens = LocalSecureLanDesignTokens.current
    val background = if (selected) {
        tokens.colors.surfaceLevel3
    } else {
        interactive.backgroundColor.copy(alpha = if (interactive.hovered || interactive.focused) 1f else 0f)
    }
    val contentColor = if (selected) tokens.colors.textPrimary else tokens.colors.textSecondary
    Surface(
        modifier = modifier
            .calmFocusRing(interactive.focused, tokens.radius.small)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Tab,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(tokens.radius.small),
        color = background,
        border = if (selected) BorderStroke(1.dp, tokens.colors.borderSubtle.copy(alpha = 0.56f)) else null,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.body2,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun FileLinkForm(
    state: ComposeQuickShareState,
    filePath: String,
    onChooseFile: () -> Unit,
    onFileDropped: (Path) -> Unit,
    onCreateFile: () -> Unit,
) {
    val tokens = LocalSecureLanDesignTokens.current
    var dragActive by remember { mutableStateOf(false) }
    val dropTarget = remember(onFileDropped) {
        object : DragAndDropTarget {
            override fun onEntered(event: DragAndDropEvent) {
                dragActive = true
            }

            override fun onExited(event: DragAndDropEvent) {
                dragActive = false
            }

            override fun onEnded(event: DragAndDropEvent) {
                dragActive = false
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                dragActive = false
                val droppedFile = runCatching {
                    (event.dragData() as? DragData.FilesList)
                        ?.readFiles()
                        ?.asSequence()
                        ?.mapNotNull { uri -> runCatching { Path.of(URI.create(uri)) }.getOrNull() }
                        ?.firstOrNull(Files::isRegularFile)
                }.getOrNull() ?: return false
                onFileDropped(droppedFile)
                return true
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 58.dp)
                .dragAndDropTarget(
                    shouldStartDragAndDrop = { event ->
                        runCatching { event.dragData() is DragData.FilesList }.getOrDefault(false)
                    },
                    target = dropTarget,
                ),
            shape = RoundedCornerShape(tokens.radius.small),
            border = BorderStroke(
                width = if (dragActive) 2.dp else 1.dp,
                color = if (dragActive) MaterialTheme.colors.primary else tokens.colors.borderSubtle.copy(alpha = 0.48f),
            ),
            color = if (dragActive) {
                MaterialTheme.colors.primary.copy(alpha = 0.10f)
            } else {
                tokens.colors.surfaceLevel2.copy(alpha = 0.48f)
            },
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = SecureLanIcons.File,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.56f),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (dragActive) "Drop file here" else state.selectedFileName.ifBlank { "No file selected" },
                        style = MaterialTheme.typography.body2,
                        color = when {
                            dragActive -> MaterialTheme.colors.primary
                            state.hasSelectedFile -> MaterialTheme.colors.onSurface
                            else -> MaterialTheme.colors.onSurface.copy(alpha = 0.54f)
                        },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!state.hasSelectedFile) {
                        Text(
                            text = "Drag and drop a file here, or choose one",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.44f),
                        )
                    }
                }
                CompactButton(
                    onClick = onChooseFile,
                    tone = CompactButtonTone.TERTIARY,
                ) { Text(if (state.hasSelectedFile) "Change" else "Choose") }
            }
        }

        if (state.hasSelectedFile) {
            Text(
                text = filePath,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.48f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        CompactButton(
            onClick = onCreateFile,
            enabled = state.canCreateFileLinkNow,
            modifier = Modifier.fillMaxWidth().heightIn(min = 34.dp),
        ) {
            Text(
                text = when {
                    !state.hasSelectedFile -> "Choose a file first"
                    else -> "Create file link"
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TextLinkForm(
    state: ComposeQuickShareState,
    textDraft: String,
    onTextDraftChange: (String) -> Unit,
    onCreateText: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = textDraft,
            onValueChange = onTextDraftChange,
            label = { Text("Text to share") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4,
        )
        CompactButton(
            onClick = onCreateText,
            enabled = state.canCreateTextLinkNow,
            modifier = Modifier.fillMaxWidth().heightIn(min = 34.dp),
        ) {
            Text(
                text = when {
                    !state.hasText -> "Enter text first"
                    else -> "Create text link"
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun InlineGuidance(text: String) {
    val tokens = LocalSecureLanDesignTokens.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(tokens.radius.small),
        color = tokens.colors.surfaceLevel2.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, tokens.colors.borderSubtle.copy(alpha = 0.40f)),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.64f),
        )
    }
}
