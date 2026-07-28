package com.shterneregen.securelan.desktop.compose.ui.steganography

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shterneregen.securelan.desktop.compose.state.steganography.ComposeSteganographyState
import com.shterneregen.securelan.desktop.compose.ui.components.CompactButton
import java.nio.file.Path

@Composable
internal fun SteganographyHidePanel(
    state: ComposeSteganographyState,
    coverPath: String,
    onChooseCover: () -> Unit,
    onPasteCover: () -> Unit,
    onCoverSelected: (Path) -> Unit,
    outputPath: String,
    onOutputPathChange: (String) -> Unit,
    onChooseOutput: () -> Unit,
    message: String,
    onMessageChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    encrypt: Boolean,
    onEncryptChange: (Boolean) -> Unit,
    onHide: () -> Unit,
    savedOutput: Path?,
    onOpenOutputFolder: () -> Unit,
    onSendOutput: (() -> Unit)?,
    sendOutputLabel: String?,
    previewOnly: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SteganographyImageDropZone(
            label = "Cover image",
            value = coverPath,
            emptyHint = "Choose an image to hide a message in",
            onChoose = onChooseCover,
            onPaste = onPasteCover,
            onImageSelected = onCoverSelected,
            enabled = !previewOnly,
        )

        if (state.capacity != null) {
            SteganographyInfoSurface(
                title = "Message capacity",
                body = state.capacityText,
                positive = state.messageFitsCapacity,
            )
        }

        OutlinedTextField(
            value = message,
            onValueChange = onMessageChange,
            label = { Text("Message to hide") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            maxLines = 8,
            isError = !state.messageFitsCapacity,
        )
        Text(
            text = state.messageCapacityLabel,
            modifier = Modifier.align(Alignment.End),
            style = MaterialTheme.typography.caption,
            color = if (state.messageFitsCapacity) {
                MaterialTheme.colors.onSurface.copy(alpha = 0.56f)
            } else {
                MaterialTheme.colors.error
            },
        )

        SteganographyPasswordRow(
            checked = encrypt,
            onCheckedChange = onEncryptChange,
            label = "Protect hidden message with a password",
            password = password,
            onPasswordChange = onPasswordChange,
        )

        SteganographyFileRow(
            value = outputPath,
            onValueChange = onOutputPathChange,
            label = "Output BMP",
            placeholder = "Suggested after the cover is selected",
            buttonText = "Choose location",
            onChoose = onChooseOutput,
            enabled = !previewOnly,
        )

        val error = steganographyStatusIsError(state.statusText)
        val hint = hideReadinessHint(state)
        SteganographyWorkflowStatus(
            label = when {
                savedOutput != null -> "Completed"
                error -> "Error"
                state.canHideMessage -> "Ready"
                else -> "Needs input"
            },
            detail = savedOutput?.let { "Hidden message saved to ${it.fileName}." } ?: hint,
            error = error,
            completed = savedOutput != null,
            ready = state.canHideMessage,
        )

        Button(
            onClick = onHide,
            enabled = !previewOnly && state.canHideMessage,
            modifier = Modifier.align(Alignment.End),
        ) {
            Text("Hide message")
        }

        if (savedOutput != null) {
            SteganographyInfoSurface(
                title = "Stego BMP created",
                body = savedOutput.toString(),
                positive = true,
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CompactButton(onClick = onOpenOutputFolder) { Text("Open folder") }
                    if (onSendOutput != null && !sendOutputLabel.isNullOrBlank()) {
                        CompactButton(onClick = onSendOutput) { Text(sendOutputLabel) }
                    }
                }
            }
        }

        Text(
            text = "The source image is not modified. The result is always saved as BMP.",
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.56f),
        )
    }
}

private fun hideReadinessHint(state: ComposeSteganographyState): String = when {
    steganographyStatusIsError(state.statusText) -> state.statusText
    !state.hasCover -> "Choose, paste, or drop a cover image."
    !state.hasMessage -> "Enter a message to hide."
    !state.messageFitsCapacity -> "Shorten the message or select a larger image."
    state.passwordRequiredForHide && !state.passwordReady -> "Enter a password."
    !state.hasOutput -> "Choose where to save the stego BMP."
    else -> "Ready to create a BMP containing the hidden message."
}
