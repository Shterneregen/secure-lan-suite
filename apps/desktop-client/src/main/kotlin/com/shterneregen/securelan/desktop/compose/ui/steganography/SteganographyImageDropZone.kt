package com.shterneregen.securelan.desktop.compose.ui.steganography

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.LocalSecureLanDesignTokens
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButton
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButtonTone
import com.shterneregen.securelan.desktop.compose.util.isSupportedImagePath
import com.shterneregen.securelan.desktop.compose.util.toPreviewImageBitmap
import java.net.URI
import java.nio.file.Path

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun SteganographyImageDropZone(
    label: String,
    value: String,
    emptyHint: String,
    onChoose: () -> Unit,
    onPaste: () -> Unit,
    onImageSelected: (Path) -> Unit,
    enabled: Boolean,
) {
    val tokens = LocalSecureLanDesignTokens.current
    var dragActive by remember { mutableStateOf(false) }
    val target = remember(onImageSelected) {
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
                val droppedPath = runCatching {
                    (event.dragData() as? DragData.FilesList)
                        ?.readFiles()
                        ?.asSequence()
                        ?.mapNotNull { uri -> runCatching { Path.of(URI.create(uri)) }.getOrNull() }
                        ?.firstOrNull(::isSupportedImagePath)
                }.getOrNull() ?: return false
                onImageSelected(droppedPath)
                return true
            }
        }
    }
    val preview = remember(value) {
        value.trim().takeIf(String::isNotEmpty)
            ?.let { runCatching { Path.of(it) }.getOrNull() }
            ?.toPreviewImageBitmap()
    }
    val selectedName = remember(value) {
        value.trim().takeIf(String::isNotEmpty)
            ?.let { runCatching { Path.of(it).fileName?.toString() }.getOrNull() }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.subtitle2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.86f),
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (preview == null) 116.dp else 88.dp)
                .dragAndDropTarget(
                    shouldStartDragAndDrop = { event ->
                        enabled && runCatching { event.dragData() is DragData.FilesList }.getOrDefault(false)
                    },
                    target = target,
                )
                .semantics { contentDescription = "$label image drop zone" },
            shape = RoundedCornerShape(tokens.radius.medium),
            border = BorderStroke(
                width = if (dragActive) 2.dp else 1.dp,
                color = if (dragActive) MaterialTheme.colors.primary else tokens.colors.borderSubtle,
            ),
            color = if (dragActive) {
                MaterialTheme.colors.primary.copy(alpha = 0.10f)
            } else {
                tokens.colors.surfaceLevel2.copy(alpha = 0.64f)
            },
        ) {
            Row(
                modifier = Modifier.padding(tokens.spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (preview != null) {
                    Image(
                        bitmap = preview,
                        contentDescription = "Selected image preview",
                        modifier = Modifier.size(72.dp),
                        contentScale = ContentScale.Crop,
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
                ) {
                    Text(
                        text = selectedName ?: if (dragActive) "Drop the image here" else emptyHint,
                        style = MaterialTheme.typography.body2,
                        color = if (dragActive) {
                            MaterialTheme.colors.primary
                        } else {
                            MaterialTheme.colors.onSurface.copy(alpha = 0.74f)
                        },
                    )
                    if (value.isNotBlank()) {
                        Text(
                            text = value,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.54f),
                        )
                    } else {
                        Text(
                            text = "PNG, JPEG, GIF, or BMP · drag and drop supported",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.54f),
                        )
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        CompactButton(onClick = onChoose, enabled = enabled) { Text("Choose image") }
                        CompactButton(
                            onClick = onPaste,
                            enabled = enabled,
                            tone = CompactButtonTone.TERTIARY,
                        ) {
                            Text("Paste image")
                        }
                    }
                }
            }
        }
    }
}
