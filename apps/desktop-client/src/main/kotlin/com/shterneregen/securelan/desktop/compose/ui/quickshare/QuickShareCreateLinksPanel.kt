package com.shterneregen.securelan.desktop.compose.ui.quickshare

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

private enum class LinkCreationMode { FILE, TEXT }

@Composable
internal fun QuickShareCreateLinksPanel(
    state: ComposeQuickShareState,
    filePath: String,
    onChooseFile: () -> Unit,
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

@Composable
private fun FileLinkForm(
    state: ComposeQuickShareState,
    filePath: String,
    onChooseFile: () -> Unit,
    onCreateFile: () -> Unit,
) {
    val tokens = LocalSecureLanDesignTokens.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(min = 38.dp),
            shape = RoundedCornerShape(tokens.radius.small),
            border = BorderStroke(1.dp, tokens.colors.borderSubtle.copy(alpha = 0.48f)),
            color = tokens.colors.surfaceLevel2.copy(alpha = 0.48f),
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
                Text(
                    text = state.selectedFileName.ifBlank { "No file selected" },
                    style = MaterialTheme.typography.body2,
                    color = if (state.hasSelectedFile) {
                        MaterialTheme.colors.onSurface
                    } else {
                        MaterialTheme.colors.onSurface.copy(alpha = 0.42f)
                    },
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
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
                    !state.running -> "Start sharing first"
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
                    !state.running -> "Start sharing first"
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
